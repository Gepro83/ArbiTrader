package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.COINBASEPRO_KEY
import at.gpro.arbitrader.KRAKEN_KEY
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.knowm.xchange.service.marketdata.MarketDataService
import java.lang.Thread.sleep

private val LOG = KotlinLogging.logger {}

internal class WebSocketProviderTwoExchangesIT {

    companion object {
        private val COINBASE =
            WebSocketExchangeBuilder.buildAndConnectFrom(
                CoinbaseProStreamingExchange::class.java,
                COINBASEPRO_KEY,
                listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)
            ) ?: fail("Could not build Coinbase exchange")

        private val KRAKEN =
            WebSocketExchangeBuilder.buildAndConnectFrom(
                KrakenStreamingExchange::class.java,
                KRAKEN_KEY,
                listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)
            ) ?: fail("Could not build Kraken exchange")

        private val TEST_PROVIDER = WebSocketProvider(listOf(COINBASE, KRAKEN))

        @AfterAll
        @JvmStatic
        internal fun tearDown() {
            TEST_PROVIDER.stop()
        }
    }

    @Test
    fun `getOrderbooks BTC_EUR every 50ms for 300ms seconds`() {
        val startMillis = System.currentTimeMillis()
        while(System.currentTimeMillis() - startMillis < 300) {
            LOG.info { "got orderbooks: ${TEST_PROVIDER.getOrderBooks(CurrencyPair.BTC_EUR)}" }
            sleep(50)
        }
    }

    @Test
    fun `getOrderbooks ETH_EUR must not be empty after 500ms`() {
        sleep(500)
        assertThat(TEST_PROVIDER.getOrderBooks(CurrencyPair.ETH_EUR), not(empty()))
    }

    @Test
    fun `instantiate calls getOrderBook on streamingMarketDataService for all pairs`() {
        val marketDataServiceMock = mockk<MarketDataService>(relaxed = true)
        val exchangeMock = mockk<WebSocketExchange>(relaxed = true) {
            every {
                supportedPairs
            } returns listOf(CurrencyPair.ETH_EUR, CurrencyPair.BTC_EUR, CurrencyPair.XRP_EUR)
            every {
                marketDataService
            } returns marketDataServiceMock

        }
        WebSocketProvider(listOf(exchangeMock))

        verify {
            marketDataServiceMock.getOrderBook(XchangePair.ETH_EUR)
            marketDataServiceMock.getOrderBook(XchangePair.BTC_EUR)
            marketDataServiceMock.getOrderBook(XchangePair.XRP_EUR)
        }
    }
}