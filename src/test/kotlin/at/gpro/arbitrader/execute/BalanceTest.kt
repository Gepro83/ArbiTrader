package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class BalanceTest {

    private class MockExchange(val balance: Double) : Exchange {
        val placedOrders: MutableList<Order> = ArrayList()

        override fun getName(): String = "BalanceTest"
        override fun getFee(): Double = 0.0
        override fun place(order: Order) { placedOrders.add(order) }
        override fun getBalance(currency: Currency): BigDecimal = BigDecimal(balance)
    }

    @Test
    fun `reduce amount when balance too low at buy exchange`() {
        val buyExchange = MockExchange(90.0)
        val sellExchange = MockExchange(100.0)
        MarketPlacer().placeTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                ArbiTrade(
                    amount = BigDecimal(1),
                    buyPrice = ExchangePrice(100, buyExchange),
                    sellPrice = ExchangePrice(111, sellExchange),
                )
            )
        )

        assertThat(buyExchange.placedOrders.first().amount, `is`(equalTo(BigDecimal.valueOf(0.9))))
        assertThat(sellExchange.placedOrders.first().amount, `is`(equalTo(BigDecimal.valueOf(0.9))))
    }

    // check 3 exchanges with conflicting trades

}