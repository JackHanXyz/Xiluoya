package moe.shizuku.manager.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import moe.shizuku.manager.R
import moe.shizuku.manager.lspatch.LspatchHelper
import moe.shizuku.manager.lspatch.LspatchPatchActivity

/**
 * Xiluoya: 模块页 —— LSPatch 的安装状态与本机打补丁入口。
 *
 * 引擎（lspatchengine）直接编进了 App，所以打补丁不需要先装 LSPatch manager。
 */
class ModulesFragment : Fragment(R.layout.fragment_modules) {

    private lateinit var status: TextView
    private lateinit var openButton: MaterialButton
    private lateinit var patchButton: MaterialButton
    private lateinit var installButton: MaterialButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        status = view.findViewById(R.id.lspatch_status)
        openButton = view.findViewById(R.id.btn_open)
        patchButton = view.findViewById(R.id.btn_patch)
        installButton = view.findViewById(R.id.btn_install)

        openButton.setOnClickListener {
            if (!LspatchHelper.launch(requireContext())) {
                Toast.makeText(requireContext(), R.string.home_lspatch_launch_failed, Toast.LENGTH_SHORT)
                    .show()
            }
        }

        patchButton.setOnClickListener {
            LspatchPatchActivity.start(requireContext())
        }

        installButton.setOnClickListener {
            installButton.isEnabled = false
            installButton.text = getString(R.string.home_lspatch_installing)
            LspatchHelper.install(requireContext()) { ok, message ->
                view.post {
                    installButton.isEnabled = true
                    installButton.text = getString(R.string.home_lspatch_button_install)
                    refresh()
                    if (!ok) {
                        Toast.makeText(
                            requireContext(),
                            getString(R.string.home_lspatch_install_failed, message),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }

        refresh()
    }

    private fun refresh() {
        val context = context ?: return
        val installed = LspatchHelper.isInstalled(context)
        status.text = if (installed) {
            getString(R.string.home_lspatch_description_installed, LspatchHelper.version(context) ?: "?")
        } else {
            getString(R.string.home_lspatch_description)
        }
        applyTitle()
        openButton.isEnabled = installed
        installButton.visibility = if (installed) View.GONE else View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) refresh()
    }

    private fun applyTitle() {
        val bar = (activity as? AppCompatActivity)?.supportActionBar ?: return
        val context = context ?: return
        bar.title = getString(R.string.nav_module)
        bar.subtitle = if (LspatchHelper.isInstalled(context)) {
            getString(R.string.subtitle_lspatch, LspatchHelper.version(context) ?: "?")
        } else {
            getString(R.string.subtitle_lspatch_absent)
        }
    }
}
