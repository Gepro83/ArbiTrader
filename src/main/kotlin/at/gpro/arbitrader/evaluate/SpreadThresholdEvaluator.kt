package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.control.TradeEvaluator
import at.gpro.arbitrader.entity.ExchangeArbiTrade
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class SpreadThresholdEvaluator(private val threshold: Double): TradeEvaluator  {

    override fun isWorthy(trade: ExchangeArbiTrade): Boolean = calcSpread(trade) > threshold
    override fun score(trade: ExchangeArbiTrade): Double = calcSpread(trade)

    private fun calcSpread(trade: ExchangeArbiTrade): Double =
        SpreadCalculator.calculateSpread(trade) -
                trade.buyExchangePrice.exchange.getFee() -
                trade.sellExchangePrice.exchange.getFee()
}

