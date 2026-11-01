package moe.shizuku.manager.lspatch

import android.content.Context
import org.lsposed.patch.LSPatch
import org.lsposed.patch.util.Logger
import java.io.File

/**
 * Xiluoya: LSPatch patch 引擎的 Android 封装。
 *
 * 引擎（lspatchengine 模块）是 LSPatch 0.6 的 patch + apkzlib 原样移植，纯 JVM 代码。
 * 唯一的改动是把资源读取从 ClassLoader 换成可注入的 ResourceLoader —— Android 上
 * ClassLoader 读不到 APK 的 assets，必须走 Context.getAssets()。
 */
object LspatchPatcher {

    const val OUTPUT_SUFFIX = "-lspatched.apk"

    private var resourceLoaderInstalled = false

    private fun installResourceLoader(context: Context) {
        if (resourceLoaderInstalled) return
        LSPatch.setResourceLoader { path ->
            val p = if (path.startsWith("assets/")) path.substring("assets/".length) else path
            context.assets.open(p)
        }
        // ManifestEditor 用同一套 "assets/xxx" 约定读 public.xml（属性 ID 映射表），
        // Android 上同样走不到 ClassLoader，需要接到 assets。
        com.wind.meditor.utils.Utils.inputStreamProvider =
            java.util.function.Function<String, java.io.InputStream> { path: String ->
                val p = if (path.startsWith("assets/")) path.substring("assets/".length) else path
                try {
                    context.assets.open(p) as java.io.InputStream
                } catch (t: Throwable) {
                    null as java.io.InputStream
                }
            }
        resourceLoaderInstalled = true
    }

    /**
     * 同步执行打补丁，务必放在后台线程。
     *
     * @return 生成的 apk 文件
     * @throws Throwable 引擎内部抛 PatchError(Error) 或 IO 异常
     */
    fun patch(
        context: Context,
        srcApk: File,
        outputDir: File,
        embedModules: List<File> = emptyList(),
        onLog: (String) -> Unit
    ): File {
        installResourceLoader(context)
        outputDir.mkdirs()
        // 清掉上一轮产物，避免拿到旧文件
        outputDir.listFiles { f -> f.name.endsWith(OUTPUT_SUFFIX) }?.forEach { it.delete() }

        val logger = object : Logger() {
            override fun d(msg: String) {
                onLog(msg.trimEnd())
            }

            override fun i(msg: String) {
                onLog(msg.trimEnd())
            }

            override fun e(msg: String) {
                onLog("! " + msg.trimEnd())
            }
        }

        val args = ArrayList<String>()
        args.add(srcApk.absolutePath)
        args.add("-o")
        args.add(outputDir.absolutePath)
        args.add("-f")
        if (embedModules.isNotEmpty()) {
            args.add("-m")
            embedModules.forEach { args.add(it.absolutePath) }
        }

        val engine = LSPatch(logger, *args.toTypedArray())
        engine.doCommandLine()

        // LSPatch 的输出名是 <原名>-<versionCode>-lspatched.apk，别猜，直接扫目录
        return outputDir.listFiles { f -> f.name.endsWith(OUTPUT_SUFFIX) }
            ?.maxByOrNull { it.lastModified() }
            ?: throw IllegalStateException("patched apk not produced in ${outputDir.absolutePath}")
    }
}
