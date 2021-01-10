package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade

interface TradeSelector {
    fun selectTrades(trades: List<ArbiTrade>) : List<ArbiTrade>
}