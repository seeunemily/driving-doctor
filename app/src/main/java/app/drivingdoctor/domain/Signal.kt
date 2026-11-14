package app.drivingdoctor.domain

/** 운전 1회 기록. 이름·위치 같은 개인 정보는 담지 않는다. */
data class Trip(
    val startMillis: Long,
    val minutes: Int,
    val km: Double,
    val night: Boolean,
    /** 방향지시등을 켠 채 일정 거리 이상 달린 횟수 */
    val turnSignalLeftOn: Int = 0,
    /** 안전 경고 횟수 */
    val warnings: Int = 0,
    /** 주차 브레이크를 채우지 않고 문을 연 횟수 */
    val exitWithoutParkingBrake: Int = 0,
    /** 경로 재탐색 횟수 */
    val reroutes: Int = 0,
    /** 브레이크 페달을 밟은 횟수 */
    val brakePresses: Int = 0,
)

/**
 * 운전 주치의가 보는 6가지 변화 신호.
 * value(): 한 주 운전 기록을 하나의 값으로 바꾼다. 운전 횟수·거리가 달라도 비교할 수 있게 비율로 만든다.
 */
enum class Signal(
    val title: String,
    val unit: String,
    val sdk: String,
    /** 늘었다고 말하려면 기준선보다 최소 이만큼은 커야 한다(작은 숫자의 우연한 흔들림을 거른다) */
    val minChange: Double,
) {
    TURN_SIGNAL("방향지시등을 켠 채 달림", "회 / 운전 10회", "TurnSignal · Odometer", 1.0),
    SAFETY_WARNING("차량 안전 경고", "회 / 운전 10회", "Safety", 1.0),
    EXIT_HABIT("주차 브레이크 없이 문 열림", "회 / 운전 10회", "Safety · Door", 1.0),
    LOST_WAY("경로 재탐색", "회 / 운전 10회", "NaviHelper", 1.0),
    NIGHT_LONG("야간·1시간 넘는 운전", "% / 전체 운전", "Odometer", 10.0),
    BRAKE("브레이크 밟는 빈도", "회 / km", "Brake · Odometer", 0.3);

    fun value(trips: List<Trip>): Double {
        if (trips.isEmpty()) return 0.0
        val per10 = { f: (Trip) -> Int -> trips.sumOf(f) * 10.0 / trips.size }
        return when (this) {
            TURN_SIGNAL -> per10 { it.turnSignalLeftOn }
            SAFETY_WARNING -> per10 { it.warnings }
            EXIT_HABIT -> per10 { it.exitWithoutParkingBrake }
            LOST_WAY -> per10 { it.reroutes }
            NIGHT_LONG -> trips.count { it.night || it.minutes >= 60 } * 100.0 / trips.size
            BRAKE -> {
                val km = trips.sumOf { it.km }
                if (km <= 0.0) 0.0 else trips.sumOf { it.brakePresses } / km
            }
        }
    }
}
