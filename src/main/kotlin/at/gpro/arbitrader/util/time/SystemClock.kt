package at.gpro.arbitrader.util.time

import java.time.Duration

class SystemClock : Clock {

    override fun makeTimer(duration: Duration): Timer = SystemTimer(duration.toMillis())

    private class SystemTimer(private val durationMillis: Long) : Timer {
        private var startTimeMillis : Long? = null

        override fun start() {
            startTimeMillis = System.currentTimeMillis()
        }

        override fun hasExpired() =
            if (startTimeMillis != null)
                System.currentTimeMillis() - startTimeMillis!! > durationMillis
            else
                false

    }
}
