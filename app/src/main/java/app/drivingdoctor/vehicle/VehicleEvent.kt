package app.drivingdoctor.vehicle

import kotlinx.coroutines.flow.SharedFlow

/**
 * 앱이 쓰는 차량 신호. Pleos Vehicle SDK 값과 시뮬레이터 값이 모두 이 형태로 들어온다.
 * 분석 코드(domain)는 신호가 어디서 왔는지 모른다.
 */
sealed interface VehicleEvent {
    /** 방향지시등 켬·끔 (TurnSignal) */
    data class TurnSignal(val on: Boolean) : VehicleEvent
    /** 누적 주행거리 km (Odometer) */
    data class Distance(val totalKm: Double) : VehicleEvent
    /** 브레이크 페달 (Brake) */
    data class BrakePedal(val pressed: Boolean) : VehicleEvent
    /** 주차 브레이크 경고등 상태 (Safety · Telltale PARKING_BRAKE) */
    data class ParkingBrake(val engaged: Boolean) : VehicleEvent
    /** 기어 P 여부 (Gear) */
    data class Gear(val park: Boolean) : VehicleEvent
    /** 운전석 문 열림 (Door) */
    data object DriverDoorOpened : VehicleEvent
    /** 주행 보조·안전 경고 발생 (Safety · Telltale) */
    data class Warning(val name: String) : VehicleEvent
    /** 경로 재탐색 (NaviHelper) */
    data object Reroute : VehicleEvent
}

interface VehicleSignalSource {
    /** 화면에 보여 줄 신호 출처 이름 */
    val label: String
    val isSimulated: Boolean
    val events: SharedFlow<VehicleEvent>
    fun start()
    fun stop()
}
