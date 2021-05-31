package at.gpro.arbitrader.execute

import at.gpro.arbitrader.TestUtils
import at.gpro.arbitrader.TestUtils.newTestExchange
import at.gpro.arbitrader.entity.CurrencyPair
import at.gpro.arbitrader.entity.ExchangeArbiTrade
import at.gpro.arbitrader.entity.ExchangePrice
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.io.File

internal class CsvLoggerTest {

    val testLogFile = File("src/test/resources/csvLoggerTest.csv")

    @AfterEach
    internal fun tearDown() {
        testLogFile.delete()
    }

    @Test
    internal fun `files starts with header`() {
        CsvLogger(testLogFile, 0).placeTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                TestUtils.newTestExchangeTrade(1, 2, 5),
                TestUtils.newTestExchangeTrade(1, 3, 3),
                TestUtils.newTestExchangeTrade(1, 1, 7)
            ))

        assertThat(testLogFile.readLines()[0], `is`(CsvLogger.CSV_HEADER))
    }

    @Test
    internal fun `file has one line per trade plus header`() {
        CsvLogger(testLogFile, 0).placeTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                TestUtils.newTestExchangeTrade(1, 2, 5),
                TestUtils.newTestExchangeTrade(1, 3, 3),
                TestUtils.newTestExchangeTrade(1, 1, 7)
            ))

        assertThat(testLogFile.readLines().size, `is`(4))
    }

    @Test
    internal fun `line matching trade`() {
        CsvLogger(testLogFile, 0).placeTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                ExchangeArbiTrade(
                    5,
                    ExchangePrice(10, newTestExchange("buyExchange")),
                    ExchangePrice(12, newTestExchange("sellExchange")),
                )
            )
        )

        val firstTradeCells = testLogFile.readLines()[1].split(";")
        assertThat(firstTradeCells[1], `is`("0.2")) // spread
        assertThat(firstTradeCells[2], `is`("5")) // amount
        assertThat(firstTradeCells[3], `is`("10")) // buyPrice
        assertThat(firstTradeCells[4], `is`("buyExchange"))
        assertThat(firstTradeCells[5], `is`("12")) // sellPrice
        assertThat(firstTradeCells[6], `is`("sellExchange"))
        assertThat(firstTradeCells[7], `is`("BTC_EUR"))
    }

    @Test
    internal fun `append to file if already exists`() {
        testLogFile.createNewFile()
        testLogFile.writeText(CsvLogger.CSV_HEADER + System.lineSeparator() + "someline" + System.lineSeparator())

        CsvLogger(testLogFile, 0).placeTrades(
            CurrencyPair.BTC_EUR,
            listOf(
                ExchangeArbiTrade(
                    2,
                    ExchangePrice(10, newTestExchange("buyExchange")),
                    ExchangePrice(12, newTestExchange("sellExchange")),
                )
            )
        )

        val lines = testLogFile.readLines()

        assertThat(lines[1], `is`("someline"))

        val firstTradeCells = lines[2].split(";")
        assertThat(firstTradeCells[1], `is`("0.2")) // spread
        assertThat(firstTradeCells[2], `is`("2")) // amount
        assertThat(firstTradeCells[3], `is`("10")) // buyPrice
        assertThat(firstTradeCells[4], `is`("buyExchange"))
        assertThat(firstTradeCells[5], `is`("12")) // sellPrice
        assertThat(firstTradeCells[6], `is`("sellExchange"))
        assertThat(firstTradeCells[7], `is`("BTC_EUR"))
    }
}