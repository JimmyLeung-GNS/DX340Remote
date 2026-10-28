package org.dx340.remote.dx340_phone

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Base64
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodChannel
import org.dx340.remote.controller.RemoteModel
import org.dx340.remote.controller.RemoteService
import org.json.JSONArray
import org.json.JSONObject

/** UI bridge only: the bound native service owns the connection, epochs and retries. */
class MainActivity : FlutterActivity(), EventChannel.StreamHandler {
    private var model: RemoteModel? = null
    private var sink: EventChannel.EventSink? = null
    private var bound = false
    private var permissionReply: MethodChannel.Result? = null
    private val operations = setOf("GET_STATE", "GET_QUEUE", "SET_PLAYING", "NEXT", "PREVIOUS", "SEEK", "SELECT_SOURCE", "PLAY_ITEM", "PLAY_QUEUE_ITEM")
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            model = (binder as RemoteService.LocalBinder).service.model
            model?.observer = ::emit
            emit()
        }
        override fun onServiceDisconnected(name: ComponentName) {
            model?.observer = null; model = null
            sink?.error("SERVICE_LOST", "Connection service stopped. Reopen the remote.", null)
        }
    }
    override fun configureFlutterEngine(engine: FlutterEngine) {
        super.configureFlutterEngine(engine)
        EventChannel(engine.dartExecutor.binaryMessenger, "org.dx340.remote/events").setStreamHandler(this)
        MethodChannel(engine.dartExecutor.binaryMessenger, "org.dx340.remote/commands").setMethodCallHandler { call, result ->
            try {
                if (call.method == "permissions") {
                    if (permissionReply != null) { result.error("BUSY", "Permission request already open.", null); return@setMethodCallHandler }
                    permissionReply = result
                    val permissions = mutableListOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                    if (android.os.Build.VERSION.SDK_INT >= 33) permissions += Manifest.permission.POST_NOTIFICATIONS
                    requestPermissions(permissions.toTypedArray(), 340)
                    return@setMethodCallHandler
                }
                if (call.method == "snapshot") { result.success(snapshot()); return@setMethodCallHandler }
                val remote = model ?: error("Bluetooth service is starting. Try again in a moment.")
                when (call.method) {
                    "scan" -> { requireBluetoothPermission(); remote.scan() }
                    "connect" -> {
                        requireBluetoothPermission()
                        val device = remote.found.firstOrNull { it.address == call.argument<String>("id") } ?: error("Find DX340 again before connecting.")
                        startForegroundService(Intent(this, RemoteService::class.java))
                        remote.connect(device)
                    }
                    "disconnect" -> { remote.disconnect(); stopService(Intent(this, RemoteService::class.java)) }
                    "authorize" -> { requireBluetoothPermission(); remote.authorize() }
                    "resume" -> remote.refreshOnForeground()
                    "browse" -> remote.browse(call.argument<String>("parent"), (call.argument<Number>("page")?.toInt() ?: 0).also { require(it in 0..100000) })
                    "send" -> {
                        val type = call.argument<String>("type") ?: error("Missing command.")
                        require(type in operations) { "Unsupported bridge command." }
                        val args = call.argument<Map<String, Any?>>("args") ?: emptyMap()
                        remote.send(type, JSONObject(args))
                    }
                    else -> { result.notImplemented(); return@setMethodCallHandler }
                }
                emit(); result.success(null)
            } catch (error: Exception) { result.error("NATIVE_ACTION", error.message ?: "Bluetooth action failed.", null) }
        }
        bound = bindService(Intent(this, RemoteService::class.java), serviceConnection, BIND_AUTO_CREATE)
    }
    private fun requireBluetoothPermission() {
        require(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
            checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) { "Allow Nearby Devices access in Setup." }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 340) {
            permissionReply?.success(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED)
            permissionReply = null
        }
    }
    private fun snapshot(): String {
        val remote = model
        val devices = JSONArray()
        remote?.found?.forEach { device ->
            val name = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "DX340"
            devices.put(JSONObject().put("id", device.address).put("name", name))
        }
        return JSONObject().put("bridgeVersion", 1).put("status", remote?.status ?: "Preparing Bluetooth remote…")
            .put("authorized", remote?.authorized ?: false).put("mutationPending", remote?.mutationPending ?: false)
            .put("state", remote?.state ?: JSONObject()).put("sources", remote?.sources ?: JSONArray())
            .put("library", remote?.library ?: JSONObject()).put("queue", remote?.queue ?: JSONArray())
            .put("queueRevision", remote?.queueRevision ?: "").put("devices", devices)
            .put("artworkBase64", remote?.artworkBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) } ?: JSONObject.NULL).toString()
    }
    private fun emit() { sink?.success(snapshot()) }
    override fun onListen(arguments: Any?, events: EventChannel.EventSink) { sink = events; emit() }
    override fun onCancel(arguments: Any?) { sink = null }
    override fun onDestroy() {
        permissionReply?.error("ACTIVITY_CLOSED", "Permission request interrupted.", null); permissionReply = null
        model?.observer = null; sink = null
        if (bound) unbindService(serviceConnection)
        super.onDestroy()
    }
}
