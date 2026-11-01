package moe.shizuku.manager.settings

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import moe.shizuku.manager.databinding.ItemSettingChoiceBinding
import moe.shizuku.manager.databinding.ItemSettingHeaderBinding
import moe.shizuku.manager.databinding.ItemSettingSwitchBinding

/**
 * Xiluoya: 卡片式设置列表。
 */
class SettingsAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val items = mutableListOf<SettingsItem>()

    fun submit(list: List<SettingsItem>) {
        items.clear()
        items.addAll(list.filter {
            when (it) {
                is SettingsItem.SwitchRow -> it.visible
                is SettingsItem.ChoiceRow -> it.visible
                else -> true
            }
        })
        notifyDataSetChanged()
    }

    override fun getItemCount() = items.size

    override fun getItemViewType(position: Int) = when (items[position]) {
        is SettingsItem.Header -> TYPE_HEADER
        is SettingsItem.SwitchRow -> TYPE_SWITCH
        is SettingsItem.ChoiceRow -> TYPE_CHOICE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderHolder(ItemSettingHeaderBinding.inflate(inflater, parent, false))
            TYPE_SWITCH -> SwitchHolder(ItemSettingSwitchBinding.inflate(inflater, parent, false))
            else -> ChoiceHolder(ItemSettingChoiceBinding.inflate(inflater, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is SettingsItem.Header -> (holder as HeaderHolder).bind(item)
            is SettingsItem.SwitchRow -> (holder as SwitchHolder).bind(item)
            is SettingsItem.ChoiceRow -> (holder as ChoiceHolder).bind(item)
        }
    }

    private class HeaderHolder(val binding: ItemSettingHeaderBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem.Header) {
            binding.title.text = item.title
        }
    }

    private class SwitchHolder(val binding: ItemSettingSwitchBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem.SwitchRow) {
            binding.title.text = item.title
            binding.summary.text = item.summary
            binding.summary.visibility = if (item.summary.isNullOrEmpty()) View.GONE else View.VISIBLE

            // 复用时会带着旧监听，先摘掉再赋值，避免误触发
            binding.switchWidget.setOnCheckedChangeListener(null)
            binding.switchWidget.isChecked = item.checked
            binding.switchWidget.setOnCheckedChangeListener { _, isChecked ->
                item.onToggle(isChecked)
            }
        }
    }

    private class ChoiceHolder(val binding: ItemSettingChoiceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: SettingsItem.ChoiceRow) {
            binding.title.text = item.title
            binding.summary.text = item.summary
            binding.summary.visibility = if (item.summary.isNullOrEmpty()) View.GONE else View.VISIBLE
            binding.value.text = item.value
            binding.root.setOnClickListener { item.onClick() }
        }
    }

    companion object {
        private const val TYPE_HEADER = 0
        private const val TYPE_SWITCH = 1
        private const val TYPE_CHOICE = 2
    }
}
