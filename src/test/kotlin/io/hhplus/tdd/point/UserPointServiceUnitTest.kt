package io.hhplus.tdd.point

import io.hhplus.tdd.database.UserPointTable
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class UserPointServiceUnitTest {
    /**
     * UserPointTable의 구현을 보니 저장되지않은 회원의 경우 id와 point가 0인 UserPoint를 반환하는 것을 확인.
     * 의도한 시나리오라고 생각되어 검증하는 테스트를 작성.
     */
    @Test
    @DisplayName("테이블에 존재하지않는 유저의 경우 포인트를 조회하면 0포인트가 조회된다")
    fun getNoUserPoint() {
        // given
        val mockUserPointTable = mock<UserPointTable>()
        val userPointService = UserPointService(mockUserPointTable)
        `when`(
            mockUserPointTable.selectById(1L)
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
        val stubUser = UserPoint(
            id = 2L,
            point = 100L,
            updateMillis = 10000L,
        )
        val mockUserPointTable = mock<UserPointTable>()
        val userPointService = UserPointService(mockUserPointTable)

        `when`(
            mockUserPointTable.selectById(2L)
        ).thenReturn(stubUser)

        // when
        val userPoint = userPointService.getUserPoint(2L)

        // then
        assertThat(userPoint.id).isEqualTo(2L)
        assertThat(userPoint.point).isEqualTo(100L)
        assertThat(userPoint.updateMillis).isEqualTo(10000L)
    }
}