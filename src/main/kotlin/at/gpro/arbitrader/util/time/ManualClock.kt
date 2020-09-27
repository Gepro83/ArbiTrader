package at.gpro.arbitrader.util.time

import java.time.Duration

class ManualClock : Clock {
    private var timersExpired = false
    override fun makeTimer(duration: Duration) = object : Timer {
        override fun start() {}
        override fun hasExpired() = timersExpired
    }

    fun expireTimers() {
        timersExpired = true
    }
}