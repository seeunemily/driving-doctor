package app.drivingdoctor

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.drivingdoctor.domain.Baseline
import app.drivingdoctor.domain.Change
import app.drivingdoctor.domain.ChangeDetector
import app.drivingdoctor.domain.ConditionAdvisor
import app.drivingdoctor.domain.ConditionCheck
import app.drivingdoctor.domain.Feeling
import app.drivingdoctor.domain.Notice
import app.drivingdoctor.domain.ReportText
import app.drivingdoctor.domain.SampleHistory
import app.drivingdoctor.domain.Signal
import app.drivingdoctor.domain.Trip
import app.drivingdoctor.domain.TripSession
import app.drivingdoctor.domain.TripSnapshot
import app.drivingdoctor.domain.Weekly
import app.drivingdoctor.vehicle.SimulatedVehicleSource
import app.drivingdoctor.vehicle.VehicleEvent
import app.drivingdoctor.vehicle.VehicleSources
import app.drivingdoctor.voice.Speaker
import kotlinx.coroutines.launch

/** 보낸 기록. auto = 설정대로 자동으로 보낸 것 */
data class Sent(val to: String, val what: String, val text: String, val at: Long, val auto: Boolean = true)

/** 가족이 처음 한 번 정해 두는 자동 알림 설정. 할아버지는 아무것도 누르지 않는다. */
enum class AutoRule(val title: String, val whenText: String) {
    HARD_DAY("컨디션 '힘들어'", "대답하면 바로"),
    REST("운전 중 쉬어 가기 권유", "권하면 바로"),
    WEEKLY("주간 리포트", "매주 일요일 저녁 7시"),
}

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val outbox = mutableStateListOf<Sent>()
    val family = listOf("큰딸", "아들", "손녀")
    val recipients = mutableStateListOf("큰딸", "아들")
    val rules = mutableStateListOf(*AutoRule.entries.toTypedArray())

    /** 설정에 맞으면 받는 가족에게 자동으로 보낸다. 시연 차에는 메신저가 없어 보낸 기록에 남긴다. */
    fun autoSend(rule: AutoRule, what: String, text: String) {
        if (rule !in rules || recipients.isEmpty()) return
        outbox.add(0, Sent(recipients.joinToString(", "), what, text, now()))
    }

    fun weeklyText(): String = ReportText.familyMessage(changes)

    val source = VehicleSources.create(app)
    val simulator: SimulatedVehicleSource? = source as? SimulatedVehicleSource
    val speaker = Speaker(app)

    /** 운전 기록은 앱 저장소에 남아 껐다 켜도 이어진다. */
    private val store = TripStore(app)
    private val savedTrips = mutableStateListOf<Trip>().apply { addAll(store.load()) }
    /** 실제 기록으로 평소 기준을 잡을 수 없는 동안(첫 4주)은 시연용 8주 기록을 깔아 둔다. */
    private val sample = SampleHistory.generate(endMillis = now())
    private val liveTrips = mutableStateListOf<Trip>()

    var trip by mutableStateOf(TripSnapshot()); private set
    var lastNotice by mutableStateOf<Notice?>(null); private set
    var check by mutableStateOf<ConditionCheck?>(null); private set
    /** D·R단이면 주행 중. 화면이 주행 화면으로 넘어간다. */
    var moving by mutableStateOf(false); private set
    val finishedTrips: Int get() = liveTrips.size

    private var session = TripSession(now())
    private var askedAt = now()
    private var parked = false

    /** 실제 기록만으로 기준이 서면 실제 기록만, 아니면 시연 기록 위에 실제 기록을 얹는다. */
    val usesSample: Boolean get() = Baseline.from(Weekly.group(savedTrips, now() + 1)) == null
    val weeks: List<List<Trip>> get() =
        Weekly.group(if (usesSample) sample.flatten() + savedTrips else savedTrips.toList(), now() + 1)
    val baseline: Baseline get() = Baseline.from(weeks)!!
    val changes: List<Change> get() = ChangeDetector.compare(baseline, weeks.last())
    fun series(s: Signal): List<Double> = weeks.map { s.value(it) }

    init {
        viewModelScope.launch {
            source.events.collect { e ->
                if (e is VehicleEvent.Gear) { parked = e.park; moving = !e.park }
                val n = session.onEvent(e, now())
                android.util.Log.i("DrivingDoctor", "신호 $e" + (n?.let { " → 안내 ${it.name}" } ?: ""))
                trip = session.snapshot
                if (n != null) {
                    lastNotice = n; speaker.say(n.speech)
                    if (n == Notice.TAKE_A_BREAK) autoSend(AutoRule.REST, "쉬어 가기 알림",
                        "할아버지 운전 중 차량 안전 경고가 반복돼 쉬어 가시길 권했어요. 전화 한 통 부탁드려요.")
                }
                // 차에서는 P에 두고 문을 열면 운전 1회가 끝난 것으로 본다
                if (e == VehicleEvent.DriverDoorOpened && parked && !source.isSimulated && trip.km > 0.1) finishTrip()
            }
        }
        source.start()
        // 주간 리포트: 일요일 저녁 7시가 지나면 그 주에 한 번 자동으로 보낸다
        viewModelScope.launch {
            while (true) {
                val c = java.util.Calendar.getInstance()
                val week = c.get(java.util.Calendar.YEAR) * 100 + c.get(java.util.Calendar.WEEK_OF_YEAR)
                if (c.get(java.util.Calendar.DAY_OF_WEEK) == java.util.Calendar.SUNDAY && c.get(java.util.Calendar.HOUR_OF_DAY) >= 19 && week != weeklySent) {
                    weeklySent = week; autoSend(AutoRule.WEEKLY, "주간 리포트", weeklyText())
                }
                kotlinx.coroutines.delay(60_000)
            }
        }
    }

    private var weeklySent = 0

    /** 다음 주간 리포트 보낼 때(일요일 19:00) */
    fun nextWeekly(): Long {
        val c = java.util.Calendar.getInstance()
        c.set(java.util.Calendar.HOUR_OF_DAY, 19); c.set(java.util.Calendar.MINUTE, 0); c.set(java.util.Calendar.SECOND, 0)
        while (c.get(java.util.Calendar.DAY_OF_WEEK) != java.util.Calendar.SUNDAY || c.timeInMillis < now()) c.add(java.util.Calendar.DAY_OF_MONTH, 1)
        return c.timeInMillis
    }

    fun ask() {
        askedAt = now()
        check = null
        speaker.say("할아버지, 오늘 몸은 좀 어떠세요?")
    }

    fun answer(f: Feeling) {
        check = ConditionCheck(f, now() - askedAt, now())
        speaker.say(ConditionAdvisor.advise(f).speech)
        if (f == Feeling.HARD) autoSend(AutoRule.HARD_DAY, "오늘 컨디션",
            "할아버지가 오늘 컨디션이 좋지 않다고 하셨어요.\n시장 같이 가실 수 있는 분?")
    }

    fun finishTrip() {
        val t = session.finish(now())
        liveTrips.add(t)
        savedTrips.add(t)
        runCatching { store.append(t) }
        session = TripSession(now())
        trip = session.snapshot
        lastNotice = null
        simulator?.depart()
    }

    override fun onCleared() {
        source.stop()
        speaker.shutdown()
    }

    private fun now() = System.currentTimeMillis()
}
