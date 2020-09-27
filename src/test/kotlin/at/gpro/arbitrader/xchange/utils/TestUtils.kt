package at.gpro.arbitrader.xchange.utils

import org.knowm.xchange.dto.Order
import org.knowm.xchange.dto.trade.LimitOrder
import org.knowm.xchange.instrument.Instrument
import java.math.BigDecimal

object TestUtils {
    internal fun makeOrder(
        orderType: Order.OrderType,
        amount: Int,
        price: Int
    ): LimitOrder {
        return LimitOrder.Builder(orderType, object : Instrument() {})
            .limitPrice(BigDecimal(price))
            .originalAmount(BigDecimal(amount))
            .build()
    }

    internal fun makeAskOrder(
        amount: Int,
        price: Int
    ) = makeOrder(Order.OrderType.ASK, amount, price)

    internal fun makeBidOrder(
        amount: Int,
        price: Int
    ) = makeOrder(Order.OrderType.BID, amount, price)
}
