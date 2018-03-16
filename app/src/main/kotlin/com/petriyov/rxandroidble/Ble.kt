package com.petriyov.rxandroidble

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import java.util.*

/**
 * Created by Eugene on 1/25/2018.
 */
data class ServiceInfo(val serviceUuid: UUID, val characteristics: List<CharacterisiticInfo>)

data class CharacterisiticInfo(val characteristicUuid: UUID, var descriptorUuid: List<UUID>? = null,
                               var propertiesFlags: Int? = null, var permissionsFlags: Int? = null)

data class DeviceInfo(val address: String, val name: String? = null)

data class ReadRequest(val deviceInfo: DeviceInfo, val characterisiticInfo: CharacterisiticInfo, val requestId: Int,
                       val offset: Int)

data class WriteRequest(val deviceInfo: DeviceInfo, val characterisiticInfo: CharacterisiticInfo, val requestId: Int,
                        val offset: Int, val value: ByteArray?, val responseNeeded: Boolean)

enum class ResponseStatus(val value: Int) {
    SUCCESS(BluetoothGatt.GATT_SUCCESS), FAILURE(BluetoothGatt.GATT_FAILURE)
}

enum class BluetoothSupportLevel {
    OK, NO_BLUETOOTH, NO_BLE, NO_BLE_PERIPHERAL, DISABLED
}

enum class BleCharacteristicProperty(val value: Int) {
    PROPERTY_WRITE(BluetoothGattCharacteristic.PROPERTY_WRITE), PROPERTY_READ(BluetoothGattCharacteristic.PROPERTY_READ),
    PROPERTY_WRITE_NO_RESPONSE(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE),
    PROPERTY_NOTIFY(BluetoothGattCharacteristic.PROPERTY_NOTIFY)
}

enum class BleCharacteristicPermission(val value: Int) {
    PERMISSION_READ(BluetoothGattCharacteristic.PERMISSION_READ),
    PERMISSION_WRITE(BluetoothGattCharacteristic.PERMISSION_WRITE)
}

enum class BleState(val state: Int) {
    CONNECTION_STATE_DISCONNECTED(BluetoothProfile.STATE_DISCONNECTED),
    CONNECTION_STATE_CONNECTED(BluetoothProfile.STATE_CONNECTED),
    CONNECTION_STATE_CONNECTING(BluetoothProfile.STATE_CONNECTING),
    CONNECTION_STATE_DISCONNECTING(BluetoothProfile.STATE_DISCONNECTING),
    CONNECTION_STATE_UNKNOWN(-1);

    companion object {

        fun valueOf(state: Int): BleState {
            return when (state) {
                CONNECTION_STATE_CONNECTED.state -> CONNECTION_STATE_CONNECTED
                CONNECTION_STATE_DISCONNECTED.state -> CONNECTION_STATE_DISCONNECTED
                CONNECTION_STATE_CONNECTING.state -> CONNECTION_STATE_CONNECTING
                CONNECTION_STATE_DISCONNECTING.state -> CONNECTION_STATE_DISCONNECTING
                else -> CONNECTION_STATE_UNKNOWN
            }
        }
    }
}
