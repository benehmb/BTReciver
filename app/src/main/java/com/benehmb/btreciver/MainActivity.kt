package com.benehmb.btreciver

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import app.akexorcist.bluetotohspp.library.BluetoothSPP
import app.akexorcist.bluetotohspp.library.BluetoothState
import app.akexorcist.bluetotohspp.library.DeviceList
import kotlinx.android.synthetic.main.activity_main.*
import app.akexorcist.bluetotohspp.library.BluetoothSPP.AutoConnectionListener



private const val REQUEST_ENABLE_BT = 9274

class MainActivity : AppCompatActivity() {
    private val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val hasBluetoothAdapter = bluetoothAdapter != null
    private lateinit var bluetooth: BluetoothSPP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        btnSend.setOnClickListener {
            bluetooth.send(sendText.text.toString(), true)
        }
        bluetooth = BluetoothSPP(this)
        enableBluetooth()

        bluetooth.setOnDataReceivedListener { _, message ->
            textOut.text = message
            //println(data)
        }

        bluetooth.setAutoConnectionListener(object : AutoConnectionListener {
            override fun onNewConnection(name: String, address: String) {
                // Do something when reaching for new connection device
            }

            override fun onAutoConnectionStarted() {
                // Do something when auto connection has started
            }
        })

        reconnect.setOnClickListener {
            bluetooth.disconnect()
            bluetooth.stopService()
            chooseDevice()
        }

        bluetooth.setBluetoothStateListener {
            state.text = getString(when (it) {
                BluetoothState.STATE_CONNECTING -> R.string.state_connecting
                BluetoothState.STATE_CONNECTED -> R.string.state_connected
                BluetoothState.STATE_LISTEN -> R.string.state_listening
                BluetoothState.STATE_NONE -> R.string.state_none
                else -> R.string.state_unknown
            })
            if(it == BluetoothState.STATE_CONNECTED){
                state.append(" to " + bluetooth.connectedDeviceName)
            }
        }
    }

    private fun chooseDevice() {
        bluetooth.setupService()
        bluetooth.startService(BluetoothState.DEVICE_ANDROID)

        val intent = Intent(applicationContext, DeviceList::class.java)
        startActivityForResult(intent, BluetoothState.REQUEST_CONNECT_DEVICE)
    }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BluetoothState.REQUEST_CONNECT_DEVICE) {
            if (resultCode == Activity.RESULT_OK) {
                bluetooth.connect(data)
            }
        } else if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is up and running, we're good to go
                chooseDevice()
            } else {
                // Bluetooth is not enabled. We'll just send another request
                // Please don't do this in actual production releases
                enableBluetooth()
            }
        }
    }

    private fun enableBluetooth() {
        if (hasBluetoothAdapter) {
            if (!bluetoothAdapter.isEnabled) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else {
                chooseDevice()
            }
        } else {
            // Device doesn't support bluetooth
            Toast.makeText(this, "No bluetooth adapter found!", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
