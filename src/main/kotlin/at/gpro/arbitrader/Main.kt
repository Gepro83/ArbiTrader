package at.gpro.arbitrader

import at.gpro.arbitrader.control.TradeController
import at.gpro.arbitrader.entity.OrderType
import at.gpro.arbitrader.evaluate.SpreadThresholdEvaluator
import at.gpro.arbitrader.execute.MarketPlacer
import at.gpro.arbitrader.find.ArbiTradeFinderFacade
import at.gpro.arbitrader.kraken.Kraken
import at.gpro.arbitrader.security.model.ApiKey
import at.gpro.arbitrader.security.model.ApiKeyStore
import at.gpro.arbitrader.xchange.WebSocketExchange
import at.gpro.arbitrader.xchange.WebSocketExchangeBuilder
import at.gpro.arbitrader.xchange.WebSocketProvider
import at.gpro.arbitrader.xchange.utils.CurrencyConverter
import at.gpro.arbitrader.xchange.utils.XchangeCurrency
import at.gpro.arbitrader.xchange.utils.XchangeOrderBook
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.binance.BinanceStreamingExchange
import info.bitrich.xchangestream.bitstamp.v2.BitstampStreamingExchange
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingMarketDataService
import io.ktor.util.reflect.*
import io.reactivex.functions.Consumer
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.*
import mu.KotlinLogging
import org.knowm.xchange.currency.Currency
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.account.Balance
import org.knowm.xchange.dto.account.Wallet
import org.knowm.xchange.dto.trade.MarketOrder
import java.io.File
import java.math.BigDecimal
import java.util.concurrent.Executors


private val LOG = KotlinLogging.logger {}

private val mainScope = CoroutineScope(Dispatchers.IO + Job())

//fun main() {
//    val filteredWritter = File("filtered-19.9.log").writer()
//    File("19.9.log").useLines { lines->
//
//        lines.filter { it.contains("was tried to be reduced below 0!").not() }
//            .forEach { line ->
//                filteredWritter.write(line + System.lineSeparator())
//            }
//    }
//
//    filteredWritter.close()
//}

fun main2() {
    val apiKeyPath = "/Users/gprohaska/Documents/crypto/ApiKeys.json"
    val API_KEY_STORE = ApiKeyStore.from(File(apiKeyPath))
    val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
    val BINANCE_KEY = API_KEY_STORE?.getKey("Binance") ?: throw Exception("Could not find Binance key")
    val BITSTAMP_KEY = API_KEY_STORE?.getKey("Bitstamp") ?: throw Exception("Could not find Binance key")
    val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")
    val CEXIO_KEY = API_KEY_STORE?.getKey("CexIO") ?: throw Exception("Could not find CexIO key")

    LOG.info { "Arbitrader starting!" }

    val currenctPairs = listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)

    val bitstamp = makeBitstamp(BITSTAMP_KEY, currenctPairs)

    val bsBTC = bitstamp.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "bitstamp BTC: $bsBTC" }
    val bsEUR = bitstamp.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "bitstamp EUR: $bsEUR" }
    val bsETH = bitstamp.getBalance(at.gpro.arbitrader.entity.Currency.ETH)
    LOG.debug { "bitstamp ETH: $bsETH" }


    val updateProvider = WebSocketProvider(
        listOf(bitstamp),
        currenctPairs.map { CurrencyConverter().convert(it) }
    )

    bitstamp.place(at.gpro.arbitrader.entity.Order(
        OrderType.ASK, BigDecimal("0.01"),
        at.gpro.arbitrader.entity.CurrencyPair.ETH_EUR
    ))

}

private fun makeBitstamp(
    BITSTAMP_KEY: ApiKey,
    currenctPairs: List<CurrencyPair>
): WebSocketExchange {
    var subscriptionsBitstamp: MutableMap<CurrencyPair, Consumer<Order>> = HashMap()
    var placeOrderBitstamp: ((MarketOrder) -> String)? = null

    val bitstamp = WebSocketExchangeBuilder.buildAndConnectFrom(
        BitstampStreamingExchange::class.java,
        BITSTAMP_KEY,
        0.005,
        currenctPairs,
        subscribeOrders = { pair, consumer ->
            subscriptionsBitstamp[pair] = consumer
        },
        placeOrder = { order ->
            val orderId = placeOrderBitstamp!!(order)
            mainScope.launch {
                delay(15)
                val filledOrder = MarketOrder.Builder(order.type, order.instrument)
                    .id(orderId)
                    .orderStatus(Order.OrderStatus.FILLED)
                    .build()
                subscriptionsBitstamp[order.instrument]?.accept(filledOrder)
            }
            orderId

        },
        getWallet = {
            if (accountService.accountInfo.wallets.size == 1)
                accountService.accountInfo.wallet
            else
                accountService.accountInfo.wallets.entries.first().value
        }
    )!!

    placeOrderBitstamp = { bitstamp.tradeService.placeMarketOrder(it) }
    return bitstamp
}

fun main3(args: Array<String>) {
    val apiKeyPath = args.getOrNull(0) ?: "/Users/gprohaska/Documents/crypto/ApiKeys.json"
    val API_KEY_STORE = ApiKeyStore.from(File(apiKeyPath))
    val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")

    val selfMadeKraken = Kraken(KRAKEN_KEY)
    var subscriptions : MutableMap<CurrencyPair, Consumer<Order>> = HashMap()

    val krakenBTC = Currency("XXBT")
    val krakenEUR = Currency("ZEUR")
    val krakenETH = Currency("XETH")


    val kraken = WebSocketExchangeBuilder.buildAndConnectFrom(
        KrakenStreamingExchange::class.java,
        KRAKEN_KEY,
        0.0026,
        listOf(CurrencyPair.BTC_EUR),
        subscribeOrders = { pair, consumer ->
            subscriptions[pair] = consumer
        },
        placeOrder = { order ->
            val orderId = selfMadeKraken.place(order)
            mainScope.launch {
                delay(15)
                val filledOrder = MarketOrder.Builder(order.type, order.instrument)
                    .id(orderId)
                    .orderStatus(Order.OrderStatus.FILLED)
                    .build()
                subscriptions[order.instrument]?.accept(filledOrder)
            }
            orderId
        },
        getWallet = {
            val actualWallet = accountService.accountInfo.wallets.entries.first().value
            Wallet.Builder.from(
                actualWallet.balances.values.plus(
                    Balance(XchangeCurrency.EUR, actualWallet.balances[krakenEUR]?.available ?: BigDecimal.ZERO)
                ).plus(
                    Balance(XchangeCurrency.BTC, actualWallet.balances[krakenBTC]?.available ?: BigDecimal.ZERO)
                ).plus(
                    Balance(XchangeCurrency.ETH, actualWallet.balances[krakenETH]?.available ?: BigDecimal.ZERO)
                )
            )
                .build()
        }
    )!!

    kraken.streamingMarketDataService.getOrderBook(XchangePair.BTC_EUR).subscribe {
        println("streaming:")
        println(it)
        println("mine:")
        println(selfMadeKraken.getOrderBook(XchangePair.BTC_EUR))
    }



//    val order = MarketOrder.Builder(Order.OrderType.BID, XchangePair.BTC_EUR)
//        .originalAmount(BigDecimal("0.001"))
//        .build()
//
//    kraken.place(order)
}

fun main(args: Array<String>) {

    val apiKeyPath = args.getOrNull(0) ?: "/Users/gprohaska/Documents/crypto/ApiKeys.json"
    val API_KEY_STORE = ApiKeyStore.from(File(apiKeyPath))
    val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
    val BINANCE_KEY = API_KEY_STORE?.getKey("Binance") ?: throw Exception("Could not find Binance key")
    val BITSTAMP_KEY = API_KEY_STORE?.getKey("Bitstamp") ?: throw Exception("Could not find Binance key")
    val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")
    val CEXIO_KEY = API_KEY_STORE?.getKey("CexIO") ?: throw Exception("Could not find CexIO key")

    LOG.info { "Arbitrader starting!" }

    val currenctPairs = listOf(XchangePair.BTC_EUR, XchangePair.ETH_EUR)

    val binance = WebSocketExchangeBuilder.buildAndConnectFrom(
        BinanceStreamingExchange::class.java,
        BINANCE_KEY,
        0.001,
        currenctPairs
    )!!

    val bitstamp = makeBitstamp(BITSTAMP_KEY, currenctPairs)

    val coinbase = WebSocketExchangeBuilder.buildAndConnectFrom(
        CoinbaseProStreamingExchange::class.java,
        COINBASEPRO_KEY,
        0.0025,
        currenctPairs
    )!!

    val selfMadeKraken = Kraken(KRAKEN_KEY)
    var subscriptions : MutableMap<CurrencyPair, Consumer<Order>> = HashMap()

    val krakenBTC = Currency("XXBT")
    val krakenEUR = Currency("ZEUR")
    val krakenETH = Currency("XETH")

    val kraken = WebSocketExchangeBuilder.buildAndConnectFrom(
        KrakenStreamingExchange::class.java,
        KRAKEN_KEY,
        0.0026,
        currenctPairs,
        subscribeOrders = { pair, consumer ->
            subscriptions[pair] = consumer
        },
        placeOrder = { order ->
            val orderId = selfMadeKraken.place(order)
            mainScope.launch {
                delay(15)
                val filledOrder = MarketOrder.Builder(order.type, order.instrument)
                    .id(orderId)
                    .orderStatus(Order.OrderStatus.FILLED)
                    .build()
                subscriptions[order.instrument]?.accept(filledOrder)
            }
            orderId
        },
        getWallet = {
            val actualWallet = accountService.accountInfo.wallets.entries.first().value
            Wallet.Builder.from(
                actualWallet.balances.values.plus(
                    Balance(XchangeCurrency.EUR, actualWallet.balances[krakenEUR]?.available ?: BigDecimal.ZERO)
                ).plus(
                    Balance(XchangeCurrency.BTC, actualWallet.balances[krakenBTC]?.available ?: BigDecimal.ZERO)
                ).plus(
                    Balance(XchangeCurrency.ETH, actualWallet.balances[krakenETH]?.available ?: BigDecimal.ZERO)
                )
            )
                .build()
        }
    )!!

    val krBTC = kraken.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "kraken BTC: $krBTC" }
    val krEUR = kraken.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "kraken EUR: $krEUR" }
    val krETH = kraken.getBalance(at.gpro.arbitrader.entity.Currency.ETH)
    LOG.debug { "kraken ETH: $krETH" }

    val binBTC = binance.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "binance BTC: $binBTC" }
    val binEUR = binance.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "binance EUR: $binEUR" }
    val binETH = binance.getBalance(at.gpro.arbitrader.entity.Currency.ETH)
    LOG.debug { "binance ETH: $binETH" }

    val cbBTC = coinbase.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "coinbase BTC: $cbBTC" }
    val cbEUR = coinbase.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "coinbase EUR: $cbEUR" }
    val cbETH = coinbase.getBalance(at.gpro.arbitrader.entity.Currency.ETH)
    LOG.debug { "coinbase ETH: $cbETH" }

    val bsBTC = bitstamp.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "bitstamp BTC: $bsBTC" }
    val bsEUR = bitstamp.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "bitstamp EUR: $bsEUR" }
    val bsETH = bitstamp.getBalance(at.gpro.arbitrader.entity.Currency.ETH)
    LOG.debug { "bitstamp ETH: $bsETH" }

    LOG.debug { "total BTC: ${krBTC + binBTC + cbBTC + bsBTC}" }
    LOG.debug { "total EUR: ${krEUR + binEUR + cbEUR + bsEUR}" }
    LOG.debug { "total ETH: ${krETH + binETH + cbETH + bsETH}" }

    val btcOrderBookObservable = PublishSubject.create<XchangeOrderBook>()
    val ethOrderBookObservable = PublishSubject.create<XchangeOrderBook>()

    val updateProvider = WebSocketProvider(
        listOf(coinbase, kraken, bitstamp, binance),
        currenctPairs.map { CurrencyConverter().convert(it) },
        orderBooks = { pair ->
            if (this.instanceOf(KrakenStreamingMarketDataService::class))
                when(pair) {
                    XchangePair.BTC_EUR -> btcOrderBookObservable
                    XchangePair.ETH_EUR -> ethOrderBookObservable
                    else -> throw UnsupportedOperationException("Pair $pair not supported for kraken orderbook")
                }
            else
                getOrderBook(pair)
        }
    )

    Executors.newSingleThreadExecutor().submit {
        while(true) {
            selfMadeKraken.getOrderBook(XchangePair.BTC_EUR)?.let {
                btcOrderBookObservable.onNext(it)
            }
        }
    }


    Executors.newSingleThreadExecutor().submit {
        while(true) {
            selfMadeKraken.getOrderBook(XchangePair.ETH_EUR)?.let {
                ethOrderBookObservable.onNext(it)
            }
        }
    }

//    binance.place(at.gpro.arbitrader.entity.Order(
//        OrderType.BID, BigDecimal("0.028"),
//        at.gpro.arbitrader.entity.CurrencyPair.BTC_EUR
//    ))
//    binance.place(at.gpro.arbitrader.entity.Order(
//        OrderType.BID, BigDecimal("0.35"),
//        at.gpro.arbitrader.entity.CurrencyPair.ETH_EUR
//    ))
////
//    coinbase.place(at.gpro.arbitrader.entity.Order(OrderType.ASK, BigDecimal("0.04"),
//        at.gpro.arbitrader.entity.CurrencyPair.BTC_EUR
//    ))

//
    TradeController(
        updateProvider,
        ArbiTradeFinderFacade(),
        SpreadThresholdEvaluator(0.0005),
        MarketPlacer(0.015),
//        CsvLogger(File("log.log"), 500),
        currenctPairs.map { CurrencyConverter().convert(it) }
    ).run()

    LOG.info { "Started!"}

}