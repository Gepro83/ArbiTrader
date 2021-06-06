package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.ceil

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

        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(1, Currency.BTC)
        mockKraken.setBalance(50, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val marketPlacer = MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = BigDecimal.ONE,
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(59),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(
            BigDecimal(50).setScale(Currency.BTC.scale).divide(
                BigDecimal(59).plus(BigDecimal.valueOf(0.05).times(BigDecimal(59))),
                RoundingMode.HALF_DOWN
            )
        )))
    }

    @Test
    fun `do not reduce amount if balancemargin is not reached by any exchange`() {

        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(5, Currency.BTC)
        mockKraken.setBalance(ceil(150 * 1.101).toInt(), Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val marketPlacer = MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = BigDecimal.ONE,
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(BigDecimal(3))))
    }

    @Test
    fun `apply safePrice reduction when balancemargin is not reached by buyexchange`() {

        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(3, Currency.BTC)
        val buyBalance = ceil(150 * 1.05).toInt()
        mockKraken.setBalance(buyBalance, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val marketPlacer = MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = BigDecimal.ONE,
                    amount = BigDecimal(4),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(
            BigDecimal(buyBalance).setScale(Currency.BTC.scale).divide(
                BigDecimal(50).plus(BigDecimal.valueOf(0.05).times(BigDecimal(50))),
                RoundingMode.HALF_DOWN
            )
        )))
    }

    @Test
    fun `apply safePrice reduction when balancemargin is not reached by buyexchange and sellcurrency is limited`() {
        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(4, Currency.BTC)
        val buyBalance = ceil(150 * 1.05).toInt()
        mockKraken.setBalance(buyBalance, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val marketPlacer = MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = BigDecimal.ONE,
                    amount = BigDecimal(5),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(
            BigDecimal(buyBalance).setScale(Currency.BTC.scale).divide(
                BigDecimal(50).plus(BigDecimal.valueOf(0.05).times(BigDecimal(50))),
                RoundingMode.HALF_DOWN
            )
        )))
    }

    @Test
    fun `use up all sellcurrency if that is limiting`() {
        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(1, Currency.BTC)
        mockKraken.setBalance(300, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val marketPlacer = MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(TestScoredTrade(
                    score = BigDecimal.ONE,
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(BigDecimal.ONE)))
    }

    @Test
    fun `multiple trades not enough sellbalance`() {
        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(5, Currency.BTC)
        mockKraken.setBalance(30000, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        ).placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(
                TestScoredTrade(
                    score = BigDecimal.ONE,
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                ),
                TestScoredTrade(
                    score = BigDecimal.TEN,
                    amount = BigDecimal(4),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(65)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(BigDecimal(5))))
    }


    @Test
    fun `calculate safe amount for each trade in order of highest score`() {
        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(5, Currency.BTC)
        mockKraken.setBalance(150, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        ).placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(
                TestScoredTrade(
                    score = BigDecimal.TEN,
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(65)
                ),
                TestScoredTrade(
                    score = BigDecimal.ONE,
                    amount = BigDecimal(2),
                    buyPrice = BigDecimal(55),
                    sellPrice = BigDecimal(65)
                )
            )
        )

        val amount = mockCoinbase.placedOrders.first().amount

        assertThat(amount, `is`(equalTo(
            BigDecimal.ONE.plus( // first trade can be done without restriction
            BigDecimal(100).setScale(Currency.BTC.scale).divide(
                BigDecimal(55).plus(BigDecimal.valueOf(0.05).times(BigDecimal(55))),
                RoundingMode.HALF_DOWN
            )
        ))))

    }


    @Test
    fun `do not keep balance accross placeTrades() calls`() {
        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(5, Currency.BTC)
        mockKraken.setBalance(150, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val marketPlacer = MarketPlacer(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )
        marketPlacer.placeTrades(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trades = listOf(
                TestScoredTrade(
                    score = BigDecimal.ONE,
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
                    score = BigDecimal.ONE,
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(100),
                    sellPrice = BigDecimal(110)
                )
            )
        )

        val amount = mockCoinbase.placedOrders[1].amount

        assertThat(amount, `is`(equalTo(BigDecimal.ONE)))

    }

}