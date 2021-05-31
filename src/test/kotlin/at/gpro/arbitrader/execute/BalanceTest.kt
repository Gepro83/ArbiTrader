package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.Currency
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.Order
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

    @Test
    fun `reduce amount by applying safe margin price increase`() {

        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(1, Currency.BTC)
        mockKraken.setBalance(50, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val balanceKeeper = BalanceKeeper(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        val amount = balanceKeeper.getSafeAmount(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trade = SimpleArbiTrade(
                    amount = BigDecimal(1),
                    buyPrice = BigDecimal(59),
                    sellPrice = BigDecimal(60)
                )
        )

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

        val balanceKeeper = BalanceKeeper(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        val amount = balanceKeeper.getSafeAmount(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trade = SimpleArbiTrade(
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
        )

        assertThat(amount, `is`(equalTo(BigDecimal(3))))
    }

    @Test
    fun `apply safePrice reduction when balancemargin is not reached by buyexchange`() {

        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(3, Currency.BTC)
        val buyBalance = ceil(150 * 1.05).toInt()
        mockKraken.setBalance(buyBalance, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val balanceKeeper = BalanceKeeper(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        val amount = balanceKeeper.getSafeAmount(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trade = SimpleArbiTrade(
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
        )

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

        val balanceKeeper = BalanceKeeper(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        val amount = balanceKeeper.getSafeAmount(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trade = SimpleArbiTrade(
                    amount = BigDecimal(5),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
        )

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

        val balanceKeeper = BalanceKeeper(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        val amount = balanceKeeper.getSafeAmount(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trade = SimpleArbiTrade(
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
                )
        )

        assertThat(amount, `is`(equalTo(BigDecimal.ONE)))
    }

    @Test
    fun `consider previous reducebalance calls`() {
        mockCoinbase.setBalance(100, Currency.EUR)
        mockCoinbase.setBalance(5, Currency.BTC)
        mockKraken.setBalance(300, Currency.EUR)
        mockKraken.setBalance(2, Currency.BTC)

        val balanceKeeper = BalanceKeeper(
            safePriceMargin = 0.05,
            balanceMargin = 0.1
        )

        balanceKeeper.reduceBalance(mockCoinbase, BigDecimal(2), Currency.BTC)
        balanceKeeper.reduceBalance(mockCoinbase, BigDecimal(1), Currency.BTC)

        // must not influence calculations
        balanceKeeper.reduceBalance(mockKraken, BigDecimal(1), Currency.BTC)
        balanceKeeper.reduceBalance(mockCoinbase, BigDecimal(50), Currency.EUR)

        val amount = balanceKeeper.getSafeAmount(
            buyExchange = mockKraken,
            sellExchange = mockCoinbase,
            pair = CurrencyPair.BTC_EUR,
            trade = SimpleArbiTrade(
                    amount = BigDecimal(3),
                    buyPrice = BigDecimal(50),
                    sellPrice = BigDecimal(60)
            )
        )

        assertThat(amount, `is`(equalTo(BigDecimal(2))))
    }

}