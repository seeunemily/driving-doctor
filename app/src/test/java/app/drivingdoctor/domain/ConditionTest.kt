package app.drivingdoctor.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConditionTest {

    @Test fun `짧은 대답을 세 가지로 고른다`() {
        assertEquals(Feeling.GOOD, AnswerParser.parse("괜찮다"))
        assertEquals(Feeling.GOOD, AnswerParser.parse("응 좋아"))
        assertEquals(Feeling.TIRED, AnswerParser.parse("조금 피곤하네"))
        assertEquals(Feeling.HARD, AnswerParser.parse("오늘은 좀 힘들다"))
        assertEquals(Feeling.HARD, AnswerParser.parse("몸이 안 좋아"))
        assertEquals(Feeling.HARD, AnswerParser.parse("몸이 안좋아"))
    }

    @Test fun `알아듣지 못하면 버튼으로 다시 묻는다`() {
        assertNull(AnswerParser.parse("시장"))
    }

    @Test fun `힘든 날은 가족에게 먼저 알린다`() {
        val a = ConditionAdvisor.advise(Feeling.HARD)
        assertTrue(a.askFamily && a.suggestShortRoute)
        assertTrue(!ConditionAdvisor.advise(Feeling.GOOD).askFamily)
    }

    @Test fun `가족에게 보내는 글은 진단이 아니라고 밝힌다`() {
        val weeks = SampleHistory.generate()
        val changes = ChangeDetector.compare(Baseline.from(weeks)!!, weeks.last())
        val msg = ReportText.familyMessage(changes)
        assertTrue(msg.contains("방향지시등"))
        assertTrue(msg.contains("진단이 아닌 운전 기록"))
    }
}
