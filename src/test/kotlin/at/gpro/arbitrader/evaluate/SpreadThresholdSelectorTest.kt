package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.TestUtils.newTestExchange
import at.gpro.arbitrader.entity.ExchangeArbiTrade
import at.gpro.arbitrader.entity.ExchangePrice
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SpreadThresholdSelectorTest {

    @Test
    fun `isWorthy true for worthy trade`() {
        assertTrue(SpreadThresholdEvaluator(0.0).isWorthy(ExchangeArbiTrade(
            BigDecimal.ONE,
            buyExchangePrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
            sellExchangePrice = ExchangePrice(111, newTestExchange("testexchange1", 0.06)),
        )))
    }

    @Test
    internal fun `isWorthy false for too expensive trade`() {
        assertFalse(SpreadThresholdEvaluator(0.0).isWorthy(ExchangeArbiTrade(
                BigDecimal.ONE,
                buyExchangePrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
                sellExchangePrice = ExchangePrice(109, newTestExchange("testexchange1", 0.06)),
        )))
    }

}