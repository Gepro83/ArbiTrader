package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.TestUtils.newTestExchange
import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.ExchangePrice
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class SpreadThresholdSelectorTest {

    @Test
    fun `drop trades with too high fees`() {
        val tooExpensiveTrade =
            ArbiTrade(
                BigDecimal.ONE,
                buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
                sellPrice = ExchangePrice(109, newTestExchange("testexchange1", 0.06)),
            )

        val worthyTrade =
            ArbiTrade(
                BigDecimal.ONE,
                buyPrice = ExchangePrice(100, newTestExchange("testexchange1", 0.04)),
                sellPrice = ExchangePrice(111, newTestExchange("testexchange1", 0.06)),
            )

        val selectedTrades = SpreadThresholdSelector(0.0).selectTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                tooExpensiveTrade,
                worthyTrade
            )
        )

        assertThat(selectedTrades, contains(worthyTrade))
    }


    @Test
    fun `reduce amount when balance too low at buy exchange`() {

        val trade = SpreadThresholdSelector(threshold = 0.0)
            .selectTrades(
                CurrencyPair.BTC_EUR,
                listOf(
                        ArbiTrade(
                            amount = BigDecimal(1),
                            buyPrice = ExchangePrice(
                                100,
                                newTestExchange(
                                    "testexchange1",
                                    0.0,
                                    90
                                )),
                            sellPrice = ExchangePrice(
                                111,
                                newTestExchange(
                                    "testexchange2",
                                    0.0,
                                    100
                                )),
                        )
                )
            )
            .first()

        assertThat(trade.amount, `is`(equalTo(BigDecimal.valueOf(0.9))))
    }
}