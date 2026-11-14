package app.drivingdoctor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BaselineTest {

    private fun trip(turn: Int = 0, warn: Int = 0, km: Double = 5.0, brakes: Int = 10, night: Boolean = false, min: Int = 20) =
        Trip(0L, min, km, night, turnSignalLeftOn = turn, warnings = warn, brakePresses = brakes)

    /** 10번 운전 중 n번 켠 채 달린 한 주 */
    private fun week(turnTimes: Int, size: Int = 10) = List(size) { i -> trip(turn = if (i < turnTimes) 1 else 0) }

    @Test fun `4주가 안 되면 기준선을 만들지 않는다`() {
        assertNull(Baseline.from(List(3) { week(1) }))
    }

    @Test fun `빈 주가 있으면 기준선을 만들지 않는다`() {
        assertNull(Baseline.from(listOf(week(1), emptyList(), week(1), week(1))))
    }

    @Test fun `기준선은 처음 4주 평균이다`() {
        val b = Baseline.from(listOf(week(1), week(1), week(2), week(0), week(9)))
        assertNotNull(b)
        // 운전 10회당 1, 1, 2, 0 → 평균 1.0 (5주째 9는 기준선에 들어가지 않는다)
        assertEquals(1.0, b!![Signal.TURN_SIGNAL].mean, 1e-9)
    }

    @Test fun `평소와 같으면 SAME`() {
        val b = Baseline.from(List(4) { week(1) })!!
        val c = ChangeDetector.compare(b, week(1)).first { it.signal == Signal.TURN_SIGNAL }
        assertEquals(Level.SAME, c.level)
    }

    @Test fun `3배로 늘면 CHANGED, 배수도 3`() {
        val b = Baseline.from(List(4) { week(1) })!!
        val c = ChangeDetector.compare(b, week(3)).first { it.signal == Signal.TURN_SIGNAL }
        assertEquals(Level.CHANGED, c.level)
        assertEquals(3.0, c.ratio!!, 1e-9)
    }

    @Test fun `작은 숫자의 흔들림은 알리지 않는다`() {
        // 기준선 1.0 → 이번 주 1.5: 1.5배지만 운전 10회당 0.5회 차이라 우연일 수 있다
        val b = Baseline.from(List(4) { week(1) })!!
        val c = ChangeDetector.judge(Signal.TURN_SIGNAL, b[Signal.TURN_SIGNAL], 1.5)
        assertEquals(Level.SAME, c.level)
    }

    @Test fun `평소 흔들림이 큰 사람은 더 크게 달라져야 CHANGED`() {
        // 기준선이 0, 4, 0, 4 처럼 들쭉날쭉하면(평균 2, 표준편차 2) 4는 평소 범위다
        val b = Baseline.from(listOf(week(0), week(4), week(0), week(4)))!!
        val c = ChangeDetector.compare(b, week(4)).first { it.signal == Signal.TURN_SIGNAL }
        assertTrue(c.level != Level.CHANGED)
    }

    @Test fun `줄어든 변화는 알리지 않는다`() {
        val b = Baseline.from(List(4) { week(4) })!!
        val c = ChangeDetector.compare(b, week(0)).first { it.signal == Signal.TURN_SIGNAL }
        assertEquals(Level.SAME, c.level)
    }

    @Test fun `기준선이 0이던 일이 생기면 배수 없이 늘었다고 말한다`() {
        val b = Baseline.from(List(4) { week(0) })!!
        val c = ChangeDetector.compare(b, week(3)).first { it.signal == Signal.TURN_SIGNAL }
        assertEquals(Level.CHANGED, c.level)
        assertNull(c.ratio)
        assertEquals("방향지시등을 켠 채 달린 일이 지난 4주에는 거의 없다가 이번 주 늘었어요", ReportText.sentence(c))
    }

    @Test fun `브레이크는 km당 횟수로 본다`() {
        val v = Signal.BRAKE.value(listOf(trip(km = 4.0, brakes = 8), trip(km = 6.0, brakes = 12)))
        assertEquals(2.0, v, 1e-9)
    }

    @Test fun `야간 또는 1시간 넘는 운전 비중`() {
        val v = Signal.NIGHT_LONG.value(listOf(trip(night = true), trip(min = 70), trip(), trip()))
        assertEquals(50.0, v, 1e-9)
    }

    @Test fun `시연 기록은 방향지시등 신호만 달라진다`() {
        val weeks = SampleHistory.generate()
        val b = Baseline.from(weeks)!!
        val changes = ChangeDetector.compare(b, weeks.last())
        val changed = changes.filter { it.level == Level.CHANGED }.map { it.signal }
        assertEquals(listOf(Signal.TURN_SIGNAL), changed)
        assertEquals("평소와 달라진 신호 1가지", ReportText.headline(changes))
    }
}
