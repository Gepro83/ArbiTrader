package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.security.model.ApiKey
import at.gpro.arbitrader.xchange.utils.CurrencyPairConverter
import at.gpro.arbitrader.xchange.utils.ExchangeConverter
import at.gpro.arbitrader.xchange.utils.XchangePair
import info.bitrich.xchangestream.core.ProductSubscription
import info.bitrich.xchangestream.core.StreamingExchange
import info.bitrich.xchangestream.core.StreamingExchangeFactory
import info.bitrich.xchangestream.core.StreamingMarketDataService
import io.reactivex.Completable
import org.knowm.xchange.ExchangeSpecification
import org.knowm.xchange.dto.meta.ExchangeMetaData
import org.knowm.xchange.service.account.AccountService
import org.knowm.xchange.service.marketdata.MarketDataService
import org.knowm.xchange.service.trade.TradeService
import si.mazi.rescu.SynchronizedValueFactory
import java.math.BigDecimal

class WebSocketExchange(
    private val xchange: StreamingExchange,
    fee: BigDecimal,
    val supportedPairs: List<CurrencyPair>
) : StreamingExchange, Exchange {

    private val exchange = ExchangeConverter().convert(xchange, fee)

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
    override fun getFee(): BigDecimal = exchange.getFee()
}


class WebSocketExchangeBuilder {
    companion object {
        fun <T> buildAndConnectFrom(
            exchangeClass : Class<T>,
            key: ApiKey,
            fee: BigDecimal,
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
            fee: BigDecimal = BigDecimal.ZERO,
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