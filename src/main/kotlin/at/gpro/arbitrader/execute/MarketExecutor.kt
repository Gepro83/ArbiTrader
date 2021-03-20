package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.CurrencyTrade
import at.gpro.arbitrader.entity.Order

class MarketExecutor : TradeExecutor {
    override fun executeTrades(trades: List<CurrencyTrade>) {
        trades.forEach {
            it.trade.buyPrice.exchange.place(
                Order.ask(it.trade.amount, it.pair)
            )
            it.trade.sellPrice.exchange.place(
                Order.bid(it.trade.amount, it.pair)
            )
        }
    }
}