package moe.shizuku.manager.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.home.HomeAdapter
import moe.shizuku.manager.home.HomeViewModel
import rikka.recyclerview.addEdgeSpacing
import rikka.recyclerview.addItemSpacing
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.Shizuku

/**
 * Xiluoya: 首页 —— 服务状态卡 + 启动入口，复用原来的 HomeAdapter。
 */
class HomeFragment : Fragment(R.layout.fragment_home) {

    private val homeModel: HomeViewModel by activityViewModels()
    private val appsModel by lazy { appsViewModel() }
    private val adapter by lazy { HomeAdapter(homeModel, appsModel) }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        homeModel.reload()
        appsModel.load()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        homeModel.reload()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null
        recyclerView.fixEdgeEffect()
        recyclerView.addItemSpacing(top = 8f, bottom = 8f, unit = TypedValue.COMPLEX_UNIT_DIP)
        recyclerView.addEdgeSpacing(
            top = 4f, bottom = 4f, left = 16f, right = 16f,
            unit = TypedValue.COMPLEX_UNIT_DIP
        )

        homeModel.serviceStatus.observe(viewLifecycleOwner) {
            adapter.updateData()
            if (!isHidden) updateSubtitle()
        }
        appsModel.grantedCount.observe(viewLifecycleOwner) { adapter.updateData() }

        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
    }

    override fun onResume() {
        super.onResume()
        applyTitle()
        homeModel.reload()
    }

    // hide/show 切换不会触发 onResume，标题必须在这里同步
    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            applyTitle()
            homeModel.reload()
        }
    }

    private fun applyTitle() {
        val bar = (activity as? AppCompatActivity)?.supportActionBar ?: return
        bar.title = getString(R.string.app_display_name)
        updateSubtitle()
    }

    private fun updateSubtitle() {
        val bar = (activity as? AppCompatActivity)?.supportActionBar ?: return
        val status = homeModel.serviceStatus.value?.data
        bar.subtitle = when {
            status == null || !status.isRunning -> getString(R.string.subtitle_not_running)
            status.uid == 0 -> getString(R.string.subtitle_running_root)
            else -> getString(R.string.subtitle_running_adb)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
    }
}
