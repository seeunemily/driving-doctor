package app.drivingdoctor.domain

import app.drivingdoctor.vehicle.VehicleEvent
import java.util.Calendar

/** 주행 중 음성으로만 하는 안내 */
enum class Notice(val speech: String) {
    TURN_SIGNAL_LEFT_ON("깜빡이가 아직 켜져 있어요."),
    TAKE_A_BREAK("할아버지, 잠깐 쉬었다 가실까요?"),
    PARKING_BRAKE("내리시기 전에 주차 브레이크를 채워 주세요."),
}

data class TripSnapshot(
    val km: Double = 0.0,
    val turnSignalLeftOn: Int = 0,
    val warnings: Int = 0,
    val exitWithoutParkingBrake: Int = 0,
    val reroutes: Int = 0,
    val brakePresses: Int = 0,
    val turnSignalOn: Boolean = false,
)

/**
 * 운전 1회 동안 차량 신호를 받아 센다. 화면 없이 소리로 안내할 일이 생기면 Notice를 돌려준다.
 * 차량 SDK와 시뮬레이터가 같은 코드를 지난다.
 */
class TripSession(private val startMillis: Long) {

    companion object {
        /** 방향지시등을 켠 채 이 거리 이상 달리면 "켠 채 달림" 1회 */
        const val TURN_SIGNAL_KM = 0.5
        /** 이 시간 안에 경고가 이 횟수 이상이면 쉬어 가기를 권한다 */
        const val WARNING_WINDOW_MS = 10 * 60 * 1000L
        const val WARNING_COUNT = 3
    }

    private var startKm: Double? = null
    private var km = 0.0
    private var turnOnAtKm: Double? = null
    private var turnCounted = false
    private var parkingBrake = false
    private val warningTimes = ArrayDeque<Long>()
    private var breakSuggested = false

    var snapshot = TripSnapshot(); private set

    fun onEvent(e: VehicleEvent, now: Long): Notice? {
        var notice: Notice? = null
        when (e) {
            is VehicleEvent.Distance -> {
                val s = startKm ?: e.totalKm.also { startKm = it }
                km = (e.totalKm - s).coerceAtLeast(0.0)
                val on = turnOnAtKm
                if (on != null && !turnCounted && e.totalKm - on >= TURN_SIGNAL_KM) {
                    turnCounted = true
                    snapshot = snapshot.copy(turnSignalLeftOn = snapshot.turnSignalLeftOn + 1)
                    notice = Notice.TURN_SIGNAL_LEFT_ON
                }
                snapshot = snapshot.copy(km = km)
            }
            is VehicleEvent.TurnSignal -> {
                turnOnAtKm = if (e.on) (startKm ?: 0.0) + km else null
                turnCounted = false
                snapshot = snapshot.copy(turnSignalOn = e.on)
            }
            is VehicleEvent.BrakePedal -> if (e.pressed) snapshot = snapshot.copy(brakePresses = snapshot.brakePresses + 1)
            is VehicleEvent.ParkingBrake -> parkingBrake = e.engaged
            is VehicleEvent.Gear -> Unit
            VehicleEvent.DriverDoorOpened -> if (!parkingBrake) {
                snapshot = snapshot.copy(exitWithoutParkingBrake = snapshot.exitWithoutParkingBrake + 1)
                notice = Notice.PARKING_BRAKE
            }
            is VehicleEvent.Warning -> {
                snapshot = snapshot.copy(warnings = snapshot.warnings + 1)
                warningTimes.addLast(now)
                while (warningTimes.isNotEmpty() && now - warningTimes.first() > WARNING_WINDOW_MS) warningTimes.removeFirst()
                if (!breakSuggested && warningTimes.size >= WARNING_COUNT) {
                    breakSuggested = true
                    notice = Notice.TAKE_A_BREAK
                }
            }
            VehicleEvent.Reroute -> snapshot = snapshot.copy(reroutes = snapshot.reroutes + 1)
        }
        return notice
    }

    fun finish(endMillis: Long): Trip {
        val hour = Calendar.getInstance().apply { timeInMillis = startMillis }.get(Calendar.HOUR_OF_DAY)
        return Trip(
            startMillis = startMillis,
            minutes = ((endMillis - startMillis) / 60_000).toInt(),
            km = snapshot.km,
            night = hour >= 20 || hour < 6,
            turnSignalLeftOn = snapshot.turnSignalLeftOn,
            warnings = snapshot.warnings,
            exitWithoutParkingBrake = snapshot.exitWithoutParkingBrake,
            reroutes = snapshot.reroutes,
            brakePresses = snapshot.brakePresses,
        )
    }
}
