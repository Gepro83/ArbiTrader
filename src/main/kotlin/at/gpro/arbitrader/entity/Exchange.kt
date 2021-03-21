package at.gpro.arbitrader.entity

import java.math.BigDecimal

interface Exchange {
    fun getName(): String
    fun getFee(): Double
    fun place(order: Order)
    fun getBalance(pair: Currency): BigDecimal
}