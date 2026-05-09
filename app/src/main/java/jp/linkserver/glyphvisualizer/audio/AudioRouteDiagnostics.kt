package jp.linkserver.glyphvisualizer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build

internal object AudioRouteDiagnostics {
    fun isMusicActive(context: Context): Boolean {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return false
        return audioManager.isMusicActive
    }

    fun isBluetoothOutputLikelyConnected(context: Context): Boolean {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return false
        activePlaybackDevices(audioManager)?.let { activeDevices ->
            return isBluetoothDeviceTypes(activeDevices.mapNotNull { playbackDeviceType(it) })
        }
        return isBluetoothOutputLikelyConnected(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
    }

    fun outputSignature(context: Context): String {
        val audioManager = context.getSystemService(AudioManager::class.java) ?: return "audioManager=null"
        return outputSignature(audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS))
    }

    fun describeDevices(devices: Array<AudioDeviceInfo>): String {
        val sinks = devices.filter { it.isSink }
        if (sinks.isEmpty()) {
            return "none"
        }
        return sinks.joinToString(separator = ", ") { device ->
            buildString {
                append(typeName(device.type))
                val productName = device.productName?.toString()?.trim().orEmpty()
                if (productName.isNotEmpty()) {
                    append("(")
                    append(productName)
                    append(")")
                }
            }
        }
    }

    fun snapshot(context: Context): String {
        val audioManager = context.getSystemService(AudioManager::class.java)
        if (audioManager == null) {
            return "audioManager=null"
        }

        val outputs = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val outputSummary = describeDevices(outputs)
        val activePlaybackSummary = describeActivePlaybackDevices(audioManager)

        val communicationDevice = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.communicationDevice?.let { device ->
                "${typeName(device.type)}(${device.productName})"
            } ?: "none"
        } else {
            "unsupported"
        }

        val bluetoothLikelyConnected = isBluetoothOutputLikelyConnected(outputs)

        return buildString {
            append("model=")
            append(Build.MANUFACTURER)
            append("/")
            append(Build.MODEL)
            append(", mode=")
            append(audioModeName(audioManager.mode))
            append(", musicActive=")
            append(audioManager.isMusicActive)
            append(", speakerphoneOn=")
            append(audioManager.isSpeakerphoneOn)
            append(", bluetoothScoOn=")
            append(audioManager.isBluetoothScoOn)
            append(", bluetoothOutputLikelyConnected=")
            append(bluetoothLikelyConnected)
            append(", communicationDevice=")
            append(communicationDevice)
            append(", outputs=[")
            append(outputSummary)
            append("]")
            append(", activePlayback=[")
            append(activePlaybackSummary)
            append("]")
        }
    }

    private fun describeActivePlaybackDevices(audioManager: AudioManager): String {
        val activeDevices = activePlaybackDevices(audioManager) ?: return "unknown"
        if (activeDevices.isEmpty()) return "none"
        return activeDevices.joinToString(separator = ", ") { device ->
            buildString {
                append(typeName(playbackDeviceType(device) ?: -1))
                val productName = playbackDeviceName(device).trim()
                if (productName.isNotEmpty()) {
                    append("(")
                    append(productName)
                    append(")")
                }
            }
        }
    }

    private fun audioModeName(mode: Int): String = when (mode) {
        AudioManager.MODE_NORMAL -> "NORMAL"
        AudioManager.MODE_RINGTONE -> "RINGTONE"
        AudioManager.MODE_IN_CALL -> "IN_CALL"
        AudioManager.MODE_IN_COMMUNICATION -> "IN_COMMUNICATION"
        AudioManager.MODE_CALL_SCREENING -> "CALL_SCREENING"
        AudioManager.MODE_CALL_REDIRECT -> "CALL_REDIRECT"
        AudioManager.MODE_COMMUNICATION_REDIRECT -> "COMM_REDIRECT"
        else -> mode.toString()
    }

    private fun typeName(type: Int): String = when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "EARPIECE"
        AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> "SPEAKER"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> "WIRED_HEADPHONES"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "LINE_ANALOG"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "LINE_DIGITAL"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BT_SCO"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BT_A2DP"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB_ACCESSORY"
        AudioDeviceInfo.TYPE_DOCK -> "DOCK"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_HEARING_AID -> "HEARING_AID"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE_HEADSET"
        AudioDeviceInfo.TYPE_BLE_SPEAKER -> "BLE_SPEAKER"
        AudioDeviceInfo.TYPE_BLE_BROADCAST -> "BLE_BROADCAST"
        else -> "TYPE_$type"
    }

    private fun isBluetoothOutputLikelyConnected(devices: Array<AudioDeviceInfo>): Boolean {
        return isBluetoothDeviceTypes(devices.map { it.type })
    }

    private fun isBluetoothDeviceTypes(types: List<Int>): Boolean {
        return types.any {
            it == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                it == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                it == AudioDeviceInfo.TYPE_BLE_HEADSET ||
                it == AudioDeviceInfo.TYPE_BLE_SPEAKER ||
                it == AudioDeviceInfo.TYPE_BLE_BROADCAST
        }
    }

    private fun activePlaybackDevices(audioManager: AudioManager): List<Any>? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        return runCatching {
            val method = AudioManager::class.java.getMethod(
                "getAudioDevicesForAttributes",
                AudioAttributes::class.java
            )
            @Suppress("UNCHECKED_CAST")
            method.invoke(
                audioManager,
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            ) as? List<Any>
        }.getOrNull()
    }

    private fun playbackDeviceType(device: Any): Int? {
        return runCatching {
            device.javaClass.getMethod("getType").invoke(device) as? Int
        }.getOrNull()
    }

    private fun playbackDeviceName(device: Any): String {
        return runCatching {
            device.javaClass.getMethod("getName").invoke(device)?.toString().orEmpty()
        }.getOrElse { "" }
    }

    private fun outputSignature(devices: Array<AudioDeviceInfo>): String {
        return devices
            .filter { it.isSink }
            .sortedWith(
                compareBy<AudioDeviceInfo>({ it.type }, { it.id }, { it.productName?.toString().orEmpty() })
            )
            .joinToString(separator = "|") { device ->
                "${device.type}:${device.id}:${device.productName?.toString().orEmpty()}"
            }
    }
}
