package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.Order
import at.gpro.arbitrader.entity.OrderType
import at.gpro.arbitrader.security.model.ApiKey
import at.gpro.arbitrader.xchange.utils.CurrencyPairConverter
import at.gpro.arbitrader.xchange.utils.ExchangeConverter
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.core.ProductSubscription
import info.bitrich.xchangestream.core.StreamingExchange
import info.bitrich.xchangestream.core.StreamingExchangeFactory
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.reactivex.Completable
import mu.KotlinLogging
import org.knowm.xchange.ExchangeSpecification
import org.knowm.xchange.dto.meta.ExchangeMetaData
import org.knowm.xchange.dto.trade.MarketOrder
import org.knowm.xchange.service.account.AccountService
import org.knowm.xchange.service.marketdata.MarketDataService
import org.knowm.xchange.service.trade.TradeService
import org.knowm.xchange.utils.OrderValuesHelper
import si.mazi.rescu.SynchronizedValueFactory
import java.math.BigDecimal

private val LOG = KotlinLogging.logger {}

typealias XchangeOrderType = org.knowm.xchange.dto.Order.OrderType

class WebSocketExchange(
    private val xchange: StreamingExchange,
    fee: Double,
    val supportedPairs: List<CurrencyPair>
) : StreamingExchange, Exchange {

    private val exchange = ExchangeConverter().convert(xchange, fee)
    private val pairConverter = CurrencyPairConverter()
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

    private fun place(
        pair: XchangePair,
        orderType: XchangeOrderType,
        amount: BigDecimal
    ) {
        if (orderValuesHelpers[pair]?.amountUnderMinimum(amount) == true) {
            LOG.debug { "not placing - amount too low - ${getName()} - $pair - $amount" }
            return
        }

        val placeAmount = orderValuesHelpers[pair]?.adjustAmount(amount) ?: amount

        val xchangeOrder = MarketOrder.Builder(orderType, pair)
            .originalAmount(placeAmount)
            .build()

        LOG.debug { "Placing $orderType order at ${getName()} - $pair - $placeAmount" }

        try {
            val orderId = xchange.tradeService.placeMarketOrder(xchangeOrder)
            LOG.debug { "done - orderId: $orderId" }
        } catch (e: Exception) {
            LOG.debug { "exception during order placement: $e" }
        }
    }
}


class WebSocketExchangeBuilder {
    companion object {
        fun <T> buildAndConnectFrom(
            exchangeClass : Class<T>,
            key: ApiKey,
            fee: Double,
            currenctPairs : List<XchangePair>
        ) : WebSocketExchange? = _buildAndConnectFrom(exchangeClass, currenctPairs, key, fee)

        fun <T> buildAndConnectFrom(
            exchangeClass : Class<T>,
            currenctPairs : List<XchangePair>
        ) : WebSocketExchange? = _buildAndConnectFrom(exchangeClass, currenctPairs)

        private fun <T> _buildAndConnectFrom(
            exchangeClass : Class<T>,
            currenctPairs : List<XchangePair>,
            key: ApiKey? = null,
            fee: Double = 0.0,
        ) : WebSocketExchange? {
            val productSubscription = buildProductSubscription(currenctPairs)

            val specification  = key?.let { buildSpecification(exchangeClass, it) }
                ?: buildSpecification(exchangeClass)

            val xchange = StreamingExchangeFactory.INSTANCE.createExchange(specification)
                .apply { connect(productSubscription).blockingAwait() }

            return WebSocketExchange(xchange, fee, CurrencyPairConverter().convertToCurrencyPair(currenctPairs))
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