package moe.shizuku.manager.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import moe.shizuku.manager.databinding.HomeItemContainerBinding
import moe.shizuku.manager.databinding.HomeLocalAdbBinding
import moe.shizuku.manager.lspatch.LocalAdbHelper
import rikka.recyclerview.BaseViewHolder
import rikka.recyclerview.BaseViewHolder.Creator

/**
 * Xiluoya: 首页的「本地 ADB 启动」卡片。
 * 不需要电脑，手机自己通过 127.0.0.1 拉起服务。
 */
class LocalAdbViewHolder(private val binding: HomeLocalAdbBinding, root: View) :
    BaseViewHolder<Any>(root) {

    companion object {
        val CREATOR = Creator<Any> { inflater: LayoutInflater, parent: ViewGroup? ->
            val outer = HomeItemContainerBinding.inflate(inflater, parent, false)
            val inner = HomeLocalAdbBinding.inflate(inflater, outer.root, true)
            LocalAdbViewHolder(inner, outer.root)
        }
    }

    init {
        // 端口探测放到点击后做，别在 onBind 里阻塞主线程
        binding.button1.setOnClickListener {
            val activity = context as? android.app.Activity ?: return@setOnClickListener
            LocalAdbHelper.start(activity)
        }
    }
}
