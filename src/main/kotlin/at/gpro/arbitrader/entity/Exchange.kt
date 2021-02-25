package at.gpro.arbitrader.entity

import java.math.BigDecimal

interface Exchange {
    fun getName(): String
    fun getFee(): BigDecimal
}