package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradePlacer
import at.gpro.arbitrader.entity.*
import kotlinx.coroutines.*
import java.math.BigDecimal

class MarketPlacer : TradePlacer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun placeTrades(pair: CurrencyPair, trades: List<ArbiTrade>) {
        val coroutines: MutableList<Deferred<Unit>> = ArrayList(trades.size * 2)

        deriveExchangeOrders(pair, trades).forEach { (exchange, order) ->
            coroutines.add(placeAsync(order, exchange))
        }

        runBlocking {
            coroutines.awaitAll()
        }
    }

    private fun deriveExchangeOrders(
        pair: CurrencyPair,
        trades: List<ArbiTrade>
    ): List<Pair<Exchange, Order>> {
        val tradesPerSellExchange = trades.groupBy { it.sellPrice.exchange }
        val tradesPerBuyExchange = trades.groupBy { it.buyPrice.exchange }

        val orderPerSellExchange = reduceToOrder(tradesPerSellExchange, pair, OrderType.BID)
        val orderPerBuyExchange = reduceToOrder(tradesPerBuyExchange, pair, OrderType.ASK)

        return ArrayList<Pair<Exchange, Order>>().apply {
            addAll(orderPerBuyExchange)
            addAll(orderPerSellExchange)
        }
    }

    private fun reduceToOrder(
        tradesPerExchange: Map<Exchange, List<ArbiTrade>>,
        pair: CurrencyPair,
        orderType: OrderType
    ) = tradesPerExchange.mapValues { (exchange, trades) ->
        val totalAmount = trades
            .map { it.amount }
            .reduce(BigDecimal::plus)

        Order(orderType, totalAmount, pair)
    }.toList()

    private fun placeAsync(order: Order, exchange: Exchange) =
        scope.async {
            exchange.place(order)
        }
}