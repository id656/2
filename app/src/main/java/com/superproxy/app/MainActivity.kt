package com.superproxy.app

import android.app.Activity
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private val REQ_VPN = 1
    private val REQ_IMPORT = 2
    private lateinit var group: RadioGroup
    private lateinit var btn: Button
    private var profiles = listOf<Profile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33)
            requestPermissions(arrayOf("android.permission.POST_NOTIFICATIONS"), 0)

        val pad = (16 * resources.displayMetrics.density).toInt()
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad * 2, pad, pad) }
        group = RadioGroup(this)
        group.setOnCheckedChangeListener { _, id -> if (id >= 100) ProfileStore.setSelected(this, id - 100) }
        btn = Button(this).apply { setOnClickListener { toggle() } }
        val imp = Button(this).apply {
            text = "Импорт списка…"
            setOnClickListener {
                startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "*/*"
                }, REQ_IMPORT)
            }
        }
        val scroll = ScrollView(this).apply { addView(group) }
        root.addView(scroll, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
        root.addView(btn); root.addView(imp)
        setContentView(root)
        render()
    }

    override fun onResume() { super.onResume(); refreshButton() }

    private fun render() {
        profiles = ProfileStore.load(this)
        group.removeAllViews()
        val sel = ProfileStore.selected(this).coerceIn(0, (profiles.size - 1).coerceAtLeast(0))
        profiles.forEachIndexed { i, p ->
            group.addView(RadioButton(this).apply { id = 100 + i; text = "${p.name}\n${p.hostPort}" })
        }
        if (profiles.isNotEmpty()) group.check(100 + sel)
        refreshButton()
    }

    private fun refreshButton() { btn.text = if (ProxyVpnService.running) "Отключить" else "Подключить" }

    private fun delayedRefresh() = Handler(Looper.getMainLooper()).postDelayed({ refreshButton() }, 700)

    private fun toggle() {
        if (ProxyVpnService.running) {
            startService(Intent(this, ProxyVpnService::class.java).setAction(ProxyVpnService.ACTION_STOP))
            delayedRefresh(); return
        }
        if (profiles.isEmpty()) { Toast.makeText(this, "Нет профилей", Toast.LENGTH_SHORT).show(); return }
        val prep = VpnService.prepare(this)
        if (prep != null) startActivityForResult(prep, REQ_VPN) else startVpn()
    }

    private fun startVpn() {
        val p = profiles[ProfileStore.selected(this).coerceIn(0, profiles.size - 1)]
        startForegroundService(Intent(this, ProxyVpnService::class.java).putExtra(ProxyVpnService.EXTRA_URL, p.url))
        delayedRefresh()
    }

    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        when {
            req == REQ_VPN && res == RESULT_OK -> startVpn()
            req == REQ_IMPORT && res == RESULT_OK && data?.data != null -> {
                val list = contentResolver.openInputStream(data.data!!)?.bufferedReader()?.use { ProfileStore.parse(it.readText()) } ?: emptyList()
                if (list.isEmpty()) Toast.makeText(this, "Не удалось разобрать файл", Toast.LENGTH_LONG).show()
                else {
                    ProfileStore.save(this, list)
                    ProfileStore.setSelected(this, list.indexOfFirst { it.isDefault }.coerceAtLeast(0))
                    render()
                }
            }
        }
    }
}
