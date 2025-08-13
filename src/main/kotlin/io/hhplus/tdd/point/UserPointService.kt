package io.hhplus.tdd.point

import io.hhplus.tdd.concurrency.UserLockManager
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class UserPointService(
    private val userPointTable: UserPointTable,
    private val pointHistoryTable: PointHistoryTable,
    private val userLockManager: UserLockManager,
) {
    fun getUserPoint(userId: Long): UserPoint =
        userLockManager.withUserLock(userId) {
            userPointTable.selectById(userId)
        }

    fun chargeUserPoint(
        userId: Long,
        point: Long,
    ): UserPoint =
        userLockManager.withUserLock(userId) {
            userPointTable
                .selectById(userId)
                .charge(point)
                .runCatching {
                    userPointTable.insertOrUpdate(
                        id = this.id,
                        amount = this.point,
                    )
                }.onSuccess { userPoint ->
                    pointHistoryTable.insert(
                        id = userPoint.id,
                        amount = point,
                        transactionType = TransactionType.CHARGE,
                        updateMillis = userPoint.updateMillis,
                    )
                }.getOrThrow()
        }

    fun reduceUserPoint(
        userId: Long,
        point: Long,
    ): UserPoint =
        userLockManager.withUserLock(userId) {
            userPointTable
                .selectById(userId)
                .reduce(point)
                .runCatching {
                    userPointTable.insertOrUpdate(
                        id = this.id,
                        amount = this.point,
                    )
                }.onSuccess { userPoint ->
                    pointHistoryTable.insert(
                        id = userPoint.id,
                        amount = point,
                        transactionType = TransactionType.USE,
                        updateMillis = userPoint.updateMillis,
                    )
                }.getOrThrow()
        }

    fun listUserPointHistories(userId: Long): List<PointHistory> =
        userLockManager.withUserLock(userId) {
            pointHistoryTable.selectAllByUserId(userId)
        }
}
