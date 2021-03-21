package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.*
import kotlinx.coroutines.*
import java.math.BigDecimal

class MarketExecutor : TradeExecutor {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun executeTrades(trades: List<CurrencyTrade>) {
        val coroutines : MutableList<Deferred<Unit>> = ArrayList(trades.size * 2)

        val tradesPerPair = trades.groupBy { it.pair }

        val exchangeOrders = tradesPerPair.flatMap { (pair, pairTrades) ->
            deriveExchangeOrders(pairTrades, pair)
        }

        exchangeOrders.forEach { (exchange, order) ->
            coroutines.add(placeAsync(order, exchange))
        }

        runBlocking {
            coroutines.awaitAll()
        }
    }

    private fun deriveExchangeOrders(
        trades: List<CurrencyTrade>,
        pair: CurrencyPair
    ): List<Pair<Exchange, Order>> {
        val tradesPerSellExchange = trades.groupBy { it.trade.sellPrice.exchange }
        val tradesPerBuyExchange = trades.groupBy { it.trade.buyPrice.exchange }

        val orderPerSellExchange = reduceToOrder(tradesPerSellExchange, pair, OrderType.BID)
        val orderPerBuyExchange = reduceToOrder(tradesPerBuyExchange, pair, OrderType.ASK)

        return ArrayList<Pair<Exchange, Order>>().apply {
            addAll(orderPerBuyExchange)
            addAll(orderPerSellExchange)
        }
    }

    private fun reduceToOrder(
        tradesPerExchange: Map<Exchange, List<CurrencyTrade>>,
        pair: CurrencyPair,
        orderType: OrderType
    ) = tradesPerExchange.mapValues { (exchange, trades) ->
        val totalAmount = trades
            .map { it.trade.amount }
            .reduce(BigDecimal::plus)

        Order(orderType, totalAmount, pair)
    }.toList()

    private fun placeAsync(order: Order, exchange: Exchange) =
        scope.async {
            exchange.place(order)
        }
}