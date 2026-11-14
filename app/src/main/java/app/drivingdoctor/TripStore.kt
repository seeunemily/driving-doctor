package app.drivingdoctor

import android.content.Context
import app.drivingdoctor.domain.Trip
import app.drivingdoctor.domain.TripCodec
import java.io.File

/** 운전 기록 저장소. 앱 전용 저장 공간의 파일 하나에 한 줄씩 덧붙인다. 밖으로 보내지 않는다. */
class TripStore(context: Context) {
    private val file = File(context.filesDir, "trips.csv")

    fun load(): List<Trip> =
        if (!file.exists()) emptyList() else file.readLines().mapNotNull { TripCodec.decode(it) }

    fun append(t: Trip) = file.appendText(TripCodec.encode(t) + "\n")

    fun clear() { file.delete() }
}
