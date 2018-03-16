package com.petriyov.rxandroidble

/**
 * Created by Eugene on 1/26/2018.
 */
/**
 * Base exception for BLE actions
 */
sealed class BleServerException(message: String?) : RuntimeException(message) {

    var code: Int? = null
        private set(value) {}

    constructor(message: String?, code: Int) : this(message) {
        this.code = code
    }
}

/**
 * Thrown to indicate that bluetooth is not available
 */
class BluetoothNotAvailableException(message: String) : BleServerException(message)

/**
 * Thrown to indicate that services are empty
 */
class EmptyServicesBleException(message: String) : BleServerException(message)

/**
 * Thrown to indicate that gatt server can't be opened
 */
class GattServerBleException(message: String) : BleServerException(message)

/**
 * Thrown to indicate that gatt server is already started
 */
class GattServerAlreadyStartedBleException(message: String) : BleServerException(message)

/**
 * Thrown to indicate that Bluetooth Low Energy is unavailable
 */
class LeNotAvailableBleException(message: String) : BleServerException(message)

/**
 * Thrown to indicate that Gatt server is not connected
 */
class NotConnectedBleException(message: String) : BleServerException(message)

/**
 * Thrown to indicate that wrong address was specified
 */
class WrongAddressBleException(message: String) : BleServerException(message)

/**
 * Thrown to indicate that advertisement start failed
 */
class AdvertisementBleException(message: String?, code: Int) : BleServerException(message, code) {
    override fun equals(other: Any?): Boolean {
        return (other != null && other is AdvertisementBleException && other.code == code)
    }
}
