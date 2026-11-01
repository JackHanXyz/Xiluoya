package moe.shizuku.manager.settings

/**
 * Xiluoya: 设置页的数据项。
 * 用普通 RecyclerView 渲染，替代 PreferenceFragmentCompat —— 后者无法做卡片样式。
 */
sealed class SettingsItem {

    /** 分组标题 */
    data class Header(val title: String) : SettingsItem()

    /** 开关行 */
    data class SwitchRow(
        val title: String,
        val summary: String?,
        val checked: Boolean,
        val visible: Boolean = true,
        val onToggle: (Boolean) -> Unit
    ) : SettingsItem()

    /** 可选项，点击弹选择框；value 显示在右侧 */
    data class ChoiceRow(
        val title: String,
        val summary: String? = null,
        val value: String,
        val visible: Boolean = true,
        val onClick: () -> Unit
    ) : SettingsItem()
}
