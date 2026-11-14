package app.drivingdoctor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drivingdoctor.AppViewModel
import app.drivingdoctor.domain.Change
import app.drivingdoctor.domain.Level
import app.drivingdoctor.domain.ReportText

/** 장면 4: 할아버지의 평소(처음 4주)와 이번 주를 신호 6가지로 비교한다. */
@Composable
fun ReportScreen(vm: AppViewModel, go: (Page) -> Unit) {
    val changes = vm.changes
    val top = ReportText.topChange(changes)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Panel(Modifier.fillMaxWidth(), pad = 28.dp, accent = top != null) {
            T(ReportText.headline(changes), 20.sp, if (top != null) C.amber else C.calm, FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            T(top?.let { ReportText.sentence(it) } ?: "다른 항목도 평소와 같아요", 34.sp, weight = FontWeight.Bold, serif = true, lineHeight = 46.sp)
            Spacer(Modifier.height(6.dp))
            T("일요일 저녁 7시에 가족에게 자동으로 보내요", 18.sp, C.mut)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                BigButton("소리로 읽어 드리기", Modifier.weight(1f), height = 68.dp) {
                    vm.speaker.say(top?.let { "할아버지, 이번 주 운전 기록이에요. " + ReportText.sentence(it) } ?: "이번 주 운전은 평소와 같아요.")
                }
                BigButton("가족 알림 보기", Modifier.weight(1f), height = 68.dp) { go(Page.FAMILY) }
                BigButton("진료 기록 보기", Modifier.weight(1f), height = 68.dp) { go(Page.CLINIC) }
            }
        }
        Panel(Modifier.fillMaxWidth(), pad = 24.dp) {
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                T("신호", 15.sp, C.dim, modifier = Modifier.weight(2f))
                T("평소", 15.sp, C.dim, modifier = Modifier.width(90.dp))
                T("이번 주", 15.sp, C.dim, modifier = Modifier.width(90.dp))
                T("비교", 15.sp, C.dim, modifier = Modifier.weight(0.8f))
                T("", 15.sp, modifier = Modifier.width(150.dp))
            }
            changes.forEach { RowItem(it) }
            Spacer(Modifier.height(8.dp))
            T("평소 = 처음 4주 할아버지 운전 · 진단이 아닌 운전 습관 비교" + if (vm.usesSample) " · 시연 기록 포함" else "", 14.sp, C.dim)
        }
    }
}

@Composable
private fun RowItem(c: Change) {
    val hot = c.level != Level.SAME
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(2f).padding(end = 12.dp)) {
            T(c.signal.title, 20.sp, if (hot) C.ink else C.mut, if (hot) FontWeight.Bold else FontWeight.Normal)
            T("${c.signal.unit} · ${c.signal.sdk}", 13.sp, C.dim)
        }
        T(ReportText.fmt(c.baseline), 22.sp, C.mut, modifier = Modifier.width(90.dp))
        T(ReportText.fmt(c.now), 22.sp, if (hot) C.amber else C.ink, FontWeight.Bold, modifier = Modifier.width(90.dp))
        CompareBar(c, Modifier.weight(0.8f).height(26.dp))
        Row(Modifier.width(150.dp), horizontalArrangement = Arrangement.End) {
            Tag(when (c.level) { Level.SAME -> "평소와 같음"; Level.WATCH -> "살펴보기"; Level.CHANGED -> "평소보다 많음" },
                when (c.level) { Level.SAME -> C.dim; Level.WATCH -> C.mut; Level.CHANGED -> C.amber })
        }
    }
}

@Composable
private fun CompareBar(c: Change, modifier: Modifier) {
    Canvas(modifier.padding(end = 12.dp)) {
        val max = maxOf(c.baseline, c.now, 1e-6) * 1.15
        val h = size.height / 2 - 3
        drawRoundRect(C.line, Offset(0f, 0f), Size(size.width, h), CornerRadius(6f))
        drawRoundRect(C.dim, Offset(0f, 0f), Size((size.width * c.baseline / max).toFloat(), h), CornerRadius(6f))
        drawRoundRect(C.line, Offset(0f, h + 6), Size(size.width, h), CornerRadius(6f))
        drawRoundRect(if (c.level != Level.SAME) C.amber else C.calm, Offset(0f, h + 6),
            Size((size.width * c.now / max).toFloat(), h), CornerRadius(6f))
    }
}
