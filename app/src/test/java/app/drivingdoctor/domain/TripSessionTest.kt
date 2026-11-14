package app.drivingdoctor.domain

import app.drivingdoctor.vehicle.VehicleEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TripSessionTest {

    private val s = TripSession(startMillis = 0L)
    private var t = 0L
    private fun send(e: VehicleEvent) = s.onEvent(e, t)

    @Test fun `깜빡이를 켠 채 0점5km 달리면 한 번 알린다`() {
        send(VehicleEvent.Distance(100.0))
        send(VehicleEvent.TurnSignal(true))
        assertNull(send(VehicleEvent.Distance(100.3)))
        assertEquals(Notice.TURN_SIGNAL_LEFT_ON, send(VehicleEvent.Distance(100.6)))
        // 같은 켬에 대해서는 다시 알리지 않는다
        assertNull(send(VehicleEvent.Distance(101.5)))
        assertEquals(1, s.snapshot.turnSignalLeftOn)
        assertEquals(1.5, s.snapshot.km, 1e-9)
    }

    @Test fun `회전 후 바로 끄면 세지 않는다`() {
        send(VehicleEvent.Distance(100.0))
        send(VehicleEvent.TurnSignal(true))
        send(VehicleEvent.Distance(100.1))
        send(VehicleEvent.TurnSignal(false))
        send(VehicleEvent.Distance(102.0))
        assertEquals(0, s.snapshot.turnSignalLeftOn)
    }

    @Test fun `주차 브레이크 없이 문을 열면 센다`() {
        send(VehicleEvent.ParkingBrake(false))
        assertEquals(Notice.PARKING_BRAKE, send(VehicleEvent.DriverDoorOpened))
        send(VehicleEvent.ParkingBrake(true))
        assertNull(send(VehicleEvent.DriverDoorOpened))
        assertEquals(1, s.snapshot.exitWithoutParkingBrake)
    }

    @Test fun `10분 안에 경고 3번이면 쉬어 가기를 한 번 권한다`() {
        t = 0; assertNull(send(VehicleEvent.Warning("FCA")))
        t = 60_000; assertNull(send(VehicleEvent.Warning("LKA")))
        t = 120_000; assertEquals(Notice.TAKE_A_BREAK, send(VehicleEvent.Warning("FCA")))
        t = 130_000; assertNull(send(VehicleEvent.Warning("FCA")))
        assertEquals(4, s.snapshot.warnings)
    }

    @Test fun `경고가 띄엄띄엄이면 권하지 않는다`() {
        t = 0; send(VehicleEvent.Warning("FCA"))
        t = 11 * 60_000L; send(VehicleEvent.Warning("FCA"))
        t = 22 * 60_000L; assertNull(send(VehicleEvent.Warning("FCA")))
    }

    @Test fun `운전을 마치면 운전 1회 기록이 된다`() {
        send(VehicleEvent.Distance(50.0))
        send(VehicleEvent.BrakePedal(true)); send(VehicleEvent.BrakePedal(false))
        send(VehicleEvent.BrakePedal(true))
        send(VehicleEvent.Reroute)
        send(VehicleEvent.Distance(57.0))
        val trip = s.finish(endMillis = 25 * 60_000L)
        assertEquals(25, trip.minutes)
        assertEquals(7.0, trip.km, 1e-9)
        assertEquals(2, trip.brakePresses)
        assertEquals(1, trip.reroutes)
    }
}
