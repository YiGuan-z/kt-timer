package com.cqsd.timer

import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

/**
 *
 * @author caseycheng
 * @date 2023/7/17-13:12
 * @doc
 **/
class Timer {
    private var jobQueue: Array<JobPair?> = Array(128) { null }
    var startFlag: Boolean = false
        private set

    private fun getNext(): Int {
        var count = 0
        synchronized(jobQueue) {
            for (pair in jobQueue) {
                count++
                if (pair == null) {
                    return count - 1
                }
            }
            jobQueue = Array(jobQueue.size * 2) { runCatching { jobQueue[it] }.getOrNull() }
        }
        if (count >= jobQueue.size) {
            throw RuntimeException("jobQueue is full")
        }
        return count + 1
    }

    private fun push(job: JobPair): Int {
        synchronized(jobQueue) {
            val id = getNext()
            if (startFlag) throw RuntimeException("对象已封闭，请不要再添加内容")
            jobQueue[id] = job
            return id
        }
    }

    private suspend fun launchJob(job: JobPair) {
        val (f, s) = job
        while (f.second) {
            delay(f.first)
            s.first()
        }
    }

    /**
     * 添加一个设定的工作
     * @throws RuntimeException 当[start]方法调用后，该对象就会进行封闭操作，不可再向内部写入工作。
     */
    fun addJob(unit: TimeUnit, time: Long, job: MJob): JobId {
        return push(VarPair(unit.toMillis(time), true) to VarPair(job, null))
    }

    /**
     * 给定一个id，用于取消该定时任务的工作
     */
    fun cancel(id: JobId) {
        synchronized(jobQueue) {
            val job = runCatching { jobQueue[id] }.getOrNull()
            if (job != null) {
                job.first.second = false
                job.second.second!!.cancel()
            }
        }
    }

    /**
     * 重新给一个任务设定工作时间
     * @throws RuntimeException 任务id找不到对应任务时
     */
    fun changeJobTime(jobId: JobId, unit: TimeUnit, time: Long) {
        synchronized(jobQueue) {
            val job = jobQueue[jobId]
            requireNotNull(job) { "通过$jobId 无法找到对应的工作！！！" }
            job.first.first = unit.toMillis(time)
        }
    }

    /**
     * 启动定时器
     */
    suspend fun start() {
        startFlag = true
        withContext(Dispatchers.IO) {
            synchronized(jobQueue) {
                for (pair in jobQueue) {
                    if (pair != null) {
                        val job = launch {
                            launchJob(pair)
                        }
                        pair.second.second = job
                    }
                }
            }
        }
    }

    /**
     * 取消里面的所有任务
     */
    fun cancelAll() {
        synchronized(jobQueue) {
            for (pair in jobQueue) {
                if (pair == null) continue
                val (conf, job) = pair
                val (_, flag) = conf
                if (flag) {
                    conf.second = false
                    job.second!!.cancel()
                }
            }
        }
    }

    /**
     * 清除里面的所有任务
     */
    fun clear() {
        synchronized(jobQueue) {
            cancelAll()
            for (i in 0..jobQueue.lastIndex) {
                if (jobQueue[i] != null) {
                    jobQueue[i] = null
                }
            }
            startFlag = false
        }
    }
}


typealias MJob = suspend () -> Unit
typealias JobPair = Pair<VarPair<Long, Boolean>, VarPair<MJob, Job?>>
typealias JobId = Int

data class VarPair<A, B>(var first: A, var second: B)