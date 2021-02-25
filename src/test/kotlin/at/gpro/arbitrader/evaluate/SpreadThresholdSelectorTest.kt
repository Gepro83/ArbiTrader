package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.TestUtils.newTestExchange
import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.ExchangePrice
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class SpreadThresholdSelectorTest {

    @Test
    fun `drop trades with too high fees`() {
        val tooExpensiveTrade = ArbiTrade(
            BigDecimal.ONE,
            buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
            sellPrice = ExchangePrice(109, newTestExchange("testexchange1", 0.06)),
        )
        val worthyTrade = ArbiTrade(
            BigDecimal.ONE,
            buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
            sellPrice = ExchangePrice(111, newTestExchange("testexchange1", 0.06)),
        )
        val selectedTrades = SpreadThresholdSelector(0.0).selectTrades(
            listOf(
                tooExpensiveTrade,
                worthyTrade
            )
        )

        assertThat(selectedTrades, contains(worthyTrade))
    }


}