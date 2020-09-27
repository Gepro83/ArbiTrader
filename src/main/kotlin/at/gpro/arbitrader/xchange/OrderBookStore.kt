package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.OrderBook
import java.util.concurrent.ConcurrentHashMap

class OrderBookStore {
    private val exchangeToOrderBooks : MutableMap<Exchange, OrderBook> = ConcurrentHashMap()

    fun getBooksFor(pair: CurrencyPair): List<OrderBook> {
        return ArrayList(exchangeToOrderBooks.values)
    }

    fun update(orderBook: OrderBook, pair: CurrencyPair) {
        exchangeToOrderBooks[orderBook.exchange] = orderBook
    }
}