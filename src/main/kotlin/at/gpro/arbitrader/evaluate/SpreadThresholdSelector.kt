package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.control.TradeSelector
import at.gpro.arbitrader.entity.ArbiTrade
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class SpreadThresholdSelector(private val threshold: Double): TradeSelector {

    override fun selectTrades(trades: List<ArbiTrade>): List<ArbiTrade> =
        trades.filter { calcSpreadAndLog(it) > threshold }

    private fun calcSpreadAndLog(trade: ArbiTrade): Double =
        SpreadCalculator.calculateSpread(trade)
//            .also { LOG.info { "Spread: ${it * 100}% - $trade" } }
}

