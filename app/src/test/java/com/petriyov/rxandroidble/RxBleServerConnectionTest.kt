package com.petriyov.rxandroidble

import android.bluetooth.*
import android.content.Context
import com.petriyov.rxandroidble.internal.RxBleServerConnectionImpl
import org.junit.Assert
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.mockito.Mockito.*
import org.mockito.internal.verification.Times
import rx.observers.TestSubscriber
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.*


/**
 * Created by Eugene on 1/29/2018.
 */
class RxBleServerConnectionTest {
    companion object {

        private lateinit var mMockContext: Context

        private lateinit var mMockBluetoothManager: BluetoothManager

        private lateinit var mMockBluetoothGattServer: BluetoothGattServer

        private lateinit var rxBleServerConnection: RxBleServerConnection

        private lateinit var bluetoothGattServerCallback: BluetoothGattServerCallback

        private lateinit var bluetoothGattServerCallbackCaptor: ArgumentCaptor<BluetoothGattServerCallback>

        @BeforeClass
        @JvmStatic
        fun setUp() {
            mMockContext = Mockito.mock(Context::class.java)
            mMockBluetoothManager = Mockito.mock(BluetoothManager::class.java)
            mMockBluetoothGattServer = Mockito.mock(BluetoothGattServer::class.java)
            bluetoothGattServerCallbackCaptor = ArgumentCaptor.forClass<BluetoothGattServerCallback,
                    BluetoothGattServerCallback>(BluetoothGattServerCallback::class.java)
            Mockito.`when`(mMockBluetoothManager.openGattServer(Mockito.eq(mMockContext),
                    bluetoothGattServerCallbackCaptor.capture()))
                    .thenReturn(mMockBluetoothGattServer)
        }
    }


    @Before
    fun beforeMethod() {
        rxBleServerConnection = RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext)
        bluetoothGattServerCallback = bluetoothGattServerCallbackCaptor.value
    }

    @Test
    fun test_connectionStateChanges() {
        val testSubscriber = TestSubscriber<Pair<DeviceInfo, BleState>>()
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val testAddress = "testAddress"
        val testName = "testName"
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        rxBleServerConnection.connectionStateChanges().subscribe(testSubscriber)
        bluetoothGattServerCallback.onConnectionStateChange(mockBluetoothDevice, 0, BleState.CONNECTION_STATE_CONNECTED.state)
        testSubscriber.assertValue(Pair(DeviceInfo(testAddress, testName), BleState.CONNECTION_STATE_CONNECTED))
    }

    @Test
    fun test_characteristicReadRequests() {
        val testSubscriber = TestSubscriber<ReadRequest>()
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val testAddress = "testAddress"
        val testName = "testName"
        val mockBluetoothGattCharacteristic = mock(BluetoothGattCharacteristic::class.java)
        val testOffset = 1
        val testDesciptorUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val testRequestId = 2
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        rxBleServerConnection.characteristicReadRequests().subscribe(testSubscriber)
        `when`(mockBluetoothGattCharacteristic.uuid).thenReturn(testDesciptorUUID)
        bluetoothGattServerCallback.onCharacteristicReadRequest(mockBluetoothDevice, testRequestId, testOffset,
                mockBluetoothGattCharacteristic)
        testSubscriber.assertValue(
                ReadRequest(DeviceInfo(testAddress, testName),
                        CharacterisiticInfo(mockBluetoothGattCharacteristic.uuid), testRequestId, testOffset))
    }

    @Test
    fun test_characteristicWriteRequests() {
        val testSubscriber = TestSubscriber<WriteRequest>()
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val testAddress = "testAddress"
        val testName = "testName"
        val mockBluetoothGattCharacteristic = mock(BluetoothGattCharacteristic::class.java)
        val testOffset = 1
        val testDesciptorUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val testValue = ByteArray(1)
        val responseNeeded = true
        val testRequestId = 2
        `when`(mockBluetoothGattCharacteristic.uuid).thenReturn(testDesciptorUUID)
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        rxBleServerConnection.characteristicWriteRequests().subscribe(testSubscriber)
        bluetoothGattServerCallback.onCharacteristicWriteRequest(mockBluetoothDevice, testRequestId,
                mockBluetoothGattCharacteristic, false, responseNeeded, testOffset, testValue)
        testSubscriber.assertValue(
                WriteRequest(DeviceInfo(testAddress, testName),
                        CharacterisiticInfo(mockBluetoothGattCharacteristic.uuid), testRequestId,
                        testOffset, testValue, responseNeeded))
    }

    @Test
    fun test_descriptorWriteRequest() {
        val testSubscriber = TestSubscriber<WriteRequest>()
        rxBleServerConnection.descriptorWriteRequests().subscribe(testSubscriber)
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val testAddress = "testAddress"
        val testName = "testName"
        val mockBluetoothGattDescriptor = mock(BluetoothGattDescriptor::class.java)
        val testOffset = 1
        val testDesciptorUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val testValue = ByteArray(1)
        val responseNeeded = true
        val testRequestId = 2
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockBluetoothGattDescriptor.uuid).thenReturn(testDesciptorUUID)
        bluetoothGattServerCallback.onDescriptorWriteRequest(mockBluetoothDevice, testRequestId,
                mockBluetoothGattDescriptor, false, responseNeeded, testOffset, testValue)
        testSubscriber.assertValue(
                WriteRequest(DeviceInfo(testAddress, testName),
                        CharacterisiticInfo(mockBluetoothGattDescriptor.uuid), testRequestId,
                        testOffset, testValue, responseNeeded))
    }

    @Test
    fun test_descriptorReadRequests() {
        val testAddress = "testAddress"
        val testName = "testName"
        val testOffset = 1
        val testRequestId = 2
        val testDesciptorUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val mockBluetoothGattDescriptor = mock(BluetoothGattDescriptor::class.java)
        val testSubscriber = TestSubscriber<ReadRequest>()
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockBluetoothGattDescriptor.uuid).thenReturn(testDesciptorUUID)
        rxBleServerConnection.descriptorReadRequests().subscribe(testSubscriber)
        bluetoothGattServerCallback.onDescriptorReadRequest(mockBluetoothDevice, testRequestId, testOffset,
                mockBluetoothGattDescriptor)
        testSubscriber.assertValue(
                ReadRequest(DeviceInfo(testAddress, testName),
                        CharacterisiticInfo(mockBluetoothGattDescriptor.uuid), testRequestId, testOffset))
    }

    @Test
    fun test_addServices_closed() {
        clearInvocations(mMockBluetoothGattServer)
        closeConnection()
        val testSubscriber = TestSubscriber<Unit>()
        rxBleServerConnection.addServices(arrayListOf()).subscribe(testSubscriber)
        testSubscriber.assertError(NotConnectedBleException::class.java)
    }

    @Test
    fun test_addServices_ok() {
        clearInvocations(mMockBluetoothGattServer)
        val bluetoothGattServiceCaptor = ArgumentCaptor.forClass<BluetoothGattService,
                BluetoothGattService>(BluetoothGattService::class.java)
        val testUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val testPermissions = 10
        val testProperties = 11
        val characterisiticInfo = CharacterisiticInfo(testUUID, arrayListOf(testUUID), testProperties, testPermissions)
        val testServiceInfo = ServiceInfo(testUUID, arrayListOf(characterisiticInfo))
        val testServicesList = arrayListOf(testServiceInfo)
        val testSubscriber = TestSubscriber<Unit>()
        rxBleServerConnection.addServices(testServicesList).subscribe(testSubscriber)
        verify(mMockBluetoothGattServer).addService(bluetoothGattServiceCaptor.capture())
    }

    @Test
    fun test_stopConnection_closed() {
        clearInvocations(mMockBluetoothGattServer)
        closeConnection()
        val testSubscriber = TestSubscriber<Unit>()
        rxBleServerConnection.closeServer().subscribe(testSubscriber)
        testSubscriber.assertError(NotConnectedBleException::class.java)
    }

    @Test
    fun test_stopConnection() {
        clearInvocations(mMockBluetoothGattServer)
        closeConnection()
        val addServicesSubscriber = TestSubscriber<Boolean>()
        rxBleServerConnection.isClosed().subscribe(addServicesSubscriber)
        addServicesSubscriber.assertValue(true)
    }

    @Test
    fun test_isClosed() {
        clearInvocations(mMockBluetoothGattServer)
        val addServicesSubscriber = TestSubscriber<Boolean>()
        rxBleServerConnection.isClosed().subscribe(addServicesSubscriber)
        addServicesSubscriber.assertValue(false)
        closeConnection()
        addServicesSubscriber.assertValues(false, true)
    }

    @Test
    fun test_sendResponse_closed() {
        closeConnection()
        val responseSubscriber = TestSubscriber<Boolean>()
        val testAddress = "test"
        val requestId = 1
        val status = ResponseStatus.SUCCESS
        val offset = 2
        val value = ByteArray(2)
        rxBleServerConnection.sendResponse(testAddress, requestId, status, offset, value).subscribe(responseSubscriber)
        responseSubscriber.assertError(NotConnectedBleException::class.java)
    }

    @Test
    fun test_sendResponse_wrong_address() {
        val testSubscriber = TestSubscriber<Boolean>()
        val testAddress = "test"
        val requestId = 1
        val status = ResponseStatus.SUCCESS
        val offset = 2
        val value = ByteArray(2)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf())
        rxBleServerConnection.sendResponse(testAddress, requestId, status, offset, value).subscribe(testSubscriber)
        testSubscriber.assertError(WrongAddressBleException::class.java)
    }

    @Test
    fun test_sendResponse_ok() {
        clearInvocations(mMockBluetoothGattServer)
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val testAddress = "testAddress"
        val testName = "testName"
        val responseResult = true
        val requestId = 1
        val status = ResponseStatus.SUCCESS
        val offset = 2
        val value = ByteArray(2)
        val sendResponseSubscriber = TestSubscriber<Boolean>()
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mMockBluetoothGattServer.sendResponse(ArgumentMatchers.any(BluetoothDevice::class.java),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(ByteArray::class.java))).thenReturn(responseResult)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf(mockBluetoothDevice))
        rxBleServerConnection.sendResponse(testAddress, requestId, status, offset, value).subscribe(sendResponseSubscriber)
        sendResponseSubscriber.assertValue(responseResult)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, requestId, status.value, offset, value)
    }


    @Test
    fun test_notifyCharacteristicChanged_closed() {
        closeConnection()
        val testSubscriber = TestSubscriber<Boolean>()
        val testUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val testData = ByteArray(1)
        val confirm = true
        rxBleServerConnection.notifyCharacteristicChanged(testUUID, testUUID, testData, confirm).subscribe(testSubscriber)
        testSubscriber.assertError(NotConnectedBleException::class.java)
    }

    @Test
    fun test_notifyCharacteristicChanged_wrong_device() {
        val testSubscriber = TestSubscriber<Boolean>()
        val testUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val testData = ByteArray(1)
        val confirm = true
        rxBleServerConnection.notifyCharacteristicChanged(testUUID, testUUID, testData, confirm).subscribe(testSubscriber)
        testSubscriber.assertValue(false)
    }

    @Test
    fun test_notifyCharacteristicChanged_wrong_characteristics() {
        val testAddress = "testAddress"
        val testName = "testName"
        val testUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val actualUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1016")
        val testRequestId = 1
        val testData = ByteArray(1)
        val confirm = true
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val mockDescriptor = mock(BluetoothGattDescriptor::class.java)
        val mockService = mock(BluetoothGattService::class.java)
        val mockCharacteristic = mock(BluetoothGattCharacteristic::class.java)
        val testSubscriber = TestSubscriber<Boolean>()
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockCharacteristic.uuid).thenReturn(testUUID)
        `when`(mockDescriptor.characteristic).thenReturn(mockCharacteristic)
        `when`(mockDescriptor.uuid).thenReturn(testUUID)
        `when`(mockService.getCharacteristic(testUUID)).thenReturn(mockCharacteristic)
        `when`(mMockBluetoothGattServer.getService(testUUID)).thenReturn(mockService)
        bluetoothGattServerCallback.onDescriptorWriteRequest(mockBluetoothDevice, testRequestId, mockDescriptor,
                false, false, 0, null)
        rxBleServerConnection.notifyCharacteristicChanged(testUUID, actualUUID, testData, confirm).subscribe(testSubscriber)
        testSubscriber.assertValue(false)
    }

    @Test
    fun test_notifyCharacteristicChanged_ok() {
        clearInvocations(mMockBluetoothGattServer)
        val testAddress = "testAddress"
        val testName = "testName"
        val testUUID = UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015")
        val requestId = 4
        val testData = ByteArray(1)
        val confirm = true
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val mockDescriptor = mock(BluetoothGattDescriptor::class.java)
        val mockService = mock(BluetoothGattService::class.java)
        val mockCharacteristic = mock(BluetoothGattCharacteristic::class.java)
        val mockWriteRequest = mock(WriteRequest::class.java)
        val mockDeviceInfo = mock(DeviceInfo::class.java)
        val mockCharacterisiticInfo = mock(CharacterisiticInfo::class.java)
        val mockCharacteristics = arrayListOf(mockCharacteristic)
        val services = arrayListOf(mockService)
        val writeSubscriber = TestSubscriber<Boolean>()
        val testSubscriber = TestSubscriber<Boolean>()
        val characterisitcCallbackCaptor: ArgumentCaptor<BluetoothGattCharacteristic> =
                ArgumentCaptor.forClass<BluetoothGattCharacteristic,
                        BluetoothGattCharacteristic>(BluetoothGattCharacteristic::class.java)
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockCharacteristic.uuid).thenReturn(testUUID)
        `when`(mockDescriptor.characteristic).thenReturn(mockCharacteristic)
        `when`(mockDescriptor.uuid).thenReturn(testUUID)
        `when`(mockService.getCharacteristic(testUUID)).thenReturn(mockCharacteristic)
        `when`(mockService.characteristics).thenReturn(mockCharacteristics)
        `when`(mockDeviceInfo.address).thenReturn(testAddress)
        `when`(mockWriteRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mockWriteRequest.requestId).thenReturn(requestId)
        `when`(mockWriteRequest.characterisiticInfo).thenReturn(mockCharacterisiticInfo)
        `when`(mockWriteRequest.value).thenReturn(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
        `when`(mockCharacterisiticInfo.characteristicUuid).thenReturn(testUUID)
        `when`(mockCharacteristic.getDescriptor(testUUID)).thenReturn(mockDescriptor)
        `when`(mMockBluetoothGattServer.getService(testUUID)).thenReturn(mockService)
        `when`(mMockBluetoothGattServer.services).thenReturn(services)
        `when`(mMockBluetoothGattServer.sendResponse(ArgumentMatchers.any(BluetoothDevice::class.java),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(ByteArray::class.java))).thenReturn(true)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf(mockBluetoothDevice))
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.SUCCESS).subscribe(writeSubscriber)
        writeSubscriber.assertNoErrors()
        rxBleServerConnection.notifyCharacteristicChanged(testUUID, testUUID, testData, confirm).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mockCharacteristic).setValue(testData)
        verify(mMockBluetoothGattServer).notifyCharacteristicChanged(eq(mockBluetoothDevice),
                characterisitcCallbackCaptor.capture(), eq(confirm))
        Assert.assertEquals(mockCharacteristic, characterisitcCallbackCaptor.value)
    }

    @Test
    fun test_cancelConnection_closed() {
        val testAddress = "testAddress"
        val testSubscriber = TestSubscriber<Unit>()
        rxBleServerConnection.closeServer().subscribe()
        rxBleServerConnection.cancelConnection(testAddress).subscribe(testSubscriber)
        testSubscriber.assertError(NotConnectedBleException::class.java)
    }

    @Test
    fun test_cancelConnection_wrong_device() {
        val testAddress = "testAddress"
        val testSubscriber = TestSubscriber<Unit>()
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf())
        rxBleServerConnection.cancelConnection(testAddress).subscribe(testSubscriber)
        testSubscriber.assertError(WrongAddressBleException::class.java)
    }

    @Test
    fun test_cancelConnection_ok() {
        clearInvocations(mMockBluetoothGattServer)
        val testAddress = "testAddress"
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val testSubscriber = TestSubscriber<Unit>()
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf(mockBluetoothDevice))
        rxBleServerConnection.cancelConnection(testAddress).subscribe(testSubscriber)
        verify(mMockBluetoothGattServer).cancelConnection(mockBluetoothDevice)
        testSubscriber.assertNoErrors()
    }

    @Test
    fun test_sendDescriptorReadResponse_closed() {
        closeConnection()
        val testSubscriber = TestSubscriber<Boolean>()
        val mockReadRequest = mock(ReadRequest::class.java)
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertError(NotConnectedBleException::class.java)
    }

    @Test
    fun test_sendDescriptorReadResponse_wrong_device() {
        val testSubscriber = TestSubscriber<Boolean>()
        val mockReadRequest = mock(ReadRequest::class.java)
        val mockDeviceInfo = mock(DeviceInfo::class.java)
        val testAddress = "testAddress"
        `when`(mockDeviceInfo.address).thenReturn(testAddress)
        `when`(mockReadRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf())
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(false)
    }

    @Test
    fun test_sendDescriptorReadResponse_failure() {
        clearInvocations(mMockBluetoothGattServer)
        val testAddress = "testAddress"
        val testName = "testName"
        val testUUID = UUID.randomUUID()
        val requestId = 4
        val mockReadRequest = mock(ReadRequest::class.java)
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val mockDeviceInfo = mock(DeviceInfo::class.java)
        val mockCharacterisiticInfo = mock(CharacterisiticInfo::class.java)
        var testSubscriber = TestSubscriber<Boolean>()
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockDeviceInfo.address).thenReturn(testAddress)
        `when`(mockCharacterisiticInfo.characteristicUuid).thenReturn(testUUID)
        `when`(mockReadRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mockReadRequest.characterisiticInfo).thenReturn(mockCharacterisiticInfo)
        `when`(mockReadRequest.requestId).thenReturn(requestId)
        `when`(mMockBluetoothGattServer.services).thenReturn(arrayListOf())
        `when`(mMockBluetoothGattServer.sendResponse(ArgumentMatchers.any(BluetoothDevice::class.java),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(ByteArray::class.java))).thenReturn(true)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf(mockBluetoothDevice))
        // test FAILURE status
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.FAILURE).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockReadRequest.requestId,
                BluetoothGatt.GATT_FAILURE, 0, null)
        clearInvocations(mMockBluetoothGattServer)
        // test wrong descriptor
        testSubscriber = TestSubscriber()
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockReadRequest.requestId,
                BluetoothGatt.GATT_FAILURE, 0, null)
    }

    @Test
    fun test_sendDescriptorReadResponse_ok() {
        clearInvocations(mMockBluetoothGattServer)
        val enableBytes = byteArrayOf(0x01, 0x00)
        val disableBytes = byteArrayOf(0x00, 0x00)
        setFinalStatic(BluetoothGattDescriptor::class.java.getField("ENABLE_NOTIFICATION_VALUE"), enableBytes)
        setFinalStatic(BluetoothGattDescriptor::class.java.getField("DISABLE_NOTIFICATION_VALUE"), disableBytes)
        val testAddress = "testAddress"
        val testName = "testName"
        val testUUID = UUID.randomUUID()
        val requestId = 4
        val mockReadRequest = mock(ReadRequest::class.java)
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val mockDeviceInfo = mock(DeviceInfo::class.java)
        val mockCharacterisiticInfo = mock(CharacterisiticInfo::class.java)
        val mockService = mock(BluetoothGattService::class.java)
        val mockCharacteristic = mock(BluetoothGattCharacteristic::class.java)
        val mockDescriptor = mock(BluetoothGattDescriptor::class.java)
        val mockCharacteristics = arrayListOf(mockCharacteristic)
        val services = arrayListOf(mockService)
        val mockWriteRequest = mock(WriteRequest::class.java)
        var testSubscriber = TestSubscriber<Boolean>()
        `when`(mockWriteRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mockWriteRequest.requestId).thenReturn(requestId)
        `when`(mockWriteRequest.characterisiticInfo).thenReturn(mockCharacterisiticInfo)
        `when`(mockWriteRequest.value).thenReturn(enableBytes)
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockDeviceInfo.address).thenReturn(testAddress)
        `when`(mockCharacterisiticInfo.characteristicUuid).thenReturn(testUUID)
        `when`(mockReadRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mockReadRequest.characterisiticInfo).thenReturn(mockCharacterisiticInfo)
        `when`(mockReadRequest.requestId).thenReturn(requestId)
        `when`(mockCharacteristic.getDescriptor(testUUID)).thenReturn(mockDescriptor)
        `when`(mockCharacteristic.uuid).thenReturn(testUUID)
        `when`(mockDescriptor.characteristic).thenReturn(mockCharacteristic)
        `when`(mockService.characteristics).thenReturn(mockCharacteristics)
        `when`(mMockBluetoothGattServer.services).thenReturn(services)
        `when`(mMockBluetoothGattServer.sendResponse(eq(mockBluetoothDevice), eq(requestId), ArgumentMatchers.anyInt(),
                ArgumentMatchers.anyInt(), ArgumentMatchers.any())).thenReturn(true)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf(mockBluetoothDevice))
        // test enabled state
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockReadRequest.requestId,
                BluetoothGatt.GATT_SUCCESS, 0, disableBytes)
        clearInvocations(mMockBluetoothGattServer)
        testSubscriber = TestSubscriber()
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        // test disabled state
        testSubscriber = TestSubscriber()
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockReadRequest.requestId,
                BluetoothGatt.GATT_SUCCESS, 0, enableBytes)
    }

    @Test
    fun test_sendDescriptorWriteResponse_closed() {
        closeConnection()
        val testSubscriber = TestSubscriber<Boolean>()
        val mockWriteRequest = mock(WriteRequest::class.java)
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertError(NotConnectedBleException::class.java)
    }

    @Test
    fun test_sendDescriptorWriteResponse_wrong_device() {
        val testSubscriber = TestSubscriber<Boolean>()
        val mockWriteRequest = mock(WriteRequest::class.java)
        val mockDeviceInfo = mock(DeviceInfo::class.java)
        val testAddress = "testAddress"
        `when`(mockDeviceInfo.address).thenReturn(testAddress)
        `when`(mockWriteRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf())
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(false)
    }

    @Test
    fun test_sendDescriptorWriteResponse_failure() {
        clearInvocations(mMockBluetoothGattServer)
        val testAddress = "testAddress"
        val testName = "testName"
        val testUUID = UUID.randomUUID()
        val requestId = 3
        val mockWriteRequest = mock(WriteRequest::class.java)
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val mockDeviceInfo = mock(DeviceInfo::class.java)
        val mockCharacterisiticInfo = mock(CharacterisiticInfo::class.java)
        var testSubscriber = TestSubscriber<Boolean>()
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockDeviceInfo.address).thenReturn(testAddress)
        `when`(mockCharacterisiticInfo.characteristicUuid).thenReturn(testUUID)
        `when`(mockWriteRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mockWriteRequest.characterisiticInfo).thenReturn(mockCharacterisiticInfo)
        `when`(mockWriteRequest.requestId).thenReturn(requestId)
        `when`(mMockBluetoothGattServer.services).thenReturn(arrayListOf())
        `when`(mMockBluetoothGattServer.sendResponse(ArgumentMatchers.any(BluetoothDevice::class.java),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.any(ByteArray::class.java))).thenReturn(true)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf(mockBluetoothDevice))
        // test FAILURE status
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.FAILURE).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockWriteRequest.requestId,
                BluetoothGatt.GATT_FAILURE, 0, null)
        clearInvocations(mMockBluetoothGattServer)
        // test wrong descriptor
        testSubscriber = TestSubscriber()
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockWriteRequest.requestId,
                BluetoothGatt.GATT_FAILURE, 0, null)
    }

    @Test
    fun test_sendDescriptorWriteResponse_ok() {
        clearInvocations(mMockBluetoothGattServer)
        val enableBytes = byteArrayOf(0x01, 0x00)
        val disableBytes = byteArrayOf(0x00, 0x00)
        setFinalStatic(BluetoothGattDescriptor::class.java.getField("ENABLE_NOTIFICATION_VALUE"), enableBytes)
        setFinalStatic(BluetoothGattDescriptor::class.java.getField("DISABLE_NOTIFICATION_VALUE"), disableBytes)
        val testAddress = "testAddress"
        val testName = "testName"
        val testUUID = UUID.randomUUID()
        val requestId = 4
        val mockWriteRequest = mock(WriteRequest::class.java)
        val mockBluetoothDevice = mock(BluetoothDevice::class.java)
        val mockDeviceInfo = mock(DeviceInfo::class.java)
        val mockCharacterisiticInfo = mock(CharacterisiticInfo::class.java)
        val mockService = mock(BluetoothGattService::class.java)
        val mockCharacteristic = mock(BluetoothGattCharacteristic::class.java)
        val mockDescriptor = mock(BluetoothGattDescriptor::class.java)
        val mockCharacteristics = arrayListOf(mockCharacteristic)
        val services = arrayListOf(mockService)
        val mockReadRequest = mock(ReadRequest::class.java)
        var testSubscriber = TestSubscriber<Boolean>()
        `when`(mockReadRequest.characterisiticInfo).thenReturn(mockCharacterisiticInfo)
        `when`(mockReadRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mockReadRequest.requestId).thenReturn(requestId)
        `when`(mockBluetoothDevice.address).thenReturn(testAddress)
        `when`(mockBluetoothDevice.name).thenReturn(testName)
        `when`(mockDeviceInfo.address).thenReturn(testAddress)
        `when`(mockWriteRequest.deviceInfo).thenReturn(mockDeviceInfo)
        `when`(mockWriteRequest.requestId).thenReturn(requestId)
        `when`(mockWriteRequest.characterisiticInfo).thenReturn(mockCharacterisiticInfo)
        `when`(mockWriteRequest.value).thenReturn(enableBytes)
        `when`(mockCharacterisiticInfo.characteristicUuid).thenReturn(testUUID)
        `when`(mockCharacteristic.uuid).thenReturn(testUUID)
        `when`(mockCharacteristic.getDescriptor(testUUID)).thenReturn(mockDescriptor)
        `when`(mockDescriptor.characteristic).thenReturn(mockCharacteristic)
        `when`(mockService.characteristics).thenReturn(mockCharacteristics)
        `when`(mMockBluetoothGattServer.services).thenReturn(services)
        `when`(mMockBluetoothGattServer.sendResponse(ArgumentMatchers.any(BluetoothDevice::class.java),
                ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt(),
                ArgumentMatchers.any())).thenReturn(true)
        `when`(mMockBluetoothManager.getConnectedDevices(ArgumentMatchers.anyInt())).thenReturn(arrayListOf(mockBluetoothDevice))
        //test write enabled status
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(false)
        verify(mMockBluetoothGattServer, Times(0)).sendResponse(mockBluetoothDevice, mockWriteRequest.requestId,
                BluetoothGatt.GATT_SUCCESS, 0, enableBytes)
        clearInvocations(mMockBluetoothGattServer)
        testSubscriber = TestSubscriber()
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockReadRequest.requestId,
                BluetoothGatt.GATT_SUCCESS, 0, enableBytes)
        `when`(mockWriteRequest.responseNeeded).thenReturn(true)
        `when`(mockWriteRequest.value).thenReturn(disableBytes)
        //test write disabled status
        testSubscriber = TestSubscriber()
        rxBleServerConnection.sendDescriptorWriteResponse(mockWriteRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockWriteRequest.requestId,
                BluetoothGatt.GATT_SUCCESS, 0, null)
        clearInvocations(mMockBluetoothGattServer)
        testSubscriber = TestSubscriber()
        rxBleServerConnection.sendDescriptorReadResponse(mockReadRequest, ResponseStatus.SUCCESS).subscribe(testSubscriber)
        testSubscriber.assertValue(true)
        verify(mMockBluetoothGattServer).sendResponse(mockBluetoothDevice, mockReadRequest.requestId,
                BluetoothGatt.GATT_SUCCESS, 0, disableBytes)
    }

    private fun closeConnection() {
        val testSubscriber = TestSubscriber<Unit>()
        val isClosedSubscriber = TestSubscriber<Boolean>()
        rxBleServerConnection.closeServer().subscribe(testSubscriber)
        testSubscriber.assertValue(Unit)
        rxBleServerConnection.isClosed().subscribe(isClosedSubscriber)
        isClosedSubscriber.assertValue(true)
    }

    @Throws(Exception::class)
    private fun setFinalStatic(field: Field, newValue: Any) {
        field.isAccessible = true
        val modifiersField = Field::class.java.getDeclaredField("modifiers")
        modifiersField.isAccessible = true
        modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
        field.set(null, newValue)
    }
}
