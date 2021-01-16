package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyTrade

interface TradeExecutor {
    fun executeTrades(trades: List<CurrencyTrade>)
}