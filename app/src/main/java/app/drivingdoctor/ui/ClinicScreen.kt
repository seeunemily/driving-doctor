package app.drivingdoctor.ui

import android.content.Intent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drivingdoctor.AppViewModel
import app.drivingdoctor.domain.Level
import app.drivingdoctor.domain.ReportText
import app.drivingdoctor.domain.Signal

/** 장면 4: 진료 때 보여 드릴 8주 운전 변화 기록. 진단하지 않고 기록만 보여 준다. */
@Composable
fun ClinicScreen(vm: AppViewModel) {
    val changes = vm.changes
    var pick by remember { mutableStateOf(ReportText.topChange(changes)?.signal ?: Signal.TURN_SIGNAL) }
    val series = vm.series(pick)
    val base = vm.baseline[pick]

    val scroll = rememberScrollState()

    Column(Modifier.fillMaxSize().verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Signal.entries.forEach { s ->
                val hot = changes.first { it.signal == s }.level != Level.SAME
                Chip(if (hot) "${s.title} ●" else s.title, s == pick) { pick = s }
            }
        }
        Panel(Modifier.fillMaxWidth(), pad = 28.dp) {
            T("일상 운전 변화 기록 · 8주", 22.sp, weight = FontWeight.Bold, serif = true)
            T("${pick.title} (${pick.unit}) · 띠 = 처음 4주 평소 범위", 16.sp, C.mut)
            Spacer(Modifier.height(16.dp))
            WeekChart(series, base.mean, base.sd, Modifier.fillMaxWidth().height(260.dp))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                series.indices.forEach { i -> T("${i + 1}주", 15.sp, if (i < 4) C.dim else C.mut, modifier = Modifier.weight(1f), align = TextAlign.Center) }
            }
        }
        Panel(Modifier.fillMaxWidth(), pad = 24.dp) {
            Row(Modifier.fillMaxWidth()) {
                T("항목", 15.sp, C.dim, modifier = Modifier.weight(1.6f))
                T("평소(1–4주)", 15.sp, C.dim, modifier = Modifier.weight(1f))
                (5..8).forEach { T("${it}주", 15.sp, C.dim, modifier = Modifier.weight(0.7f)) }
            }
            Spacer(Modifier.height(8.dp))
            Signal.entries.forEach { s ->
                val v = vm.series(s); val b = vm.baseline[s].mean
                Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    T(s.title, 18.sp, if (s == pick) C.amber else C.ink, modifier = Modifier.weight(1.6f))
                    T(ReportText.fmt(b), 18.sp, C.mut, modifier = Modifier.weight(1f))
                    (4..7).forEach { T(ReportText.fmt(v[it]), 18.sp, modifier = Modifier.weight(0.7f)) }
                }
            }
            Spacer(Modifier.height(12.dp))
            T("기록 방법: 차량 신호(Pleos Vehicle SDK)를 운전 1회마다 저장 · 의료 판단은 담당 의료진이 함 · 진단 아님", 14.sp, C.dim, lineHeight = 20.sp)
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(C.panel2).padding(18.dp),
            ) {
                T("주간 리포트와 함께 가족에게 자동으로 가요", 18.sp, C.ink, FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                T("진료 때 가족이 담당 의료진에게 보여 드리거나 전달해요", 16.sp, C.mut)
            }
        }
    }
}

@Composable
private fun WeekChart(values: List<Double>, mean: Double, sd: Double, modifier: Modifier) {
    Canvas(modifier.padding(horizontal = 8.dp)) {
        val max = maxOf(values.maxOrNull() ?: 0.0, mean + sd) * 1.25 + 1e-6
        val stepX = size.width / values.size
        val x = { i: Int -> stepX * i + stepX / 2 }
        val y = { v: Double -> (size.height * (1 - v / max)).toFloat() }
        // 평소 범위 띠
        val top = y(mean + sd); val bot = y((mean - sd).coerceAtLeast(0.0))
        drawRect(C.panel2, Offset(0f, top), Size(size.width, (bot - top).coerceAtLeast(4f)))
        drawLine(C.dim, Offset(0f, y(mean)), Offset(size.width, y(mean)), strokeWidth = 2f)
        // 선
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
        drawPath(path, C.amber, style = Stroke(width = 5f))
        values.forEachIndexed { i, v ->
            drawCircle(if (i < 4) C.mut else C.amber, radius = 9f, center = Offset(x(i), y(v)))
        }
    }
}
