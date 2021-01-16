package at.gpro.arbitrader.control

import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.find.ArbiTradeFinder
import mu.KotlinLogging

private val LOG = KotlinLogging.logger {}

class TradeController(
    private val tradeFinder: ArbiTradeFinder,
    private val updateProvider: UpdateProvider,
    private val tradeSelector: TradeSelector,
    private val tradeExecutor: TradeExecutor,
    private val currencyPairs: List<CurrencyPair>
) {

    fun runUntil(isStopped: () -> Boolean) {
        LOG.debug { "trade controller started" }
        while(!isStopped())
            runMainLoop()
    }

    private fun runMainLoop() {


        val orderBooks = updateProvider.getOrderBooks(CurrencyPair.BTC_EUR)
//        val findTrades = tradeFinder.findTrades(orderBooks)
//        findTrades
    }

//    private fun checkPair(pair)
}