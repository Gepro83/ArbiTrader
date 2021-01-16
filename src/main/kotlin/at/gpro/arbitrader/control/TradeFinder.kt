package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.order.OrderBook

interface TradeFinder {
    fun findTrades(orderBook: OrderBook, compareOderBook: OrderBook): List<ArbiTrade>
}