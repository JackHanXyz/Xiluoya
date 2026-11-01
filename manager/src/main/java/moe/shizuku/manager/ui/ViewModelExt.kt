package moe.shizuku.manager.ui

import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import moe.shizuku.manager.management.AppsViewModel

/**
 * Xiluoya: AppsViewModel 需要 Context 参数，AndroidX 的默认 Factory 造不出来，
 * 这里给 Fragment 提供一个作用域在 Activity 上的工厂，保证跨 tab 共享同一份实例。
 */
@Suppress("UNCHECKED_CAST")
fun Fragment.appsViewModel(): AppsViewModel {
    val factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AppsViewModel(requireContext().applicationContext) as T
        }
    }
    return ViewModelProvider(requireActivity(), factory)[AppsViewModel::class.java]
}
