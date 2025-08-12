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
}
