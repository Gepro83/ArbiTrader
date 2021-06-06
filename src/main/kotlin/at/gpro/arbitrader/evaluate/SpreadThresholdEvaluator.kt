package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.control.TradeEvaluator
import at.gpro.arbitrader.entity.ExchangeArbiTrade
import at.gpro.arbitrader.entity.ScoredArbiTrade
import mu.KotlinLogging
import java.math.BigDecimal

private val LOG = KotlinLogging.logger {}

class SpreadThresholdEvaluator(private val threshold: Double): TradeEvaluator  {

    override fun isWorthy(trade: ExchangeArbiTrade): Boolean = calcSpread(trade) > threshold

    override fun score(trade: ExchangeArbiTrade): ScoredArbiTrade = object : ScoredArbiTrade {
        override val score: BigDecimal
            get() = BigDecimal(calcSpread(trade))
        override val amount: BigDecimal
            get() = trade.amount
        override val buyPrice: BigDecimal
            get() = trade.buyPrice
        override val sellPrice: BigDecimal
            get() = trade.sellPrice
    }

    private fun calcSpread(trade: ExchangeArbiTrade): Double =
        SpreadCalculator.calculateSpread(trade) -
                trade.buyExchangePrice.exchange.getFee() -
                trade.sellExchangePrice.exchange.getFee()
}

