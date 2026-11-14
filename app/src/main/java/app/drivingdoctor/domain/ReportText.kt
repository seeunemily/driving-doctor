package app.drivingdoctor.domain

import kotlin.math.roundToInt

/** 리포트 문장. 사실만 말하고 진단이나 평가는 하지 않는다. */
object ReportText {

    fun sentence(c: Change): String {
        val what = when (c.signal) {
            Signal.TURN_SIGNAL -> "방향지시등을 켠 채 달린 일이"
            Signal.SAFETY_WARNING -> "차량 안전 경고가"
            Signal.EXIT_HABIT -> "주차 브레이크 없이 문을 연 일이"
            Signal.LOST_WAY -> "경로를 다시 찾은 일이"
            Signal.NIGHT_LONG -> "야간·장시간 운전 비중이"
            Signal.BRAKE -> "브레이크를 밟는 횟수가"
        }
        if (c.level == Level.SAME) return "${c.signal.title}: 평소와 같아요"
        val r = c.ratio
        val how = when {
            r == null -> "지난 4주에는 거의 없다가 이번 주 늘었어요"
            r >= 1.95 -> "평소보다 ${r.roundToInt()}배 늘었어요"
            else -> "평소보다 ${((r - 1) * 100).roundToInt()}% 늘었어요"
        }
        return "$what $how"
    }

    /** 주간 리포트 맨 위 한 줄. 가장 크게 달라진 신호 하나만 말한다. */
    fun headline(changes: List<Change>): String {
        val n = changes.count { it.level != Level.SAME }
        return if (n == 0) "이번 주 운전은 평소와 같아요" else "평소와 달라진 신호 ${n}가지"
    }

    fun topChange(changes: List<Change>): Change? = changes.filter { it.level != Level.SAME }
        .maxByOrNull { (it.ratio ?: 10.0) + if (it.level == Level.CHANGED) 100 else 0 }

    /** 가족·진료실에 보내는 글. 할아버지가 허락한 요약만 담는다. */
    fun familyMessage(changes: List<Change>): String {
        val changed = changes.filter { it.level != Level.SAME }
        val sb = StringBuilder("[운전 주치의 주간 리포트]\n")
        if (changed.isEmpty()) sb.append("이번 주 운전은 평소와 같았어요.\n")
        else changed.forEach { sb.append("• ").append(sentence(it)).append('\n') }
        sb.append("평소 = 처음 4주 운전 기록. 진단이 아닌 운전 기록입니다.")
        return sb.toString()
    }

    fun fmt(v: Double): String = if (v >= 10) v.roundToInt().toString() else "%.1f".format(v)
}
