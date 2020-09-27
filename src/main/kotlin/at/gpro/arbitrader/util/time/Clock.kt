package at.gpro.arbitrader.util.time

import java.time.Duration


interface Timer {
    fun start()
    fun hasExpired(): Boolean
}

interface Clock {
    fun makeTimer(duration: Duration) : Timer
}