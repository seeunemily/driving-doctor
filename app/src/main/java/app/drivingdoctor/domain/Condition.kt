package app.drivingdoctor.domain

/** 출발 전 컨디션 대답 */
enum class Feeling(val label: String) {
    GOOD("괜찮아"),
    TIRED("조금 피곤해"),
    HARD("힘들어"),
}

data class ConditionCheck(val feeling: Feeling, val answerMillis: Long, val atMillis: Long)

/** 대답에 따라 앱이 하는 일 */
data class Advice(val speech: String, val suggestShortRoute: Boolean, val suggestComfort: Boolean, val askFamily: Boolean)

object ConditionAdvisor {
    fun advise(f: Feeling): Advice = when (f) {
        Feeling.GOOD -> Advice("좋아요. 안전하게 다녀오세요.", false, false, false)
        Feeling.TIRED -> Advice("오늘은 가까운 길로 안내할게요. 시트와 실내 온도를 편하게 맞춰 드릴까요?", true, true, false)
        Feeling.HARD -> Advice("가족에게 알렸어요. 대신 모셔다 드릴 수 있는 분이 있는지 물어볼게요.", true, true, true)
    }
}

/**
 * 짧은 말 대답을 세 가지 중 하나로 고른다. 낱말 몇 개를 찾는 단순한 규칙이다.
 * 알아듣지 못하면 null을 돌려주고, 화면 버튼으로 다시 묻는다.
 */
object AnswerParser {
    private val hard = listOf("힘들", "아프", "아파", "못 가", "못가", "어지러", "안 좋", "안좋")
    private val tired = listOf("피곤", "조금", "그냥 그래", "그저 그래", "졸려", "졸리")
    private val good = listOf("괜찮", "좋아", "좋다", "좋네", "멀쩡", "응", "그래", "가자")

    fun parse(text: String): Feeling? {
        val t = text.trim()
        val tight = t.replace(" ", "")
        val has = { words: List<String> -> words.any { t.contains(it) || tight.contains(it.replace(" ", "")) } }
        return when {
            has(hard) -> Feeling.HARD
            has(tired) -> Feeling.TIRED
            has(good) -> Feeling.GOOD
            else -> null
        }
    }
}
