package io.hhplus.tdd.point

data class UserPoint(
    val id: Long,
    val point: Long,
    val updateMillis: Long,
) {
    fun charge(amount: Long): UserPoint {
        if (amount <= 0) {
            throw IllegalArgumentException("충전할 포인트는 양의 정수여야 합니다.")
        }

        return this.copy(
            point = this.point + amount,
            updateMillis = System.currentTimeMillis(),
        )
    }

    fun reduce(point: Long): UserPoint {
        if (point <= 0) {
            throw IllegalArgumentException("사용할 포인트는 양의 정수여야 합니다.")
        }
        if (this.point < point) {
            throw IllegalArgumentException("포인트가 부족합니다. 현재 포인트: ${this.point}, 사용하려는 포인트: $point")
        }

        return this.copy(
            point = this.point - point,
            updateMillis = System.currentTimeMillis(),
        )
    }
}
