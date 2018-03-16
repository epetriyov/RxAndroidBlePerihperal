package com.petriyov.rxandroidble.example

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.text.SpannableStringBuilder
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.petriyov.rxandroidble.*
import kotlinx.android.synthetic.main.activity_main.*
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import java.lang.Long
import java.nio.ByteBuffer
import java.util.*

class MainActivity : AppCompatActivity() {

    val spannableLogBuilder = SpannableStringBuilder()

    val dateFormatterForView = DateTimeFormat.forPattern("dd/MM/yyyy")

    private var fromDateTime = DateTime()

    private var toDateTime = DateTime()

    private var rxBleServerConnection: RxBleServerConnection? = null

    private lateinit var rxBleServer: RxBleServer

    private var isServerStarted = false

    private var systemWriteRequest: WriteRequest? = null

    private var daraWriteRequest: WriteRequest? = null

    private val serviceUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
    private val periodUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1016")
    private val systemUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1017")
    private val dataUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1018")
    private val CLIENT_CHARACTERISTIC_CONFIG_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val periodCharacteristic = CharacterisiticInfo(periodUUID, arrayListOf(CLIENT_CHARACTERISTIC_CONFIG_UUID),
            BleCharacteristicProperty.PROPERTY_READ.value
                    or BleCharacteristicProperty.PROPERTY_NOTIFY.value,
            BleCharacteristicPermission.PERMISSION_READ.value or BleCharacteristicPermission.PERMISSION_WRITE.value)

    private val systemCharacteristic = CharacterisiticInfo(systemUUID, null,
            BleCharacteristicProperty.PROPERTY_WRITE.value,
            BleCharacteristicPermission.PERMISSION_WRITE.value)

    private val dataCharacteristic = CharacterisiticInfo(dataUUID, null,
            BleCharacteristicProperty.PROPERTY_WRITE.value,
            BleCharacteristicPermission.PERMISSION_WRITE.value)

    companion object {
        private val TAG = "RxBleServerExample"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        rxBleServer = RxBleServer.getInstance(this)
        btn_start.setOnClickListener {
            if (isServerStarted) {
                stopGattServer()
                isServerStarted = false
                btn_start.text = "Start server"
                panel_control.setEnabledState(false)
            } else {
                startGattServer()
            }
        }
        panel_control.setEnabledState(false)
        edit_from.text = fromDateTime.toString(dateFormatterForView)
        edit_to.text = toDateTime.toString(dateFormatterForView)
        edit_from.setOnClickListener({ showDatePicker(CalendarMode.FROM) })
        edit_to.setOnClickListener({ showDatePicker(CalendarMode.TO) })
        btn_send_period.setOnClickListener({ notifyFromToChanges() })
        btn_system_success.setOnClickListener({
            if (systemWriteRequest != null) {
                rxBleServerConnection?.sendResponse(systemWriteRequest!!.deviceInfo.address, systemWriteRequest!!.requestId,
                        ResponseStatus.SUCCESS, systemWriteRequest!!.offset,
                        systemWriteRequest!!.value)?.subscribe({
                    log("Write response sent : $systemWriteRequest")
                }, {
                    log("Error during write response: ${it.message}", it)
                })
                systemWriteRequest = null
            }
        })
        btn_system_failure.setOnClickListener({
            if (systemWriteRequest != null) {
                rxBleServerConnection?.sendResponse(systemWriteRequest!!.deviceInfo.address, systemWriteRequest!!.requestId,
                        ResponseStatus.FAILURE, systemWriteRequest!!.offset,
                        systemWriteRequest!!.value)?.subscribe({
                    log("Write response sent : $systemWriteRequest")
                }, {
                    log("Error during write response: ${it.message}", it)
                })
                systemWriteRequest = null
            }
        })
        btn_data_success.setOnClickListener({
            if (daraWriteRequest != null) {
                rxBleServerConnection?.sendResponse(daraWriteRequest!!.deviceInfo.address, daraWriteRequest!!.requestId,
                        ResponseStatus.SUCCESS, daraWriteRequest!!.offset,
                        daraWriteRequest!!.value)?.subscribe({
                    log("Write response sent : $daraWriteRequest")
                }, {
                    log("Error during write response: ${it.message}", it)
                })
                daraWriteRequest = null
            }
        })
        btn_data_failure.setOnClickListener({
            if (daraWriteRequest != null) {
                rxBleServerConnection?.sendResponse(daraWriteRequest!!.deviceInfo.address, daraWriteRequest!!.requestId,
                        ResponseStatus.FAILURE, daraWriteRequest!!.offset,
                        daraWriteRequest!!.value)?.subscribe({
                    log("Write response sent : $daraWriteRequest")
                }, {
                    log("Error during write response: ${it.message}", it)
                })
                daraWriteRequest = null
            }
        })
//        btn_add.setOnClickListener { addService() }
//        btn_notify.setOnClickListener { notifyCharacteristicChanges() }
    }

    private fun log(text: String, it: Throwable?) {
        scroll_log.post({
            spannableLogBuilder.append(text)
            spannableLogBuilder.append("\n")
            Log.d(TAG, text, it)
            text_log.text = "$spannableLogBuilder"
            scroll_log.fullScroll(View.FOCUS_DOWN)
        })
    }

    private fun showDatePicker(calendarMode: CalendarMode) {
        val newFragment = DatePickerFragment.newInstance(if (CalendarMode.FROM == calendarMode) fromDateTime else toDateTime)
        newFragment.dateSelects().subscribe({
            when (calendarMode) {
                CalendarMode.TO -> {
                    toDateTime = it
                    edit_to.text = it.toString(dateFormatterForView)
                }
                CalendarMode.FROM -> {
                    fromDateTime = it
                    edit_from.text = it.toString(dateFormatterForView)
                }
            }
        })
        newFragment.show(supportFragmentManager, "datePicker")
    }

    private val fromToBytes = ByteBuffer.allocate(2 * Long.SIZE / java.lang.Byte.SIZE)
            .putLong(fromDateTime.millis)
            .putLong(toDateTime.millis)
            .array()

    private fun notifyFromToChanges() {
        rxBleServerConnection?.notifyCharacteristicChanged(serviceUUID, periodUUID,
                fromToBytes, true)
                ?.subscribe({
                    log("Notification sent to characteristic $periodUUID with value : $fromToBytes")
                },
                        {
                            log("Error during notify changes: ${it.message}", it)
                        })
    }

    private fun addService() {
        rxBleServerConnection?.addServices(arrayListOf(ServiceInfo(UUID.randomUUID(), arrayListOf(periodCharacteristic))))
                ?.subscribe({
                    log("Service added")
                }, {
                    log("Error during add service: ${it.message}", it)
                })
    }

    private fun startGattServer() {
        rxBleServer.checkBluetoothSupportLevel()
                .doOnNext({ log("Bluetooth support level: ${it.name}") })
                .filter({ it == BluetoothSupportLevel.OK })
                .flatMap {
                    rxBleServer.startAdvertising("test", arrayListOf(serviceUUID))
                }
                .flatMap {
                    rxBleServer.startGattServer(arrayListOf(ServiceInfo(serviceUUID,
                            arrayListOf(systemCharacteristic, periodCharacteristic, dataCharacteristic))))
                }
                .subscribe({ subscribeToConnectionEvents(it) }, {
                    log("Error during start server: ${it.message}", it)
                })
    }

    private fun subscribeToConnectionEvents(connection: RxBleServerConnection) {
        isServerStarted = true
        btn_start.text = "Stop server"
        panel_control.setEnabledState(true)
        log("Everything is ok, subscribed to reads/writes")
        rxBleServerConnection = connection
        connection.characteristicReadRequests()
                .filter({ it.characterisiticInfo.characteristicUuid == periodUUID })
                .subscribe({
                    log("Characteristic read request: $it")
                    rxBleServerConnection?.sendResponse(it.deviceInfo.address, it.requestId, ResponseStatus.SUCCESS, it.offset,
                            fromToBytes)?.subscribe({
                        log("Read response sent")
                    }, {
                        log("Error during read response: " + it.message, it)
                    })
                })
        connection.descriptorWriteRequests()
                .filter({ it.characterisiticInfo.characteristicUuid == CLIENT_CHARACTERISTIC_CONFIG_UUID })
                .doOnNext({
                    log("Descriptor write request: $it")
                })
                .flatMap { connection.sendDescriptorWriteResponse(it, ResponseStatus.SUCCESS) }
                .subscribe({
                    log("Send descriptor write response: $it")
                })
        connection.characteristicWriteRequests()
                .filter({ it.characterisiticInfo.characteristicUuid == systemUUID })
                .subscribe({
                    log("Characteristic write request: $it")
                    text_system.post { text_system.text = "CHAR_SYSTEM characteristic value: ${it.value}" }
                    systemWriteRequest = it
                }, {
                    log("Error during write response: ${it.message}", it)
                })
        connection.characteristicWriteRequests()
                .filter({ it.characterisiticInfo.characteristicUuid == dataUUID })
                .subscribe({
                    log("Characteristic write request: $it")
                    text_data.post { text_data.text = "CHAR_DATA characteristic value: ${it.value}" }
                    daraWriteRequest = it
                })
        connection.connectionStateChanges().subscribe({
            log("Connection state changed: $it")
            text_connection.post({
                if (it.second == BleState.CONNECTION_STATE_CONNECTED) {
                    text_connection.text = "Device with address ${it.first.address} connected"
                } else if (it.second == BleState.CONNECTION_STATE_DISCONNECTED) {
                    text_connection.text = "Device with address ${it.first.address} disconnected"
                }
            })

        })
        connection.descriptorReadRequests().subscribe({
            log("Descriptor read request: $it")
        })
    }

    private fun stopGattServer() {
        rxBleServer.stopAdvertising().subscribe({
            log("Advertisement stopped")
        }, {
            log("Error during stop advertising: ${it.message}", it)
        })
        rxBleServerConnection?.closeServer()?.subscribe({
            log("Gatt server stopped")
        })
    }

    private fun log(text: String) {
        log(text, null)
    }

}

enum class CalendarMode {
    FROM, TO
}

fun View.setEnabledState(enabled: Boolean) {
    isEnabled = enabled
    if (this is ViewGroup) {
        for (idx in 0 until childCount) {
            getChildAt(idx).setEnabledState(enabled)
        }
    }
}
