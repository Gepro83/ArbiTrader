package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.TestUtils.newTestExchange
import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.ExchangePrice
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SpreadThresholdSelectorTest {

    @Test
    fun `isWorthy true for worthy trade`() {
        assertTrue(SpreadThresholdSelector(0.0).isWorthy(ArbiTrade(
            BigDecimal.ONE,
            buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
            sellPrice = ExchangePrice(111, newTestExchange("testexchange1", 0.06)),
        )))
    }

    @Test
    internal fun `isWorthy false for too expensive trade`() {
        assertFalse(SpreadThresholdSelector(0.0).isWorthy(ArbiTrade(
                BigDecimal.ONE,
                buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
                sellPrice = ExchangePrice(109, newTestExchange("testexchange1", 0.06)),
        )))
    }

}