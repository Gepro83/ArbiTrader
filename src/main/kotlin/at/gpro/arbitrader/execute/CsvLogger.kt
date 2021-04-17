package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.ArbiTrade
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.evaluate.SpreadCalculator
import mu.KotlinLogging
import java.io.File

private val LOG = KotlinLogging.logger {}

class CsvLogger(
    private val file: File,
    private val delayBetweenExecuteMS: Long
): TradeExecutor {

    init {
        if(!file.exists()) {
            file.createNewFile()
            file.writeText(CSV_HEADER + System.lineSeparator())
        }
    }

    companion object {
        val CSV_HEADER = "TIMESTAMP;SPREAD;AMOUNT;BUY_PRICE;BUY_EXCHANGE;SELL_PRICE;SELL_EXCHANGE;PAIR"
    }

    override fun executeTrades(pair: CurrencyPair, trades: List<ArbiTrade>) {
        if(trades.isNotEmpty()) {
            file.appendText(trades.joinToString(System.lineSeparator()) { getLineFor(it, pair) } + System.lineSeparator())
            Thread.sleep(delayBetweenExecuteMS)
        }
    }

    private fun getLineFor(trade: ArbiTrade, pair: CurrencyPair): String =
        listOf(
            System.currentTimeMillis(),
            SpreadCalculator.calculateSpread(trade),
            trade.amount,
            trade.buyPrice.price,
            trade.buyPrice.exchange.getName(),
            trade.sellPrice.price,
            trade.sellPrice.exchange.getName(),
            pair,
        ).joinToString(";")
            .also { LOG.debug { it }}

}