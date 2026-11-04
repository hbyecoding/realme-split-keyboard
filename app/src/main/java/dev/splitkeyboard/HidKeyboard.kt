package dev.splitkeyboard

import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.Context
import android.util.Log

@SuppressLint("MissingPermission")
class HidKeyboard(private val context: Context, private val status: (String) -> Unit) {
    val adapter: BluetoothAdapter? = context.getSystemService(BluetoothManager::class.java)?.adapter
    private var hid: BluetoothHidDevice? = null
    private var host: BluetoothDevice? = null
    private var registered = false
    private var opening = false
    private var registering = false
    private var closed = false
    private var reportId = 1
    private var currentReport = ByteArray(8)
    private var sentKeys = 0
    private val preferences = context.getSharedPreferences("bluetooth", Context.MODE_PRIVATE)
    private var lastConnectAttempt = 0L
    private fun message(text: String) { Log.i("SplitKeyboard", text); status(text) }
    private val callback = object : BluetoothHidDevice.Callback() {
        override fun onAppStatusChanged(device: BluetoothDevice?, ready: Boolean) {
            if (closed) return
            registering = false
            registered = ready
            if (!ready) host = null
            message(if (ready) "键盘已注册，请配对或连接 Mac" else "键盘未注册，请保持 App 在前台后重试")
            if (ready) reconnect()
        }
        override fun onConnectionStateChanged(device: BluetoothDevice, state: Int) {
            if (closed) return
            if (state == BluetoothProfile.STATE_CONNECTED) {
                host = device
                send(ByteArray(8))
                message("已连接：${device.name ?: device.address}")
            } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                if (host?.address == device.address) host = null
                message("连接已断开")
            } else message("连接状态：$state")
        }
        override fun onGetReport(device: BluetoothDevice, type: Byte, id: Byte, bufferSize: Int) {
            if (closed) return
            if (type == BluetoothHidDevice.REPORT_TYPE_INPUT && id.toInt() == reportId)
                hid?.replyReport(device, type, id, currentReport.copyOf(if (bufferSize > 0) minOf(bufferSize, 8) else 8))
            else hid?.reportError(device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ)
        }
        override fun onSetProtocol(device: BluetoothDevice, protocol: Byte) {
            reportId = if (protocol == BluetoothHidDevice.PROTOCOL_BOOT_MODE) 0 else 1
            Log.i("SplitKeyboard", "protocol=$protocol reportId=$reportId")
        }
    }
    private val listener = object : BluetoothProfile.ServiceListener {
        override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
            if (closed) { adapter?.closeProfileProxy(profile, proxy); return }
            opening = false
            hid = proxy as BluetoothHidDevice
            register()
        }
        override fun onServiceDisconnected(profile: Int) {
            opening = false; registering = false; registered = false; hid = null; host = null
            message("蓝牙服务已断开，请重试")
        }
    }
    fun start() {
        if (closed) return
        if (adapter?.isEnabled != true) { message("请先打开蓝牙"); return }
        if (hid != null) { if (!registered) register() else reconnect(); return }
        if (!opening) {
            opening = adapter.getProfileProxy(context, listener, BluetoothProfile.HID_DEVICE)
            message(if (opening) "正在获取 HID 服务" else "设备不支持 HID 服务")
        }
    }
    private fun register() {
        if (registering || registered || closed) return
        val descriptor = intArrayOf(
            0x05,0x01,0x09,0x06,0xA1,0x01,0x85,0x01,
            0x05,0x07,0x19,0xE0,0x29,0xE7,0x15,0x00,0x25,0x01,
            0x75,0x01,0x95,0x08,0x81,0x02,
            0x95,0x01,0x75,0x08,0x81,0x01,
            0x95,0x06,0x75,0x08,0x15,0x00,0x25,0x65,
            0x05,0x07,0x19,0x00,0x29,0x65,0x81,0x00,0xC0
        ).map { it.toByte() }.toByteArray()
        val sdp = BluetoothHidDeviceAppSdpSettings("Realme Split Keyboard", "双手机分体键盘", "SplitKeyboard", BluetoothHidDevice.SUBCLASS1_KEYBOARD, descriptor)
        registering = hid?.registerApp(sdp, null, null, context.mainExecutor, callback) == true
        if (!registering)
            message("HID 注册请求失败，请重试并查看日志")
    }
    fun devices(): List<BluetoothDevice> = adapter?.bondedDevices?.toList().orEmpty()
    fun connect(device: BluetoothDevice) {
        preferences.edit().putString("host", device.address).apply()
        if (!registered) { message("请等待键盘注册成功"); return }
        lastConnectAttempt = android.os.SystemClock.elapsedRealtime()
        message("正在连接：${device.name ?: device.address}")
        if (hid?.connect(device) != true) message("连接请求失败，请在 Mac 蓝牙设置中重新配对")
    }
    private fun reconnect() {
        if (host != null || android.os.SystemClock.elapsedRealtime() - lastConnectAttempt < 10000) return
        val address = preferences.getString("host", null) ?: return
        devices().firstOrNull { it.address == address }?.let { connect(it) }
    }
    fun send(report: ByteArray) {
        currentReport = report.copyOf()
        try {
            val pressed = report.any { it.toInt() != 0 }
            val device = host
            if (device == null) {
                if (pressed) message("按键未发送：HID 尚未连接，请点连接 Mac")
                return
            }
            val accepted = hid?.sendReport(device, reportId, report) == true
            Log.i("SplitKeyboard", "send accepted=$accepted registered=$registered id=$reportId")
            if (!accepted) message("按键发送失败，请重新连接 Mac")
            else if (pressed) {
                sentKeys++
                message("蓝牙已接收按键 $sentKeys · ${device.name ?: device.address}")
            }
        } catch (_: SecurityException) { host = null; message("蓝牙权限已失效，请重新授权") }
    }
    fun close() {
        send(ByteArray(8))
        closed = true
        hid?.unregisterApp()
        hid?.let { adapter?.closeProfileProxy(BluetoothProfile.HID_DEVICE, it) }
        hid = null; host = null; registered = false; registering = false
    }
}
