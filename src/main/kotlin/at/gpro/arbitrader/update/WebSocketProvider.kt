package at.gpro.arbitrader.update

import at.gpro.arbitrader.control.UpdateProvider
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.order.OrderBook
import at.gpro.arbitrader.utils.xchange.WebSocketExchange
import info.bitrich.xchangestream.core.StreamingExchange
import io.reactivex.disposables.Disposable
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val LOG = KotlinLogging.logger {}

typealias XchangeOrderBook = org.knowm.xchange.dto.marketdata.OrderBook?
typealias XchangeExchange = org.knowm.xchange.Exchange
typealias XchangePair = org.knowm.xchange.currency.CurrencyPair

class WebSocketProvider(private val exchanges : List<WebSocketExchange>) : UpdateProvider {

    private val subscriptions : List<Disposable>
    private val exchangeToOrderbook : MutableMap<StreamingExchange, OrderBook> = ConcurrentHashMap()

    private val orderBookConverter = OrderBookConverter()

    init {
        subscriptions = exchanges.map { subscribeOrderBooks(it) }
    }

    private fun subscribeOrderBooks(exchange: StreamingExchange): Disposable =
        exchange.streamingMarketDataService
            .getOrderBook(XchangePair.BTC_EUR)
            .subscribe { orderBook -> onOrderBookUpdate(orderBook, exchange) }

    private fun onOrderBookUpdate(orderBook: XchangeOrderBook,
                                  exchange : StreamingExchange) {
        if (orderBook == null) {
            LOG.warn { "Received null orderbook from ${exchange.defaultExchangeSpecification.exchangeName}" }
            return
        }

        exchangeToOrderbook[exchange] = orderBookConverter.convert(orderBook, exchange)
    }

    override fun getOrderBooks(currencyPair: CurrencyPair): List<OrderBook> = exchangeToOrderbook.values.toList()

    fun stop() {
        subscriptions.forEach(Disposable::dispose)
        exchanges.forEach { it.disconnect().blockingAwait() }
    }
}