package io.hhplus.tdd.concurrency

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Component
class UserLockManager {
    private val userLocks = ConcurrentHashMap<Long, ReentrantLock>()

    fun <T> withUserLock(
        userId: Long,
        block: () -> T,
    ): T {
        val lock = userLocks.computeIfAbsent(userId) { ReentrantLock() }
        lock.lock()
        try {
            return block()
        } finally {
            lock.unlock()
        }
    }
}
