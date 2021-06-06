package at.gpro.arbitrader.entity

import java.math.BigDecimal

interface ArbiTrade {
    val amount: BigDecimal
    val buyPrice: BigDecimal
    val sellPrice: BigDecimal
}