package at.gpro.arbitrader.update

import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.Offer
import at.gpro.arbitrader.entity.order.OrderBook
import org.knowm.xchange.dto.trade.LimitOrder

class OrderBookConverter {
    fun convert(orderBook: XchangeOrderBook, exchange: XchangeExchange) : OrderBook {
        return OrderBook(exchange = convert(exchange),
            sellOffers = getSellOffers(orderBook),
            buyOffers = getBuyOffers(orderBook)
        )
    }

    private fun convert(exchange: XchangeExchange) = object : Exchange {
        override fun getName(): String = exchange.exchangeSpecification.exchangeName
    }

    private fun getBuyOffers(orderBook: XchangeOrderBook) = orderBook?.bids?.let { toOffers(it) } ?: emptyList()

    private fun getSellOffers(orderBook: XchangeOrderBook) = orderBook?.asks?.let { toOffers(it) } ?: emptyList()

    private fun toOffers(orders: List<LimitOrder>) = orders.map { Offer(it.originalAmount, it.limitPrice) }
}