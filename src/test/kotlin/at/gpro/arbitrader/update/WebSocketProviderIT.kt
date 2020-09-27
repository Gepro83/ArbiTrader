package at.gpro.arbitrader.update

import at.gpro.arbitrader.COINBASEPRO_KEY
import at.gpro.arbitrader.KRAKEN_KEY
import at.gpro.arbitrader.utils.xchange.WebSocketExchangeBuilder
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import mu.KotlinLogging
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.knowm.xchange.currency.CurrencyPair

private val LOG = KotlinLogging.logger {}

internal class WebSocketProviderIT {

    private val COINBASE =
        WebSocketExchangeBuilder.buildAndConnectFrom(
            CoinbaseProStreamingExchange::class.java,
            COINBASEPRO_KEY,
            listOf(CurrencyPair.BTC_USD)
        ) ?: fail("Could not build Coinbase exchange")

    private val KRAKEN =
        WebSocketExchangeBuilder.buildAndConnectFrom(
            KrakenStreamingExchange::class.java,
            KRAKEN_KEY,
            listOf(CurrencyPair.BTC_EUR)
        ) ?: fail("Could not build Kraken exchange")

    @Test
    fun `kraken and coinbase getOrderbooks every 50ms for 5 seconds`() {
        val provider = WebSocketProvider(listOf(
                COINBASE,
                KRAKEN
            )
        )

        val startMillis = System.currentTimeMillis()
        while(System.currentTimeMillis() - startMillis < 5000) {
            LOG.info { "got orderbooks: ${provider.getOrderBooks()}" }
            Thread.sleep(50)
        }
    }
}