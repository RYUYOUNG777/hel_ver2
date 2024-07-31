package com.example.hel_ver2

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DeviceAdapter(private val devices: List<BluetoothDevice>, private val onClick: (BluetoothDevice) -> Unit) :
    RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    class DeviceViewHolder(itemView: View, private val onClick: (BluetoothDevice) -> Unit) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(android.R.id.text1)
        private var currentDevice: BluetoothDevice? = null

        init {
            itemView.setOnClickListener {
                currentDevice?.let {
                    onClick(it)
                }
            }
        }

        fun bind(device: BluetoothDevice) {
            currentDevice = device
            deviceName.text = device.name ?: device.address
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(android.R.layout.simple_list_item_1, parent, false)
        return DeviceViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size
}
