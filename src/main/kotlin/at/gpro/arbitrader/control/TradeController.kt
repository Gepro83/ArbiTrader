package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.CurrencyTrade
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class TradeController(
    private val updateProvider: UpdateProvider,
    private val tradeFinder: TradeFinder,
    private val tradeSelector: TradeSelector,
    private val tradeExecutor: TradeExecutor,
    private val currencyPairs: List<CurrencyPair>
) {
    private var isStopped: () -> Boolean = { true }

    fun runUntil(isStopped: () -> Boolean) {
        LOG.debug { "trade controller started" }
        this.isStopped = isStopped
        updateProvider.onUpdate { onOrderBookUpdate() }
    }

    private fun onOrderBookUpdate() {
        currencyPairs.forEach { checkPair(it) }
    }

    private fun checkPair(pair: CurrencyPair) {
        val orderBooks = updateProvider.getOrderBooks(pair)

        if(orderBooks.size < 3)
            return

        val trades = ArrayList(tradeFinder.findTrades(orderBooks[0], orderBooks[1]))
        trades.addAll(tradeFinder.findTrades(orderBooks[1], orderBooks[2]))
        trades.addAll(tradeFinder.findTrades(orderBooks[0], orderBooks[2]))

        if (orderBooks.size == 4) {
            trades.addAll(tradeFinder.findTrades(orderBooks[3], orderBooks[0]))
            trades.addAll(tradeFinder.findTrades(orderBooks[3], orderBooks[1]))
            trades.addAll(tradeFinder.findTrades(orderBooks[3], orderBooks[2]))
        }

        val selectedTrades = tradeSelector.selectTrades(trades)

        tradeExecutor.executeTrades(selectedTrades.map { CurrencyTrade(pair, it) })

    }
}