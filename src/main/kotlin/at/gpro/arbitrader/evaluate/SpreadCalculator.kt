package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.entity.ArbiTrade
import java.math.BigDecimal
import java.math.RoundingMode

object SpreadCalculator {

    fun calculateSpread(trade: ArbiTrade): Double =
        trade.sellPrice.price
            .divide(trade.buyPrice.price, 20, RoundingMode.FLOOR)
            .minus(BigDecimal.ONE)
            .toDouble()

}
