package dev.splitkeyboard

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.view.Gravity
import android.widget.*

class MainActivity : Activity() {
    private lateinit var hid: HidKeyboard
    private lateinit var keyboard: KeyboardView
    private lateinit var status: TextView
    private lateinit var discoveryStatus: TextView
    private lateinit var detailStatus: TextView
    private fun showStatus(message: String) {
        detailStatus.text = message
        status.text = when {
            message.startsWith("已连接") || message.startsWith("蓝牙已接收") -> "已连接\n⚙"
            message.startsWith("正在连接") -> "连接中\n⚙"
            message.contains("已注册") -> "待连接\n⚙"
            message.contains("断开") -> "已断开\n⚙"
            else -> "待连接\n⚙"
        }
    }
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val refreshDiscovery = object : Runnable {
        override fun run() {
            if (::hid.isInitialized && allowed()) hid.start()
            if (::hid.isInitialized && allowed() && (Build.VERSION.SDK_INT < 31 || checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)) {
                try {
                    val visible = hid.adapter?.scanMode == BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
                    discoveryStatus.text = if (visible) "Mac 可搜索到：${hid.adapter?.name}"
                        else "当前不可发现 · 展开设置后点配对"
                } catch (_: SecurityException) { discoveryStatus.text = "需要附近设备权限才能检查可发现状态" }
            } else discoveryStatus.text = "扫描诊断权限未授予；可点系统配对，键盘连接不受此权限限制"
            handler.postDelayed(this, 1000)
        }
    }
    private val permissions: Array<String>
        get() = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN) else emptyArray()
    private fun allowed() = permissions.filter { it != Manifest.permission.BLUETOOTH_SCAN }.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        val root = FrameLayout(this).apply { setBackgroundColor(0xff0d121c.toInt()) }
        status = TextView(this).apply { text = "连接\n⚙"; textSize = 11f; gravity = Gravity.CENTER; setTextColor(0xff9fc9dc.toInt()); contentDescription = "连接状态，点击打开设置" }
        detailStatus = TextView(this).apply { text = "请授权蓝牙后连接 Mac"; textSize = 12f; setTextColor(0xffeeeeee.toInt()) }
        discoveryStatus = TextView(this).apply { textSize = 11f; setTextColor(0xff9fc9dc.toInt()); setPadding(16, 0, 16, 0) }
        hid = HidKeyboard(this) { message -> runOnUiThread {
            showStatus(message)
            if (::keyboard.isInitialized && (message.startsWith("已连接") || message.contains("已断开"))) keyboard.release()
        } }
        keyboard = KeyboardView(this) { report -> if (allowed()) hid.send(report) }
        val settings = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(0xf20d121c.toInt()); visibility = android.view.View.GONE }
        val toolbar = LinearLayout(this)
        status.setOnClickListener {
            keyboard.release()
            settings.visibility = if (settings.visibility == android.view.View.GONE) android.view.View.VISIBLE else android.view.View.GONE
        }
        fun button(label: String, action: () -> Unit) {
            toolbar.addView(Button(this).apply { text = label; textSize = 12f; setOnClickListener { action() } }, LinearLayout.LayoutParams(0, (48 * resources.displayMetrics.density).toInt(), 1f))
        }
        val prefs = getSharedPreferences("layout", MODE_PRIVATE)
        keyboard.vibrationEnabled = prefs.getBoolean("vibration", true)
        val gutter = (64 * resources.displayMetrics.density).toInt()
        fun side(left: Boolean) {
            keyboard.setPadding(gutter, 0, 0, 0)
            keyboard.side(left)
            status.layoutParams = FrameLayout.LayoutParams(gutter, (64 * resources.displayMetrics.density).toInt(), Gravity.BOTTOM or Gravity.LEFT)
        }
        side(prefs.getBoolean("left", true))
        button("左手") { side(true); prefs.edit().putBoolean("left", true).apply(); settings.visibility = android.view.View.GONE }
        button("右手") { side(false); prefs.edit().putBoolean("left", false).apply(); settings.visibility = android.view.View.GONE }
        toolbar.addView(Button(this).apply {
            text = if (keyboard.vibrationEnabled) "震动开" else "震动关"
            textSize = 12f
            setOnClickListener {
                keyboard.vibrationEnabled = !keyboard.vibrationEnabled
                prefs.edit().putBoolean("vibration", keyboard.vibrationEnabled).apply()
                text = if (keyboard.vibrationEnabled) "震动开" else "震动关"
            }
        }, LinearLayout.LayoutParams(0, (48 * resources.displayMetrics.density).toInt(), 1f))
        button("启用蓝牙") { enable() }
        button("配对") {
            if (allowed()) {
                hid.start()
                startActivityForResult(Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300), 2)
            } else enable()
        }
        button("系统配对") { startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)) }
        button("连接 Mac") {
            if (allowed()) {
                val devices = hid.devices()
                if (devices.isEmpty()) showStatus("尚无已配对设备，请点配对，然后在 Mac 蓝牙设置中选择手机")
                else try { AlertDialog.Builder(this).setTitle("选择已配对的 Mac")
                    .setItems(devices.map { it.name ?: it.address }.toTypedArray()) { _, index -> hid.connect(devices[index]); settings.visibility = android.view.View.GONE }.show()
                } catch (_: SecurityException) { showStatus("蓝牙权限已失效，请重新授权") }
            } else enable()
        }
        button("释放全部") { keyboard.release() }
        root.addView(keyboard, FrameLayout.LayoutParams(-1, -1))
        root.addView(status)
        settings.addView(toolbar)
        settings.addView(detailStatus)
        settings.addView(discoveryStatus)
        root.addView(settings, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
        setContentView(root)
        if (allowed()) hid.start()
    }
    private fun enable() {
        if (!permissions.all { checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) { requestPermissions(permissions, 1); return }
        if (hid.adapter == null) { showStatus("此设备没有可用蓝牙适配器"); return }
        try {
            if (hid.adapter?.isEnabled != true) startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
            else hid.start()
        } catch (_: SecurityException) { showStatus("蓝牙权限已失效，请重新授权") }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (allowed()) hid.start() else showStatus("蓝牙权限未授予，点击启用蓝牙后重新授权")
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 2) {
            android.util.Log.i("SplitKeyboard", "discoverable result=$resultCode")
            discoveryStatus.text = if (resultCode > 0) {
                try { "可发现 $resultCode 秒：Mac 请搜索「${hid.adapter?.name ?: "手机蓝牙名称"}」 · 点击设置" }
                catch (_: SecurityException) { "可发现 $resultCode 秒，请在 Mac 蓝牙设置搜索手机 · 点击设置" }
            } else "未允许可发现，Mac 无法搜索到手机；请再次点配对并允许 · 点击设置"
        }
    }
    override fun onResume() { super.onResume(); handler.removeCallbacks(refreshDiscovery); handler.post(refreshDiscovery); if (::hid.isInitialized && allowed()) hid.start() }
    override fun onPause() { handler.removeCallbacks(refreshDiscovery); if (::keyboard.isInitialized) keyboard.release(); super.onPause() }
    override fun onDestroy() { if (::hid.isInitialized && allowed()) hid.close(); super.onDestroy() }
}
