package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.entity.Exchange
import java.math.BigDecimal

class ExchangeConverter {
    fun convert(xchange: XchangeExchange, fee: BigDecimal) =
        object : Exchange {
            override fun getName(): String = xchange.exchangeSpecification.exchangeName
            override fun getFee(): BigDecimal = fee
            override fun toString(): String = getName()
        }
}