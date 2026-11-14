package app.drivingdoctor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.drivingdoctor.AppViewModel

enum class Page(val title: String, val icon: ImageVector) {
    DEPART("출발 전", Icons.Filled.Face),
    DRIVE("주행", Icons.Filled.PlayArrow),
    FAMILY("가족 알림", Icons.Filled.Person),
    REPORT("주간 리포트", Icons.Filled.DateRange),
    CLINIC("진료 기록", Icons.Filled.Info),
}

@Composable
fun AppScreen(vm: AppViewModel) {
    var page by remember { mutableStateOf(Page.DEPART) }
    val back = remember { mutableStateListOf<Page>() }
    val go = { p: Page -> if (p != page) { back.add(page); page = p } }
    // 출발하면(주차 해제) 주행 화면으로 넘어간다
    LaunchedEffect(vm.moving) { if (vm.moving) go(Page.DRIVE) }
    BackHandler(enabled = back.isNotEmpty()) { page = back.removeAt(back.lastIndex) }

    Row(Modifier.fillMaxSize().background(C.bg).safeDrawingPadding()) {
        Rail(page) { back.clear(); page = it }
        Column(Modifier.fillMaxSize().padding(start = 8.dp, end = 32.dp, top = 24.dp, bottom = 24.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                T(page.title, 30.sp, weight = FontWeight.Bold, serif = true, modifier = Modifier.weight(1f))
                Tag("차량 신호 · ${vm.source.label}", if (vm.source.isSimulated) C.mut else C.calm)
            }
            Spacer(Modifier.height(20.dp))
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    Page.DEPART -> DepartureScreen(vm, go)
                    Page.DRIVE -> DrivingScreen(vm)
                    Page.FAMILY -> FamilyScreen(vm)
                    Page.REPORT -> ReportScreen(vm, go)
                    Page.CLINIC -> ClinicScreen(vm)
                }
            }
        }
    }
}

@Composable
private fun Rail(page: Page, onPick: (Page) -> Unit) {
    Column(
        Modifier.width(132.dp).fillMaxHeight().padding(vertical = 24.dp, horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        T("운전\n주치의", 20.sp, C.amber, FontWeight.Bold, serif = true, lineHeight = 24.sp)
        Spacer(Modifier.height(16.dp))
        Page.entries.forEach { p ->
            val on = p == page
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                    .background(if (on) C.amberSoft else C.bg)
                    .clickable { onPick(p) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(p.icon, null, tint = if (on) C.amber else C.mut, modifier = Modifier.size(30.dp))
                Spacer(Modifier.height(4.dp))
                T(p.title, 16.sp, if (on) C.amber else C.mut, if (on) FontWeight.Bold else FontWeight.Normal)
            }
        }
    }
}
