package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.find.ArbiTradeFinder
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class TradeController(
    private val tradeFinder: ArbiTradeFinder,
    private val updateProvider: UpdateProvider,
    private val tradeExecutor: TradeExecutor,
    private val tradeSelector: TradeSelector
) {

    fun runUntil(isStopped: () -> Boolean) {
        LOG.debug { "trade controller started" }
        while(!isStopped())
            runMainLoop()
    }

    private fun runMainLoop() {
        TODO("needs testing for different currencypairs")

        val orderBooks = updateProvider.getOrderBooks(CurrencyPair.BTC_EUR)
//        val findTrades = tradeFinder.findTrades(orderBooks)
//        findTrades
    }
}