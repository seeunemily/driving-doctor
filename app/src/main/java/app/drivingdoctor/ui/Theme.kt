package app.drivingdoctor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 해 질 녘 차 안 조명 같은 어두운 바탕 + 호박색 강조. 고령 운전자가 읽기 쉽게 글씨를 크게 둔다. */
object C {
    val bg = Color(0xFF12161D)
    val panel = Color(0xFF1A2029)
    val panel2 = Color(0xFF222935)
    val line = Color(0xFF2E3644)
    val ink = Color(0xFFF6EFE6)
    val mut = Color(0xFFB9B0A5)
    val dim = Color(0xFF7D766E)
    val amber = Color(0xFFF0B37E)
    val amberSoft = Color(0x33F0B37E)
    val calm = Color(0xFF8FB8A8)
}

val Serif = FontFamily.Serif

@Composable
fun DrivingDoctorTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = C.amber, onPrimary = C.bg, background = C.bg, surface = C.panel,
            onBackground = C.ink, onSurface = C.ink,
        ),
        content = content,
    )
}

@Composable
fun T(
    text: String, size: TextUnit = 20.sp, color: Color = C.ink, weight: FontWeight = FontWeight.Normal,
    serif: Boolean = false, modifier: Modifier = Modifier, lineHeight: TextUnit = TextUnit.Unspecified,
    align: TextAlign? = null,
) {
    // 어르신 눈높이: 보조 글씨도 20sp 아래로 내려가지 않게 키운다
    val big = if (size.value < 20f) (size.value + 4f).coerceAtMost(20f).sp else size
    val lh = if (lineHeight.isSp && size.value > 0f) (lineHeight.value * big.value / size.value).sp else lineHeight
    Text(
        text, modifier = modifier, color = color, fontSize = big, fontWeight = weight,
        fontFamily = if (serif) Serif else FontFamily.Default, lineHeight = lh, textAlign = align,
        style = androidx.compose.ui.text.TextStyle(lineBreak = androidx.compose.ui.text.style.LineBreak(androidx.compose.ui.text.style.LineBreak.Strategy.HighQuality, androidx.compose.ui.text.style.LineBreak.Strictness.Normal, androidx.compose.ui.text.style.LineBreak.WordBreak.Phrase)), // 어절 단위 줄바꿈
    )
}

@Composable
fun Panel(modifier: Modifier = Modifier, pad: Dp = 24.dp, accent: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .clip(RoundedCornerShape(20.dp))
            .background(C.panel)
            .border(1.dp, if (accent) C.amber else C.line, RoundedCornerShape(20.dp))
            .padding(pad),
        content = content,
    )
}

/** 큰 누름 버튼. 고령 운전자가 한 번에 누를 수 있게 높이를 넉넉히 둔다. */
@Composable
fun BigButton(text: String, modifier: Modifier = Modifier, filled: Boolean = false, height: Dp = 88.dp, size: TextUnit = 24.sp, onClick: () -> Unit) {
    Box(
        modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (filled) C.amber else C.panel2)
            .border(1.dp, if (filled) C.amber else C.line, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .height(height)
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        T(text, size, if (filled) C.bg else C.ink, FontWeight.Bold)
    }
}

@Composable
fun Chip(text: String, on: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (on) C.amberSoft else C.panel2)
            .border(1.dp, if (on) C.amber else C.line, RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 10.dp),
    ) { T(text, 18.sp, if (on) C.amber else C.mut, FontWeight.Medium) }
}

@Composable
fun Tag(text: String, color: Color = C.mut) {
    Box(
        Modifier.clip(RoundedCornerShape(8.dp)).background(C.panel2).padding(horizontal = 10.dp, vertical = 4.dp),
    ) { T(text, 14.sp, color) }
}
