package app.drivingdoctor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripLogTest {

    @Test fun `저장한 기록을 그대로 읽는다`() {
        val t = Trip(1_700_000_000_000L, 23, 7.25, true, 1, 2, 0, 1, 15)
        assertEquals(t, TripCodec.decode(TripCodec.encode(t)))
    }

    @Test fun `깨진 줄은 버린다`() {
        assertNull(TripCodec.decode("1,2,3"))
        assertNull(TripCodec.decode("a,b,c,d,e,f,g,h,i"))
    }

    @Test fun `기록을 날짜로 주에 나눈다`() {
        val end = 100 * Weekly.WEEK
        val thisWeek = Trip(end - 1000, 10, 3.0, false)
        val lastWeek = Trip(end - Weekly.WEEK - 1000, 10, 3.0, false)
        val tooOld = Trip(end - 9 * Weekly.WEEK, 10, 3.0, false)
        val weeks = Weekly.group(listOf(thisWeek, lastWeek, tooOld), end)
        assertEquals(8, weeks.size)
        assertEquals(listOf(thisWeek), weeks[7])
        assertEquals(listOf(lastWeek), weeks[6])
        assertEquals(2, weeks.sumOf { it.size })
    }

    @Test fun `시연 기록을 날짜로 다시 나눠도 주별 구성이 같다`() {
        val end = 3000 * Weekly.WEEK
        val weeks = SampleHistory.generate(endMillis = end)
        assertEquals(weeks, Weekly.group(weeks.flatten(), end))
    }
}
