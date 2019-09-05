package com.sample.timer

import android.app.Service
import android.content.Intent
import android.os.Binder
import android.os.CountDownTimer
import android.os.IBinder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class TimerService : Service() {

    companion object {
        const val INITIAL_TIME: Long = 2 * 60 * 1000 // 2 minutes
        const val BOOST_TIME: Long = 10 * 1000 // 10 seconds
        const val TIMER_SERVICE_INTENT = "timerIntentFilter"
    }

    private var countDownTimer: CountDownTimer? = null

    private var remainingMinutes = 0L
    private var remainingSeconds = 0L
    private var remainingMilliSeconds = 0L

    var remainingTime = INITIAL_TIME

    private var timerIntent = Intent(TIMER_SERVICE_INTENT)

    private val binder = LocalBinder()

    inner class LocalBinder : Binder() {
        fun getTimerService(): TimerService = this@TimerService
    }

    override fun onCreate() {
        super.onCreate()

        startCountDown()
    }

    fun startCountDown() {
        GlobalScope.launch(Dispatchers.Main) { // do these steps in an another thread for better performance
            countDownTimer = object : CountDownTimer(remainingTime, 1) {
                override fun onTick(millisUntilFinished: Long) {
                    remainingTime = millisUntilFinished
                    remainingMinutes = (millisUntilFinished / 1000) / 60
                    remainingSeconds = millisUntilFinished / 1000
                    remainingMilliSeconds = millisUntilFinished

                    remainingSeconds %= 60
                    remainingMilliSeconds %= 100

                    timerIntent.putExtra("remaining_mins", remainingMinutes)
                    timerIntent.putExtra("remaining_secs", remainingSeconds)
                    timerIntent.putExtra("remaining_millis", remainingMilliSeconds)
                    sendBroadcast(timerIntent)
                }

                override fun onFinish() {
                    timerIntent.putExtra("timer_finished_bool", true)
                    sendBroadcast(timerIntent)
                }
            }.start()
        }
    }

    fun cancelCountDown() {
        countDownTimer?.cancel()
        countDownTimer = null
    }

    override fun onBind(intent: Intent?): IBinder? {
        return binder
    }

    override fun onDestroy() {
        cancelCountDown()
        super.onDestroy()
    }
}