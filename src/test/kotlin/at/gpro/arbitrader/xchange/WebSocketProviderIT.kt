package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.COINBASEPRO_KEY
import at.gpro.arbitrader.KRAKEN_KEY
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import mu.KotlinLogging
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

private val LOG = KotlinLogging.logger {}

internal class WebSocketProviderIT {

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
    fun `kraken and coinbase getOrderbooks every 50ms for 1 seconds`() {
        val startMillis = System.currentTimeMillis()
        while(System.currentTimeMillis() - startMillis < 1000) {
            LOG.info { "got orderbooks: ${TEST_PROVIDER.getOrderBooks(CurrencyPair.BTC_EUR)}" }
            Thread.sleep(50)
        }
    }
}