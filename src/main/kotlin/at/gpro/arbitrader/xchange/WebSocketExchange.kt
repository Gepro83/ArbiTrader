package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.*
import at.gpro.arbitrader.security.model.ApiKey
import at.gpro.arbitrader.xchange.utils.*
import info.bitrich.xchangestream.core.ProductSubscription
import info.bitrich.xchangestream.core.StreamingExchange
import info.bitrich.xchangestream.core.StreamingExchangeFactory
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.reactivex.Completable
import io.reactivex.functions.Consumer
import mu.KotlinLogging
import org.knowm.xchange.ExchangeSpecification
import org.knowm.xchange.dto.account.Wallet
import org.knowm.xchange.dto.meta.ExchangeMetaData
import org.knowm.xchange.dto.trade.MarketOrder
import org.knowm.xchange.service.account.AccountService
import org.knowm.xchange.service.marketdata.MarketDataService
import org.knowm.xchange.service.trade.TradeService
import org.knowm.xchange.utils.OrderValuesHelper
import si.mazi.rescu.SynchronizedValueFactory
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val LOG = KotlinLogging.logger {}

typealias XchangeOrderType = org.knowm.xchange.dto.Order.OrderType

class WebSocketExchange(
    private val xchange: StreamingExchange,
    fee: Double,
    val supportedPairs: List<CurrencyPair>,
    private val subscribeOrders: (XchangePair, Consumer<XchangeOrder>) -> Unit = { pair, consumer ->
        xchange.streamingTradeService.getOrderChanges(pair).subscribe(consumer)
    },
    private val place: (MarketOrder) -> String = { xchange.tradeService.placeMarketOrder(it) },
    private val getWallet: StreamingExchange.() -> Wallet =  {
        if(accountService.accountInfo.wallets.size == 1)
            accountService.accountInfo.wallet
        else
            accountService.accountInfo.wallets.entries.first().value
    }
) : StreamingExchange, Exchange {

    companion object {
        const val ORDER_FILL_TIMEOUT_SECONDS = 2L
    }

    private val exchange = ExchangeConverter().convert(xchange, fee)
    private val pairConverter = CurrencyConverter()
    private val orderValuesHelpers: Map<XchangePair, OrderValuesHelper> = supportedPairs
        .map { pairConverter.convert(it) }
        .mapNotNull { pair ->
            val currencyPairMetaData = xchange?.exchangeMetaData?.currencyPairs?.get(pair)
            if (currencyPairMetaData != null)
                pair to OrderValuesHelper(currencyPairMetaData)
            else
                null
        }
        .toMap()

    private var wallet = Wallet.Builder.from(emptyList()).build()

    private fun updateWallet() {
        LOG.debug { "${getName()} - updating wallet" }

        wallet = getWallet()

        wallet.balances.filter { it.value.available > BigDecimal.ZERO }
            .map { it.key to it.value.available }
            .let { LOG.debug {
                "${getName()} - Refreshed balance: $it"
            } }
    }

    init {
        updateWallet()
        supportedPairs.forEach { pair ->
            subscribeOrders(pairConverter.convert(pair)) {
                onOrderChange(it, pair)
            }
        }
    }

    private val orderLatchesMap : MutableMap<String, CountDownLatch> = ConcurrentHashMap()

    private fun onOrderChange(order: XchangeOrder?, pair: CurrencyPair) {
        LOG.debug { "${getName()} order change: $order, $pair" }

        if (order?.status == org.knowm.xchange.dto.Order.OrderStatus.FILLED) {
            LOG.debug { "FILLED !" }
            orderLatchesMap[order?.id]?.countDown()
            orderLatchesMap.remove(order?.id)
        }

    }

    // StreamingExchange

    override fun isAlive(): Boolean = xchange.isAlive
    override fun connect(vararg args: ProductSubscription): Completable = xchange.connect(*args)
    override fun getExchangeMetaData(): ExchangeMetaData  = xchange.exchangeMetaData
    override fun getStreamingMarketDataService(): StreamingMarketDataService = xchange.streamingMarketDataService
    override fun getAccountService(): AccountService = xchange.accountService
    override fun disconnect(): Completable = xchange.disconnect()
    override fun getExchangeSpecification(): ExchangeSpecification = xchange.exchangeSpecification
    override fun remoteInit() = xchange.remoteInit()
    override fun getExchangeSymbols(): MutableList<XchangePair> = xchange.exchangeSymbols
    override fun getDefaultExchangeSpecification(): ExchangeSpecification = xchange.defaultExchangeSpecification
    override fun getTradeService(): TradeService = xchange.tradeService
    override fun getMarketDataService(): MarketDataService = xchange.marketDataService
    override fun getNonceFactory(): SynchronizedValueFactory<Long> = xchange.nonceFactory
    override fun applySpecification(specification: ExchangeSpecification) = xchange.applySpecification(specification)
    override fun useCompressedMessages(compressedMessages: Boolean) = xchange.useCompressedMessages(compressedMessages)
    override fun toString(): String = getName()

    // Exchange

    override fun getName(): String = exchange.getName()
    override fun getFee(): Double = exchange.getFee()
    @Synchronized
    override fun place(order: Order) {
        val xchangeOrderType =
            when (order.type) {
                OrderType.BID -> XchangeOrderType.BID
                OrderType.ASK -> XchangeOrderType.ASK
            }

        place(pairConverter.convert(order.pair), xchangeOrderType, order.amount)
    }

    override fun getBalance(currency: Currency): BigDecimal = wallet.getBalance(currency.toXchangeCurrency()).available



    fun place(
        pair: XchangePair,
        orderType: XchangeOrderType,
        amount: BigDecimal
    ) {
        if (orderValuesHelpers[pair]?.amountUnderMinimum(amount) == true) {
            LOG.debug { "${getName()} - not placing - amount too low - ${getName()} - $pair - $amount" }
            return
        }

        val placeAmount = amount.setScale(pairConverter.convert(pair).mainCurrency.scale, RoundingMode.DOWN)

        if (placeAmount == BigDecimal.ZERO)
            return

//        orderValuesHelpers[pair]?.adjustAmount(amount) ?: amount.also {
//            LOG.debug { "${getName()} - no order values helper for ${getName()}" }
//        }

        val xchangeOrder = MarketOrder.Builder(orderType, pair)
            .originalAmount(placeAmount)
            .build()

        LOG.debug { "${getName()} - Placing $orderType order at ${getName()} - $pair - $placeAmount" }

        try {
            val latch = CountDownLatch(1)

            val orderId = place(xchangeOrder)
            LOG.debug { "${getName()} - done - orderId: $orderId - waiting for order to fill" }
            orderLatchesMap[orderId] = latch

            if(!latch.await(ORDER_FILL_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                LOG.warn { "${getName()} - Order not filled within timeout!" }
            else
                LOG.debug { "${getName()} - order filled !" }

        } catch (e: Exception) {
            LOG.error(e) { "${getName()} - exception during order placement: $e" }
        }

        updateWallet()

    }
}


class WebSocketExchangeBuilder {
    companion object {
        fun <T> buildAndConnectFrom(
            exchangeClass : Class<T>,
            key: ApiKey,
            fee: Double,
            currenctPairs : List<XchangePair>,
            subscribeOrders: ((XchangePair, Consumer<XchangeOrder>) -> Unit)? = null,
            placeOrder: ((MarketOrder) -> String)? = null,
            getWallet: (StreamingExchange.() -> Wallet)? = null

        ) : WebSocketExchange? = _buildAndConnectFrom(exchangeClass, currenctPairs, key, fee, subscribeOrders, placeOrder, getWallet)

        fun <T> buildAndConnectFrom(
            exchangeClass : Class<T>,
            currenctPairs : List<XchangePair>
        ) : WebSocketExchange? = _buildAndConnectFrom(exchangeClass, currenctPairs)

        private fun <T> _buildAndConnectFrom(
            exchangeClass : Class<T>,
            currenctPairs : List<XchangePair>,
            key: ApiKey? = null,
            fee: Double = 0.0,
            subscribeOrders: ((XchangePair, Consumer<XchangeOrder>) -> Unit)? = null,
            placeOrder: ((MarketOrder) -> String)? = null,
            getWallet: (StreamingExchange.() -> Wallet)? = null
        ) : WebSocketExchange? {
            val productSubscription = buildProductSubscription(currenctPairs)

            val specification  = key?.let { buildSpecification(exchangeClass, it) }
                ?: buildSpecification(exchangeClass)

            val xchange = StreamingExchangeFactory.INSTANCE.createExchange(specification)
                .apply { connect(productSubscription).blockingAwait() }

            return if (subscribeOrders != null)
                    WebSocketExchange(xchange, fee, CurrencyConverter().convertToCurrencyPair(currenctPairs), subscribeOrders, placeOrder!!, getWallet!!)
            else
                WebSocketExchange(xchange, fee, CurrencyConverter().convertToCurrencyPair(currenctPairs))
        }


        private fun buildSpecification(exchangeClass: Class<*>) =
            StreamingExchangeFactory.INSTANCE
                .createExchangeWithoutSpecification(exchangeClass.name)
                .defaultExchangeSpecification

        private fun buildSpecification(exchangeClass: Class<*>, key: ApiKey) =
            buildSpecification(exchangeClass).apply(key)

        private fun ExchangeSpecification.apply(key: ApiKey) : ExchangeSpecification {
            apiKey = key.apiKey
            secretKey = key.secret
            key.userName?.let { userName = it }
            setExchangeSpecificParametersItem(
                "Binance_Orderbook_Use_Higher_Frequency",
                true
            )
            key.specificParameter?.let {
                setExchangeSpecificParametersItem(
                    it.key,
                    it.value
                )

            }
            return this
        }

        private fun buildProductSubscription(currenctPairs: List<XchangePair>) =
            ProductSubscription
                .create()
                .apply {
                    currenctPairs.forEach {
                        addAll(it)
                    }
                }
                .build()
    }
}