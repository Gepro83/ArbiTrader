package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.ArbiTrade
import java.math.BigDecimal

data class SimpleArbiTrade(
    override val amount: BigDecimal,
    override val buyPrice: BigDecimal,
    override val sellPrice: BigDecimal
) : ArbiTrade

