package at.gpro.arbitrader.execute

import at.gpro.arbitrader.control.TradeExecutor
import at.gpro.arbitrader.entity.*
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal

internal class MarketExecutorTest {

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

        override fun getBalance(pair: Currency) = BigDecimal(1000000)
    }

    private class SlowExchange(private val placeDelay: Long): Exchange {
        override fun getName(): String = "SlowExchange"
        override fun getFee(): Double = 0.0
        override fun place(order: Order) {
            Thread.sleep(placeDelay)
        }

        override fun getBalance(pair: Currency) = BigDecimal(10000000)
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
        val executor: TradeExecutor = MarketExecutor()

        executor.executeTrades(
            listOf(
                CurrencyTrade(
                    CurrencyPair.BTC_EUR,
                    ArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, mockBuyExchange),
                        sellPrice = ExchangePrice(11, mockSellExchange),
                    )
                )
            ))

        assertThat(mockBuyExchange.placedOrders, contains(Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR)))
        assertThat(mockSellExchange.placedOrders, contains(Order.bid(BigDecimal(1), CurrencyPair.BTC_EUR)))
    }

    @Test
    fun `execute trades in paralell`() {
        val executor = MarketExecutor()

        val before = System.currentTimeMillis()

        executor.executeTrades(
            listOf(
                CurrencyTrade(
                    CurrencyPair.BTC_EUR,
                    ArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, SlowExchange(300)),
                        sellPrice = ExchangePrice(11, SlowExchange(300)),
                    )
                ),
                CurrencyTrade(
                    CurrencyPair.BTC_EUR,
                    ArbiTrade(
                        1,
                        buyPrice = ExchangePrice(10, SlowExchange(300)),
                        sellPrice = ExchangePrice(11, SlowExchange(300)),
                    )
                )
            )
        )

        val after = System.currentTimeMillis()

        assertThat(after - before, lessThan(600))
    }

    @Test
    fun `exception in place trade propagating`() {

        assertThrows<RuntimeException> {
            MarketExecutor().executeTrades(
                listOf(
                    CurrencyTrade(
                        CurrencyPair.BTC_EUR,
                        ArbiTrade(
                            1,
                            buyPrice = ExchangePrice(10, SlowExchange(300)),
                            sellPrice = ExchangePrice(11, object : Exchange {
                                override fun getName(): String = "asd"
                                override fun getFee(): Double = 1.0
                                override fun place(order: Order) {
                                    Thread.sleep(50)
                                    throw RuntimeException()
                                }
                                override fun getBalance(pair: Currency) = BigDecimal(100000)
                            }),
                        )
                    )
                )
            )
        }
    }


    @Test
    fun `combine trades to same exchange`() {

        val buyExchange = MockExchange()
        val sellExchange = MockExchange()

        val aTrade = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal.ONE,
                buyPrice = ExchangePrice(100, buyExchange),
                sellPrice = ExchangePrice(111, sellExchange),
        ))
        val anotherTrade = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal.TEN,
                buyPrice = ExchangePrice(100, buyExchange),
                sellPrice = ExchangePrice(111, sellExchange),
        ))

        val executor = MarketExecutor()
        executor.executeTrades(listOf(aTrade, anotherTrade))

        assertThat(buyExchange.placedOrders, contains(Order.ask(BigDecimal(11), CurrencyPair.BTC_EUR)))
        assertThat(sellExchange.placedOrders, contains(Order.bid(BigDecimal(11), CurrencyPair.BTC_EUR)))
    }

    @Test
    internal fun `do not combine trades of differenct currencies`() {

        val buyExchange = MockExchange()
        val sellExchange = MockExchange()

        val aTrade = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal.ONE,
                buyPrice = ExchangePrice(100, buyExchange),
                sellPrice = ExchangePrice(111, sellExchange),
            ))
        val anotherTrade = CurrencyTrade(
            CurrencyPair.ETH_EUR,
            ArbiTrade(
                BigDecimal.TEN,
                buyPrice = ExchangePrice(100, buyExchange),
                sellPrice = ExchangePrice(111, sellExchange),
            ))

        val executor = MarketExecutor()
        executor.executeTrades(listOf(aTrade, anotherTrade))

        assertThat(buyExchange.placedOrders, containsInAnyOrder(
            Order.ask(BigDecimal(1), CurrencyPair.BTC_EUR),
            Order.ask(BigDecimal(10), CurrencyPair.ETH_EUR)
        ))
        assertThat(sellExchange.placedOrders, containsInAnyOrder(
            Order.bid(BigDecimal(1), CurrencyPair.BTC_EUR),
            Order.bid(BigDecimal(10), CurrencyPair.ETH_EUR)
        ))
    }

    @Test
    fun `do not combine trades of differenct exchanges`() {
        val kraken = MockExchange()
        val coinbase = MockExchange()
        val bitstamp = MockExchange()

        val krakenToCoinbase = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal.ONE,
                buyPrice = ExchangePrice(100, kraken),
                sellPrice = ExchangePrice(111, coinbase),
            )
        )
        val coinbaseToBitstamp = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal.TEN,
                buyPrice = ExchangePrice(100, coinbase),
                sellPrice = ExchangePrice(111, bitstamp),
            ))
        val bitstampToKraken = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal(3),
                buyPrice = ExchangePrice(100, bitstamp),
                sellPrice = ExchangePrice(111, kraken),
            ))

        val executor = MarketExecutor()
        executor.executeTrades(listOf(krakenToCoinbase, coinbaseToBitstamp, bitstampToKraken))

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

        val krakenToCoinbase = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal.ONE,
                buyPrice = ExchangePrice(100, kraken),
                sellPrice = ExchangePrice(111, coinbase),
            )
        )
        val coinbaseToBitstamp = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal.TEN,
                buyPrice = ExchangePrice(100, bitstamp),
                sellPrice = ExchangePrice(111, coinbase),
            ))
        val bitstampToKraken = CurrencyTrade(
            CurrencyPair.BTC_EUR,
            ArbiTrade(
                BigDecimal(3),
                buyPrice = ExchangePrice(100, bitstamp),
                sellPrice = ExchangePrice(111, kraken),
            ))

        val executor = MarketExecutor()
        executor.executeTrades(listOf(krakenToCoinbase, coinbaseToBitstamp, bitstampToKraken))

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