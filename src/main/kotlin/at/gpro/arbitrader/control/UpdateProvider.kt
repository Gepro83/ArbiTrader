package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.order.OrderBook

interface UpdateProvider {
    fun getOrderBooks() : List<OrderBook>
}