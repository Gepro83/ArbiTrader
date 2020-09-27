package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.entity.order.Offer
import at.gpro.arbitrader.entity.order.OrderBook
import org.knowm.xchange.dto.trade.LimitOrder

class OrderBookConverter {
    fun convert(orderBook: XchangeOrderBook, exchange: XchangeExchange) : OrderBook {
        return OrderBook(exchange = ExchangeConverter().convert(exchange),
            sellOffers = getSellOffers(orderBook),
            buyOffers = getBuyOffers(orderBook)
        )
    }

    private fun getBuyOffers(orderBook: XchangeOrderBook) = orderBook?.bids?.let { toOffers(it) } ?: emptyList()

    private fun getSellOffers(orderBook: XchangeOrderBook) = orderBook?.asks?.let { toOffers(it) } ?: emptyList()

    private fun toOffers(orders: List<LimitOrder>) = orders.map { Offer(it.originalAmount, it.limitPrice) }
}