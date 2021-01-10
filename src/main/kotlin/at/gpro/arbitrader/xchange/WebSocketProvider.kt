package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.control.UpdateProvider
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.OrderBook
import at.gpro.arbitrader.util.OrderBookStore
import at.gpro.arbitrader.xchange.utils.CurrencyPairConverter
import at.gpro.arbitrader.xchange.utils.OrderBookConverter
import at.gpro.arbitrader.xchange.utils.XchangeOrderBook
import io.reactivex.disposables.Disposable
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class WebSocketProvider(private val exchanges : List<WebSocketExchange>) : UpdateProvider {

    private val subscriptions : List<Disposable>
    private val orderBookStore = OrderBookStore()

    private val orderBookConverter = OrderBookConverter()
    private val pairConverter = CurrencyPairConverter()

    init {
        subscriptions = exchanges
            .map { subscribeOrderBooks(it) }
            .flatten()
    }

    private fun subscribeOrderBooks(exchange: WebSocketExchange) : List<Disposable> =
        exchange.supportedPairs
            .map { subscribeOrderBookFor(exchange, it) }
            .toList()

    private fun subscribeOrderBookFor(
        exchange: WebSocketExchange,
        pair: CurrencyPair
    ) = exchange.streamingMarketDataService
            .getOrderBook(pairConverter.convert(pair))
            .subscribe { orderBook -> onOrderBookUpdate(orderBook, pair, exchange) }

    private fun onOrderBookUpdate(orderBook: XchangeOrderBook?,
                                  currencyPair: CurrencyPair,
                                  exchange : Exchange
    ) {
        if (orderBook == null) {
            LOG.warn { "Received null orderbook from ${exchange.getName()}" }
            return
        }

        orderBookStore.update(orderBookConverter.convert(orderBook, exchange), currencyPair)
    }

    override fun getOrderBooks(currencyPair: CurrencyPair): List<OrderBook> = orderBookStore.getBooksFor(currencyPair)

    fun stop() {
        subscriptions.forEach(Disposable::dispose)
        exchanges.forEach { it.disconnect().blockingAwait() }
    }
}