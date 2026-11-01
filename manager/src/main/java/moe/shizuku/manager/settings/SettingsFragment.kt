package moe.shizuku.manager.settings

import android.content.ComponentName
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import moe.shizuku.manager.receiver.BootCompleteReceiver
import moe.shizuku.manager.R
import moe.shizuku.manager.ShizukuSettings
import moe.shizuku.manager.app.ThemeHelper
import rikka.material.app.LocaleDelegate
import rikka.recyclerview.addItemSpacing
import rikka.shizuku.manager.ShizukuLocales
import java.util.Locale

/**
 * Xiluoya: 设置页。
 *
 * 原来是 PreferenceFragmentCompat，条目样式被框架锁死（改不了圆角/背景），
 * 这里改成普通 RecyclerView + 卡片行，和其他页视觉统一。
 */
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private val adapter = SettingsAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view as RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
        recyclerView.itemAnimator = null
        recyclerView.addItemSpacing(top = 5f, bottom = 5f, unit = TypedValue.COMPLEX_UNIT_DIP)

        rebuild()
    }

    override fun onResume() {
        super.onResume()
        applyTitle()
        rebuild()
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (!hidden) {
            applyTitle()
            rebuild()
        }
    }

    private fun applyTitle() {
        val bar = (activity as? AppCompatActivity)?.supportActionBar ?: return
        bar.title = getString(R.string.nav_settings)
        bar.subtitle = null
    }

    private fun rebuild() {
        if (!isAdded) return
        adapter.submit(buildItems())
    }

    private fun buildItems(): List<SettingsItem> {
        val context = requireContext()
        val items = mutableListOf<SettingsItem>()

        // 启动
        items += SettingsItem.Header(getString(R.string.settings_startup))
        items += SettingsItem.SwitchRow(
            title = getString(R.string.settings_start_on_boot),
            summary = getString(R.string.settings_start_on_boot_summary),
            checked = isBootReceiverEnabled(),
            onToggle = { setBootReceiverEnabled(it) }
        )

        // 语言
        items += SettingsItem.Header(getString(R.string.settings_language))
        items += SettingsItem.ChoiceRow(
            title = getString(R.string.settings_language),
            value = currentLanguageName(),
            onClick = { showLanguageDialog() }
        )

        // 界面外观
        items += SettingsItem.Header(getString(R.string.settings_user_interface))
        items += SettingsItem.ChoiceRow(
            title = getString(R.string.dark_theme),
            value = currentNightModeName(),
            onClick = { showNightModeDialog() }
        )

        // 黑色主题只在开了夜间模式时有意义
        if (ShizukuSettings.getNightMode() != AppCompatDelegate.MODE_NIGHT_NO) {
            items += SettingsItem.SwitchRow(
                title = getString(R.string.settings_black_night_theme),
                summary = getString(R.string.settings_black_night_theme_summary),
                checked = ThemeHelper.isBlackNightTheme(context),
                onToggle = { value ->
                    ShizukuSettings.getPreferences().edit()
                        .putBoolean(ThemeHelper.KEY_BLACK_NIGHT_THEME, value).apply()
                    activity?.recreate()
                }
            )
        }

        // 系统取色只有 Android 12+ 支持
        items += SettingsItem.SwitchRow(
            title = getString(R.string.settings_use_system_color),
            summary = null,
            checked = ThemeHelper.isUsingSystemColor(),
            visible = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S,
            onToggle = { value ->
                ShizukuSettings.getPreferences().edit()
                    .putBoolean(ThemeHelper.KEY_USE_SYSTEM_COLOR, value).apply()
                activity?.recreate()
            }
        )

        return items
    }

    // ---------- 开机启动 ----------

    private fun bootComponent(): ComponentName =
        ComponentName(requireContext(), BootCompleteReceiver::class.java)

    private fun isBootReceiverEnabled(): Boolean =
        requireContext().packageManager.getComponentEnabledSetting(bootComponent()) !=
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED

    private fun setBootReceiverEnabled(enabled: Boolean) {
        requireContext().packageManager.setComponentEnabledSetting(
            bootComponent(),
            if (enabled) PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            else PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }

    // ---------- 语言 ----------

    private fun currentLanguageName(): String {
        val current = ShizukuSettings.getPreferences()
            .getString(ShizukuSettings.LANGUAGE, "SYSTEM") ?: "SYSTEM"
        val index = ShizukuLocales.LOCALES.indexOf(current)
        if (index <= 0) return getString(R.string.follow_system)
        return ShizukuLocales.DISPLAY_LOCALES.getOrNull(index)?.toString() ?: current
    }

    private fun showLanguageDialog() {
        val tags = ShizukuLocales.LOCALES
        val display = ShizukuLocales.DISPLAY_LOCALES
        val names = Array(tags.size) { i ->
            if (i == 0) getString(R.string.follow_system) else display.getOrNull(i)?.toString() ?: tags[i]
        }
        val current = ShizukuSettings.getPreferences()
            .getString(ShizukuSettings.LANGUAGE, "SYSTEM") ?: "SYSTEM"
        val checked = tags.indexOf(current).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.settings_language)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                val tag = tags[which]
                ShizukuSettings.getPreferences().edit()
                    .putString(ShizukuSettings.LANGUAGE, tag).apply()
                LocaleDelegate.defaultLocale = if (tag == "SYSTEM") {
                    LocaleDelegate.systemLocale
                } else {
                    Locale.forLanguageTag(tag)
                }
                dialog.dismiss()
                activity?.recreate()
            }
            .show()
    }

    // ---------- 夜间模式 ----------

    private fun currentNightModeName(): String {
        val names = resources.getStringArray(R.array.night_mode)
        val values = resources.getIntArray(R.array.night_mode_value)
        val index = values.indexOf(ShizukuSettings.getNightMode())
        return if (index in names.indices) names[index] else names.last()
    }

    private fun showNightModeDialog() {
        val names = resources.getStringArray(R.array.night_mode)
        val values = resources.getIntArray(R.array.night_mode_value)
        val checked = values.indexOf(ShizukuSettings.getNightMode()).coerceAtLeast(0)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.dark_theme)
            .setSingleChoiceItems(names, checked) { dialog, which ->
                val value = values[which]
                ShizukuSettings.getPreferences().edit()
                    .putInt(ShizukuSettings.NIGHT_MODE, value).apply()
                AppCompatDelegate.setDefaultNightMode(value)
                dialog.dismiss()
                activity?.recreate()
            }
            .show()
    }
}
