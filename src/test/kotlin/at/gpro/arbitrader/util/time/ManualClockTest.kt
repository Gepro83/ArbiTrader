package at.gpro.arbitrader.util.time

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.Thread.sleep
import java.time.Duration

internal class ManualClockTest {

    @Test
    fun expireTimers() {
        val clock = ManualClock()
        val aTimer = clock.makeTimer(Duration.ofMillis(1)).apply { start() }
        val secondTimer = clock.makeTimer(Duration.ofMillis(1)).apply { start() }

        sleep(2)
        assertFalse(aTimer.hasExpired())
        assertFalse(secondTimer.hasExpired())

        clock.expireTimers()

        assertTrue(aTimer.hasExpired())
        assertTrue(secondTimer.hasExpired())
    }
}