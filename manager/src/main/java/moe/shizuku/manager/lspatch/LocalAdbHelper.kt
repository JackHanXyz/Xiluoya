package moe.shizuku.manager.lspatch

import android.app.Activity
import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.R
import moe.shizuku.manager.starter.StarterActivity
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Xiluoya: 本机 ADB 启动。
 *
 * adbd 切到 TCP 模式后，手机自己可以连 127.0.0.1 启动服务，不需要电脑。
 * 真机 adbd 是 5555，MuMu 等模拟器是 7555，两个都探。
 */
object LocalAdbHelper {

    private val PORTS = intArrayOf(5555, 7555)

    fun findPort(): Int? = PORTS.firstOrNull { port ->
        try {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 800) }
            true
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * 探测端口并拉起 StarterActivity；端口不通时弹提示。
     * 需要在主线程调用（内部会自己开后台线程做探测）。
     */
    fun start(activity: Activity) {
        Thread {
            val port = findPort()
            activity.runOnUiThread {
                if (activity.isFinishing || activity.isDestroyed) return@runOnUiThread
                if (port != null) {
                    activity.startActivity(
                        Intent(activity, StarterActivity::class.java)
                            .putExtra(StarterActivity.EXTRA_IS_ROOT, false)
                            .putExtra(StarterActivity.EXTRA_HOST, "127.0.0.1")
                            .putExtra(StarterActivity.EXTRA_PORT, port)
                            .putExtra(StarterActivity.EXTRA_USE_BUNDLED_KEY, true)
                    )
                } else {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.settings_local_adb_title)
                        .setMessage(R.string.local_adb_not_listening)
                        .setPositiveButton(android.R.string.ok, null)
                        .show()
                }
            }
        }.start()
    }
}
