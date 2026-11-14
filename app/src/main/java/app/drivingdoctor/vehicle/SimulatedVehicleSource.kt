package app.drivingdoctor.vehicle

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Pleos 차량 서비스가 없는 기기(일반 휴대폰·에뮬레이터)에서 쓰는 시뮬레이터.
 * 화면의 시뮬레이터 버튼이 Pleos Connect Emulator의 Virtual Console 역할을 한다.
 */
class SimulatedVehicleSource : VehicleSignalSource {
    override val label = "시뮬레이터"
    override val isSimulated = true

    private val flow = MutableSharedFlow<VehicleEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<VehicleEvent> = flow.asSharedFlow()

    private var km = 12_480.0
    private var turnOn = false

    override fun start() {
        emit(VehicleEvent.Distance(km))
        emit(VehicleEvent.ParkingBrake(engaged = false))
        emit(VehicleEvent.Gear(park = false))
    }

    override fun stop() = Unit

    fun toggleTurnSignal() { turnOn = !turnOn; emit(VehicleEvent.TurnSignal(turnOn)) }
    fun drive(stepKm: Double = 0.2) { km += stepKm; emit(VehicleEvent.Distance(km)) }
    fun brake() { emit(VehicleEvent.BrakePedal(true)); emit(VehicleEvent.BrakePedal(false)) }
    fun warning(name: String = "FCA") = emit(VehicleEvent.Warning(name))
    fun reroute() = emit(VehicleEvent.Reroute)
    fun park(withParkingBrake: Boolean) {
        emit(VehicleEvent.Gear(park = true))
        emit(VehicleEvent.ParkingBrake(engaged = withParkingBrake))
    }
    fun openDoor() = emit(VehicleEvent.DriverDoorOpened)
    fun depart() {
        emit(VehicleEvent.ParkingBrake(engaged = false))
        emit(VehicleEvent.Gear(park = false))
    }

    private fun emit(e: VehicleEvent) { flow.tryEmit(e) }
}
