package moe.shizuku.manager.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import moe.shizuku.manager.R
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeLspatchBinding
import moe.shizuku.manager.lspatch.LspatchHelper
import moe.shizuku.manager.lspatch.LspatchPatchActivity
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

/**
 * Xiluoya: 首页的 LSPatch 卡片。
 * 已安装 -> [打开 LSPatch] + [给 APK 打补丁]；未安装 -> [安装内置版本]。
 */
class LspatchViewHolder(private val binding: HomeLspatchBinding, root: View) :
    BaseViewHolder<Any>(root) {

    companion object {
        val CREATOR = Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeLspatchBinding.inflate(inflater, outer.root, true)
            LspatchViewHolder(inner, outer.root)
        }
    }

    private inline val open get() = binding.button1
    private inline val action get() = binding.button2

    init {
        binding.button3.setOnClickListener {
            LspatchPatchActivity.start(context)
        }

        open.setOnClickListener {
            if (!LspatchHelper.launch(context)) {
                Toast.makeText(context, R.string.home_lspatch_launch_failed, Toast.LENGTH_SHORT).show()
            }
        }

        action.setOnClickListener { installBundled() }
    }

    private fun installBundled() {
        action.isEnabled = false
        action.text = context.getString(R.string.home_lspatch_installing)
        LspatchHelper.install(context) { ok, message ->
            itemView.post {
                refresh()
                if (!ok) {
                    Toast.makeText(
                        context,
                        context.getString(R.string.home_lspatch_install_failed, message),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun refresh() {
        val installed = LspatchHelper.isInstalled(context)

        open.visibility = if (installed) View.VISIBLE else View.GONE
        action.isEnabled = true

        action.visibility = if (installed) View.GONE else View.VISIBLE
        action.text = context.getString(R.string.home_lspatch_button_install)
        action.setIconResource(R.drawable.ic_install_24dp)

        binding.text1.text = if (installed) {
            context.getString(
                R.string.home_lspatch_description_installed,
                LspatchHelper.version(context) ?: "?"
            )
        } else {
            context.getString(R.string.home_lspatch_description)
        }
    }

    override fun onBind() = refresh()
}
