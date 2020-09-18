package at.gpro.arbitrader

import at.gpro.arbitrader.tradecontroller.exchange.Exchange
import java.math.BigDecimal

val TWO = BigDecimal(2)
val THREE = BigDecimal(3)
val FOUR = BigDecimal(4)
val FIVE = BigDecimal(5)
val SIX = BigDecimal(6)
val SEVEN = BigDecimal(7)

val TESTEXCHANGE = object : Exchange {
    override fun toString(): String = "TestExchange"
}