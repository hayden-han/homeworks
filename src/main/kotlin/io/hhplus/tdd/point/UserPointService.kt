package io.hhplus.tdd.point

import io.hhplus.tdd.database.UserPointTable
import org.springframework.stereotype.Service

@Service
class UserPointService(private val userPointTable: UserPointTable) {
    fun getUserPoint(userId: Long): UserPoint {
        // TODO: 특정 유저의 포인트를 조회하는 기능을 작성해주세요.
        return UserPoint(0, 0, 0)
    }
}