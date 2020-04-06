package net.ienlab.autodmb

import android.annotation.SuppressLint
import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.*
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

val TAG = "AutoDmbTAG"

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val sharedPreference = getSharedPreferences("${packageName}_pref", Context.MODE_PRIVATE)

        val serviceIntent = Intent(this, DmbRunService::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }

        val pm = packageManager
        val pkgAppsList = pm.getInstalledPackages(PackageManager.GET_META_DATA)
        val appsList: MutableList<AppInfo> = mutableListOf()
        val appsNameList: MutableList<String> = mutableListOf()
        val appsLabelList: MutableList<String> = mutableListOf()
        val apkInfoExtractor = ApkInfoExtractor(this)

        for (app in pkgAppsList) {
            val appInfo = AppInfo()
            appInfo.appLabel = apkInfoExtractor.GetAppName(app.packageName)
            appInfo.packageName = app.packageName
            appsList.add(appInfo)
        }
        appsList.sortWith(compareBy {it.appLabel})

        for (info in appsList) {
            appsNameList.add(info.packageName)
            appsLabelList.add(info.appLabel)
        }

        val mAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, appsLabelList)

        app_list.adapter = mAdapter
        app_list.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                sharedPreference.edit().putString("runPackageName", appsNameList[position]).apply()
                stopService(serviceIntent)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent)
                } else {
                    startService(serviceIntent)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {

            }
        }

        for (i in appsList.indices) {
            if (appsNameList[i] == sharedPreference.getString("runPackageName", "com.android.vending")) {
                app_list.setSelection(i)
                Log.d(TAG, appsNameList[i])
                break
            }
        }




        val devicePolicyManager = applicationContext.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val componentName = ComponentName(applicationContext, ShutdownConfigAdminReceiver::class.java)

        if (!devicePolicyManager.isAdminActive(componentName)) {
            var intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN)
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName)
            startActivityForResult(intent, 0)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            keyguardManager.requestDismissKeyguard(this, null)
        } else {
            this.window.addFlags(
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }

        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        var batteryReceiver = object: BroadcastReceiver() {
            @SuppressLint("InvalidWakeLockTag")
            override fun onReceive(context: Context, intent: Intent) {
                var action = intent.action
                when (action) {
                    Intent.ACTION_POWER_CONNECTED -> {
                        Toast.makeText(applicationContext, "전원 연결됨. 앱 실행 및 미디어 볼륨 최대", Toast.LENGTH_SHORT).show()
                        var launchIntent = packageManager.getLaunchIntentForPackage(sharedPreference.getString("runPackageName", "com.android.vending")!!)
                        launchIntent!!.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        if (launchIntent != null) {
                            startActivity(launchIntent)
                        }

                        am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), AudioManager.FLAG_SHOW_UI)
                    }
                    Intent.ACTION_POWER_DISCONNECTED -> {
                        Toast.makeText(applicationContext, "런처로 돌아갑니다.", Toast.LENGTH_SHORT).show()

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
                        devicePolicyManager.lockNow()
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

    override fun onBackPressed() {

    }
}
