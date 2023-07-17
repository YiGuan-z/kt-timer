package com.cqsd.timer

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.util.concurrent.TimeUnit


class TimerTest {
    @Test
    fun testTimer() = runBlocking {
        val timer = Timer().apply {
            addJob(TimeUnit.SECONDS, 1) { println("1") }
            addJob(TimeUnit.SECONDS, 2) { println("2") }
        }
        launch {
            timer.start()
        }
        delay(5000)
        timer.cancel(0)
        delay(5000)
        timer.cancelAll()
    }
}