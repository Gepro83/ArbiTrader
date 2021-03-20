package at.gpro.arbitrader.xchange.utils

import at.gpro.arbitrader.entity.Exchange
import at.gpro.arbitrader.entity.Order

class ExchangeConverter {
    fun convert(xchange: XchangeExchange, fee: Double) =
        object : Exchange {
            override fun getName(): String = xchange.exchangeSpecification.exchangeName
            override fun getFee(): Double = fee
            override fun place(order: Order) {
                TODO("Not yet implemented")
            }

            override fun toString(): String = getName()
        }
}