package at.gpro.arbitrader

import at.gpro.arbitrader.control.TradeController
import at.gpro.arbitrader.evaluate.SpreadThresholdSelector
import at.gpro.arbitrader.execute.CsvLogger
import at.gpro.arbitrader.find.ArbiTradeFinderFacade
import at.gpro.arbitrader.security.model.ApiKeyStore
import at.gpro.arbitrader.xchange.WebSocketExchangeBuilder
import at.gpro.arbitrader.xchange.WebSocketProvider
import at.gpro.arbitrader.xchange.utils.CurrencyPairConverter
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.binance.BinanceStreamingExchange
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange
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

    val currenctPairs = listOf(
        XchangePair.BTC_EUR,
        XchangePair.ETH_EUR,
        XchangePair.ETH_BTC,
        XchangePair.XRP_EUR,
        XchangePair.XRP_BTC,
//        XchangePair.XRP_ETH
    )

    val coinbase = WebSocketExchangeBuilder.buildAndConnectFrom(
        CoinbaseProStreamingExchange::class.java,
        COINBASEPRO_KEY,
        currenctPairs
    )!!

    val kraken = WebSocketExchangeBuilder.buildAndConnectFrom(
        KrakenStreamingExchange::class.java,
        KRAKEN_KEY,
        currenctPairs
    )!!

    val bitstamp = WebSocketExchangeBuilder.buildAndConnectFrom(
        BitstampStreamingExchange::class.java,
        currenctPairs
    )!!

    val binance = WebSocketExchangeBuilder.buildAndConnectFrom(
        BinanceStreamingExchange::class.java,
        currenctPairs
    )!!


    val myPairs = CurrencyPairConverter().convertToCurrencyPair(currenctPairs)

    TradeController(
        WebSocketProvider(listOf(coinbase, kraken, bitstamp, binance), myPairs),
        ArbiTradeFinderFacade(),
        SpreadThresholdSelector(0.0035),
        CsvLogger(File("LOG.csv"), 500),
        myPairs
    ).runUntil { false }

}