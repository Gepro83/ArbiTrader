package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradePlacer
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.ScoredArbiTrade
import mu.KotlinLogging
import java.io.File

private val LOG = KotlinLogging.logger {}

class CsvLogger(
    private val file: File,
    private val delayBetweenExecuteMS: Long
): TradePlacer {

    init {
        if(!file.exists()) {
            file.createNewFile()
            file.writeText(CSV_HEADER + System.lineSeparator())
        }
    }

    companion object {
        val CSV_HEADER = "TIMESTAMP;SPREAD;AMOUNT;BUY_PRICE;BUY_EXCHANGE;SELL_PRICE;SELL_EXCHANGE;PAIR"
    }

    private fun getLineFor(trade: ScoredArbiTrade, buyExchange: Exchange, sellExchange: Exchange, pair: CurrencyPair): String =
        listOf(
            System.currentTimeMillis(),
            trade.score,
            trade.amount,
            trade.buyPrice,
            buyExchange,
            trade.sellPrice,
            sellExchange,
            pair,
        ).joinToString(";")
//            .also { LOG.debug { it }}

    override fun placeTrades(
        pair: CurrencyPair,
        buyExchange: Exchange,
        sellExchange: Exchange,
        trades: List<ScoredArbiTrade>
    ) {
        if(trades.isNotEmpty()) {
            file.appendText(trades.joinToString(System.lineSeparator()) { getLineFor(it, buyExchange, sellExchange, pair) } + System.lineSeparator())
            Thread.sleep(delayBetweenExecuteMS)
        }
    }

}