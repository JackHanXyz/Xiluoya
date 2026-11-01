package moe.shizuku.manager.adb

import android.util.Base64
import android.util.Log
import moe.shizuku.manager.application
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec

/**
 * Xiluoya: 内置于 assets 的 adb 私钥。
 *
 * 用途：手机在 adbd 处于 TCP 模式（127.0.0.1:5555）时，App 需要以"已被授权的 host"
 * 身份完成 AUTH 认证。用户设备的 /data/misc/adb 被 SELinux 锁死（shell 都读写不了），
 * App 生成的 key 无法写入 adb_keys，所以只能复用一只已经在 adb_keys 里的 key
 * （即构建时打包进来的那只）。
 *
 * 该 key 也用于无线调试配对流程（与官方行为一致，统一身份）。
 */
object BundledAdbKey {

    private const val TAG = "BundledAdbKey"
    private const val ASSET_NAME = "adbkey"

    @Volatile
    private var cached: RSAPrivateKey? = null

    @Volatile
    private var loaded = false

    fun get(): RSAPrivateKey? {
        if (loaded) return cached
        synchronized(this) {
            if (loaded) return cached
            loaded = true
            cached = load()
        }
        return cached
    }

    private fun load(): RSAPrivateKey? = try {
        val pem = application.assets.open(ASSET_NAME).bufferedReader().use { it.readText() }
        val base64 = pem
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace(Regex("\\s"), "")
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(bytes)) as RSAPrivateKey
    } catch (t: Throwable) {
        Log.w(TAG, "bundled adb key not available", t)
        null
    }
}
