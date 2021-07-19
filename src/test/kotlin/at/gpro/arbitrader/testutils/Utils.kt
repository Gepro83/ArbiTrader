package at.gpro.arbitrader.testutils

import java.lang.Thread.sleep
import java.util.concurrent.Executors

fun runInThread(block: () -> Unit) {
    Executors.newSingleThreadExecutor().submit(block)
    sleep(200) // some time for thread to start
}