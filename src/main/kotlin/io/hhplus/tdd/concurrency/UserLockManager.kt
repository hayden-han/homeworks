package io.hhplus.tdd.concurrency

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock

@Component
class UserLockManager {
    private val userIdAndLockMap = ConcurrentHashMap<Long, ReferenceCountingLock>()

    /**
     * 1. userId-lock이 매핑된 맵에서 사용자 ID에 해당하는 락을 획득
     * 2. 블록 실행 후 락을 해제
     * 3. userId에 대한 락이 더 이상 필요하지 않으면 맵에서 제거
     */
    fun <T> execute(
        userId: Long,
        block: () -> T,
    ): T =
        userIdAndLockMap.acquire(userId)
            .run {
                try {
                    execute(block)
                } finally {
                    userIdAndLockMap.cleanUpIfNotPresent(userId)
                }
            }

    // 맵에 userId에 해당하는 lock을 획득하거나 없다면 새로 생성하여 반환
    private fun ConcurrentHashMap<Long, ReferenceCountingLock>.acquire(userId: Long): ReferenceCountingLock =
        this.compute(userId) { _, existing ->
            (existing ?: ReferenceCountingLock(ReentrantLock()))
        }!!

    // 락이 제거 가능한 상태인지 확인하고 제거
    private fun ConcurrentHashMap<Long, ReferenceCountingLock>.cleanUpIfNotPresent(userId: Long): ReferenceCountingLock? =
        this.compute(userId) { _, existing ->
            // existing이 null인 경우는 이미 다른 스레드에 의해 제거된 경우
            if (existing == null) {
                null
            } else if (existing.isRemovable()) {
                // 제거 가능한 상태를 다시 확인하여 경쟁 조건 방지
                null
            } else {
                existing
            }
        }

    private class ReferenceCountingLock(
        private val lock: ReentrantLock,
        private val referenceCount: AtomicInteger = AtomicInteger(0),
    ) {
        // 락이 제거 가능한(미사용) 상태인지 판단: 보유 X, 대기자 X, 참조 카운트 0
        fun isRemovable(): Boolean = !lock.isLocked && !lock.hasQueuedThreads() && referenceCount.get() == 0

        inline fun <T> execute(block: () -> T): T {
            referenceCount.incrementAndGet()
            lock.lock()
            return try {
                block()
            } finally {
                lock.unlock()
                referenceCount.decrementAndGet()
            }
        }
    }
}
