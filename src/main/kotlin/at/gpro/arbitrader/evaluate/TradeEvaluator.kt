package at.gpro.arbitrader.evaluate

import at.gpro.arbitrader.entity.Trade

data class ScoredTrade(
    val trade: Trade,
    val score: Int
)

interface TradeEvaluator {
    fun evaluate(trades: List<Trade>) : List<ScoredTrade>
}