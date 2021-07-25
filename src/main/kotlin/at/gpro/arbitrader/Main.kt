package at.gpro.arbitrader

import at.gpro.arbitrader.kraken.Kraken
import at.gpro.arbitrader.security.model.ApiKeyStore
import at.gpro.arbitrader.xchange.WebSocketExchange
import at.gpro.arbitrader.xchange.WebSocketExchangeBuilder
import at.gpro.arbitrader.xchange.utils.CurrencyConverter
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.binance.BinanceStreamingExchange
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import mu.KotlinLogging
import org.knowm.xchange.currency.Currency
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.account.Wallet
import org.knowm.xchange.dto.trade.MarketOrder
import java.io.File
import java.math.BigDecimal

private val API_KEY_STORE = ApiKeyStore.from(File("/Users/gprohaska/Documents/crypto/ApiKeys.json"))
private val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
private val BINANCE_KEY = API_KEY_STORE?.getKey("Binance") ?: throw Exception("Could not find Binance key")
private val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")
private val CEXIO_KEY = API_KEY_STORE?.getKey("CexIO") ?: throw Exception("Could not find CexIO key")

private val LOG = KotlinLogging.logger {}

fun main() {
    LOG.info { "Arbitrader starting!" }

    val currenctPairs = listOf(
        XchangePair.BTC_EUR,
        XchangePair.ETH_EUR,
        XchangePair.ETH_BTC
    )

//    val binance = WebSocketExchangeBuilder.buildAndConnectFrom(
//        BinanceStreamingExchange::class.java,
//        BINANCE_KEY,
//        0.0025,
//        currenctPairs
//    )!!
//
//    val coinbase = WebSocketExchangeBuilder.buildAndConnectFrom(
//        CoinbaseProStreamingExchange::class.java,
//        COINBASEPRO_KEY,
//        0.0025,
//        currenctPairs
//    )!!

    val kraken = WebSocketExchangeBuilder.buildAndConnectFrom(
        KrakenStreamingExchange::class.java,
        KRAKEN_KEY,
        0.0025,
        currenctPairs
    )!!

    LOG.info { "CONNECTED" }
    LOG.debug { kraken.getBalance(at.gpro.arbitrader.entity.Currency.BTC) }

    Kraken(KRAKEN_KEY).place(
        MarketOrder.Builder(Order.OrderType.ASK, CurrencyPair.BTC_EUR)
        .originalAmount(BigDecimal("0.03"))
        .build())

//    cex.place(CurrencyPair.BTC_EUR, XchangeOrderType.ASK, BigDecimal("0.05"))

//    printbalance(cex, listOf(CurrencyPair.BTC_EUR))

//    kraken.place(Order(OrderType.ASK, BigDecimal("0.01"), at.gpro.arbitrader.entity.CurrencyPair.BTC_EUR))
//    kraken.place(Order(OrderType.BID, BigDecimal("0.01"), at.gpro.arbitrader.entity.CurrencyPair.ETH_EUR))

//    val order = LimitOrder.Builder(org.knowm.xchange.dto.Order.OrderType.BID, CurrencyPair.ETH_EUR)
//        .limitPrice(BigDecimal("2200"))
//        .originalAmount(BigDecimal("0.01"))
//        .build()

//    kraken.tradeService.placeMarketOrder(MarketOrder.Builder(org.knowm.xchange.dto.Order.OrderType.BID, CurrencyPair.ETH_EUR)
//        .originalAmount(BigDecimal("0.001"))
//        .build())

//    kraken.tradeService.placeLimitOrder(order)

//    printbalance(binance, currenctPairs)
//    printbalance(coinbase, currenctPairs)
//    printbalance(kraken, currenctPairs)


//    checkForTrades(currenctPairs)

}

private fun printbalance(exchange: WebSocketExchange, currenctPairs: List<CurrencyPair>) {
    LOG.info { "${exchange.getName()} Balance:" }

    try {
        val wallet = exchange.accountService.accountInfo.wallet
        printbalance(currenctPairs, wallet)
    } catch(e: UnsupportedOperationException) {
        exchange.accountService.accountInfo.wallets.forEach { (name, wallet) ->
            LOG.info { "Wallet $name" }
            wallet.balances.forEach { (currency, balance) ->
                Currency.XBT
                LOG.info { "$currency - ${balance.available}" }
            }
        }
    }
}

private fun printbalance(
    currenctPairs: List<CurrencyPair>,
    wallet: Wallet
) {
    currenctPairs.forEach {
        val baseAmount = wallet.getBalance(it.base).available
        LOG.info { "${it.base} - $baseAmount" }
        val counterAmount = wallet.getBalance(it.counter).available
        LOG.info { "${it.counter} - $counterAmount" }
    }
}

private fun checkForTrades(currenctPairs: List<CurrencyPair>) {
    val coinbase = WebSocketExchangeBuilder.buildAndConnectFrom(
        CoinbaseProStreamingExchange::class.java,
        COINBASEPRO_KEY,
        0.0025,
        currenctPairs
    )!!

    val kraken = WebSocketExchangeBuilder.buildAndConnectFrom(
        KrakenStreamingExchange::class.java,
        KRAKEN_KEY,
        0.002,
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


    val myPairs = CurrencyConverter().convertToCurrencyPair(currenctPairs)

//    TradeController(
//        WebSocketProvider(listOf(coinbase, kraken, bitstamp, binance), myPairs),
//        ArbiTradeFinderFacade(),
//        SpreadThresholdSelector(0.0040),
//        CsvLogger(File("LOG.csv"), 0),
//        myPairs
//    ).runUntil { false }
}