package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.EMPTY_TEST_EXCHANGE
import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.ExchangePrice
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.Test

internal class SpreadCalculatorTest {

    @Test
    internal fun `spread 0 for trades with same price`() {
        val spread = ArbiTrade(
            amount = 1,
            ExchangePrice(5, EMPTY_TEST_EXCHANGE),
            ExchangePrice(5, EMPTY_TEST_EXCHANGE)
        )

        assertThat(SpreadCalculator.calculateSpread(spread), `is`(0.0))
    }

    @Test
    internal fun `20% spread for 20% price difference at amount 1 `() {
        val spread = ArbiTrade(
            amount = 1,
            buyPrice = ExchangePrice(10, EMPTY_TEST_EXCHANGE),
            sellPrice = ExchangePrice(12, EMPTY_TEST_EXCHANGE)
        )

        assertThat(SpreadCalculator.calculateSpread(spread), `is`(0.2))
    }

    @Test
    internal fun `spread for number with necessary scale`() {
        val spread = ArbiTrade(
            amount = 1,
            buyPrice = ExchangePrice(9, EMPTY_TEST_EXCHANGE),
            sellPrice = ExchangePrice(13, EMPTY_TEST_EXCHANGE)
        )

        assertThat(SpreadCalculator.calculateSpread(spread), `is`(0.4444444444444444))
    }
}