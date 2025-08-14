package io.hhplus.tdd.point

import io.hhplus.tdd.concurrency.UserLockManager
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors

/**
 * UserPointService의 동시성 테스트들
 * - 동일 userId에 대해 동시에 여러번 충전 요청이 들어와도 포인트가 정확히 누적된다.
 * - 동일 userId에 대해 동시에 여러번 차감 요청이 들어와도 포인트가 정확히 차감된다.
 * - 동일 userId에 대해 동시에 여러번 충전과 차감 요청이 들어와도 포인트가 정확히 누적되고 차감된다.
 */
class UserPointServiceConcurrencyTest {
    @Test
    @DisplayName("동일 userId에 대해 동시에 여러번 충전 요청이 들어와도 포인트가 정확히 누적된다")
    fun concurrentChargeUserPoint() {
        // given
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val userLockManager = UserLockManager()
        val service = UserPointService(userPointTable, pointHistoryTable, userLockManager)

        val threadCount = 10
        val chargeAmount = 100L
        val userId = 1L
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        // when
        repeat(threadCount) {
            executor.submit {
                service.chargeUserPoint(userId, chargeAmount)
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()

        // then
        val userPoint = service.getUserPoint(userId)
        assertThat(userPoint.point).isEqualTo(threadCount * chargeAmount)
    }

    @Test
    @DisplayName("동일 userId에 대해 동시에 여러번 차감 요청이 들어와도 포인트가 정확히 차감된다")
    fun concurrentReduceUserPoint() {
        // given
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val userLockManager = UserLockManager()
        val service = UserPointService(userPointTable, pointHistoryTable, userLockManager)

        val threadCount = 10
        val chargeAmount = 100L
        val reduceAmount = 50L
        val userId = 2L

        // 먼저 충분히 충전
        service.chargeUserPoint(userId, threadCount * reduceAmount)

        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)

        // when
        repeat(threadCount) {
            executor.submit {
                service.reduceUserPoint(userId, reduceAmount)
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()

        // then
        val userPoint = service.getUserPoint(userId)
        assertThat(userPoint.point).isEqualTo(0L)
    }

    @Test
    @DisplayName("동일 userId에 대해 동시에 여러번 충전과 차감 요청이 들어와도 포인트가 정확히 누적되고 차감된다")
    fun concurrentChargeAndReduceUserPoint() {
        // given
        val userPointTable = UserPointTable()
        val pointHistoryTable = PointHistoryTable()
        val userLockManager = UserLockManager()
        val service = UserPointService(userPointTable, pointHistoryTable, userLockManager)

        val threadCount = 10
        val chargeAmount = 100L
        val reduceAmount = 50L
        val userId = 3L

        val latch = CountDownLatch(threadCount * 2)
        val executor = Executors.newFixedThreadPool(threadCount)

        // when - 충전과 차감 요청을 번갈아 실행
        repeat(threadCount) {
            executor.submit {
                service.chargeUserPoint(userId, chargeAmount)
                latch.countDown()
            }
            executor.submit {
                service.reduceUserPoint(userId, reduceAmount)
                latch.countDown()
            }
        }
        latch.await()
        executor.shutdown()

        // then
        val expectedPoint = 500L // 충전 10회, 차감 10회 후 최종 포인트는 500L, (10 * 100) - (10 * 50)
        val userPoint = service.getUserPoint(userId)
        assertThat(userPoint.point).isEqualTo(expectedPoint)
    }
}
