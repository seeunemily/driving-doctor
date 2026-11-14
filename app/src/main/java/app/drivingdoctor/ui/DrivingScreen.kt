package app.drivingdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import app.drivingdoctor.domain.Notice
import app.drivingdoctor.domain.TripSession
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drivingdoctor.AppViewModel
import app.drivingdoctor.domain.ReportText

/** 장면 2·5: 주행 중에는 화면을 쓰지 않는다. 큰 글씨 상태와 마지막 음성 안내만 보인다. */
@Composable
fun DrivingScreen(vm: AppViewModel) {
    val t = vm.trip
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Panel(Modifier.fillMaxWidth().weight(1f), pad = 32.dp, accent = vm.lastNotice != null) {
                T("운전 주치의 · 음성 안내", 18.sp, C.mut)
                Spacer(Modifier.height(12.dp))
                T(vm.lastNotice?.speech ?: "주행 중에는 소리로만 안내해요", 40.sp,
                    if (vm.lastNotice != null) C.amber else C.ink, FontWeight.Bold, serif = true, lineHeight = 52.sp)
                Spacer(Modifier.height(14.dp))
                T(if (vm.moving) "운전 중 · ${ReportText.fmt(t.km)} km · 화면은 보지 않으셔도 돼요"
                  else "주차 중 · 출발하면 이 화면으로 바뀌어요", 24.sp, C.calm, FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                T("안내 기준", 16.sp, C.dim)
                Spacer(Modifier.height(10.dp))
                val n = vm.lastNotice
                Rule("깜빡이 켠 채 달림", "켠 채 0.5km 이상 · 방향지시등 + 계기판",
                    if (t.turnSignalOn) "켜짐" else "꺼짐", n == Notice.TURN_SIGNAL_LEFT_ON)
                Rule("쉬어 가기 권유", "10분 안에 안전 경고 3회 · 경고등",
                    "${t.warnings.coerceAtMost(TripSession.WARNING_COUNT)} / ${TripSession.WARNING_COUNT}", n == Notice.TAKE_A_BREAK)
                Rule("주차 브레이크 확인", "P단에서 문 열 때 해제 상태 · 경고등 + 문",
                    "${t.exitWithoutParkingBrake}회", n == Notice.PARKING_BRAKE, last = true)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Stat("이번 운전", "${ReportText.fmt(t.km)} km", Modifier.weight(1f))
                Stat("깜빡이 켠 채 달림", "${t.turnSignalLeftOn}회", Modifier.weight(1f))
                Stat("안전 경고", "${t.warnings}회", Modifier.weight(1f))
                Stat("브레이크", "${t.brakePresses}회", Modifier.weight(1f))
            }
        }
        vm.simulator?.let { sim ->
            Panel(Modifier.width(300.dp).fillMaxHeight().verticalScroll(rememberScrollState()), pad = 20.dp) {
                T("신호 시뮬레이터", 20.sp, weight = FontWeight.Bold)
                T("차 대신 신호를 넣어 봅니다", 15.sp, C.dim)
                Spacer(Modifier.height(16.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    val b = Modifier.fillMaxWidth()
                    BigButton(if (t.turnSignalOn) "깜빡이 끄기" else "깜빡이 켜기", b, height = 56.dp, size = 20.sp) { sim.toggleTurnSignal() }
                    BigButton("0.2km 달리기", b, height = 56.dp, size = 20.sp) { sim.drive() }
                    BigButton("브레이크", b, height = 56.dp, size = 20.sp) { sim.brake() }
                    BigButton("안전 경고", b, height = 56.dp, size = 20.sp) { sim.warning() }
                    BigButton("경로 재탐색", b, height = 56.dp, size = 20.sp) { sim.reroute() }
                    BigButton("주차 브레이크 없이 내리기", b, height = 56.dp, size = 20.sp) { sim.park(false); sim.openDoor() }
                    BigButton("운전 마치기", b, filled = true, height = 56.dp, size = 20.sp) { sim.park(true); vm.finishTrip() }
                }
                Spacer(Modifier.height(10.dp))
                T("마친 운전 ${vm.finishedTrips}회는 주간 리포트 이번 주 칸에 더해져요", 14.sp, C.dim)
            }
        }
    }
}

/** 안내 규칙 한 줄: 이름 · 조건과 쓰는 차량 신호 · 지금 값. 안내가 나간 규칙은 호박색 */
@Composable
private fun Rule(name: String, cond: String, value: String, fired: Boolean, last: Boolean = false) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(if (fired) C.amberSoft else C.panel2)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(10.dp).clip(RoundedCornerShape(50)).background(if (fired) C.amber else C.line))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            T(name, 21.sp, if (fired) C.amber else C.ink, FontWeight.SemiBold)
            T(cond, 15.sp, C.dim)
        }
        T(value, 22.sp, if (fired) C.amber else C.mut, FontWeight.Bold)
    }
    if (!last) Spacer(Modifier.height(10.dp))
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier) {
    Panel(modifier, pad = 18.dp) {
        T(label, 16.sp, C.mut)
        Spacer(Modifier.height(4.dp))
        T(value, 30.sp, weight = FontWeight.Bold)
    }
}
