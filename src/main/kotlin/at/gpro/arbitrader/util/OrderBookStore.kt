package at.gpro.arbitrader.util

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.OrderBook
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val LOG = KotlinLogging.logger {}

class OrderBookStore(private val retentionDurationMillis: Long = 1300) {
    private val exchangeMap : MutableMap<Exchange, MutableMap<CurrencyPair, OrderBook>> = ConcurrentHashMap()

    private var lastLog = 0L
    private var isLogTime = true

    fun getBooksFor(pair: CurrencyPair): List<OrderBook> {
        removeExpired(pair)
        return filterAllBooksFor(pair)
    }

    private fun filterAllBooksFor(pair: CurrencyPair): List<OrderBook> {
        val bookList: MutableList<OrderBook> = ArrayList()
        exchangeMap.values.forEach { currencyMap ->
            currencyMap[pair]?.let { orderBook ->
                bookList.add(orderBook)
            }
        }

        return bookList
    }

    private fun removeExpired(pair: CurrencyPair) {
        exchangeMap.values.forEach { currencyMap ->
            removeExpired(currencyMap, pair)
        }
    }

    private fun removeExpired(
        currencyMap: MutableMap<CurrencyPair, OrderBook>,
        pair: CurrencyPair
    ) {
        currencyMap[pair]?.let { orderBook ->
            if (orderBook.hasExpired()) {
                currencyMap.remove(pair)
//                isLogTime = (System.currentTimeMillis() - lastLog) > 5000
//                if (isLogTime) {
//                    LOG.debug { "orderbook of ${orderBook.exchange.getName()} too old" }
//                    lastLog = System.currentTimeMillis()
//                }
            }
        }
    }

    private fun OrderBook.hasExpired(): Boolean =
        (System.currentTimeMillis() - timestamp) > retentionDurationMillis

    fun update(orderBook: OrderBook, pair: CurrencyPair) {
        getPairMap(orderBook)[pair] = orderBook
    }

    private fun getPairMap(orderBook: OrderBook) =
        exchangeMap.getOrPut(orderBook.exchange) { ConcurrentHashMap() }
}
