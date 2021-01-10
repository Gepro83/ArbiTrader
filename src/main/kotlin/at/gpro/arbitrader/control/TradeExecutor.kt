package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade

interface TradeExecutor {
    fun executeTrades(trades: List<ArbiTrade>)
}