package io.hhplus.tdd.point

import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class UserPointService(private val userPointTable: UserPointTable) {
    fun getUserPoint(userId: Long): UserPoint {
        return userPointTable.selectById(userId)
    }

    fun chargeUserPoint(userId: Long, point: Long): UserPoint {
        return userPointTable.selectById(userId)
            .charge(point)
            .run {
                userPointTable.insertOrUpdate(
                    id = this.id,
                    amount = this.point,
                )
            }
    }
}