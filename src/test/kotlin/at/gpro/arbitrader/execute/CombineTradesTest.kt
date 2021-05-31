package at.gpro.arbitrader.execute

import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class CombineTradesTest {

    private val mockBuyExchange = MockExchange()
    private val mockSellExchange = MockExchange()

    private class MockExchange : Exchange {

        private val orders: MutableList<Order> = ArrayList()

        val placedOrders: List<Order>
            get() = orders

        override fun getName(): String = "MockExchange"
        override fun getFee(): Double = 0.0
        override fun place(order: Order) {
            orders.add(order)
        }

        override fun getBalance(currency: Currency) = BigDecimal(1000000)
    }

    private class SlowExchange(private val placeDelay: Long): Exchange {
        override fun getName(): String = "SlowExchange"
        override fun getFee(): Double = 0.0
        override fun place(order: Order) {
            Thread.sleep(placeDelay)
        }

        override fun getBalance(currency: Currency) = BigDecimal(10000000)
    }

    @Test
    fun `MockExchange keeping placedOrders`() {
        val order = Order.ask(BigDecimal.ZERO, CurrencyPair.BTC_EUR)
        val exchange = MockExchange()
        exchange.place(order)
        assertThat(exchange.placedOrders, contains(order))
    }

    @Test
    fun `place trade at buy and sell exchange`() {
        val placer = MarketPlacer()

        placer.placeTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                    ExchangeArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, mockBuyExchange),
                        sellPrice = ExchangePrice(11, mockSellExchange),
                    )
                )
            )

        assertThat(mockBuyExchange.placedOrders, contains(Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR)))
        assertThat(mockSellExchange.placedOrders, contains(Order.bid(BigDecimal(1), CurrencyPair.BTC_EUR)))
    }

    @Test
    fun `execute trades in paralell`() {
        val placer = MarketPlacer()

        val before = System.currentTimeMillis()

        placer.placeTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                ExchangeArbiTrade(
                    1,
                    buyPrice = ExchangePrice(10, SlowExchange(300)),
                    sellPrice = ExchangePrice(11, SlowExchange(300)),
                ),
                ExchangeArbiTrade(
                    1,
                    buyPrice = ExchangePrice(10, SlowExchange(300)),
                    sellPrice = ExchangePrice(11, SlowExchange(300)),
                )
            )
        )

        val after = System.currentTimeMillis()

        assertThat(after - before, lessThan(600))
    }

    @Test
    fun `exception in place trade propagating`() {

        assertThrows<RuntimeException> {
            MarketPlacer().placeTrades(
                CurrencyPair.BTC_EUR,
                listOf(
                    ExchangeArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, SlowExchange(300)),
                        sellPrice = ExchangePrice(11, object : Exchange {
                            override fun getName(): String = "asd"
                            override fun getFee(): Double = 1.0
                            override fun place(order: Order) {
                                Thread.sleep(50)
                                throw RuntimeException()
                            }
                            override fun getBalance(currency: Currency) = BigDecimal(100000)
                        }),
                    )
                )
            )
        }
    }


    @Test
    fun `combine trades to same exchange`() {

        val buyExchange = MockExchange()
        val sellExchange = MockExchange()

        val aTrade =
            ExchangeArbiTrade(
                BigDecimal.ONE,
                buyExchangePrice = ExchangePrice(100, buyExchange),
                sellExchangePrice = ExchangePrice(111, sellExchange),
        )
        val anotherTrade =
            ExchangeArbiTrade(
                BigDecimal.TEN,
                buyExchangePrice = ExchangePrice(100, buyExchange),
                sellExchangePrice = ExchangePrice(111, sellExchange),
        )

        val placer = MarketPlacer()
        placer.placeTrades(CurrencyPair.BTC_EUR, listOf(aTrade, anotherTrade))

        assertThat(buyExchange.placedOrders, contains(Order.ask(BigDecimal(11), CurrencyPair.BTC_EUR)))
        assertThat(sellExchange.placedOrders, contains(Order.bid(BigDecimal(11), CurrencyPair.BTC_EUR)))
    }

    @Test
    fun `do not combine trades of differenct exchanges`() {
        val kraken = MockExchange()
        val coinbase = MockExchange()
        val bitstamp = MockExchange()

        val krakenToCoinbase =
            ExchangeArbiTrade(
                BigDecimal.ONE,
                buyExchangePrice = ExchangePrice(100, kraken),
                sellExchangePrice = ExchangePrice(111, coinbase),
            )

        val coinbaseToBitstamp =
            ExchangeArbiTrade(
                BigDecimal.TEN,
                buyExchangePrice = ExchangePrice(100, coinbase),
                sellExchangePrice = ExchangePrice(111, bitstamp),
            )
        val bitstampToKraken =
            ExchangeArbiTrade(
                BigDecimal(3),
                buyExchangePrice = ExchangePrice(100, bitstamp),
                sellExchangePrice = ExchangePrice(111, kraken),
            )

        val placer = MarketPlacer()
        placer.placeTrades(CurrencyPair.BTC_EUR, listOf(krakenToCoinbase, coinbaseToBitstamp, bitstampToKraken))

        assertThat(kraken.placedOrders, containsInAnyOrder(
            Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR),
            Order.bid(BigDecimal(3), CurrencyPair.BTC_EUR)
        ))
        assertThat(coinbase.placedOrders, containsInAnyOrder(
            Order.bid(BigDecimal(1), CurrencyPair.BTC_EUR),
            Order.ask(BigDecimal(10), CurrencyPair.BTC_EUR)
        ))
        assertThat(bitstamp.placedOrders, containsInAnyOrder(
            Order.ask(BigDecimal(3), CurrencyPair.BTC_EUR),
            Order.bid(BigDecimal(10), CurrencyPair.BTC_EUR)
        ))
    }

    @Test
    fun `combine trades from different arbitrades`() {
        val kraken = MockExchange()
        val coinbase = MockExchange()
        val bitstamp = MockExchange()

        val krakenToCoinbase =
            ExchangeArbiTrade(
                BigDecimal.ONE,
                buyExchangePrice = ExchangePrice(100, kraken),
                sellExchangePrice = ExchangePrice(111, coinbase),
            )

        val coinbaseToBitstamp =
            ExchangeArbiTrade(
                BigDecimal.TEN,
                buyExchangePrice = ExchangePrice(100, bitstamp),
                sellExchangePrice = ExchangePrice(111, coinbase),
            )
        val bitstampToKraken =
            ExchangeArbiTrade(
                BigDecimal(3),
                buyExchangePrice = ExchangePrice(100, bitstamp),
                sellExchangePrice = ExchangePrice(111, kraken),
            )

        val placer = MarketPlacer()
        placer.placeTrades(CurrencyPair.BTC_EUR, listOf(krakenToCoinbase, coinbaseToBitstamp, bitstampToKraken))

        assertThat(kraken.placedOrders, containsInAnyOrder(
            Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR),
            Order.bid(BigDecimal(3), CurrencyPair.BTC_EUR)
        ))
        assertThat(coinbase.placedOrders, contains(
            Order.bid(BigDecimal(11), CurrencyPair.BTC_EUR)
        ))
        assertThat(bitstamp.placedOrders, contains(
            Order.ask(BigDecimal(13), CurrencyPair.BTC_EUR)
        ))
    }


}