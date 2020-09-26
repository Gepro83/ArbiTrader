package at.gpro.arbitrader.update

import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.Offer
import at.gpro.arbitrader.entity.order.OrderBook
import org.knowm.xchange.dto.trade.LimitOrder

class OrderBookConverter {
    fun convert(orderBook: SdkOrderBook, exchange: SdkExchange) : OrderBook {
        return OrderBook(exchange = convert(exchange),
            sellOffers = getSellOffers(orderBook),
            buyOffers = getBuyOffers(orderBook)
        )
    }

    private fun convert(exchange: SdkExchange) = object : Exchange {
        override fun getName(): String = exchange.exchangeSpecification.exchangeName
    }

    private fun getBuyOffers(orderBook: SdkOrderBook) = orderBook?.bids?.let { toOffers(it) } ?: emptyList()

    private fun getSellOffers(orderBook: SdkOrderBook) = orderBook?.asks?.let { toOffers(it) } ?: emptyList()

    private fun toOffers(orders: List<LimitOrder>) = orders.map { Offer(it.originalAmount, it.limitPrice) }
}