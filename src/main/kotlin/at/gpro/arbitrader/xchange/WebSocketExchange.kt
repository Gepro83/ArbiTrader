package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.*
import at.gpro.arbitrader.security.model.ApiKey
import at.gpro.arbitrader.xchange.utils.*
import info.bitrich.xchangestream.core.ProductSubscription
import info.bitrich.xchangestream.core.StreamingExchange
import info.bitrich.xchangestream.core.StreamingExchangeFactory
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.reactivex.Completable
import io.reactivex.Observable
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
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private val LOG = KotlinLogging.logger {}

typealias XchangeOrderType = org.knowm.xchange.dto.Order.OrderType

class WebSocketExchange(
    private val xchange: StreamingExchange,
    fee: Double,
    val supportedPairs: List<CurrencyPair>,
    private val getOrderChanges: StreamingExchange.(XchangePair) -> Observable<XchangeOrder> = {
        streamingTradeService.getOrderChanges(it)
    },
    private val place: (MarketOrder) -> String = { xchange.tradeService.placeMarketOrder(it) }
) : StreamingExchange, Exchange {

    companion object {
        const val ORDER_FILL_TIMEOUT_SECONDS = 3L
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
        LOG.entry()

        wallet = if(xchange.accountService.accountInfo.wallets.size == 1)
            xchange.accountService.accountInfo.wallet
        else
            xchange.accountService.accountInfo.wallets.entries.first().value

        wallet.balances.filter { it.value.available > BigDecimal.ZERO }
            .map { it.key to it.value.available }
            .let { LOG.debug { "Refreshed balance: $it" } }
    }

    init {
        updateWallet()
        supportedPairs.forEach { pair ->
            xchange.getOrderChanges(pairConverter.convert(pair))
                .subscribe { onOrderChange(it, pair) }
        }
    }

    private val orderLatchesMap : MutableMap<String, CountDownLatch> = ConcurrentHashMap()

    private fun onOrderChange(order: XchangeOrder?, pair: CurrencyPair) {
        LOG.entry(order, pair)

        if (order?.status == org.knowm.xchange.dto.Order.OrderStatus.FILLED) {
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
            LOG.debug { "not placing - amount too low - ${getName()} - $pair - $amount" }
            return
        }

        val placeAmount = orderValuesHelpers[pair]?.adjustAmount(amount) ?: amount.also {
            LOG.debug { "no order values helper for ${getName()}" }
        }

        val xchangeOrder = MarketOrder.Builder(orderType, pair)
            .originalAmount(placeAmount)
            .build()

        LOG.debug { "Placing $orderType order at ${getName()} - $pair - $placeAmount" }

        try {
            val orderId = place(xchangeOrder)
            LOG.debug { "done - orderId: $orderId - waiting for order to fill" }

            val latch = CountDownLatch(1)
            orderLatchesMap[orderId] = latch
            if(!latch.await(ORDER_FILL_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOG.warn { "Order not filled within timeout - cancelling" }
                xchange.tradeService.cancelOrder(orderId).also { result ->
                    LOG.debug { "order canceled: $result" }
                }
            }

        } catch (e: Exception) {
            LOG.error(e) { "exception during order placement: $e" }
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
            getOrderChanges: (StreamingExchange.(XchangePair) -> Observable<XchangeOrder>)? = null
        ) : WebSocketExchange? = _buildAndConnectFrom(exchangeClass, currenctPairs, key, fee, getOrderChanges)

        fun <T> buildAndConnectFrom(
            exchangeClass : Class<T>,
            currenctPairs : List<XchangePair>
        ) : WebSocketExchange? = _buildAndConnectFrom(exchangeClass, currenctPairs)

        private fun <T> _buildAndConnectFrom(
            exchangeClass : Class<T>,
            currenctPairs : List<XchangePair>,
            key: ApiKey? = null,
            fee: Double = 0.0,
            getOrderChanges: (StreamingExchange.(XchangePair) -> Observable<XchangeOrder>)? = null
        ) : WebSocketExchange? {
            val productSubscription = buildProductSubscription(currenctPairs)

            val specification  = key?.let { buildSpecification(exchangeClass, it) }
                ?: buildSpecification(exchangeClass)

            val xchange = StreamingExchangeFactory.INSTANCE.createExchange(specification)
                .apply { connect(productSubscription).blockingAwait() }

            return if (getOrderChanges != null)
                WebSocketExchange(xchange, fee, CurrencyConverter().convertToCurrencyPair(currenctPairs), getOrderChanges)
            else
                WebSocketExchange(xchange, fee, CurrencyConverter().convertToCurrencyPair(currenctPairs))
        }


        private fun buildSpecification(exchangeClass: Class<*>) =
            StreamingExchangeFactory.INSTANCE
                .createExchange(exchangeClass.name)
                .defaultExchangeSpecification

        private fun buildSpecification(exchangeClass: Class<*>, key: ApiKey) =
            buildSpecification(exchangeClass).apply(key)

        private fun ExchangeSpecification.apply(key: ApiKey) : ExchangeSpecification {
            apiKey = key.apiKey
            secretKey = key.secret
            key.userName?.let { userName = it }
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
                    currenctPairs.forEach { addOrderbook(it) }
                }
                .build()
    }
}