package com.petriyov.rxandroidble.internal

import android.bluetooth.*
import android.content.Context
import com.petriyov.rxandroidble.*
import rx.Observable
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.*


/**
 * Created by Eugene on 1/26/2018.
 */
internal class RxBleServerConnectionImpl(private val mBluetoothManager: BluetoothManager, context: Context) :
        RxBleServerConnection, BluetoothGattServerCallback() {

    companion object {
        private fun buildBleService(serviceInfo: ServiceInfo?): BluetoothGattService? {
            if (serviceInfo?.characteristics == null) {
                return null
            }
            val service = BluetoothGattService(serviceInfo.serviceUuid,
                    BluetoothGattService.SERVICE_TYPE_PRIMARY)
            for (characteristic in serviceInfo.characteristics) {
                val currentCharacteristic = BluetoothGattCharacteristic(characteristic.characteristicUuid,
                        characteristic.propertiesFlags ?: 0,
                        characteristic.permissionsFlags ?: 0)
                characteristic.descriptorUuid
                        ?.map {
                            BluetoothGattDescriptor(it,
                                    characteristic.permissionsFlags ?: 0)
                        }
                        ?.forEach { currentCharacteristic.addDescriptor(it) }
                service.addCharacteristic(currentCharacteristic)
            }
            return service
        }
    }

    private var isClosed: Boolean = false

    private var closedSubject = BehaviorSubject.create<Boolean>(false)

    private val connectionStateSubject = PublishSubject.create<Pair<DeviceInfo, BleState>>()

    private val readCharacteristicSubject = PublishSubject.create<ReadRequest>()

    private val writeCharacteristicSubject = PublishSubject.create<WriteRequest>()

    private val descriptorReadSubject = PublishSubject.create<ReadRequest>()

    private val descriptorWriteSubject = PublishSubject.create<WriteRequest>()

    private val bluetoothGattServer: BluetoothGattServer = mBluetoothManager.openGattServer(context, this)
            ?: throw GattServerBleException("No services to create GATT server")

    private val registeredDevicesMap: MutableMap<UUID, MutableSet<BluetoothDevice>> = mutableMapOf()

    private fun checkClosed() {
        if (isClosed) {
            throw NotConnectedBleException("Ble gatt server not started yet")
        }
    }

    private fun getDescriptorByUUID(descriptorUuid: UUID): BluetoothGattDescriptor? {
        val bluetoothGattDescriptor = bluetoothGattServer.services.flatMap { it.characteristics }
                .mapNotNull { it.getDescriptor(descriptorUuid) }
        return if (bluetoothGattDescriptor.isEmpty()) null else bluetoothGattDescriptor[0]
    }

    override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                         descriptor: BluetoothGattDescriptor) {
        descriptorReadSubject.onNext(ReadRequest(DeviceInfo(device.address, device.name),
                CharacterisiticInfo(descriptor.uuid), requestId, offset))
    }

    override fun onCharacteristicWriteRequest(device: BluetoothDevice, requestId: Int,
                                              characteristic: BluetoothGattCharacteristic, preparedWrite: Boolean,
                                              responseNeeded: Boolean, offset: Int, value: ByteArray) {
        writeCharacteristicSubject.onNext(WriteRequest(DeviceInfo(device.address, device.name),
                CharacterisiticInfo(characteristic.uuid), requestId, offset, value, responseNeeded))
    }

    override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                             characteristic: BluetoothGattCharacteristic) {
        readCharacteristicSubject.onNext(ReadRequest(DeviceInfo(device.address, device.name),
                CharacterisiticInfo(characteristic.uuid), requestId, offset))
    }

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        connectionStateSubject.onNext(Pair(DeviceInfo(device.address, device.name), BleState.valueOf(newState)))
    }

    override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                          descriptor: BluetoothGattDescriptor,
                                          preparedWrite: Boolean, responseNeeded: Boolean, offset: Int,
                                          value: ByteArray?) {
        descriptorWriteSubject.onNext(WriteRequest(DeviceInfo(device.address, device.name),
                CharacterisiticInfo(descriptor.uuid), requestId, offset, value, responseNeeded))
    }

    override fun cancelConnection(deviceAddress: String): Observable<Unit> {
        return Observable.fromCallable {
            checkClosed()
            val device = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                    .find { bluetoothDevice -> bluetoothDevice.address == deviceAddress }
                    ?: throw WrongAddressBleException("There is no device connected with address: $deviceAddress")
            bluetoothGattServer.cancelConnection(device)
        }
    }

    override fun isClosed(): Observable<Boolean> = closedSubject

    override fun sendResponse(deviceAddress: String, requestId: Int, status: ResponseStatus, offset: Int,
                              value: ByteArray?): Observable<Boolean> {
        return Observable.fromCallable({
            checkClosed()
            val device = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                    .find { bluetoothDevice -> bluetoothDevice.address == deviceAddress }
                    ?: throw WrongAddressBleException("There is no device connected with address: $deviceAddress")
            bluetoothGattServer.sendResponse(device, requestId, status.value, offset, value)
        })
    }

    override fun notifyCharacteristicChanged(serviceUUID: UUID, characterisiticUUID: UUID, value: ByteArray,
                                             confirm: Boolean): Observable<Boolean> {
        return Observable.fromCallable {
            checkClosed()
            val devices = registeredDevicesMap[characterisiticUUID]
            val characteristic = bluetoothGattServer.getService(serviceUUID)?.getCharacteristic(characterisiticUUID)
            if (devices == null || characteristic == null) {
                false
            } else {
                characteristic.value = value
                devices.forEach { bluetoothGattServer.notifyCharacteristicChanged(it, characteristic, confirm) }
                true
                //TODO return result of sending to each device
            }
        }
    }

    override fun sendDescriptorReadResponse(readRequest: ReadRequest, status: ResponseStatus): Observable<Boolean> {
        return Observable.fromCallable {
            checkClosed()
            val device = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                    .find { bluetoothDevice -> bluetoothDevice.address == readRequest.deviceInfo.address }
            if (device == null) {
                false
            } else {
                val descriptor = getDescriptorByUUID(readRequest.characterisiticInfo.characteristicUuid)
                if (status == ResponseStatus.FAILURE || descriptor == null) {
                    bluetoothGattServer.sendResponse(device,
                            readRequest.requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null)
                } else {
                    val returnValue: ByteArray? =
                            if (registeredDevicesMap[descriptor.characteristic.uuid]?.contains(device) == true) {
                                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                            } else {
                                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                            }
                    bluetoothGattServer.sendResponse(device,
                            readRequest.requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            returnValue)
                }
            }
        }
    }

    override fun sendDescriptorWriteResponse(writeRequest: WriteRequest, status: ResponseStatus): Observable<Boolean> {
        return Observable.fromCallable {
            checkClosed()
            val device = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT)
                    .find { bluetoothDevice -> bluetoothDevice.address == writeRequest.deviceInfo.address }
            if (device == null) {
                false
            } else {
                val descriptor = getDescriptorByUUID(writeRequest.characterisiticInfo.characteristicUuid)
                if (status == ResponseStatus.FAILURE || descriptor == null) {
                    bluetoothGattServer.sendResponse(device,
                            writeRequest.requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null)
                } else {
                    var devices = registeredDevicesMap[descriptor.characteristic.uuid]
                    if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, writeRequest.value)) {
                        // Subscribe device to notifications
                        if (devices == null) {
                            devices = mutableSetOf()
                            registeredDevicesMap[descriptor.characteristic.uuid] = devices
                        }
                        devices.add(device)
                    } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, writeRequest.value)) {
                        // Unsubscribe device from notifications
                        devices?.remove(device)
                        if (devices?.isEmpty() == true) {
                            registeredDevicesMap.remove(descriptor.characteristic.uuid)
                        }
                    }

                    if (writeRequest.responseNeeded) {
                        bluetoothGattServer.sendResponse(device,
                                writeRequest.requestId,
                                BluetoothGatt.GATT_SUCCESS,
                                0,
                                null)
                    } else {
                        false
                    }
                }
            }
        }
    }

    override fun addServices(services: List<ServiceInfo>): Observable<Unit> {
        return Observable.fromCallable({
            checkClosed()
            services.mapNotNull { buildBleService(it) }.forEach { bluetoothGattServer.addService(it) }
        })
    }

    override fun descriptorReadRequests(): Observable<ReadRequest> {
        return descriptorReadSubject
    }

    override fun descriptorWriteRequests(): Observable<WriteRequest> {
        return descriptorWriteSubject
    }

    override fun closeServer(): Observable<Unit> {
        return Observable.fromCallable({
            checkClosed()
            bluetoothGattServer.close()
            isClosed = true
            closedSubject.onNext(true)
        })
    }

    override fun connectionStateChanges(): Observable<Pair<DeviceInfo, BleState>> {
        return connectionStateSubject
    }

    override fun characteristicReadRequests(): Observable<ReadRequest> {
        return readCharacteristicSubject
    }

    override fun characteristicWriteRequests(): Observable<WriteRequest> {
        return writeCharacteristicSubject
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other is RxBleServerConnectionImpl && other.bluetoothGattServer == bluetoothGattServer
    }
}
