package app.drivingdoctor.domain

import kotlin.random.Random

/**
 * 시연용 8주 운전 기록. 실제 사람의 기록이 아니다.
 * 처음 4주는 평소 운전, 5주째부터 방향지시등을 켠 채 달리는 일이 조금씩 늘어나는 흐름이다.
 * 신호별 주간 횟수를 정해 두어 시연과 시험이 매번 같다. 거리·시간만 seed로 흔든다.
 */
object SampleHistory {
    private const val DAY = 24 * 60 * 60 * 1000L

    /** 주별 운전 횟수 */
    private val TRIPS = intArrayOf(11, 10, 12, 11, 11, 10, 12, 11)
    /** 주별 신호 횟수 */
    private val TURN = intArrayOf(1, 1, 2, 1, 2, 3, 3, 4)
    private val WARN = intArrayOf(1, 2, 1, 1, 2, 1, 1, 1)
    private val EXIT = intArrayOf(0, 1, 0, 0, 0, 1, 0, 0)
    private val REROUTE = intArrayOf(1, 0, 1, 0, 1, 0, 1, 1)
    private val NIGHT = intArrayOf(0, 1, 0, 1, 0, 1, 0, 1)

    fun generate(seed: Int = 7, endMillis: Long = 1_763_000_000_000L): List<List<Trip>> {
        val rnd = Random(seed)
        val weeks = TRIPS.size
        val start = endMillis - weeks * 7 * DAY
        return (0 until weeks).map { w ->
            val n = TRIPS[w]
            // 횟수를 한 주 안에 고르게 흩어 놓는다
            fun hit(count: Int, i: Int, offset: Int) = (0 until count).any { k -> (k * n / count + offset) % n == i }
            (0 until n).map { i ->
                val km = 3.0 + rnd.nextDouble() * 9.0
                Trip(
                    startMillis = start + w * 7 * DAY + i * (7 * DAY / n),
                    minutes = (km * 3.2 + rnd.nextInt(8)).toInt(),
                    km = km,
                    night = hit(NIGHT[w], i, 5),
                    turnSignalLeftOn = if (hit(TURN[w], i, 0)) 1 else 0,
                    warnings = if (hit(WARN[w], i, 2)) 1 else 0,
                    exitWithoutParkingBrake = if (hit(EXIT[w], i, 7)) 1 else 0,
                    reroutes = if (hit(REROUTE[w], i, 4)) 1 else 0,
                    brakePresses = (km * (2.0 + rnd.nextDouble() * 0.3)).toInt(),
                )
            }
        }
    }
}
