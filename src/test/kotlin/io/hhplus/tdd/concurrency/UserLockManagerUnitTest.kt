package io.hhplus.tdd.concurrency

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.*
import java.util.concurrent.locks.ReentrantLock

/**
 * 기능확인을 위한 단위 테스트
 */
class UserLockManagerUnitTest {

    // private 필드(userIdAndLockMap)에 접근하기 위한 테스트 헬퍼
    @Suppress("UNCHECKED_CAST")
    private fun lockMapOf(target: UserLockManager): ConcurrentHashMap<Long, ReentrantLock> {
        val f = target.javaClass.getDeclaredField("userIdAndLockMap")
        f.isAccessible = true
        return f.get(target) as ConcurrentHashMap<Long, ReentrantLock>
    }

    @Test
    fun `같은 userId는 락 대기 순서대로 실행된다`() {
        val manager = UserLockManager()
        val userId = 100L
        val pool = Executors.newFixedThreadPool(8)

        val leaderStarted = CountDownLatch(1)
        val leaderRelease = CountDownLatch(1)

        // 선행 스레드가 먼저 락을 잡고 다른 스레드들이 큐에 차례로 대기하도록 유도
        pool.submit {
            manager.executeWithLock(userId) {
                leaderStarted.countDown()
                leaderRelease.await(5, TimeUnit.SECONDS)
            }
        }
        assertTrue(leaderStarted.await(2, TimeUnit.SECONDS), "선행 스레드가 시작되지 않음")

        val n = 8
        val acquireOrder = CopyOnWriteArrayList<Int>()

        // 시작 순서를 보장하기 위해 약간의 간격을 두고 제출(공정 락이 큐 순서 보장)
        repeat(n) { i ->
            pool.submit {
                manager.executeWithLock(userId) {
                    acquireOrder.add(i)
                }
            }
            Thread.sleep(5)
        }

        // 큐가 충분히 쌓였다고 판단되면 선행 스레드 해제
        leaderRelease.countDown()

        pool.shutdown()
        assertTrue(pool.awaitTermination(5, TimeUnit.SECONDS), "작업 종료 대기 타임아웃")

        assertEquals((0 until n).toList(), acquireOrder.toList(), "락 대기 순서대로 실행되지 않음")
    }

    @Test
    fun `여러 스레드가 같은 userId로 실행 후 마지막이 끝나면 맵에서 제거된다`() {
        val manager = UserLockManager()
        val userId = 200L
        val pool = Executors.newFixedThreadPool(6)

        val tasks = (0 until 6).map {
            pool.submit(Callable {
                manager.executeWithLock(userId) {
                    // 일부 작업 시간을 두어 큐가 형성되도록 함
                    Thread.sleep(10)
                }
            })
        }
        tasks.forEach { it.get(3, TimeUnit.SECONDS) }

        pool.shutdown()
        assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS))

        val map = lockMapOf(manager)
        assertFalse(map.containsKey(userId), "모든 실행이 끝난 후에도 락이 맵에서 제거되지 않음")
    }

    @Test
    fun `서로 다른 userId는 동시에 실행될 수 있다`() {
        val manager = UserLockManager()
        val pool = Executors.newFixedThreadPool(2)

        val entered1 = CountDownLatch(1)
        val entered2 = CountDownLatch(1)
        val release = CountDownLatch(1)

        pool.submit {
            manager.executeWithLock(1L) {
                entered1.countDown()
                release.await(3, TimeUnit.SECONDS)
            }
        }
        pool.submit {
            manager.executeWithLock(2L) {
                entered2.countDown()
                release.await(3, TimeUnit.SECONDS)
            }
        }

        assertTrue(entered1.await(1, TimeUnit.SECONDS))
        assertTrue(entered2.await(1, TimeUnit.SECONDS))
        // 두 작업 모두 블록 내부에 동시에 진입할 수 있음을 확인
        release.countDown()

        pool.shutdown()
        assertTrue(pool.awaitTermination(3, TimeUnit.SECONDS))
    }

    @Test
    fun `같은 스레드의 같은 userId에 대한 실행도 정상동작하고 정리된다`() {
        val manager = UserLockManager()
        val userId = 300L

        manager.executeWithLock(userId) {
            manager.executeWithLock(userId) {
                // 재진입 시도
                Thread.sleep(5)
            }
        }

        val map = lockMapOf(manager)
        assertFalse(map.containsKey(userId), "재진입 후에도 락이 맵에서 제거되지 않음")
    }
}