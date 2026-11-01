package moe.shizuku.manager.lspatch

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.adb.AdbClient
import moe.shizuku.manager.adb.AdbKey
import moe.shizuku.manager.adb.PreferenceAdbKeyStore
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Xiluoya: LSPatch 内置管理。
 *
 * LSPatch 的 manager 用 Kotlin 1.8 + Compose 2023.06 编译，和本体（Kotlin 2.1）不在同一条
 * 工具链上，硬合并需要迁移整个 Compose 栈（含已停更的 compose-destinations），因此这里采用
 * 「内置 APK + 本机安装 + 直达入口」的方案：APK 随 Xiluoya 一起分发，用户不需要另外找安装包，
 * 安装动作由 Xiluoya 通过本机 adb（127.0.0.1:5555）完成，不需要电脑。
 */
object LspatchHelper {

    private const val TAG = "LspatchHelper"
    private const val ASSET_NAME = "lspatch.apk"
    // 真机 adbd 走 5555；MuMu 等模拟器走 7555，两个都试
    private val PORTS = intArrayOf(5555, 7555)

    const val PACKAGE = "org.lsposed.lspatch"

    fun isInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    fun version(context: Context): String? = try {
        context.packageManager.getPackageInfo(PACKAGE, 0).versionName
    } catch (t: Throwable) {
        null
    }

    fun launch(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(PACKAGE) ?: return false
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            context.startActivity(intent)
            true
        } catch (t: Throwable) {
            Log.w(TAG, "launch failed", t)
            false
        }
    }

    /** adbd 是否处于 TCP 模式（本机可达） */
    fun isLocalAdbListening(): Boolean = localAdbPort() != null

    fun localAdbPort(): Int? = PORTS.firstOrNull { port ->
        try {
            Socket().use { it.connect(InetSocketAddress("127.0.0.1", port), 800) }
            true
        } catch (t: Throwable) {
            false
        }
    }

    /**
     * 用内置 APK 静默安装 LSPatch。
     * 需要 adbd 在 TCP 模式；安装动作本身走 shell 权限，不需要电脑。
     */
    fun install(context: Context, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val dir = context.getExternalFilesDir(null)
                    ?: throw IllegalStateException("external files dir unavailable")
                val apk = File(dir, ASSET_NAME)
                context.assets.open(ASSET_NAME).use { input ->
                    apk.outputStream().use { output -> input.copyTo(output) }
                }

                val port = localAdbPort()
                if (port == null) {
                    onResult(false, "adbd 未在 TCP 模式")
                    return@Thread
                }

                val key = AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                val output = StringBuilder()
                AdbClient("127.0.0.1", port, key).use { client ->
                    client.connect()
                    client.shellCommand(
                        "cp '${apk.absolutePath}' /data/local/tmp/lspatch.apk && " +
                                "pm install -r -d /data/local/tmp/lspatch.apk && " +
                                "rm -f /data/local/tmp/lspatch.apk"
                    ) { bytes ->
                        output.append(String(bytes))
                    }
                }

                val ok = isInstalled(context)
                onResult(ok, if (ok) "安装成功" else output.toString())
            } catch (t: Throwable) {
                Log.w(TAG, "install failed", t)
                onResult(false, t.message ?: t.javaClass.simpleName)
            }
        }.start()
    }
    /**
     * 用本机 adb（shell 权限）安装任意 apk。必须先 ensure external 目录可被 shell 读取。
     */
    fun installApk(context: Context, apk: File, onResult: (Boolean, String) -> Unit) {
        Thread {
            try {
                val port = localAdbPort()
                if (port == null) {
                    onResult(false, "adbd 未在 TCP 模式")
                    return@Thread
                }
                val key = AdbKey(PreferenceAdbKeyStore(ShizukuSettings.getPreferences()), "shizuku")
                val output = StringBuilder()
                AdbClient("127.0.0.1", port, key).use { client ->
                    client.connect()
                    client.shellCommand(
                        "cp '${apk.absolutePath}' /data/local/tmp/xiluoya-install.apk && " +
                                "pm install -r -d /data/local/tmp/xiluoya-install.apk; " +
                                "rc=\$?; rm -f /data/local/tmp/xiluoya-install.apk; exit \$rc"
                    ) { bytes -> output.append(String(bytes)) }
                }
                val log = output.toString()
                onResult(log.contains("Success") || log.contains("success"), log)
            } catch (t: Throwable) {
                Log.w(TAG, "installApk failed", t)
                onResult(false, t.message ?: t.javaClass.simpleName)
            }
        }.start()
    }
}
