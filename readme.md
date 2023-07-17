# kotlin Timer

一个使用kotlin协程库编写的简易定时任务库。

~~造个轮子玩~~

- ⚠️该函数需要在协程中运行
- ⚠️任务必须在调用start之前就安排好

*使用方式为:*

```kotlin
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
```