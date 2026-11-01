package moe.shizuku.manager.ui

import android.os.Bundle
import android.content.Intent
import android.net.Uri
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import moe.shizuku.manager.R
import com.google.android.material.button.MaterialButton

/**
 * Xiluoya: 关于页。技术栈 + 源码入口。
 */
class AboutFragment : Fragment(R.layout.fragment_about) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val context = requireContext()

        view.findViewById<ImageView>(R.id.app_icon).setImageDrawable(
            context.applicationInfo.loadIcon(context.packageManager)
        )

        val versionName = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (t: Throwable) {
            "?"
        }
        view.findViewById<TextView>(R.id.version).text =
            getString(R.string.about_version, versionName)

        view.findViewById<TextView>(R.id.tech_body).text = getString(R.string.about_tech_body)

        view.findViewById<MaterialButton>(R.id.btn_source).setOnClickListener {
            openUrl(context, "https://github.com/RikkaApps/Shizuku")
        }
        view.findViewById<MaterialButton>(R.id.btn_licenses).setOnClickListener {
            openUrl(context, "https://github.com/LSPosed/LSPatch")
        }
    }

    override fun onResume() {
        super.onResume()
        applyTitle()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) applyTitle()
    }

    private fun openUrl(context: android.content.Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (t: Throwable) {
            android.widget.Toast.makeText(context, url, android.widget.Toast.LENGTH_LONG).show()
        }
    }

    private fun applyTitle() {
        val bar = (activity as? AppCompatActivity)?.supportActionBar ?: return
        bar.title = getString(R.string.nav_about)
        bar.subtitle = null
    }
}
