package at.gpro.arbitrader

import at.gpro.arbitrader.kraken.Kraken
import at.gpro.arbitrader.security.model.ApiKeyStore
import at.gpro.arbitrader.xchange.WebSocketExchangeBuilder
import at.gpro.arbitrader.xchange.utils.CurrencyConverter
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.binance.BinanceStreamingExchange
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import io.reactivex.functions.Consumer
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.knowm.xchange.currency.Currency
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.trade.MarketOrder
import java.io.File

private val API_KEY_STORE = ApiKeyStore.from(File("/Users/gprohaska/Documents/crypto/ApiKeys.json"))
private val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
private val BINANCE_KEY = API_KEY_STORE?.getKey("Binance") ?: throw Exception("Could not find Binance key")
private val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")
private val CEXIO_KEY = API_KEY_STORE?.getKey("CexIO") ?: throw Exception("Could not find CexIO key")

private val LOG = KotlinLogging.logger {}

private val mainScope = CoroutineScope(Dispatchers.IO + Job())

fun main() {
    LOG.info { "Arbitrader starting!" }

    val currenctPairs = listOf(XchangePair.BTC_EUR)

//    val binance = WebSocketExchangeBuilder.buildAndConnectFrom(
//        BinanceStreamingExchange::class.java,
//        BINANCE_KEY,
//        0.001,
//        currenctPairs
//    )!!

//    val coinbase = WebSocketExchangeBuilder.buildAndConnectFrom(
//        CoinbaseProStreamingExchange::class.java,
//        COINBASEPRO_KEY,
//        0.005,
//        currenctPairs
//    )!!

    val selfMadeKraken = Kraken(KRAKEN_KEY)
    var subscriptions : MutableMap<CurrencyPair, Consumer<Order>> = HashMap()

    val krakenBTC = Currency("XXBT")
    val krakenEUR = Currency("ZEUR")

    val kraken = WebSocketExchangeBuilder.buildAndConnectFrom(
        KrakenStreamingExchange::class.java,
        KRAKEN_KEY,
        0.0020,
        currenctPairs,
        subscribeOrders = { pair, consumer ->
            subscriptions[pair] = consumer
        },
        placeOrder = { order ->
            val orderId = selfMadeKraken.place(order)
            mainScope.launch {
                delay(20)
                val filledOrder = MarketOrder.Builder(order.type, order.instrument)
                    .id(orderId)
                    .orderStatus(Order.OrderStatus.FILLED)
                    .build()
                subscriptions[order.instrument]?.accept(filledOrder)
            }
            orderId
        }
    )!!

    selfMadeKraken.nonceFactory = kraken.nonceFactory

    LOG.debug { "kraken BTC:" + kraken.getBalance(at.gpro.arbitrader.entity.Currency.BTC) }
    LOG.debug { "kraken EUR:" + kraken.getBalance(at.gpro.arbitrader.entity.Currency.EUR) }

//    LOG.debug { binance.getBalance(at.gpro.arbitrader.entity.Currency.BTC) }
//    LOG.debug { binance.getBalance(at.gpro.arbitrader.entity.Currency.EUR) }

//    LOG.debug { "coinbase BTC:" + coinbase.getBalance(at.gpro.arbitrader.entity.Currency.BTC) }
//    LOG.debug { "coinbase EUR:" + coinbase.getBalance(at.gpro.arbitrader.entity.Currency.EUR) }

//    coinbase.place(at.gpro.arbitrader.entity.Order(
//        OrderType.BID,
//        BigDecimal("0.01"),
//        at.gpro.arbitrader.entity.CurrencyPair.BTC_EUR
//    ))


//    kraken.place(at.gpro.arbitrader.entity.Order(OrderType.ASK, BigDecimal("0.003"), at.gpro.arbitrader.entity.CurrencyPair.BTC_EUR))



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