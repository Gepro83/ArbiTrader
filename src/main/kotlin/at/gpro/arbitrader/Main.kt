package at.gpro.arbitrader

import at.gpro.arbitrader.control.TradeController
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.evaluate.SpreadThresholdSelector
import at.gpro.arbitrader.execute.CsvLogger
import at.gpro.arbitrader.find.ArbiTradeFinderFacade
import at.gpro.arbitrader.security.model.ApiKeyStore
import at.gpro.arbitrader.xchange.WebSocketExchangeBuilder
import at.gpro.arbitrader.xchange.WebSocketProvider
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import mu.KotlinLogging
import java.io.File

private val API_KEY_STORE = ApiKeyStore.from(File("/Users/gprohaska/Documents/crypto/ApiKeys.json"))
private val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
private val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")

private val LOG = KotlinLogging.logger {}

fun main() {
    LOG.info { "Arbitrader starting!" }

    val coinbase = WebSocketExchangeBuilder.buildAndConnectFrom(
        CoinbaseProStreamingExchange::class.java,
        COINBASEPRO_KEY,
        listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)
    )!!

    val kraken = WebSocketExchangeBuilder.buildAndConnectFrom(
        KrakenStreamingExchange::class.java,
        KRAKEN_KEY,
        listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)
    )!!

    TradeController(
        WebSocketProvider(listOf(coinbase, kraken), listOf(CurrencyPair.BTC_EUR, CurrencyPair.ETH_EUR)),
        ArbiTradeFinderFacade(),
        SpreadThresholdSelector(0.01),
        CsvLogger(File("LOG.csv"), 5000),
        listOf(CurrencyPair.BTC_EUR, CurrencyPair.ETH_EUR)
    ).runUntil { false }

//    val productSubscription = ProductSubscription.create()
//        .addTicker(CurrencyPair.ETH_USD)
//        .addOrders(CurrencyPair.LTC_EUR)
//        .addOrderbook(CurrencyPair.BTC_USD)
//        .addTrades(CurrencyPair.BTC_USD)
//        .addUserTrades(CurrencyPair.LTC_EUR)
//        .build()
//
//    val spec = StreamingExchangeFactory.INSTANCE
//        .createExchange(CoinbaseProStreamingExchange::class.java.name)
//        .defaultExchangeSpecification
//    spec.apiKey = COINBASEPRO_KEY.apiKey
//    spec.secretKey = COINBASEPRO_KEY.secret
//    spec.setExchangeSpecificParametersItem("passphrase", COINBASEPRO_KEY.specificParameter?.value)
//    val exchange =
//        StreamingExchangeFactory.INSTANCE.createExchange(spec) as CoinbaseProStreamingExchange
//
//    exchange.connect(productSubscription).blockingAwait()
}