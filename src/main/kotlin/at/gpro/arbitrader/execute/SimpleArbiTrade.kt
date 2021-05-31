package at.gpro.arbitrader.execute

import java.math.BigDecimal

data class SimpleArbiTrade(
    override val amount: BigDecimal,
    override val buyPrice: BigDecimal,
    override val sellPrice: BigDecimal
) : ArbiTrade

interface ArbiTrade {
    val amount: BigDecimal
    val buyPrice: BigDecimal
    val sellPrice: BigDecimal
}