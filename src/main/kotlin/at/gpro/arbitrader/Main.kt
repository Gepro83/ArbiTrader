package at.gpro.arbitrader

import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange
import info.bitrich.xchangestream.core.StreamingExchange
import info.bitrich.xchangestream.core.StreamingExchangeFactory
import mu.KotlinLogging
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata.OrderBook


private val LOG = KotlinLogging.logger {}

fun main() {
    LOG.info { "Arbitrader starting!" }

    val exchange: StreamingExchange =
        StreamingExchangeFactory.INSTANCE.createExchange(BitstampStreamingExchange::class.java.name)

    // Connect to the Exchange WebSocket API. Here we use a blocking wait.
    exchange.connect().blockingAwait()


    // Subscribe order book data with the reference to the subscription.
    val subscription2 = exchange.streamingMarketDataService
        .getOrderBook(CurrencyPair.BTC_USD)
        .subscribe { orderBook: OrderBook? ->
            LOG.info { "asks: " + orderBook?.asks?.map { it.limitPrice } }
        }

    // Wait for a while to see some results arrive
    Thread.sleep(2000)

    // Unsubscribe
    subscription2.dispose()

    // Disconnect from exchange (blocking again)
    exchange.disconnect().blockingAwait()
}