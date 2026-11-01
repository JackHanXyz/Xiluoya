package moe.shizuku.manager

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.appcompat.app.AppCompatDelegate
import com.topjohnwu.superuser.Shell
import moe.shizuku.manager.ktx.logd
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.core.util.BuildUtils.atLeast30
import rikka.material.app.LocaleDelegate
import rikka.shizuku.Shizuku

lateinit var application: ShizukuApplication

class ShizukuApplication : Application() {

    companion object {

        private const val CHANNEL_SERVICE = "service_status"
        private const val NOTIFY_ID = 1001

        init {
            logd("ShizukuApplication", "init")

            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_REDIRECT_STDERR))
            if (Build.VERSION.SDK_INT >= 28) {
                // Xiluoya: 部分定制 ROM / 模拟器的 ART 会让 HiddenApiBypass 静态初始化直接抛
                // ExceptionInInitializerError，这里降级为警告，避免整个应用起不来。
                try {
                    HiddenApiBypass.setHiddenApiExemptions("")
                } catch (t: Throwable) {
                    android.util.Log.w("Xiluoya", "HiddenApiBypass init failed", t)
                }
            }
            if (atLeast30) {
                try {
                    System.loadLibrary("adb")
                } catch (t: Throwable) {
                    android.util.Log.w("Xiluoya", "loadLibrary(adb) failed", t)
                }
            }
        }
    }

    private fun init(context: Context?) {
        ShizukuSettings.initialize(context)
        LocaleDelegate.defaultLocale = ShizukuSettings.getLocale()
        AppCompatDelegate.setDefaultNightMode(ShizukuSettings.getNightMode())
    }

    override fun onCreate() {
        super.onCreate()
        application = this
        init(this)
        registerServiceNotification()
    }

    // ---------- 服务状态常驻通知 ----------

    private fun registerServiceNotification() {
        try {
            Shizuku.addBinderReceivedListenerSticky { showServiceNotification() }
            Shizuku.addBinderDeadListener { cancelServiceNotification() }
        } catch (t: Throwable) {
            android.util.Log.w("Xiluoya", "register binder notification failed", t)
        }
    }

    private fun showServiceNotification() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_SERVICE,
                    getString(R.string.notif_channel),
                    NotificationManager.IMPORTANCE_LOW
                )
            )
        }
        // API 33+ 通知是运行时权限，没批就静默跳过
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val pi = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif = NotificationCompat.Builder(this, CHANNEL_SERVICE)
            .setSmallIcon(R.drawable.ic_server_start_24dp)
            .setContentTitle(getString(R.string.app_display_name))
            .setContentText(getString(R.string.notif_running))
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
        nm.notify(NOTIFY_ID, notif)
    }

    private fun cancelServiceNotification() {
        getSystemService(NotificationManager::class.java)?.cancel(NOTIFY_ID)
    }

}
