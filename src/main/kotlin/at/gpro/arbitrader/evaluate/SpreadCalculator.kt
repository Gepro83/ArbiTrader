package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.execute.ArbiTrade
import java.math.BigDecimal
import java.math.RoundingMode

object SpreadCalculator {

    fun calculateSpread(trade: ArbiTrade): Double =
        trade.sellPrice
            .divide(trade.buyPrice, 20, RoundingMode.FLOOR)
            .minus(BigDecimal.ONE)
            .toDouble()

}
