package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.control.TradeSelector
import at.gpro.arbitrader.entity.ArbiTrade

class SpreadThresholdSelector(private val threshold: Double): TradeSelector {

    override fun selectTrades(trades: List<ArbiTrade>): List<ArbiTrade> =
        trades.filter { SpreadCalculator.calculateSpread(it) > threshold }
}