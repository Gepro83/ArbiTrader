package at.gpro.arbitrader.utils.xchange

import at.gpro.arbitrader.COINBASEPRO_KEY
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.marketdata.OrderBook
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class WebSocketExchangeBuilderIT {
    @Test
    internal fun `fromAndConnect Coinbase updates multiple currencies`() {
        val exchange = WebSocketExchangeBuilder.buildAndConnectFrom(
            CoinbaseProStreamingExchange::class.java,
            COINBASEPRO_KEY,
            listOf(CurrencyPair.BTC_EUR, CurrencyPair.BTC_USD)
        ) ?: fail("Coinbase should be buildable")

        var USDupdateReceived = CountDownLatch(1)
        var EURupdateReceived = CountDownLatch(1)

        val subscriptionUSD = exchange.streamingMarketDataService
            .getOrderBook(CurrencyPair.BTC_USD)
            .subscribe { orderBook: OrderBook? ->
                if (orderBook != null)
                    USDupdateReceived.countDown()
            }

        val subscriptionEUR = exchange.streamingMarketDataService
            .getOrderBook(CurrencyPair.BTC_EUR)
            .subscribe { orderBook: OrderBook? ->
                if (orderBook != null)
                    EURupdateReceived.countDown()
            }

        assertTrue(USDupdateReceived.await(3, TimeUnit.SECONDS))
        assertTrue(EURupdateReceived.await(3, TimeUnit.SECONDS))
        assertThat(exchange.getName(), `is`("CoinbasePro"))

        subscriptionUSD.dispose()
        subscriptionEUR.dispose()

        exchange.disconnect().blockingAwait()
    }
}