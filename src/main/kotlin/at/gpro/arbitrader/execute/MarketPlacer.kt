package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradePlacer
import at.gpro.arbitrader.entity.*
import kotlinx.coroutines.*
import java.math.BigDecimal
import java.util.*

class MarketPlacer(
    val balanceKeeper: BalanceKeeper = BalanceKeeper(0.05, 0.1)
) : TradePlacer {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun placeTrades(pair: CurrencyPair, trades: List<ExchangeArbiTrade>) {
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
        trades: List<ExchangeArbiTrade>
    ): List<Pair<Exchange, Order>> {
        val tradesPerSellExchange = trades.groupBy { it.sellExchangePrice.exchange }
        val tradesPerBuyExchange = trades.groupBy { it.buyExchangePrice.exchange }

        val orderPerSellExchange = reduceToOrder(tradesPerSellExchange, pair, OrderType.BID)
        val orderPerBuyExchange = reduceToOrder(tradesPerBuyExchange, pair, OrderType.ASK)

        return ArrayList<Pair<Exchange, Order>>().apply {
            addAll(orderPerBuyExchange)
            addAll(orderPerSellExchange)
        }
    }

    private fun reduceToOrder(
        tradesPerExchange: Map<Exchange, List<ExchangeArbiTrade>>,
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

    override fun placeTrades(
        pair: CurrencyPair,
        buyExchange: Exchange,
        sellExchange: Exchange,
        trades: SortedSet<ArbiTrade>
    ) {
    }
}