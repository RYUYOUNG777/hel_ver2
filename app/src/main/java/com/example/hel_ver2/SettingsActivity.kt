package com.example.hel_ver2

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var bluetoothGatt: BluetoothGatt? = null
    private val handler = Handler(Looper.getMainLooper())
    private val SCAN_PERIOD: Long = 10000 // 10초간 스캔
    private val devices = mutableListOf<BluetoothDevice>()
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var sendButton: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var connectedDeviceLayout: LinearLayout
    private lateinit var inputText: EditText

    companion object {
        private const val PERMISSION_REQUEST_CODE = 1
        private val BLUNO_SERVICE_UUID = UUID.fromString("0000dfb0-0000-1000-8000-00805f9b34fb")
        private val BLUNO_CHARACTERISTIC_UUID = UUID.fromString("0000dfb1-0000-1000-8000-00805f9b34fb")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val bluetoothButton: Button = findViewById(R.id.button_bluetooth_connect)
        val backButton: Button = findViewById(R.id.button_back)
        sendButton = findViewById(R.id.button_send)
        recyclerView = findViewById(R.id.recycler_view_devices)
        connectedDeviceLayout = findViewById(R.id.connected_device_layout)
        inputText = findViewById(R.id.input_text)

        deviceAdapter = DeviceAdapter(devices) { device ->
            connectToDevice(device)
        }
        recyclerView.adapter = deviceAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        bluetoothButton.setOnClickListener {
            if (checkPermissions()) {
                initBluetooth()
                scanLeDevice(true)
            } else {
                requestPermissions()
            }
        }

        sendButton.setOnClickListener {
            sendDataToBluno(inputText.text.toString())
        }

        backButton.setOnClickListener {
            finish()  // 현재 액티비티를 종료하여 이전 액티비티로 돌아갑니다.
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(this,
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT),
            PERMISSION_REQUEST_CODE
        )
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initBluetooth()
                scanLeDevice(true)
            } else {
                Toast.makeText(this, "권한이 거부되었습니다. 블루투스 작업을 수행할 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initBluetooth() {
        try {
            val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            } else {
                Toast.makeText(this, "블루투스 스캔 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun scanLeDevice(enable: Boolean) {
        try {
            if (enable) {
                handler.postDelayed({
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                        bluetoothLeScanner.stopScan(leScanCallback)
                    } else {
                        Toast.makeText(this, "블루투스 스캔 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                    }
                }, SCAN_PERIOD)
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner.startScan(leScanCallback)
                } else {
                    Toast.makeText(this, "블루투스 스캔 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothLeScanner.stopScan(leScanCallback)
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.device?.let { device ->
                if (!devices.contains(device)) {
                    devices.add(device)
                    deviceAdapter.notifyItemInserted(devices.size - 1)
                }
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.forEach { result ->
                result.device?.let { device ->
                    if (!devices.contains(device)) {
                        devices.add(device)
                        deviceAdapter.notifyItemInserted(devices.size - 1)
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Toast.makeText(this@SettingsActivity, "스캔 실패: $errorCode", Toast.LENGTH_SHORT).show()
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        try {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                bluetoothGatt = device.connectGatt(this, false, gattCallback)
            } else {
                Toast.makeText(this, "블루투스 연결 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendDataToBluno(data: String) {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "어떤 장치와도 연결되지 않았습니다", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val service = bluetoothGatt!!.getService(BLUNO_SERVICE_UUID)
            val characteristic = service?.getCharacteristic(BLUNO_CHARACTERISTIC_UUID)
            if (characteristic != null) {
                characteristic.value = data.toByteArray()
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothGatt!!.writeCharacteristic(characteristic)
                } else {
                    Toast.makeText(this, "블루투스 연결 권한이 거부되었습니다", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Bluno 특성을 찾을 수 없습니다", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
            Toast.makeText(this, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            try {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "GATT 서버에 연결되었습니다", Toast.LENGTH_SHORT).show()
                            // 장치와 연결되었을 때 RecyclerView를 숨기고 연결된 장치 UI를 표시합니다.
                            recyclerView.visibility = View.GONE
                            connectedDeviceLayout.visibility = View.VISIBLE
                        }
                        gatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "GATT 서버에서 연결이 끊어졌습니다", Toast.LENGTH_SHORT).show()
                            bluetoothGatt?.close()
                            bluetoothGatt = null
                            // 연결이 끊어졌을 때 UI를 원래대로 되돌립니다.
                            recyclerView.visibility = View.VISIBLE
                            connectedDeviceLayout.visibility = View.GONE
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    val services = gatt?.services
                    services?.forEach { service ->
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "서비스 발견됨: ${service.uuid}", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "서비스 발견 실패: $status", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    characteristic?.value?.let { value ->
                        runOnUiThread {
                            Toast.makeText(this@SettingsActivity, "특성 읽기: ${String(value)}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
            try {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "특성 쓰기 성공", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
            try {
                characteristic?.value?.let { value ->
                    runOnUiThread {
                        Toast.makeText(this@SettingsActivity, "특성 변경됨: ${String(value)}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(this@SettingsActivity, "보안 예외가 발생했습니다", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
