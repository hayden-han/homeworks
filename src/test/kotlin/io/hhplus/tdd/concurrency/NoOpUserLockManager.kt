package io.hhplus.tdd.concurrency

/**
 * No-op implementation of UserLockManager for testing purposes.
 * This class does not perform any locking and simply executes the block of code.
 * It is useful for unit tests where locking behavior is not required.
 */
class NoOpUserLockManager : UserLockManager() {
    override fun <T> execute(userId: Long, block: () -> T): T = block()
}
