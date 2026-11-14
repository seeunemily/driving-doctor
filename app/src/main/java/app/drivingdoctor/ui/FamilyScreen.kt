package app.drivingdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drivingdoctor.AppViewModel
import app.drivingdoctor.AutoRule
import app.drivingdoctor.Sent

private fun hm(t: Long) = java.text.SimpleDateFormat("HH:mm", java.util.Locale.KOREA).format(java.util.Date(t))
private fun dayHm(t: Long) = java.text.SimpleDateFormat("M/d(E) HH:mm", java.util.Locale.KOREA).format(java.util.Date(t))

/** 장면 1·3: 가족에게는 정해 둔 대로 자동으로 간다. 할아버지는 누르지 않는다. 설정은 가족이 처음 한 번 한다. */
@Composable
fun FamilyScreen(vm: AppViewModel) {
    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Panel(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            T("가족에게 간 알림", 20.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            if (vm.outbox.isEmpty()) {
                T("아직 보낸 알림이 없어요", 18.sp, C.mut)
            } else {
                vm.outbox.forEachIndexed { i, s -> SentRow(s, showText = i == 0) }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.panel2).padding(18.dp),
            ) {
                T("다음 주간 리포트 · ${dayHm(vm.nextWeekly())} 자동 전송", 18.sp, C.ink, FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                T(if (AutoRule.WEEKLY in vm.rules) vm.recipients.joinToString(", ").ifEmpty { "받는 가족 없음" } + "에게" else "주간 리포트 자동 전송 꺼짐", 16.sp, C.mut)
            }
            Spacer(Modifier.height(18.dp))
            T("가족은 받은 리포트를 보고 할아버지께 연락하거나, 진료 때 담당 의료진에게 보여 드려요", 16.sp, C.dim, lineHeight = 24.sp)
        }
        Panel(Modifier.width(380.dp).fillMaxHeight().verticalScroll(rememberScrollState())) {
            T("자동 알림 설정", 20.sp, weight = FontWeight.Bold)
            T("처음 한 번 가족과 함께 정해요", 15.sp, C.dim)
            Spacer(Modifier.height(16.dp))
            T("받는 가족", 17.sp, C.mut)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                vm.family.forEach { p ->
                    Chip(p, p in vm.recipients) { if (p in vm.recipients) vm.recipients.remove(p) else vm.recipients.add(p) }
                }
            }
            Spacer(Modifier.height(22.dp))
            T("자동으로 보내는 것", 17.sp, C.mut)
            Spacer(Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AutoRule.entries.forEach { r ->
                    val on = r in vm.rules
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            T(r.title, 18.sp, if (on) C.ink else C.mut, FontWeight.SemiBold)
                            T(r.whenText, 15.sp, C.dim)
                        }
                        Chip(if (on) "켬" else "끔", on) { if (on) vm.rules.remove(r) else vm.rules.add(r) }
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            T("운전 기록 원본은 차 안에만 두고 요약만 보내요", 15.sp, C.dim, lineHeight = 22.sp)
        }
    }
}

/** 보낸 기록 한 줄: 받는 사람 · 무엇 · 시각 */
@Composable
fun SentRow(s: Sent, showText: Boolean = false) {
    Column(
        Modifier.fillMaxWidth().padding(bottom = 10.dp).clip(RoundedCornerShape(16.dp)).background(C.amberSoft).padding(18.dp),
    ) {
        T("✓ ${s.what} → ${s.to} · ${hm(s.at)} 자동 전송", 20.sp, C.amber, FontWeight.Bold)
        if (showText) {
            Spacer(Modifier.height(8.dp))
            T(s.text, 17.sp, C.ink, lineHeight = 26.sp)
        }
        Spacer(Modifier.height(4.dp))
        T("시연 차에는 메신저가 없어 보낸 기록에 남김", 14.sp, C.mut)
    }
}
