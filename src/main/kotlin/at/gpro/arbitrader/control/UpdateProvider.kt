package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.order.OrderBook

interface UpdateProvider {
    fun getOrderBooks(currencyPair: CurrencyPair) : List<OrderBook>
    fun onUpdate(action: () -> Unit)
}