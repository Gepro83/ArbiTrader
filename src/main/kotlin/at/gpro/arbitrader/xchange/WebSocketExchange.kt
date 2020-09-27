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

class WebSocketExchange(
    private val xchange: StreamingExchange,
    val supportedPairs: List<CurrencyPair>
) : StreamingExchange, Exchange {

    private val exchange = ExchangeConverter().convert(xchange)

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
    override fun getName(): String = exchange.getName()
    override fun toString(): String = getName()
}

class WebSocketExchangeBuilder {
    companion object {
        fun <T> buildAndConnectFrom(
            exchangeClass : Class<T>,
            key: ApiKey,
            currenctPairs : List<XchangePair>
        ) : WebSocketExchange? {
            val productSubscription =
                buildProductSubscription(
                    currenctPairs
                )
            val specification  =
                buildSpecification(
                    exchangeClass,
                    key
                )

            val xchange = StreamingExchangeFactory.INSTANCE.createExchange(specification)
                .apply { connect(productSubscription).blockingAwait() }

            return WebSocketExchange(
                xchange,
                CurrencyPairConverter().convertToCurrencyPair(currenctPairs)
            )
        }

        private fun buildSpecification(exchangeClass: Class<*>, key: ApiKey) =
            StreamingExchangeFactory.INSTANCE
                .createExchange(exchangeClass.name)
                .defaultExchangeSpecification
                .apply(key)

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