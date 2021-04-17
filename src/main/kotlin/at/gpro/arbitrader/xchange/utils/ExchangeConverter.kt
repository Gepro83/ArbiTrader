package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.entity.Currency
import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.Order
import java.math.BigDecimal

class ExchangeConverter {
    fun convert(xchange: XchangeExchange, fee: Double) =
        object : Exchange {
            override fun getName(): String = xchange.exchangeSpecification.exchangeName
            override fun getFee(): Double = fee
            override fun place(order: Order) {
                TODO("Not yet implemented")
            }

            override fun getBalance(currency: Currency): BigDecimal {
                TODO("Not yet implemented")
            }

            override fun toString(): String = getName()
        }
}