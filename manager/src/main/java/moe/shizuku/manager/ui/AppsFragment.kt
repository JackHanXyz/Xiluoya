package moe.shizuku.manager.ui

import android.content.pm.PackageInfo
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.R
import moe.shizuku.manager.management.AppsAdapter
import rikka.lifecycle.Status
import rikka.recyclerview.fixEdgeEffect
import rikka.shizuku.Shizuku

/**
 * Xiluoya: 应用页 —— 已授权应用列表，带搜索。
 */
class AppsFragment : Fragment(R.layout.fragment_apps) {

    private val viewModel by lazy { appsViewModel() }
    private val adapter = AppsAdapter()

    private var emptyView: TextView? = null
    private var allPackages: List<PackageInfo>? = null
    private var query = ""

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val list = view.findViewById<RecyclerView>(R.id.list)
        val empty = view.findViewById<TextView>(R.id.empty)
        emptyView = empty

        list.layoutManager = LinearLayoutManager(requireContext())
        list.adapter = adapter
        list.itemAnimator = null
        list.fixEdgeEffect()

        viewModel.packages.observe(viewLifecycleOwner) {
            if (it.status == Status.SUCCESS) {
                allPackages = it.data
                applyFilter()
            }
        }

        view.findViewById<EditText>(R.id.search).addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                query = s?.toString()?.trim() ?: ""
                applyFilter()
            }
        })
    }

    /** 按应用名或包名过滤 */
    private fun applyFilter() {
        val pm = context?.packageManager ?: return
        val filtered = allPackages?.filter { pkg ->
            if (query.isEmpty()) true else {
                try {
                    pkg.applicationInfo?.loadLabel(pm)?.toString()
                        ?.contains(query, ignoreCase = true) == true ||
                            pkg.packageName.contains(query, ignoreCase = true)
                } catch (t: Throwable) {
                    pkg.packageName.contains(query, ignoreCase = true)
                }
            }
        }
        adapter.updateData(filtered)
        updateEmptyState()
    }

    private fun updateEmptyState() {
        val view = emptyView ?: return
        if (!Shizuku.pingBinder()) {
            view.visibility = View.VISIBLE
            view.text = getString(R.string.nav_apps_service_off)
        } else if (adapter.itemCount == 0) {
            view.visibility = View.VISIBLE
            view.text = if (query.isEmpty()) getString(R.string.nav_apps_empty)
            else getString(R.string.apps_search_no_result)
        } else {
            view.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        applyTitle()
        if (Shizuku.pingBinder()) {
            viewModel.load()
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            applyTitle()
            if (Shizuku.pingBinder()) viewModel.load()
        }
    }

    private fun applyTitle() {
        val bar = (activity as? AppCompatActivity)?.supportActionBar ?: return
        bar.title = getString(R.string.nav_apps)
        bar.subtitle = getString(R.string.subtitle_apps_count, adapter.itemCount)
        updateEmptyState()
    }
}
