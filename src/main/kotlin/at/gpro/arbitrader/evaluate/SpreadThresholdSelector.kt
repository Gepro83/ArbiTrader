package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.control.TradeSelector
import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class SpreadThresholdSelector(private val threshold: Double): TradeSelector {

    override fun selectTrades(pair: CurrencyPair, trades: List<ArbiTrade>): List<ArbiTrade> =
        trades.filter { calcSpreadAndLog(it) > threshold }

    private fun calcSpreadAndLog(trade: ArbiTrade): Double =
        SpreadCalculator.calculateSpread(trade) - trade.buyPrice.exchange.getFee() - trade.sellPrice.exchange.getFee()

}

