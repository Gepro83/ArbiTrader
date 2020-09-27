package at.gpro.arbitrader.xchange

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.order.OrderBook
import at.gpro.arbitrader.util.time.Clock
import at.gpro.arbitrader.util.time.SystemClock
import at.gpro.arbitrader.util.time.Timer
import java.time.Duration

class OrderBookStore(private val clock: Clock = SystemClock()) {
    companion object {
        private val ORDERBOOK_RETENTION_DURATION = Duration.ofMillis(100)
    }
    private val exchangeMap : MutableMap<Exchange, MutableMap<CurrencyPair, OrderBook>> = HashMap()
    private val timerMap: MutableMap<OrderBook, Timer> = HashMap()


    fun getBooksFor(pair: CurrencyPair): List<OrderBook> {
        synchronized(this) {
            removeExpired(pair)
            return filterAllBooksFor(pair)
        }
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
            if (timerMap[orderBook]?.hasExpired() == true) {
                timerMap.remove(orderBook)
                currencyMap.remove(pair)
            }
        }
    }

    fun update(orderBook: OrderBook, pair: CurrencyPair) {
        synchronized(this) {
            val pairMap = exchangeMap.getOrPut(orderBook.exchange, { HashMap() })
            pairMap[pair] = orderBook
            timerMap[orderBook] = clock.makeTimer(ORDERBOOK_RETENTION_DURATION).apply { start() }
        }
    }
}
