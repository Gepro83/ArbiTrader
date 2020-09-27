package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.OrderBook
import java.util.concurrent.ConcurrentHashMap

class OrderBookStore {
    private val exchangeMap : MutableMap<Exchange, MutableMap<CurrencyPair, OrderBook>> = ConcurrentHashMap()

    fun getBooksFor(pair: CurrencyPair): List<OrderBook> {
        val bookList : MutableList<OrderBook> = ArrayList()
        exchangeMap.values.forEach { currencyMap ->
            currencyMap[pair]?.let { bookList.add(it) }
        }
        return bookList
    }

    fun update(orderBook: OrderBook, pair: CurrencyPair) {
        val pairMap = exchangeMap.getOrPut(orderBook.exchange, { ConcurrentHashMap() })
        pairMap[pair] = orderBook
    }
}