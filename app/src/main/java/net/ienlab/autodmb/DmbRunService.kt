package net.ienlab.autodmb

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat


class DmbRunService : Service() {

    lateinit var powerManager: PowerManager
    lateinit var wakeLock: PowerManager.WakeLock

    override fun onBind(intent: Intent): IBinder? {
        return null
    }


    override fun onCreate() {
        super.onCreate()

        val sharedPreference = getSharedPreferences("${packageName}_pref", Context.MODE_PRIVATE)
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(ChannelId.ALWAYS_ID, "상시 알림", NotificationManager.IMPORTANCE_MIN)
            nm.createNotificationChannel(channel)
        }

//        val notificationIntent = Intent(this, Splash::class.java)
//        val notiIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0)

        val always = NotificationCompat.Builder(this, ChannelId.ALWAYS_ID)
        always.setContentTitle(getString(R.string.app_name))
            .setContentText("DMB 실행 대기 중")
//            .setContentIntent(notiIntent)
            .setSmallIcon(R.drawable.ic_alert)
            .setAutoCancel(true)
            .setColor(ContextCompat.getColor(applicationContext, R.color.colorAccent))
            .setShowWhen(false)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
        startForeground(3804238, always.build())

        val devicePolicyManager = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        var batteryReceiver = object: BroadcastReceiver() {
            @SuppressLint("InvalidWakeLockTag")
            override fun onReceive(context: Context, intent: Intent) {
                var cnt = 0
                var action = intent.action
                when (action) {
                    Intent.ACTION_POWER_CONNECTED -> {

                        Toast.makeText(applicationContext, "Power Connected", Toast.LENGTH_SHORT).show()
                        var launchIntent = packageManager.getLaunchIntentForPackage(sharedPreference.getString("runPackageName", "com.android.vending")!!)
                        launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }

                        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        Toast.makeText(applicationContext, "Power Disconnected", Toast.LENGTH_SHORT).show()
//                        Thread(Runnable {
//                            Instrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_HOME)
//                        }).start()

                        val intent = Intent()
                        intent.action = "android.intent.action.MAIN"
                        intent.addCategory("android.intent.category.HOME")
                        intent.addFlags(
                            Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS
                                    or Intent.FLAG_ACTIVITY_FORWARD_RESULT
                                    or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP
                                    or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        )
                        startActivity(intent)

//                        devicePolicyManager.lockNow()
                    }
                }
            }
        }

        var filter = IntentFilter()
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        filter.addAction(Intent.ACTION_BATTERY_LOW)
        filter.addAction(Intent.ACTION_BATTERY_OKAY)
        filter.addAction(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        registerReceiver(batteryReceiver, filter)


    }

}
