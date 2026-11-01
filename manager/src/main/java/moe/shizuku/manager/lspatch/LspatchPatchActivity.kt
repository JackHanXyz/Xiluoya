package moe.shizuku.manager.lspatch

import android.content.Intent
import android.net.Uri
import android.graphics.Typeface
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import moe.shizuku.manager.R
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.databinding.LspatchPatchActivityBinding
import java.io.File

/**
 * Xiluoya: 直接给本地 APK 打 LSPatch 补丁，再静默装回去。
 * 整个流程跑在 Xiluoya 里，不需要另外装 LSPatch manager。
 */
class LspatchPatchActivity : AppBarActivity() {

    private lateinit var binding: LspatchPatchActivityBinding
    private var patchedApk: File? = null
    private val sb = StringBuilder()
    private var moduleFiles: List<File> = emptyList()

    private val picker =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) startPatch(uri)
        }

    // 内嵌模块：多选 APK，拷到 cacheDir 再交给引擎
    private val modulesPicker =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (!uris.isNullOrEmpty()) copyModules(uris)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_close_24)

        binding = LspatchPatchActivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonInstall.isEnabled = false
        binding.buttonPick.setOnClickListener {
            picker.launch(arrayOf("application/vnd.android.package-archive", "*/*"))
        }
        binding.buttonInstall.setOnClickListener { install() }
        binding.buttonModules.setOnClickListener {
            modulesPicker.launch(arrayOf("*/*"))
        }
        updateModuleText()
        renderHistory()
    }

    // ---------- 补丁历史 ----------

    private fun historyPrefs() = getSharedPreferences("patch_history", MODE_PRIVATE)

    private fun readHistory(): List<org.json.JSONObject> {
        val arr = org.json.JSONArray(historyPrefs().getString("items", "[]"))
        return (0 until arr.length()).map { arr.getJSONObject(it) }
    }

    private fun saveHistory(source: String, output: File) {
        val list = readHistory().toMutableList()
        list.add(0, org.json.JSONObject()
            .put("source", source)
            .put("path", output.absolutePath)
            .put("time", System.currentTimeMillis()))
        while (list.size > 10) list.removeAt(list.size - 1)
        historyPrefs().edit().putString("items", list.joinToString(",")).apply()
    }

    private fun renderHistory() {
        val container = binding.historyContainer
        container.removeAllViews()
        val items = readHistory()
        if (items.isEmpty()) return

        val dp = resources.displayMetrics.density
        items.forEach { item ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, (6 * dp).toInt(), 0, (6 * dp).toInt())
            }
            val info = TextView(this).apply {
                val name = File(item.optString("path")).name
                text = name
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(0xFFFFFFFF.toInt())
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            val btn = Button(this).apply {
                text = getString(R.string.patch_history_install)
                textSize = 12f
                isAllCaps = false
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (32 * dp).toInt()
                )
                setOnClickListener { installFile(File(item.optString("path"))) }
            }
            row.addView(info)
            row.addView(btn)
            container.addView(row)
        }
    }

    private fun installFile(f: File) {
        if (!f.exists()) {
            Toast.makeText(this, "文件不存在", Toast.LENGTH_SHORT).show()
            return
        }
        patchedApk = f
        install()
    }

    private fun copyModules(uris: List<Uri>) {
        log("读取内嵌模块…")
        Thread {
            val dir = File(cacheDir, "modules").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val files = uris.mapNotNull { uri ->
                try {
                    val name = queryName(uri) ?: "module_${System.currentTimeMillis()}.apk"
                    val f = File(dir, name)
                    contentResolver.openInputStream(uri)?.use { input ->
                        f.outputStream().use { input.copyTo(it) }
                    } ?: return@mapNotNull null
                    f
                } catch (t: Throwable) {
                    log("模块读取失败: ${t.message}")
                    null
                }
            }
            runOnUiThread {
                moduleFiles = files
                updateModuleText()
                log("已内嵌 ${files.size} 个模块")
            }
        }.start()
    }

    private fun queryName(uri: Uri): String? =
        contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }

    private fun updateModuleText() {
        binding.textModules.text =
            if (moduleFiles.isEmpty()) getString(R.string.patch_modules_none)
            else getString(R.string.patch_modules_selected, moduleFiles.size)
    }

    private fun log(line: String) {
        runOnUiThread {
            sb.append(line).append('\n')
            binding.textLog.text = sb.toString()
            binding.textLog.post {
                (binding.textLog.parent as? android.view.View)?.let { v ->
                    if (v is android.widget.ScrollView) v.fullScroll(android.view.View.FOCUS_DOWN)
                }
            }
        }
    }

    private fun startPatch(uri: Uri) {
        sb.setLength(0)
        binding.buttonInstall.isEnabled = false
        log("读取源 APK…")

        Thread {
            try {
                val src = File(cacheDir, "src.apk")
                contentResolver.openInputStream(uri)?.use { input ->
                    src.outputStream().use { output -> input.copyTo(output) }
                } ?: throw IllegalStateException("无法读取所选文件")
                log("源文件大小: ${src.length() / 1024} KB")

                val outDir = File(cacheDir, "patch")
                val out = LspatchPatcher.patch(this, src, outDir, moduleFiles) { log(it) }

                // shell 读不到 cacheDir，要挪到外部私有目录
                val ext = getExternalFilesDir(null) ?: throw IllegalStateException("外部目录不可用")
                val dst = File(ext, out.name)
                out.copyTo(dst, overwrite = true)
                patchedApk = dst

                log("")
                log("补丁完成: ${dst.absolutePath}")

                runOnUiThread {
                    saveHistory(queryName(uri) ?: "src.apk", dst)
                    renderHistory()
                }
                runOnUiThread { binding.buttonInstall.isEnabled = true }
            } catch (t: Throwable) {
                log("")
                log("失败: ${t.message ?: t.javaClass.simpleName}")
                Log.w(TAG, "patch failed", t)
            }
        }.start()
    }

    private fun install() {
        val apk = patchedApk ?: return
        binding.buttonInstall.isEnabled = false
        log("")
        log("开始安装（本机 adb）…")

        LspatchHelper.installApk(this, apk) { ok, message ->
            log(message)
            runOnUiThread {
                binding.buttonInstall.isEnabled = true
                Toast.makeText(
                    this,
                    if (ok) R.string.home_lspatch_patch_installed else R.string.home_lspatch_install_failed,
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val TAG = "LspatchPatch"

        fun start(context: android.content.Context) {
            val intent = Intent(context, LspatchPatchActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }
    }
}
