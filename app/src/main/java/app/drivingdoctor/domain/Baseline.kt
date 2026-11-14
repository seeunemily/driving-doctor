package app.drivingdoctor.domain

import kotlin.math.sqrt

/** 신호 하나의 기준선: 처음 4주 주간 값의 평균과 흔들림(표준편차) */
data class SignalBaseline(val mean: Double, val sd: Double)

/**
 * 나만의 기준선. 다른 사람과 비교하지 않고, 이 운전자의 처음 4주를 기준으로 삼는다.
 */
class Baseline private constructor(private val bySignal: Map<Signal, SignalBaseline>) {

    operator fun get(signal: Signal): SignalBaseline = bySignal.getValue(signal)

    companion object {
        const val WEEKS = 4
        /** 4주 동안 이보다 적게 운전했으면 기준선을 만들지 않는다 */
        const val MIN_TRIPS = 8

        /** weeks: 주 단위로 묶은 운전 기록. 앞의 4주를 쓴다. 자료가 모자라면 null */
        fun from(weeks: List<List<Trip>>): Baseline? {
            if (weeks.size < WEEKS) return null
            val base = weeks.take(WEEKS)
            if (base.sumOf { it.size } < MIN_TRIPS || base.any { it.isEmpty() }) return null
            val map = Signal.entries.associateWith { s ->
                val values = base.map { s.value(it) }
                val mean = values.average()
                val sd = sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
                SignalBaseline(mean, sd)
            }
            return Baseline(map)
        }
    }
}

enum class Level { SAME, WATCH, CHANGED }

data class Change(
    val signal: Signal,
    val baseline: Double,
    val now: Double,
    val level: Level,
) {
    /** 기준선 대비 배수. 기준선이 0이면 null */
    val ratio: Double? get() = if (baseline > 1e-9) now / baseline else null
}

/**
 * 이번 주 값이 기준선에서 얼마나 달라졌는지 판정한다. 진단이 아니라 "늘었다/그대로다"만 말한다.
 *
 * - CHANGED: 기준선보다 minChange 이상 크고, 1.5배 이상이며, 평소 흔들림(2 표준편차)도 넘을 때
 * - WATCH: 기준선보다 minChange 이상 크고, 1.2배 이상일 때
 * - SAME: 그 밖
 * 줄어든 변화는 알리지 않는다(좋아진 것은 걱정거리가 아니다).
 */
object ChangeDetector {
    fun compare(baseline: Baseline, week: List<Trip>): List<Change> =
        Signal.entries.map { s -> judge(s, baseline[s], s.value(week)) }

    fun judge(signal: Signal, b: SignalBaseline, now: Double): Change {
        val diff = now - b.mean
        val ratioOk = { k: Double -> b.mean < 1e-9 || now >= b.mean * k }
        val noise = maxOf(b.sd, signal.minChange / 2)
        val level = when {
            diff >= signal.minChange && ratioOk(1.5) && diff >= 2 * noise -> Level.CHANGED
            diff >= signal.minChange && ratioOk(1.2) -> Level.WATCH
            else -> Level.SAME
        }
        return Change(signal, b.mean, now, level)
    }
}
