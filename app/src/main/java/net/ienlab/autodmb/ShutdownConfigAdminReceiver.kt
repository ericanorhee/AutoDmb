package net.ienlab.autodmb

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast


class ShutdownConfigAdminReceiver: DeviceAdminReceiver() {
    override fun onDisabled(context: Context, intent: Intent) {
        Toast.makeText(context, "관리자 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
    }

    override fun onEnabled(context: Context, intent: Intent) {
        Toast.makeText(context, "관리자 권한을 승인받았습니다.", Toast.LENGTH_SHORT).show()
    }
}