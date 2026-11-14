package app.drivingdoctor.voice

import android.content.Context
import android.os.SystemClock
import android.util.Log
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.EventListener
import ai.pleos.playground.tts.listener.OnInitializeListener
import java.util.Locale
import android.speech.tts.TextToSpeech as AndroidTts

/**
 * 음성 안내. 주행 중 안내는 화면 대신 모두 이 소리로 한다.
 * 차량(Pleos Connect)에서는 Pleos TextToSpeech SDK(차 안 음성 엔진)를 먼저 쓰고,
 * 그 엔진이 없는 일반 안드로이드 기기에서는 안드로이드 기본 음성으로 바꾼다.
 * "Perf" 로그에 준비 시간과 음성 응답 지연(말하라고 한 때부터 소리가 나기 시작할 때까지)을 남긴다.
 */
class Speaker(context: Context) {
    private val app = context.applicationContext
    private val created = SystemClock.elapsedRealtime()
    private var pleos: ai.pleos.playground.tts.TextToSpeech? = null
    private var android: AndroidTts? = null
    private var ready = false
    private var pending: String? = null
    private var askedAt = 0L
    var enabled = true

    init {
        try {
            val t = ai.pleos.playground.tts.TextToSpeech(app, Mode.ON_DEVICE)
            pleos = t
            t.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    // 리스너는 준비가 끝난 뒤에 붙여야 한다(SDK 규칙)
                    t.addEventListener(object : EventListener {
                        override fun onStart() = started()
                        override fun onReady() {}
                        override fun onDone() {}
                        override fun onError(e: ErrorType) { Log.w("Perf", "Pleos 음성 오류 $e") }
                        override fun onStop() {}
                        override fun onUpdatedRms(rms: Double) {}
                    })
                    runCatching { t.setLocale(Locale.KOREA) }
                    onReady("Pleos")
                }
                override fun onFailure(e: ErrorType) {
                    Log.w("Perf", "Pleos 음성 준비 실패 $e → 안드로이드 기본 음성")
                    runCatching { t.release() }
                    pleos = null
                    useAndroid()
                }
            })
        } catch (e: Throwable) {
            Log.w("Perf", "Pleos 음성 없음 ${e.javaClass.simpleName} → 안드로이드 기본 음성")
            pleos = null
            useAndroid()
        }
    }

    private fun useAndroid() {
        var tts: AndroidTts? = null
        tts = AndroidTts(app) { status ->
            if (status != AndroidTts.SUCCESS) { Log.w("Perf", "안드로이드 음성 준비 실패 $status"); return@AndroidTts }
            tts?.apply {
                language = Locale.KOREAN
                setSpeechRate(0.9f)
                setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(id: String) = started()
                    override fun onDone(id: String) {}
                    @Deprecated("") override fun onError(id: String) {}
                })
            }
            onReady("Android")
        }
        android = tts
    }

    private fun onReady(engine: String) {
        Log.i("Perf", "음성 준비 $engine ${SystemClock.elapsedRealtime() - created}ms")
        ready = true
        pending?.let { say(it) }
        pending = null
    }

    private fun started() {
        if (askedAt > 0) Log.i("Perf", "음성 응답 ${SystemClock.elapsedRealtime() - askedAt}ms")
        askedAt = 0
    }

    fun say(text: String) {
        if (!enabled) return
        if (!ready) { pending = text; return }
        askedAt = SystemClock.elapsedRealtime()
        pleos?.let { it.speak(text, true); return }
        android?.speak(text, AndroidTts.QUEUE_FLUSH, null, "say-$askedAt")
    }

    fun shutdown() {
        runCatching { pleos?.release() }
        android?.shutdown()
    }
}
