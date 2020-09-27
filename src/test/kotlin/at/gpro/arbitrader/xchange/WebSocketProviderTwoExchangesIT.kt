package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.COINBASEPRO_KEY
import at.gpro.arbitrader.KRAKEN_KEY
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import mu.KotlinLogging
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.empty
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
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
    fun `getOrderbooks ETH_EUR must not be empty after 1000ms`() {
        sleep(1000)
        assertThat(TEST_PROVIDER.getOrderBooks(CurrencyPair.ETH_EUR), not(empty()))
    }

    @Test
    fun `getOrderbooks BTC_EUR must not be empty after 1000ms`() {
        sleep(1000)
        assertThat(TEST_PROVIDER.getOrderBooks(CurrencyPair.BTC_EUR), not(empty()))
    }

}