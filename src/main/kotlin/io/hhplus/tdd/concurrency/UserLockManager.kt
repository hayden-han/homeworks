package io.hhplus.tdd.concurrency

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock

@Component
class UserLockManager {
    private val userIdAndLockMap = ConcurrentHashMap<Long, ReentrantLock>()

    /**
     * 1. userId-lock이 매핑된 맵에서 사용자 ID에 해당하는 락을 획득
     * 2. 블록 실행 후 락을 해제
     * 3. userId에 대한 락이 더 이상 필요하지 않으면 맵에서 제거
     */
    fun <T> executeWithLock(
        userId: Long,
        block: () -> T,
    ): T =
        userIdAndLockMap.getOrCreateLock(userId).let { lock ->
            try {
                lock.withLock(block)
            } finally {
                userIdAndLockMap.removeIfIdle(userId)
            }
        }

    // 맵에 userId에 해당하는 lock을 획득하거나 없다면 새로 생성하여 반환
    private fun ConcurrentHashMap<Long, ReentrantLock>.getOrCreateLock(userId: Long): ReentrantLock =
        this.compute(userId) { _, existing ->
            (existing ?: ReentrantLock(true)) // 공정 락(FIFO)로 생성하여 대기 순서 보장
        }!!

    private fun <T> ReentrantLock.withLock(block: () -> T): T {
        this.lock()
        return try {
            block()
        } finally {
            this.unlock()
        }
    }

    private fun ConcurrentHashMap<Long, ReentrantLock>.removeIfIdle(userId: Long): ReentrantLock? =
        this.computeIfPresent(userId) { _, existing ->
            if (existing.isLocked || existing.hasQueuedThreads()) existing else null
        }
}
