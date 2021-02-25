package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.COINBASEPRO_KEY
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import org.knowm.xchange.dto.marketdata.OrderBook
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class WebSocketExchangeBuilderIT {
    @Test
    internal fun `buildAndConnectFrom Coinbase updates multiple currencies`() {
        val exchange = WebSocketExchangeBuilder.buildAndConnectFrom(
            CoinbaseProStreamingExchange::class.java,
            COINBASEPRO_KEY,
            0.0,
            listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)
        ) ?: fail("Coinbase should be buildable")

        var BTCupdateReceived = CountDownLatch(1)
        var ETHupdateReceived = CountDownLatch(1)

        val subscriptionBTC = exchange.streamingMarketDataService
            .getOrderBook(XchangePair.BTC_EUR)
            .subscribe { orderBook: OrderBook? ->
                if (orderBook != null)
                    BTCupdateReceived.countDown()
            }

        val subscriptionETH = exchange.streamingMarketDataService
            .getOrderBook(XchangePair.ETH_EUR)
            .subscribe { orderBook: OrderBook? ->
                if (orderBook != null)
                    ETHupdateReceived.countDown()
            }

        assertTrue(BTCupdateReceived.await(3, TimeUnit.SECONDS))
        assertTrue(ETHupdateReceived.await(3, TimeUnit.SECONDS))

        assertThat(exchange.getName(), `is`("CoinbasePro"))
        assertThat(exchange.supportedPairs, contains(CurrencyPair.BTC_EUR, CurrencyPair.ETH_EUR))

        subscriptionBTC.dispose()
        subscriptionETH.dispose()

        exchange.disconnect().blockingAwait()
    }

    @Test
    internal fun `buildAndConnect Bitstamp NO API key updates multiple currencies`() {
        val exchange = WebSocketExchangeBuilder.buildAndConnectFrom(
            BitstampStreamingExchange::class.java,
            listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)
        ) ?: fail("Coinbase should be buildable")

        var BTCupdateReceived = CountDownLatch(1)
        var ETHupdateReceived = CountDownLatch(1)

        val subscriptionBTC = exchange.streamingMarketDataService
            .getOrderBook(XchangePair.BTC_EUR)
            .subscribe { orderBook: OrderBook? ->
                if (orderBook != null)
                    BTCupdateReceived.countDown()
            }

        val subscriptionETH = exchange.streamingMarketDataService
            .getOrderBook(XchangePair.ETH_EUR)
            .subscribe { orderBook: OrderBook? ->
                if (orderBook != null)
                    ETHupdateReceived.countDown()
            }

        assertTrue(BTCupdateReceived.await(3, TimeUnit.SECONDS))
        assertTrue(ETHupdateReceived.await(3, TimeUnit.SECONDS))

        assertThat(exchange.getName(), `is`("Bitstamp"))
        assertThat(exchange.supportedPairs, contains(CurrencyPair.BTC_EUR, CurrencyPair.ETH_EUR))

        subscriptionBTC.dispose()
        subscriptionETH.dispose()

        exchange.disconnect().blockingAwait()
    }
}