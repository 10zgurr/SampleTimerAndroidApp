package com.sample.timer

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import android.os.IBinder
import android.widget.Toast

class MainActivity : AppCompatActivity() {

    companion object {
        private var TAG = MainActivity::class.java.simpleName
    }

    private lateinit var timerService: TimerService
    private var hasServiceConnected: Boolean = false

    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            val binder = service as TimerService.LocalBinder
            timerService = binder.getTimerService()
            hasServiceConnected = true
        }

        override fun onServiceDisconnected(arg0: ComponentName) {
            hasServiceConnected = false
        }
    }

    private var isTimerExpired = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        image_view_increase_time.setOnClickListener {
            if (isTimerExpired)
                Toast.makeText(this, getString(R.string.timer_expired_info), Toast.LENGTH_SHORT).show()
            else {
                timerService.cancelCountDown()

                timerService.remainingTime += TimerService.BOOST_TIME
                if (timerService.remainingTime > TimerService.INITIAL_TIME)
                    timerService.remainingTime = TimerService.INITIAL_TIME

                timerService.startCountDown()
            }
        }
    }

    private val timerBroadcast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (!isDestroyed) {
                runOnUiThread {
                    intent?.let { timerIntent ->
                        timerIntent.extras?.let { timerIntentExtras ->
                            if (timerIntentExtras.containsKey("timer_finished_bool")) {
                                isTimerExpired = timerIntentExtras.getBoolean("timer_finished_bool")
                                if (isTimerExpired) {
                                    text_view_remaining_time.text = String.format(getString(R.string.remaining_time_non_minutes), 0, 0)
                                    text_view_done.visibility = View.VISIBLE
                                }
                            } else {
                                val remainingMinutes = timerIntentExtras.getLong("remaining_mins").toInt()
                                val remainingSeconds = timerIntentExtras.getLong("remaining_secs").toInt()
                                val remainingMilliSeconds = timerIntentExtras.getLong("remaining_millis").toInt()

                                text_view_remaining_time.text = when (remainingMinutes) {
                                    0 -> String.format(getString(R.string.remaining_time_non_minutes), remainingSeconds, remainingMilliSeconds / 10) // to show user the one digit of milliseconds
                                    else -> String.format(getString(R.string.remaining_time), remainingMinutes, remainingSeconds, remainingMilliSeconds / 10)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // bind to the timer service
        Intent(this, TimerService::class.java).also { intent ->
            bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        startService(Intent(this, TimerService::class.java))
        Log.d(TAG, "timer service has been started")

        registerReceiver(timerBroadcast, IntentFilter(TimerService.TIMER_SERVICE_INTENT))
        Log.d(TAG, "broadcast receiver registered")
    }

    override fun onDestroy() {
        unregisterReceiver(timerBroadcast)
        Log.d(TAG, "broadcast receiver unregistered")

        stopService(Intent(this, TimerService::class.java))
        Log.d(TAG, "timer service has been stopped")

        // unbind the timer service
        unbindService(connection)
        hasServiceConnected = false
        super.onDestroy()
    }
}
