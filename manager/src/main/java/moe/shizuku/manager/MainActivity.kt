package moe.shizuku.manager

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import moe.shizuku.manager.app.AppBarActivity
import moe.shizuku.manager.settings.SettingsFragment
import android.content.Intent
import moe.shizuku.manager.lspatch.LocalAdbHelper
import moe.shizuku.manager.ui.AboutFragment
import moe.shizuku.manager.ui.AppsFragment
import moe.shizuku.manager.ui.HomeFragment
import moe.shizuku.manager.ui.ModulesFragment

/**
 * Xiluoya: 底部导航容器。
 *
 * 四个页面：首页（服务状态与启动）/ 应用（授权管理）/ 模块（LSPatch）/ 设置。
 * 原来的 HomeActivity 保留不动，作为旧入口，避免把老逻辑拖进新结构。
 */
class MainActivity : AppBarActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun getLayoutId(): Int = R.layout.activity_main

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(false)

        bottomNav = findViewById(R.id.bottom_nav)
        bottomNav.setOnItemSelectedListener { item ->
            switchTo(item.itemId)
            true
        }
        bottomNav.setOnItemReselectedListener { /* 已在该页，不重复创建 */ }

        if (savedInstanceState == null) {
            // 首页在中间。BottomNavigationView 默认选第一项（设置），
            // 显式选中 nav_home 会触发切换；再直接调一次兜底。
            bottomNav.selectedItemId = R.id.nav_home
            switchTo(R.id.nav_home)
            handleShortcutIntent(intent)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleShortcutIntent(intent)
    }

    /** 桌面快捷方式「启动服务」走的就是这个 action */
    private fun handleShortcutIntent(intent: Intent?) {
        if (intent?.action == "moe.xiluoya.action.START_SERVICE") {
            LocalAdbHelper.start(this)
        }
    }

    private fun switchTo(id: Int) {
        val tag = when (id) {
            R.id.nav_apps -> "apps"
            R.id.nav_module -> "modules"
            R.id.nav_settings -> "settings"
            R.id.nav_about -> "about"
            else -> "home"
        }
        val existing = supportFragmentManager.findFragmentByTag(tag)
        val fragment = existing ?: when (id) {
            R.id.nav_apps -> AppsFragment()
            R.id.nav_module -> ModulesFragment()
            R.id.nav_settings -> SettingsFragment()
            R.id.nav_about -> AboutFragment()
            else -> HomeFragment()
        }

        supportFragmentManager.beginTransaction()
            .apply {
                supportFragmentManager.fragments.forEach { hide(it) }
                if (existing == null) {
                    add(R.id.fragment_container, fragment, tag)
                } else {
                    show(existing)
                }
            }
            .commit()
    }
}
