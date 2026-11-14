package app.drivingdoctor.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.model.area.DoorArea
import ai.pleos.playground.vehicle.api.model.telltale.Telltales
import ai.pleos.playground.vehicle.constant.area.DoorValue
import ai.pleos.playground.vehicle.constant.area.TurnSignalValue
import ai.pleos.playground.vehicle.constant.control.VehicleGear
import ai.pleos.playground.vehicle.constant.telltale.TelltaleState
import ai.pleos.playground.vehicle.constant.telltale.TelltaleType
import ai.pleos.playground.vehicle.listener.BrakePedalStatusListener
import ai.pleos.playground.vehicle.listener.DistanceDrivenListener
import ai.pleos.playground.vehicle.listener.DoorPositionListener
import ai.pleos.playground.vehicle.listener.GearSelectionListener
import ai.pleos.playground.vehicle.listener.TelltaleInfoListener
import ai.pleos.playground.vehicle.listener.TurnSignalStateListener
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Pleos Connect Vehicle SDK(ai.pleos.playground:Vehicle:2.3.2)에서 신호를 받는다.
 * 쓰는 API: TurnSignal, Odometer, Brake, Gear, Door, Safety(Telltale).
 * 차를 제어하는 API는 쓰지 않고 읽기만 한다.
 */
class PleosVehicleSource(context: Context) : VehicleSignalSource {
    override val label = "Pleos Vehicle SDK"
    override val isSimulated = false

    private val vehicle = Vehicle(context.applicationContext)
    private val flow = MutableSharedFlow<VehicleEvent>(extraBufferCapacity = 64)
    override val events: SharedFlow<VehicleEvent> = flow.asSharedFlow()

    private val driverDoor = DoorArea(DoorValue.ROW_1_LEFT)
    private var lastDoorPos = 0
    private val activeWarnings = mutableSetOf<TelltaleType>()

    private val turnListener = object : TurnSignalStateListener {
        override fun onTurnSignalStateUpdated(state: TurnSignalValue?) {
            if (state != null) emit(VehicleEvent.TurnSignal(state != TurnSignalValue.OFF))
        }
        override fun onFailed(e: Exception) = fail("TurnSignal", e)
    }
    private val distanceListener = object : DistanceDrivenListener {
        override fun onDistanceDrivenUpdated(value: Float?) { value?.let { emit(VehicleEvent.Distance(it.toDouble())) } }
        override fun onFailed(e: Exception) = fail("Odometer", e)
    }
    private val brakeListener = object : BrakePedalStatusListener {
        override fun onBrakePedalStatusUpdated(value: Boolean?) { emit(VehicleEvent.BrakePedal(value == true)) }
        override fun onFailed(e: Exception) = fail("Brake", e)
    }
    private val gearListener = object : GearSelectionListener {
        override fun onSelected(gear: VehicleGear?) { if (gear != null) emit(VehicleEvent.Gear(gear == VehicleGear.GEAR_PARK)) }
        override fun onFailed(e: Exception) = fail("Gear", e)
    }
    private val doorListener = object : DoorPositionListener {
        override fun onPositionUpdated(value: Int?) {
            val pos = value ?: return
            if (lastDoorPos == 0 && pos > 0) emit(VehicleEvent.DriverDoorOpened)
            lastDoorPos = pos
        }
        override fun onFailed(e: Exception) = fail("Door", e)
    }
    private val telltaleListener = object : TelltaleInfoListener {
        override fun onTelltaleInfoUpdated(telltales: Telltales?) { if (telltales != null) onTelltales(telltales) }
        override fun onFailed(e: Exception) = fail("Safety", e)
    }

    override fun start() {
        vehicle.initialize()
        safely("TurnSignal") { vehicle.getTurnSignal().registerTurnSignalState(turnListener) }
        safely("Odometer") { vehicle.getOdometer().registerDistanceDriven(distanceListener) }
        safely("Brake") { vehicle.getBrake().registerBrakePedalStatus(brakeListener) }
        safely("Gear") { vehicle.getGear().registerGearSelection(gearListener) }
        safely("Door") { vehicle.getDoor().registerDoorPosition(driverDoor, doorListener) }
        safely("Safety") { vehicle.getSafety().registerTelltales(telltaleListener) }
        handler.postDelayed(poll, POLL_MS)
    }

    /** 경고등은 바뀔 때 알림이 오지 않는 환경이 있어 2초마다 직접 읽는다(같은 상태는 다시 세지 않음). */
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val poll: Runnable = object : Runnable {
        override fun run() {
            safely("Safety poll") {
                vehicle.getSafety().getTelltalesInfo({ t -> onTelltales(t) }, { e -> fail("Safety poll", e) })
            }
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun stop() {
        handler.removeCallbacks(poll)
        safely("unregister") {
            vehicle.getTurnSignal().unregisterTurnSignalState(turnListener)
            vehicle.getOdometer().unregisterDistanceDriven(distanceListener)
            vehicle.getBrake().unregisterBrakePedalStatus(brakeListener)
            vehicle.getGear().unregisterGearSelection(gearListener)
            vehicle.getDoor().unregisterDoorPosition(driverDoor, doorListener)
        }
        safely("release") { vehicle.release() }
    }

    private var lastParkingBrake: Boolean? = null

    @Synchronized private fun onTelltales(t: Telltales) {
        val on = t.telltales.filter { it.state != TelltaleState.OFF }.map { it.type }.toSet()
        val pb = TelltaleType.PARKING_BRAKE in on || TelltaleType.ELECTRONIC_PARKING_BRAKE in on
        if (pb != lastParkingBrake) { lastParkingBrake = pb; emit(VehicleEvent.ParkingBrake(pb)) }
        // 새로 켜진 안전 경고만 센다
        val warnings = on intersect WARNING_TYPES
        (warnings - activeWarnings).forEach { emit(VehicleEvent.Warning(it.name)) }
        activeWarnings.clear(); activeWarnings += warnings
    }

    private fun emit(e: VehicleEvent) { flow.tryEmit(e) }

    private fun fail(what: String, e: Exception) { Log.w(TAG, "$what 신호 오류: ${e.message}") }

    private inline fun safely(what: String, block: () -> Unit) {
        try { block() } catch (e: Exception) { Log.w(TAG, "$what 등록 실패: ${e.message}") }
    }

    companion object {
        private const val TAG = "PleosVehicleSource"
        private const val POLL_MS = 2_000L

        /** 운전 습관 변화로 보는 경고: 전방 충돌·차로 유지·후측방·운전자 주의·과속·마스터 경고 */
        val WARNING_TYPES = setOf(
            TelltaleType.FCA, TelltaleType.LKA, TelltaleType.LCA_LEFT, TelltaleType.LCA_RIGHT,
            TelltaleType.DRIVER_ATTENTION, TelltaleType.OVER_SPEED, TelltaleType.MASTER_WARNING,
        )

        /** 이 기기에 Pleos 차량 서비스가 있는지 */
        fun isAvailable(context: Context): Boolean = try {
            context.packageManager
                .queryIntentServices(Intent("ai.pleos.playground.vehicle.VEHICLE_START"), 0)
                .isNotEmpty()
        } catch (e: Exception) { false }
    }
}

object VehicleSources {
    /** 차(또는 Pleos Connect Emulator)에서는 SDK, 그 밖에는 시뮬레이터 */
    fun create(context: Context): VehicleSignalSource =
        if (PleosVehicleSource.isAvailable(context)) PleosVehicleSource(context) else SimulatedVehicleSource()
}
