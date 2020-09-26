package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.Trade

interface TradeExecutor {
    fun executeTrades(trades: List<Trade>)
}