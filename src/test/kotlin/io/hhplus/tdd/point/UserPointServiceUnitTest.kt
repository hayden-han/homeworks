package io.hhplus.tdd.point

import io.hhplus.tdd.concurrency.NoOpUserLockManager
import io.hhplus.tdd.database.PointHistoryTable
import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class UserPointServiceUnitTest {
    /**
     * UserPointService의 getUserPoint 메서드에 대한 테스트들
     * - 테이블에 존재하지않는 유저의 경우 포인트를 조회하면 0포인트가 조회된다.
     * - 테이블에 존재하는 유저의 경우 포인트를 조회할때 저장된 값이 반환된다.
     */
    @Test
    @DisplayName("테이블에 존재하지않는 유저의 경우 포인트를 조회하면 0포인트가 조회된다")
    fun getNoUserPoint() {
        // given
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)
        `when`(
            mockUserPointTable.selectById(1L),
        ).thenReturn(UserPoint(id = 1L, point = 0L, updateMillis = 2000L))

        // when
        val userPoint = userPointService.getUserPoint(1L)

        // then
        assertThat(userPoint.id).isEqualTo(1L)
        assertThat(userPoint.point).isEqualTo(0L)
    }

    /**
     * 정상조회 케이스
     */
    @Test
    @DisplayName("테이블에 존재하는 유저의 경우 포인트를 조회할때 저장된 값이 반환된다.")
    fun getExistUserPoint() {
        // given
        val stubUser =
            UserPoint(
                id = 2L,
                point = 100L,
                updateMillis = 10000L,
            )
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)

        `when`(
            mockUserPointTable.selectById(2L),
        ).thenReturn(stubUser)

        // when
        val userPoint = userPointService.getUserPoint(2L)

        // then
        assertThat(userPoint.id).isEqualTo(2L)
        assertThat(userPoint.point).isEqualTo(100L)
        assertThat(userPoint.updateMillis).isEqualTo(10000L)
    }

    /**
     * UserPointService의 chargeUserPoint 메서드에 대한 테스트들
     * - 테이블에 존재하는 유저의 경우 포인트를 충전하면 기존 포인트에 충전된 포인트가 더해진다.
     * - 테이블에 존재하지않는 유저의 경우 포인트를 충전하면 충전한 포인트만 저장된다.
     * - 0 이하의 포인트를 충전하는 경우 IllegalArgumentException이 발생한다.
     */
    @Test
    @DisplayName("테이블에 존재하는 유저의 경우 포인트를 충전하면 기존 포인트에 충전된 포인트가 더해진다.")
    fun chargeExistingUserPoint() {
        // given
        val stubUser =
            UserPoint(
                id = 3L,
                point = 1L,
                updateMillis = 10000L,
            )
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)
        `when`(
            mockUserPointTable.selectById(3L),
        ).thenReturn(stubUser)
        `when`(
            mockUserPointTable.insertOrUpdate(3L, 4L),
        ).thenReturn(UserPoint(3L, 4L, 20000L))
        `when`(
            mockPointHistoryTable.insert(3L, 3L, TransactionType.CHARGE, 20000L),
        ).thenReturn(PointHistory(1L, 3L, TransactionType.CHARGE, 4L, 20000L))

        // when
        val userPoint = userPointService.chargeUserPoint(3L, 3L)

        // then
        assertThat(userPoint.id).isEqualTo(3L)
        assertThat(userPoint.point).isEqualTo(4L)
        assertThat(userPoint.updateMillis).isEqualTo(20000L)
        verify(mockPointHistoryTable, times(1))
            .insert(3L, 3L, TransactionType.CHARGE, 20000L)
    }

    @Test
    @DisplayName("테이블에 존재하지않는 유저의 경우 포인트를 충전하면 충전한 포인트만 저장된다")
    fun chargeNotExistingUserPoint() {
        // given
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)
        `when`(
            mockUserPointTable.selectById(4L),
        ).thenReturn(UserPoint(4L, 0L, 10000L))
        `when`(
            mockUserPointTable.insertOrUpdate(4L, 999L),
        ).thenReturn(UserPoint(4L, 999L, 20000L))
        `when`(
            mockPointHistoryTable.insert(4L, 999L, TransactionType.CHARGE, 20000L),
        ).thenReturn(PointHistory(2L, 4L, TransactionType.CHARGE, 999L, 20000L))

        // when
        val userPoint = userPointService.chargeUserPoint(4L, 999L)

        // then
        assertThat(userPoint.id).isEqualTo(4L)
        assertThat(userPoint.point).isEqualTo(999L)
        assertThat(userPoint.updateMillis).isEqualTo(20000L)
        verify(mockPointHistoryTable, times(1))
            .insert(4L, 999L, TransactionType.CHARGE, 20000L)
    }

    @ParameterizedTest
    @ValueSource(longs = [0, -1, -100, -999, Long.MIN_VALUE])
    @DisplayName("0 이하의 포인트를 충전하는 경우 IllegalArgumentException이 발생한다")
    fun chargeZeroPoint(amount: Long) {
        // given
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)
        `when`(
            mockUserPointTable.selectById(5L),
        ).thenReturn(UserPoint(5L, 0L, 10000L))

        // when
        val exception =
            assertThrows<IllegalArgumentException> {
                userPointService.chargeUserPoint(5L, amount)
            }

        // then
        assertThat(exception).message().isEqualTo("충전할 포인트는 양의 정수여야 합니다.")
    }

    /**
     * UserPointService의 reduceUserPoint 메서드에 대한 테스트들
     * - 저장된 포인트 이하를 사용하면 포인트가 차감된다.
     * - 저장된 포인트를 초과해 사용하면 IllegalArgumentException이 발생한다
     * - 0 이하의 포인트를 사용하는 경우 IllegalArgumentException이 발생한다.
     */
    @Test
    @DisplayName("저장된 포인트 이하를 사용하면 포인트가 차감된다")
    fun reducePointWithinBalanceDeductsPoint() {
        // given
        val stubUser =
            UserPoint(
                id = 6L,
                point = 100L,
                updateMillis = 10000L,
            )
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)
        `when`(
            mockUserPointTable.selectById(6L),
        ).thenReturn(stubUser)
        `when`(
            mockUserPointTable.insertOrUpdate(6L, 0L),
        ).thenReturn(UserPoint(6L, 0L, 20000L))
        `when`(
            mockPointHistoryTable.insert(6L, 100L, TransactionType.USE, 20000L),
        ).thenReturn(PointHistory(3L, 6L, TransactionType.USE, 100L, 20000L))

        // when
        val userPoint = userPointService.reduceUserPoint(6L, 100L)

        // then
        assertThat(userPoint.id).isEqualTo(6L)
        assertThat(userPoint.point).isEqualTo(0L)
        assertThat(userPoint.updateMillis).isEqualTo(20000L)
        verify(mockPointHistoryTable, times(1))
            .insert(6L, 100L, TransactionType.USE, 20000L)
    }

    @Test
    @DisplayName("저장된 포인트를 초과해 사용하면 IllegalArgumentException이 발생한다")
    fun reducePointExceedingBalanceThrowsIllegalArgumentException() {
        // given
        val stubUser =
            UserPoint(
                id = 7L,
                point = 100L,
                updateMillis = 10000L,
            )
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)
        `when`(
            mockUserPointTable.selectById(7L),
        ).thenReturn(stubUser)

        // when
        val exception =
            assertThrows<IllegalArgumentException> {
                userPointService.reduceUserPoint(7L, 101L)
            }

        // then
        assertThat(exception.message).isEqualTo("포인트가 부족합니다. 현재 포인트: 100, 사용하려는 포인트: 101")
    }

    @ParameterizedTest
    @ValueSource(longs = [0, -1, -100, -999, Long.MIN_VALUE])
    @DisplayName("0 이하의 포인트를 사용하는 경우 IllegalArgumentException이 발생한다")
    fun reduceZeroOrNegativePointThrowsIllegalArgumentException(amount: Long) {
        // given
        val mockUserPointTable = mock<UserPointTable>()
        val mockPointHistoryTable = mock<PointHistoryTable>()
        val noOpUserLockManager = NoOpUserLockManager()
        val userPointService = UserPointService(mockUserPointTable, mockPointHistoryTable, noOpUserLockManager)
        `when`(
            mockUserPointTable.selectById(8L),
        ).thenReturn(UserPoint(8L, 1000, 10000L))
        // when
        val exception =
            assertThrows<IllegalArgumentException> {
                userPointService.reduceUserPoint(8L, amount)
            }
        // then
        assertThat(exception).message().isEqualTo("사용할 포인트는 양의 정수여야 합니다.")
    }
}
