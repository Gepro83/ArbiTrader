package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.Trade
import at.gpro.arbitrader.entity.order.OrderBook

interface TradeFinder {
    fun findTrades(orderBooks: List<OrderBook>) : List<Trade>
}