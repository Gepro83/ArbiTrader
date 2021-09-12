package at.gpro.arbitrader

import at.gpro.arbitrader.control.TradeController
import at.gpro.arbitrader.evaluate.SpreadThresholdEvaluator
import at.gpro.arbitrader.execute.MarketPlacer
import at.gpro.arbitrader.find.ArbiTradeFinderFacade
import at.gpro.arbitrader.kraken.Kraken
import at.gpro.arbitrader.security.model.ApiKeyStore
import at.gpro.arbitrader.xchange.WebSocketExchangeBuilder
import at.gpro.arbitrader.xchange.WebSocketProvider
import at.gpro.arbitrader.xchange.utils.CurrencyConverter
import at.gpro.arbitrader.xchange.utils.XchangeCurrency
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.binance.BinanceStreamingExchange
import info.bitrich.xchangestream.coinbasepro.CoinbaseProStreamingExchange
import info.bitrich.xchangestream.kraken.KrakenStreamingExchange
import io.reactivex.functions.Consumer
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


private val LOG = KotlinLogging.logger {}

private val mainScope = CoroutineScope(Dispatchers.IO + Job())

fun main(args: Array<String>) {

    val apiKeyPath = args.getOrNull(0) ?: "/Users/gprohaska/Documents/crypto/ApiKeys.json"
    val API_KEY_STORE = ApiKeyStore.from(File(apiKeyPath))
    val COINBASEPRO_KEY = API_KEY_STORE?.getKey("CoinbasePro") ?: throw Exception("Could not find CoinbasePro key")
    val BINANCE_KEY = API_KEY_STORE?.getKey("Binance") ?: throw Exception("Could not find Binance key")
    val KRAKEN_KEY = API_KEY_STORE?.getKey("Kraken") ?: throw Exception("Could not find Kraken key")
    val CEXIO_KEY = API_KEY_STORE?.getKey("CexIO") ?: throw Exception("Could not find CexIO key")

    LOG.info { "Arbitrader starting!" }

    val currenctPairs = listOf(XchangePair.BTC_EUR)

    val binance = WebSocketExchangeBuilder.buildAndConnectFrom(
        BinanceStreamingExchange::class.java,
        BINANCE_KEY,
        0.001,
        currenctPairs
    )!!

    val coinbase = WebSocketExchangeBuilder.buildAndConnectFrom(
        CoinbaseProStreamingExchange::class.java,
        COINBASEPRO_KEY,
        0.005,
        currenctPairs
    )!!

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
        },
        getWallet = {
            val actualWallet = accountService.accountInfo.wallets.entries.first().value
            Wallet.Builder.from(
                actualWallet.balances.values.plus(
                    Balance(XchangeCurrency.EUR, actualWallet.balances[krakenEUR]?.available ?: BigDecimal.ZERO)
                ).plus(
                    Balance(XchangeCurrency.BTC, actualWallet.balances[krakenBTC]?.available ?: BigDecimal.ZERO)
                )
            )
                .build()
        }
    )!!

    selfMadeKraken.nonceFactory = kraken.nonceFactory

    val krBTC = kraken.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "kraken BTC: $krBTC" }
    val krEUR = kraken.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "kraken EUR: $krEUR" }

    val binBTC = binance.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "binance BTC: $binBTC" }

    val binEUR = binance.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "binance EUR: $binEUR" }

    val cbBTC = coinbase.getBalance(at.gpro.arbitrader.entity.Currency.BTC)
    LOG.debug { "coinbase BTC: $cbBTC" }

    val cbEUR = coinbase.getBalance(at.gpro.arbitrader.entity.Currency.EUR)
    LOG.debug { "coinbase EUR: $cbEUR" }

    LOG.debug { "total BTC: ${krBTC + binBTC + cbBTC}" }
    LOG.debug { "total EUR: ${krEUR + binEUR + cbEUR}" }

    val updateProvider = WebSocketProvider(
        listOf(kraken, coinbase, binance),
        currenctPairs.map { CurrencyConverter().convert(it) }
    )

//    kraken.place(at.gpro.arbitrader.entity.Order(OrderType.ASK, BigDecimal("0.04"),
//        at.gpro.arbitrader.entity.CurrencyPair.BTC_EUR
//    ))
//
//    coinbase.place(at.gpro.arbitrader.entity.Order(OrderType.ASK, BigDecimal("0.04"),
//        at.gpro.arbitrader.entity.CurrencyPair.BTC_EUR
//    ))

//
    TradeController(
        updateProvider,
        ArbiTradeFinderFacade(),
        SpreadThresholdEvaluator(0.001),
        MarketPlacer(0.05, 0.1),
//        CsvLogger(File("log.log"), 500),
        currenctPairs.map { CurrencyConverter().convert(it) }
    ).run()

    LOG.info { "Started!"}

}