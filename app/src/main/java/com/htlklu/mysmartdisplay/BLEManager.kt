package com.htlklu.mysmartdisplay

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.pm.PackageManager
import android.os.ParcelUuid
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.UUID

class BLEManager(private val context: Context) {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val scanner = bluetoothAdapter.bluetoothLeScanner
    private val serviceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")

    val discoveredDevices = mutableStateOf<List<BluetoothDevice>>(listOf())
    val connectedDevice = MutableStateFlow<BluetoothDevice?>(null)
    val red = MutableStateFlow(0.0)
    val green = MutableStateFlow(0.0)
    val blue = MutableStateFlow(0.0)
    val log = MutableStateFlow("")

    private var bluetoothGatt: BluetoothGatt? = null
    private var writeCharacteristic: BluetoothGattCharacteristic? = null

    init {
        if (bluetoothAdapter.isEnabled) {
            startScanning()
        }
    }

    fun startScanning() {
        println("startscan");
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) return
        println("haspermissions");

        discoveredDevices.value = listOf()
        val filters = listOf(ScanFilter.Builder().setServiceUuid(ParcelUuid(serviceUUID)).build())
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        scanner.startScan(filters, settings, scanCallback)
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        scanner.stopScan(scanCallback)
        bluetoothGatt = device.connectGatt(context, false, gattCallback)
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device
            if (!discoveredDevices.value.any { it.address == device.address }) {
                println("new_device: " + discoveredDevices.value);
                discoveredDevices.value = discoveredDevices.value + device
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                connectedDevice.value = gatt.device
                gatt.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                connectedDevice.value = null
                bluetoothGatt = null
                writeCharacteristic = null
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

            gatt.services.forEach { service ->
                service.characteristics.forEach { characteristic ->
                    val props = characteristic.properties
                    if (props and BluetoothGattCharacteristic.PROPERTY_WRITE > 0 ||
                        props and BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE > 0
                    ) {
                        writeCharacteristic = characteristic
                        gatt.setCharacteristicNotification(characteristic, true)
                        writeToCharacteristic("get:config")
                        gatt.readCharacteristic(characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            handleIncomingData(characteristic)
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            handleIncomingData(characteristic)
        }
    }

    private fun handleIncomingData(characteristic: BluetoothGattCharacteristic) {
        val value = characteristic.value?.toString(Charsets.UTF_8) ?: return
        log.value += "$value\n"
        val parts = value.split(";")
        if (parts.size >= 9) {
            red.value = parts[5].toDoubleOrNull() ?: 0.0
            green.value = parts[6].toDoubleOrNull() ?: 0.0
            blue.value = parts[7].toDoubleOrNull() ?: 0.0
        }
    }

    fun writeToCharacteristic(data: String) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) return

        val char = writeCharacteristic ?: return
        char.value = data.toByteArray(Charsets.UTF_8)
        bluetoothGatt?.writeCharacteristic(char)
    }

    fun setColor(channel: Char, value: Float) {
        writeToCharacteristic("$channel:${value.toInt()}")
    }
}
