package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.entity.ArbiTrade
import java.math.BigDecimal

object SpreadCalculator {

    fun calculateSpread(trade: ArbiTrade): Double =
        trade.sellPrice.price
            .divide(trade.buyPrice.price)
            .minus(BigDecimal.ONE)
            .toDouble()

}
