package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.control.TradeSelector
import at.gpro.arbitrader.entity.ArbiTrade
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class SpreadThresholdSelector(private val threshold: Double): TradeSelector  {

    override fun isWorthy(trade: ArbiTrade): Boolean = calcSpreadAndLog(trade) > threshold

    private fun calcSpreadAndLog(trade: ArbiTrade): Double =
        SpreadCalculator.calculateSpread(trade) - trade.buyPrice.exchange.getFee() - trade.sellPrice.exchange.getFee()
}

