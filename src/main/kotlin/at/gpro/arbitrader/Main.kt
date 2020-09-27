package at.gpro.arbitrader

import at.gpro.arbitrader.security.model.ApiKeyStore
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.core.ProductSubscription
import info.bitrich.xchangestream.core.StreamingExchangeFactory
import mu.KotlinLogging
import org.knowm.xchange.currency.CurrencyPair
import java.io.File

private val API_KEY_STORE = ApiKeyStore.from(File("/Users/gprohaska/Documents/crypto/ApiKeys.json"))
private val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
private val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")

private val LOG = KotlinLogging.logger {}

fun main() {
    LOG.info { "Arbitrader starting!" }


    val productSubscription = ProductSubscription.create()
        .addTicker(CurrencyPair.ETH_USD)
        .addOrders(CurrencyPair.LTC_EUR)
        .addOrderbook(CurrencyPair.BTC_USD)
        .addTrades(CurrencyPair.BTC_USD)
        .addUserTrades(CurrencyPair.LTC_EUR)
        .build()

    val spec = StreamingExchangeFactory.INSTANCE
        .createExchange(CoinbaseProStreamingExchange::class.java.name)
        .defaultExchangeSpecification
    spec.apiKey = COINBASEPRO_KEY.apiKey
    spec.secretKey = COINBASEPRO_KEY.secret
    spec.setExchangeSpecificParametersItem("passphrase", COINBASEPRO_KEY.specificParameter?.value)
    val exchange =
        StreamingExchangeFactory.INSTANCE.createExchange(spec) as CoinbaseProStreamingExchange

    exchange.connect(productSubscription).blockingAwait()
}