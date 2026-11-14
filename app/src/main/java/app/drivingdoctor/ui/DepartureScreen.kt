package app.drivingdoctor.ui

import androidx.compose.foundation.layout.padding

import androidx.compose.ui.draw.clip

import androidx.compose.foundation.clickable

import androidx.compose.foundation.background

import android.app.Activity
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drivingdoctor.AppViewModel
import app.drivingdoctor.domain.AnswerParser
import app.drivingdoctor.domain.ConditionAdvisor
import app.drivingdoctor.domain.Feeling

/** 장면 2·3: 시동을 걸면 컨디션을 묻고, 대답에 따라 다음 일을 고른다. */
@Composable
fun DepartureScreen(vm: AppViewModel, go: (Page) -> Unit) {
    val ctx = LocalContext.current
    var heard by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { if (vm.check == null) vm.ask() }

    val sttIntent = remember {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ko-KR")
            .putExtra(RecognizerIntent.EXTRA_PROMPT, "오늘 몸은 좀 어떠세요?")
    }
    val canListen = remember { sttIntent.resolveActivity(ctx.packageManager) != null }
    val listen = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val text = r.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull() ?: return@rememberLauncherForActivityResult
        heard = "\"$text\""
        AnswerParser.parse(text)?.let { vm.answer(it) } ?: run { heard = "\"$text\" · 버튼으로 골라 주세요" }
    }

    Row(Modifier.fillMaxSize(), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
        Panel(Modifier.weight(1.3f).fillMaxHeight().verticalScroll(rememberScrollState()), pad = 32.dp) {
            T("할아버지,", 36.sp, C.mut, serif = true)
            T("오늘 몸은 좀 어떠세요?", 42.sp, weight = FontWeight.Bold, serif = true)
            Spacer(Modifier.height(8.dp))
            T(heard ?: if (canListen) "말씀하시거나 눌러 주세요" else "눌러서 대답해 주세요", 20.sp, C.mut)
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Feeling.entries.forEach { f ->
                    BigButton(f.label, Modifier.fillMaxWidth(), filled = vm.check?.feeling == f, height = 72.dp, size = 28.sp) { vm.answer(f) }
                }
            }
            if (canListen) {
                Spacer(Modifier.height(16.dp))
                BigButton("말로 대답하기", Modifier.fillMaxWidth(), height = 64.dp, size = 22.sp) { listen.launch(sttIntent) }
            }
            Spacer(Modifier.height(18.dp))
            WeekGlance(vm, go)
        }

        val c = vm.check
        Panel(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), pad = 28.dp,
            accent = c != null && c.feeling != Feeling.GOOD) {
            if (c == null) {
                T("오늘 가실 곳", 18.sp, C.mut)
                Spacer(Modifier.height(8.dp))
                T("대답 듣고 함께 정해요", 28.sp, weight = FontWeight.Bold, lineHeight = 38.sp)
                Spacer(Modifier.height(16.dp))
                listOf("괜찮아" to "바로 출발", "조금 피곤해" to "가까운 길로", "힘들어" to "가족에게 자동 알림").forEach { (k, v) ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        T(k, 20.sp, C.mut, modifier = Modifier.weight(1f))
                        T(v, 20.sp, weight = FontWeight.SemiBold)
                    }
                }

            } else {
                val a = ConditionAdvisor.advise(c.feeling)
                T(a.speech, 26.sp, weight = FontWeight.Medium, lineHeight = 36.sp)
                Spacer(Modifier.height(8.dp))
                T("대답까지 ${"%.1f".format(c.answerMillis / 1000.0)}초 · 오늘의 컨디션 기록에 남겼어요", 16.sp, C.dim)
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    val b = Modifier.fillMaxWidth()
                    if (a.askFamily) vm.outbox.firstOrNull { it.what == "오늘 컨디션" }?.let { SentRow(it) }
                    BigButton(if (a.suggestShortRoute) "가까운 길로 출발" else "출발하기", b, filled = true, height = 68.dp, size = 22.sp) { go(Page.DRIVE) }
                    BigButton("다시 묻기", b, height = 60.dp, size = 20.sp) { heard = null; vm.ask() }
                }
            }
        }
    }
}

/** 이번 주 운전 한눈에: 횟수·거리·달라진 점. 누르면 주간 리포트 */
@Composable
private fun WeekGlance(vm: AppViewModel, go: (Page) -> Unit) {
    val week = vm.weeks.lastOrNull().orEmpty()
    val noticed = vm.changes.count { it.level != app.drivingdoctor.domain.Level.SAME }
    Column(
        Modifier.fillMaxWidth().clip(androidx.compose.foundation.shape.RoundedCornerShape(20.dp)).background(C.panel2)
            .clickable { go(Page.REPORT) }.padding(24.dp),
    ) {
        T("이번 주 운전", 20.sp, C.mut)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Glance("운전", "${week.size}번", Modifier.weight(1f))
            Glance("거리", "${"%.0f".format(week.sumOf { it.km })}km", Modifier.weight(1f))
            Glance("달라진 점", if (noticed == 0) "없음" else "${noticed}가지", Modifier.weight(1f), if (noticed == 0) C.calm else C.amber)
        }
        Spacer(Modifier.height(12.dp))
        T("눌러서 주간 리포트 보기 ›", 18.sp, C.amber)
    }
}

@Composable
private fun Glance(label: String, value: String, modifier: Modifier, color: androidx.compose.ui.graphics.Color = C.ink) =
    Column(modifier) {
        T(label, 18.sp, C.mut)
        T(value, 34.sp, color, FontWeight.Bold)
    }
