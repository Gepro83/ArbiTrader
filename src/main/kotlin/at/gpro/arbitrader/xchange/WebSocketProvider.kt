package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.control.UpdateProvider
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.OrderBook
import at.gpro.arbitrader.util.OrderBookStore
import at.gpro.arbitrader.xchange.utils.CurrencyConverter
import at.gpro.arbitrader.xchange.utils.OrderBookConverter
import at.gpro.arbitrader.xchange.utils.XchangeOrderBook
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import mu.KotlinLogging
import java.time.Duration
import java.time.Instant

private val LOG = KotlinLogging.logger {}

class WebSocketProvider(
    private val exchanges: List<WebSocketExchange>,
    private val pairs: List<CurrencyPair>,
    private val orderBooks: StreamingMarketDataService.(XchangePair) -> Observable<XchangeOrderBook> = {
        getOrderBook(it)
    }
) : UpdateProvider {

    private val subscriptions: List<Disposable>
    private val orderBookStore = OrderBookStore()

    private val orderBookConverter = OrderBookConverter()
    private val pairConverter = CurrencyConverter()

    private var onUpdate: () -> Unit = {}

    init {
        subscriptions = exchanges
            .map { subscribeOrderBooks(it) }
            .flatten()
    }

    private fun subscribeOrderBooks(exchange: WebSocketExchange): List<Disposable> =
        pairsToSubscribe(exchange)
            .map { subscribeOrderBookFor(exchange, it) }

    private fun pairsToSubscribe(exchange: WebSocketExchange): List<CurrencyPair> =
        pairs.ifEmpty { exchange.supportedPairs }

    private fun subscribeOrderBookFor(
        exchange: WebSocketExchange,
        pair: CurrencyPair
    ) = exchange.streamingMarketDataService
        .orderBooks(pairConverter.convert(pair))
        .subscribe(
            { orderBook -> onOrderBookUpdate(orderBook, pair, exchange) },
            { cause -> LOG.error("Error in orderbook subsrcription of ${exchange.getName()}", cause) }
        ).also { LOG.debug { "subscribed $pair at $exchange" } }

    private fun onOrderBookUpdate(
        orderBook: XchangeOrderBook?,
        currencyPair: CurrencyPair,
        exchange: Exchange
    ) {
        if (orderBook == null) {
            LOG.warn { "Received null orderbook from ${exchange.getName()}" }
            return
        }

        if (calcAgeMillis(orderBook) > 1000) {
//            LOG.warn { "orderbook too old from ${exchange.getName()}" }
            return
        }

//        LOG.debug { "${exchange.getName()} - age: ${calcAgeMillis(orderBook)}" }

        orderBookStore.update(orderBookConverter.convert(orderBook, exchange), currencyPair)

        if (calcAgeMillis(orderBook) > 1000) {
//            LOG.warn { "book from ${exchange.getName()} too old after bookstore update"}
            return
        }
        onUpdate()
    }

    private fun calcAgeMillis(orderBook: XchangeOrderBook): Long =
        try {
            Duration.between(
                orderBook?.timeStamp?.toInstant() ?: Instant.now().minusSeconds(10),
                Instant.now()
            ).toMillis()
        } catch (e: Exception) {
            Long.MAX_VALUE
        }

    override fun getOrderBooks(currencyPair: CurrencyPair): List<OrderBook> =
        if (currencyPair in pairs)
            orderBookStore.getBooksFor(currencyPair)
        else
            throw IllegalArgumentException("$currencyPair not supported by this object")

    override fun onUpdate(action: () -> Unit) {
        onUpdate = action
    }

    fun stop() {
        subscriptions.forEach(Disposable::dispose)
        exchanges.forEach { it.disconnect().blockingAwait() }
    }
}