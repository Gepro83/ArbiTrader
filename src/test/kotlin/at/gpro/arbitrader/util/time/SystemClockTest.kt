package at.gpro.arbitrader.util.time

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.time.Duration

internal class SystemClockTest {

    @Test
    fun `created timer expires after given duration`() {
        val timer = SystemClock().makeTimer(Duration.ofMillis(100))

        timer.start()

        assertFalse(timer.hasExpired())
        sleep(10)
        assertFalse(timer.hasExpired())
        sleep(90)
        assertTrue(timer.hasExpired())
    }

    @Test
    fun `created timer does not expire after given duration when start was not called`() {
        val timer = SystemClock().makeTimer(Duration.ofMillis(100))

        assertFalse(timer.hasExpired())
        sleep(10)
        assertFalse(timer.hasExpired())
        sleep(90)
        assertFalse(timer.hasExpired())
    }
}