package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.CurrencyTrade
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.Order
import kotlinx.coroutines.*

class MarketExecutor : TradeExecutor {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun executeTrades(trades: List<CurrencyTrade>) {
        val coroutines : MutableList<Deferred<Unit>> = ArrayList(trades.size * 2)

        trades.forEach { currencyTrade ->
            val buyExchange = currencyTrade.trade.buyPrice.exchange
            val sellExchange = currencyTrade.trade.sellPrice.exchange
            val amount = currencyTrade.trade.amount

            coroutines.add(placeAsync(Order.ask(amount, currencyTrade.pair), buyExchange))
            coroutines.add(placeAsync(Order.bid(amount, currencyTrade.pair), sellExchange))
        }

        runBlocking {
            coroutines.awaitAll()
        }
    }

    private fun placeAsync(order: Order, exchange: Exchange) =
        scope.async {
            exchange.place(order)
        }
}