package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradePlacer
import at.gpro.arbitrader.entity.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.math.BigDecimal
import java.util.*

class MarketPlacer(
    safePriceMargin : Double = 0.0,
    balanceMargin : Double = 0.0
) : TradePlacer {

    companion object {
        private val LOG = KotlinLogging.logger {}
    }

    private val balanceKeeper = BalanceKeeper(safePriceMargin, balanceMargin)

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

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

    private fun placeAsync(order: Order, exchange: Exchange): Deferred<Unit> =
        scope.async {
            exchange.place(order)
        }

    override fun placeTrades(
        pair: CurrencyPair,
        buyExchange: Exchange,
        sellExchange: Exchange,
        trades: SortedSet<ArbiTrade>
    ) {
        val coroutines: MutableList<Deferred<Unit>> = ArrayList(trades.size * 2)

        val amount = calculateTotalAmount(trades)
        LOG.debug { "Amount: $amount" }

        coroutines.add(placeAsync(Order(OrderType.ASK, amount, pair), buyExchange))
        coroutines.add(placeAsync(Order(OrderType.BID, amount, pair), sellExchange))

        runBlocking {
            coroutines.awaitAll()
        }
    }

    private fun calculateTotalAmount(trades: SortedSet<ArbiTrade>): BigDecimal {
        return trades.map { it.amount }
            .reduce(BigDecimal::plus)
    }
}