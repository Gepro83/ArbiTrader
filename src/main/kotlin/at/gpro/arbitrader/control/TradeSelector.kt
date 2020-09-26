package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.Trade

interface TradeSelector {
    fun selectTrades(trades: List<Trade>) : List<Trade>
}