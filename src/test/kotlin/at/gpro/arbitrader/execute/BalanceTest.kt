package at.gpro.arbitrader.execute

import at.gpro.arbitrader.FOUR
import at.gpro.arbitrader.SEVEN
import at.gpro.arbitrader.THREE
import at.gpro.arbitrader.TWO
import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.TEN
import java.math.RoundingMode

class BalanceTest {

    private class MockExchange(private val myName: String) : Exchange {
        val placedOrders: MutableList<Order> = ArrayList()

        private val balanceMap = HashMap<Currency, Int>()

        fun setBalance(balance: Int, currency: Currency) {
            balanceMap[currency] = balance
        }

        override fun getName(): String = myName
        override fun getFee(): Double = 0.0
        override fun place(order: Order) { placedOrders.add(order) }
        override fun getBalance(currency: Currency): BigDecimal = BigDecimal(balanceMap[currency] ?: 0)
    }

    private val mockKraken = MockExchange("Kraken")
    private val mockCoinbase = MockExchange("Coinbase")

    private class TestScoredTrade(
        override val amount: BigDecimal,
        override val buyPrice: BigDecimal,
        override val sellPrice: BigDecimal,
        override val score: BigDecimal
    ) : ScoredArbiTrade

    @Test
    fun `reduce amount by applying safe margin price increase`() {

        mockCoinbase.setBalance(1, Currency.BTC)
        mockKraken.setBalance(50, Currency.EUR)

        val marketPlacer = MarketPlacer(safePriceMargin = 0.05)

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = ONE,
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(59),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(
            BigDecimal(50).setScale(Currency.BTC.scale).divide(
                BigDecimal(59).times(BigDecimal(1.05)),
                RoundingMode.HALF_DOWN
            )
        )))
    }

    @Test
    fun `take average price for multiple trades`() {

        mockCoinbase.setBalance(10, Currency.BTC)
        mockKraken.setBalance(100, Currency.EUR)

        val marketPlacer = MarketPlacer(safePriceMargin = 0.05)

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(
                TestScoredTrade(
                    score = ONE,
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                ),
                TestScoredTrade(
                    score = TWO,
                    amount = BigDecimal(2),
                    buyPrice = BigDecimal(55),
                    sellPrice = BigDecimal(60)
                ),
                TestScoredTrade(
                        score = THREE,
                amount = BigDecimal(4),
                buyPrice = BigDecimal(58),
                sellPrice = BigDecimal(60)
            )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        val avergagePrice = (BigDecimal(50) * ONE + BigDecimal(55) * TWO + BigDecimal(58) * FOUR)
            .setScale(Currency.EUR.scale, RoundingMode.HALF_UP)
            .divide(SEVEN, RoundingMode.HALF_UP)
        println("test average price $avergagePrice")

        assertThat(amount, `is`(equalTo(
            BigDecimal(100).setScale(Currency.BTC.scale).divide(
                avergagePrice.times(BigDecimal(1.05)),
                RoundingMode.HALF_DOWN
            )
        )))
    }

    @Test
    fun `apply safePrice reduction when sellcurrency is limited`() {
        mockCoinbase.setBalance(4, Currency.BTC)
        mockKraken.setBalance(200, Currency.EUR)

        val marketPlacer = MarketPlacer(safePriceMargin = 0.05)

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = ONE,
                    amount = BigDecimal(5),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(
            BigDecimal(200).setScale(Currency.BTC.scale).divide(
                BigDecimal(50).times(BigDecimal(1.05)),
                RoundingMode.HALF_DOWN
            )
        )))
    }

    @Test
    fun `use up all sellcurrency if that is limiting`() {
        mockCoinbase.setBalance(1, Currency.BTC)
        mockKraken.setBalance(300, Currency.EUR)

        val marketPlacer = MarketPlacer(safePriceMargin = 0.05)

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = ONE,
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(ONE.setScale(Currency.BTC.scale))))
    }

    @Test
    fun `multiple trades not enough sellbalance`() {
        mockCoinbase.setBalance(5, Currency.BTC)
        mockKraken.setBalance(30000, Currency.EUR)

        MarketPlacer(safePriceMargin = 0.05).placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(
                TestScoredTrade(
                    score = ONE,
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                ),
                TestScoredTrade(
                    score = TEN,
                    amount = BigDecimal(4),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(65)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(BigDecimal(5).setScale(Currency.BTC.scale))))
    }


    @Test
    fun `do not keep balance accross placeTrades() calls`() {
        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(5, Currency.BTC)
        mockKraken.setBalance(150, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val marketPlacer = MarketPlacer(safePriceMargin = 0.05)

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(
                TestScoredTrade(
                    score = ONE,
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(100),
                    sellPrice = BigDecimal(110)
                )
            )
        )
        // balance does not change in mock, so this trade should still be placed
        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(
                TestScoredTrade(
                    score = ONE,
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(100),
                    sellPrice = BigDecimal(110)
                )
            )
        )

        val amount = mockCoinbase.placedOrders[1].amount

        assertThat(amount, `is`(equalTo(ONE.setScale(Currency.BTC.scale))))
    }

}