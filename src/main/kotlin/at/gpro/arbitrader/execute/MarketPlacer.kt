package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradePlacer
import at.gpro.arbitrader.entity.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.math.BigDecimal

class MarketPlacer(
    private val safePriceMargin : Double = 0.0,
    private val balanceMargin : Double = 0.0
) : TradePlacer {

    companion object {
        private val LOG = KotlinLogging.logger {}
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun placeTrades(
        pair: CurrencyPair,
        buyExchange: Exchange,
        sellExchange: Exchange,
        trades: List<ScoredArbiTrade>
    ) {
        val coroutines: MutableList<Deferred<Unit>> = ArrayList(trades.size * 2)

        val amount = calculateSafeAmount(
            buyExchange,
            sellExchange,
            pair,
            trades
        )

        coroutines.add(placeAsync(Order(OrderType.ASK, amount, pair), buyExchange))
        coroutines.add(placeAsync(Order(OrderType.BID, amount, pair), sellExchange))

        runBlocking {
            coroutines.awaitAll()
        }
    }

    private fun calculateSafeAmount(
        buyExchange: Exchange,
        sellExchange: Exchange,
        pair: CurrencyPair,
        trades: List<ScoredArbiTrade>
    ): BigDecimal {
        with(BalanceKeeper(safePriceMargin, balanceMargin)) {
            var safeAmount = BigDecimal.ZERO

            for (trade in trades.sortedByDescending { it.score }) {
                val currentAmount = getSafeAmount(
                    buyExchange,
                    sellExchange,
                    pair,
                    trade
                )

                safeAmount += currentAmount
                reduceBalance(buyExchange, safeAmount.times(trade.buyPrice), pair.payCurrency)
                reduceBalance(sellExchange, safeAmount, pair.mainCurrency)
            }

            return safeAmount
        }
    }

    private fun placeAsync(order: Order, exchange: Exchange): Deferred<Unit> =
        scope.async {
            exchange.place(order)
        }

}