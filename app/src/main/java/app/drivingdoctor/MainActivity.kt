package app.drivingdoctor

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import app.drivingdoctor.ui.AppScreen
import app.drivingdoctor.ui.DrivingDoctorTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { DrivingDoctorTheme { CarScale { AppScreen(vm) } } }
    }
}

/**
 * 화면 크기에 맞춰 전체를 키운다. 1920x720 화면은 그대로, Pleos Connect(2560x1440, 160dpi)의
 * 앱 칸(약 1680x1210)에서는 글자와 단추가 작아 보이지 않게 비율만큼 키운다.
 */
@androidx.compose.runtime.Composable
fun CarScale(content: @androidx.compose.runtime.Composable () -> Unit) {
    BoxWithConstraints {
        val scale = minOf(maxWidth.value / 1040f, maxHeight.value / 620f).coerceIn(1f, 1.8f)
        val d = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(d.density * scale, d.fontScale)) { content() }
    }
}
