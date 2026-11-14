package app.drivingdoctor.domain

/**
 * 운전 기록을 한 줄 글자로 바꿔 저장하고 다시 읽는다. 기록은 차 안(앱 저장소)에만 둔다.
 * 형식: 시작시각,분,km,야간(0/1),깜빡이,경고,하차,재탐색,브레이크
 */
object TripCodec {
    fun encode(t: Trip): String = listOf(
        t.startMillis, t.minutes, "%.3f".format(java.util.Locale.ROOT, t.km), if (t.night) 1 else 0,
        t.turnSignalLeftOn, t.warnings, t.exitWithoutParkingBrake, t.reroutes, t.brakePresses,
    ).joinToString(",")

    /** 깨진 줄은 버린다(null) */
    fun decode(line: String): Trip? {
        val p = line.trim().split(",")
        if (p.size != 9) return null
        return try {
            Trip(
                startMillis = p[0].toLong(), minutes = p[1].toInt(), km = p[2].toDouble(), night = p[3] == "1",
                turnSignalLeftOn = p[4].toInt(), warnings = p[5].toInt(), exitWithoutParkingBrake = p[6].toInt(),
                reroutes = p[7].toInt(), brakePresses = p[8].toInt(),
            )
        } catch (e: NumberFormatException) { null }
    }
}

/** 운전 기록을 주 단위로 묶는다. 마지막 칸이 이번 주다. */
object Weekly {
    const val WEEK = 7 * 24 * 60 * 60 * 1000L

    /** weekEnd: 이번 주가 끝나는 시각. 이보다 weeks주 이전 기록은 버린다. */
    fun group(trips: List<Trip>, weekEnd: Long, weeks: Int = 8): List<List<Trip>> {
        val out = List(weeks) { mutableListOf<Trip>() }
        trips.forEach { t ->
            if (t.startMillis >= weekEnd) return@forEach
            val back = ((weekEnd - 1 - t.startMillis) / WEEK).toInt()
            if (back < weeks) out[weeks - 1 - back].add(t)
        }
        return out.map { w -> w.sortedBy { it.startMillis } }
    }
}
