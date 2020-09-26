package at.gpro.arbitrader.update

import at.gpro.arbitrader.entity.order.Offer
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*

import org.junit.jupiter.api.Test
import org.knowm.xchange.ExchangeSpecification
import org.knowm.xchange.currency.CurrencyPair
import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.meta.ExchangeMetaData
import org.knowm.xchange.dto.trade.LimitOrder
import org.knowm.xchange.instrument.Instrument
import org.knowm.xchange.service.account.AccountService
import org.knowm.xchange.service.marketdata.MarketDataService
import org.knowm.xchange.service.trade.TradeService
import si.mazi.rescu.SynchronizedValueFactory
import java.math.BigDecimal
import java.util.*

internal class OrderBookConverterTest {

    companion object {
        val TEST_EXCHANGE = object : SdkExchange {
            override fun getAccountService(): AccountService {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getExchangeSpecification(): ExchangeSpecification =
                ExchangeSpecification("test exchange").apply { exchangeName = "test exchange" }

            override fun getExchangeMetaData(): ExchangeMetaData {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun remoteInit() {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getExchangeSymbols(): MutableList<CurrencyPair> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getDefaultExchangeSpecification(): ExchangeSpecification {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getTradeService(): TradeService {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getMarketDataService(): MarketDataService {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun getNonceFactory(): SynchronizedValueFactory<Long> {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

            override fun applySpecification(exchangeSpecification: ExchangeSpecification?) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        }
    }

    @Test
    fun `empty orderbook`() {
        val convertedBook = OrderBookConverter().convert(SdkOrderBook(Date(), emptyList(), emptyList()), TEST_EXCHANGE)
        assertThat(convertedBook.buyOffers, empty())
        assertThat(convertedBook.sellOffers, empty())
        assertThat(convertedBook.exchange.getName(), `is`(TEST_EXCHANGE.exchangeSpecification.exchangeName))
    }

    @Test
    fun `2 asks 2 bids`() {
        val convertedBook = OrderBookConverter().convert(
            SdkOrderBook(
                Date(),
                listOf(
                    makeOrder(Order.OrderType.ASK, BigDecimal(1), BigDecimal(12)),
                    makeOrder(Order.OrderType.ASK, BigDecimal(2), BigDecimal(13))
                ),
                listOf(
                    makeOrder(Order.OrderType.BID, BigDecimal(3), BigDecimal(4)),
                    makeOrder(Order.OrderType.BID, BigDecimal(1), BigDecimal(4))
                )
            ), TEST_EXCHANGE
        )

        assertThat(convertedBook.sellOffers, contains(
            Offer(amount = BigDecimal(1), price = BigDecimal(12)),
            Offer(amount = BigDecimal(2), price = BigDecimal(13))
            )
        )
        assertThat(convertedBook.buyOffers, contains(
            Offer(amount = BigDecimal(3), price = BigDecimal(4)),
            Offer(amount = BigDecimal(1), price = BigDecimal(4))
            )
        )
    }

    private fun makeOrder(
        orderType: Order.OrderType,
        amount: BigDecimal?,
        price: BigDecimal?
    ): LimitOrder {
        return LimitOrder.Builder(orderType, object : Instrument() {})
            .limitPrice(price)
            .originalAmount(amount)
            .build()
    }
}