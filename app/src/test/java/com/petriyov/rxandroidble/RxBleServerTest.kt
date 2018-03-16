package com.petriyov.rxandroidble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.os.ParcelUuid
import com.petriyov.rxandroidble.internal.AdvertiseModule
import com.petriyov.rxandroidble.internal.RxBleServerConnectionImpl
import com.petriyov.rxandroidble.internal.RxBleServerImpl
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.*
import org.mockito.internal.verification.Times
import rx.observers.TestSubscriber
import rx.subjects.BehaviorSubject
import rx.subjects.PublishSubject
import java.util.*


/**
 * Created by Eugene on 1/29/2018.
 */
class RxBleServerTest {

    companion object {

        private lateinit var mMockContext: Context

        private lateinit var mMockAdvertiseSettings: AdvertiseSettings

        private lateinit var mMockAdvertiseData: AdvertiseData

        private lateinit var mMockAdvertiseDataBuilder: AdvertiseData.Builder

        private lateinit var mMockPackageManager: PackageManager

        private lateinit var mMockBluetoothManager: BluetoothManager

        private lateinit var mMockBluetoothAdapter: BluetoothAdapter

        private lateinit var mMockBluetoothLeAdvertiser: BluetoothLeAdvertiser

        private lateinit var mMockBluetoothGattServer: BluetoothGattServer

        private lateinit var mMockAdvertiseProvider: AdvertiseModule

        private lateinit var rxBleServer: RxBleServer

        @BeforeClass
        @JvmStatic
        fun setUp() {
            mMockContext = mock(Context::class.java)
            mMockAdvertiseProvider = mock(AdvertiseModule::class.java)
            mMockPackageManager = mock(PackageManager::class.java)
            mMockAdvertiseSettings = mock(AdvertiseSettings::class.java)
            mMockAdvertiseData = mock(AdvertiseData::class.java)
            mMockAdvertiseDataBuilder = mock(AdvertiseData.Builder::class.java)
            mMockBluetoothManager = mock(BluetoothManager::class.java)
            mMockBluetoothAdapter = mock(BluetoothAdapter::class.java)
            mMockBluetoothLeAdvertiser = mock(BluetoothLeAdvertiser::class.java)
            mMockBluetoothGattServer = mock(BluetoothGattServer::class.java)
            `when`(mMockAdvertiseDataBuilder.addServiceUuid(ArgumentMatchers.any(ParcelUuid::class.java)))
                    .thenReturn(mMockAdvertiseDataBuilder)
            `when`(mMockAdvertiseDataBuilder.build()).thenReturn(mMockAdvertiseData)
            `when`(mMockAdvertiseProvider.provideAdvertiseDataBuilder()).thenReturn(mMockAdvertiseDataBuilder)
            `when`(mMockAdvertiseProvider.provideAdvertiseSetting()).thenReturn(mMockAdvertiseSettings)
            `when`(mMockContext.packageManager).thenReturn(mMockPackageManager)
            `when`(mMockContext.getSystemService(BLUETOOTH_SERVICE)).thenReturn(mMockBluetoothManager)
        }
    }

    private fun test_startAdvertising_failure(errorCode: Int) {
        clearInvocations(mMockBluetoothLeAdvertiser)
        clearInvocations(mMockAdvertiseDataBuilder)
        `when`(mMockBluetoothAdapter.bluetoothLeAdvertiser).thenReturn(mMockBluetoothLeAdvertiser)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        val testSubscriber = TestSubscriber<Unit>()
        val testServiceUuids = arrayListOf<UUID>(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"))
        rxBleServer.startAdvertising("test", testServiceUuids).subscribe(testSubscriber)
        val advertisementCallbackCaptor = ArgumentCaptor.forClass<AdvertiseCallback, AdvertiseCallback>(AdvertiseCallback::class.java)
        verify(mMockBluetoothLeAdvertiser).startAdvertising(
                eq(mMockAdvertiseSettings),
                eq(mMockAdvertiseData),
                advertisementCallbackCaptor.capture())
        verify(mMockAdvertiseDataBuilder, Times(1))
                .addServiceUuid(ArgumentMatchers.any(ParcelUuid::class.java))
        advertisementCallbackCaptor.value.onStartFailure(errorCode)
        testSubscriber.assertError(AdvertisementBleException("", errorCode))
    }

    @Before
    fun beforeEveryMethod() {
        rxBleServer = RxBleServerImpl(mMockContext, mMockAdvertiseProvider)
    }

    @Test
    fun test_checkBluetoothSupportLevel_no_bluetooth() {
        `when`(mMockBluetoothManager.adapter).thenReturn(null)
        var testSubscriber = TestSubscriber<BluetoothSupportLevel>()
        rxBleServer.checkBluetoothSupportLevel().subscribe(testSubscriber)
        testSubscriber.assertValue(BluetoothSupportLevel.NO_BLUETOOTH)
    }

    @Test
    fun test_checkBluetoothSupportLevel_no_ble() {
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        `when`(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)).thenReturn(false)
        var testSubscriber = TestSubscriber<BluetoothSupportLevel>()
        rxBleServer.checkBluetoothSupportLevel().subscribe(testSubscriber)
        testSubscriber.assertValue(BluetoothSupportLevel.NO_BLE)
    }

    @Test
    fun test_checkBluetoothSupportLevel_no_peripheral_mode() {
        `when`(mMockBluetoothAdapter.isMultipleAdvertisementSupported).thenReturn(false)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        `when`(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)).thenReturn(true)
        var testSubscriber = TestSubscriber<BluetoothSupportLevel>()
        rxBleServer.checkBluetoothSupportLevel().subscribe(testSubscriber)
        testSubscriber.assertValue(BluetoothSupportLevel.NO_BLE_PERIPHERAL)
    }

    @Test
    fun test_checkBluetoothSupportLevel_ok() {
        `when`(mMockBluetoothAdapter.isMultipleAdvertisementSupported).thenReturn(true)
        `when`(mMockBluetoothAdapter.isEnabled).thenReturn(true)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        `when`(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)).thenReturn(true)
        var testSubscriber = TestSubscriber<BluetoothSupportLevel>()
        rxBleServer.checkBluetoothSupportLevel().subscribe(testSubscriber)
        testSubscriber.assertValue(BluetoothSupportLevel.OK)
    }

    @Test
    fun test_checkBluetoothSupportLevel_disabled() {
        `when`(mMockBluetoothAdapter.isMultipleAdvertisementSupported).thenReturn(true)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        `when`(mMockPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)).thenReturn(true)
        var testSubscriber = TestSubscriber<BluetoothSupportLevel>()
        rxBleServer.checkBluetoothSupportLevel().subscribe(testSubscriber)
        testSubscriber.assertValue(BluetoothSupportLevel.DISABLED)
    }

    @Test
    fun test_startAdvertising_empty_services() {
        var testSubscriber = TestSubscriber<Unit>()
        rxBleServer.startAdvertising("test", arrayListOf()).subscribe(testSubscriber)
        testSubscriber.assertError(EmptyServicesBleException::class.java)
    }

    @Test
    fun test_startAdvertising_no_bluetooth() {
        `when`(mMockBluetoothManager.adapter).thenReturn(null)
        var testSubscriber = TestSubscriber<Unit>()
        val testServiceUuids = arrayListOf<UUID>(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"))
        rxBleServer.startAdvertising("test", testServiceUuids).subscribe(testSubscriber)
        testSubscriber.assertError(BluetoothNotAvailableException::class.java)
    }

    @Test
    fun test_startAdvertising_no_ble() {
        `when`(mMockBluetoothAdapter.bluetoothLeAdvertiser).thenReturn(null)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        var testSubscriber = TestSubscriber<Unit>()
        val testServiceUuids = arrayListOf<UUID>(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"))
        rxBleServer.startAdvertising("test", testServiceUuids).subscribe(testSubscriber)
        testSubscriber.assertError(LeNotAvailableBleException::class.java)
    }

    @Test
    fun test_startAdvertising_failures() {
        test_startAdvertising_failure(AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE)
        test_startAdvertising_failure(AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS)
        test_startAdvertising_failure(AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED)
        test_startAdvertising_failure(AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR)
        test_startAdvertising_failure(AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED)
        test_startAdvertising_failure(-1)
    }

    @Test
    fun test_startAdvertising_success() {
        clearInvocations(mMockBluetoothLeAdvertiser)
        clearInvocations(mMockAdvertiseDataBuilder)
        `when`(mMockBluetoothAdapter.bluetoothLeAdvertiser).thenReturn(mMockBluetoothLeAdvertiser)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        val testSubscriber = TestSubscriber<Unit>()
        val testServiceUuids = arrayListOf<UUID>(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"))
        rxBleServer.startAdvertising("test", testServiceUuids).subscribe(testSubscriber)
        val advertisementCallbackCaptor = ArgumentCaptor.forClass<AdvertiseCallback, AdvertiseCallback>(AdvertiseCallback::class.java)
        verify(mMockBluetoothLeAdvertiser).startAdvertising(
                eq(mMockAdvertiseSettings),
                eq(mMockAdvertiseData),
                advertisementCallbackCaptor.capture())
        verify(mMockAdvertiseDataBuilder, Times(1))
                .addServiceUuid(ArgumentMatchers.any(ParcelUuid::class.java))
        advertisementCallbackCaptor.value.onStartSuccess(mMockAdvertiseSettings)
        testSubscriber.assertValue(Unit)
    }

    @Test
    fun test_startGattServer_emptyServices() {
        val testSubscriber = TestSubscriber<RxBleServerConnection>()
        rxBleServer.startGattServer(arrayListOf()).subscribe(testSubscriber)
        testSubscriber.assertError(EmptyServicesBleException::class.java)
    }

    @Test
    fun test_startGattServer_error_to_create() {
        `when`(mMockBluetoothManager.openGattServer(eq(mMockContext),
                ArgumentMatchers.any(BluetoothGattServerCallback::class.java)))
                .thenReturn(null)
        val testServiceInfo = ServiceInfo(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"), arrayListOf())
        val testSubscriber = TestSubscriber<RxBleServerConnection>()
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(testSubscriber)
        testSubscriber.assertError(GattServerBleException::class.java)
    }

    @Test
    fun test_startGattServer_ok() {
        `when`(mMockBluetoothManager.openGattServer(eq(mMockContext),
                ArgumentMatchers.any(BluetoothGattServerCallback::class.java)))
                .thenReturn(mMockBluetoothGattServer)
        val testServiceInfo = ServiceInfo(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"), arrayListOf())
        val testSubject = PublishSubject.create<RxBleServerConnection>()
        val testSubscriber = TestSubscriber<RxBleServerConnection>()
        testSubject.subscribe(testSubscriber)
        testSubject.subscribe({ t -> t.closeServer().subscribe() }, {})
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(testSubject)
        testSubscriber.assertValue(RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext))
        val secondTestSubscriber = TestSubscriber<RxBleServerConnection>()
        testSubject.subscribe(secondTestSubscriber)
        // start after previous close
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(testSubject)
        secondTestSubscriber.assertValue(RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext))
    }

    @Test
    fun test_startGattServer_already_started() {
        `when`(mMockBluetoothManager.openGattServer(eq(mMockContext),
                ArgumentMatchers.any(BluetoothGattServerCallback::class.java)))
                .thenReturn(mMockBluetoothGattServer)
        val testServiceInfo = ServiceInfo(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"), arrayListOf())
        val testSubject = BehaviorSubject.create<RxBleServerConnection>()
        val testSubscriber = TestSubscriber<RxBleServerConnection>()
        testSubject.subscribe(testSubscriber)
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(testSubject)
        testSubscriber.assertValue(RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext))
        val secondTestSubscriber = TestSubscriber<RxBleServerConnection>()
        rxBleServer.startGattServer(arrayListOf()).subscribe(secondTestSubscriber)
        secondTestSubscriber.assertError(GattServerAlreadyStartedBleException::class.java)
        testSubject.value.closeServer().subscribe()
    }

    @Test
    fun test_stopAdvertising_no_bluetooth() {
        `when`(mMockBluetoothManager.adapter).thenReturn(null)
        val testSubscriber = TestSubscriber<Unit>()
        rxBleServer.stopAdvertising().subscribe(testSubscriber)
        testSubscriber.assertValue(Unit)
    }

    @Test
    fun test_stopAdvertising_no_le() {
        `when`(mMockBluetoothAdapter.bluetoothLeAdvertiser).thenReturn(null)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        val testSubscriber = TestSubscriber<Unit>()
        rxBleServer.stopAdvertising().subscribe(testSubscriber)
        testSubscriber.assertValue(Unit)
    }

    @Test
    fun test_stopAdvertising_ok() {
        clearInvocations(mMockBluetoothLeAdvertiser)
        `when`(mMockBluetoothAdapter.bluetoothLeAdvertiser).thenReturn(mMockBluetoothLeAdvertiser)
        `when`(mMockBluetoothManager.adapter).thenReturn(mMockBluetoothAdapter)
        val testSubscriber = TestSubscriber<Unit>()
        rxBleServer.stopAdvertising().subscribe(testSubscriber)
        testSubscriber.assertValue(Unit)
        verify(mMockBluetoothLeAdvertiser).stopAdvertising(ArgumentMatchers.any(AdvertiseCallback::class.java))
    }

    @Test
    fun test_establishedConnection_empty() {
        val testSubscriber = TestSubscriber<RxBleServerConnection>()
        rxBleServer.establishedConnection().subscribe(testSubscriber)
        testSubscriber.assertNoValues()
    }

    @Test
    fun test_establishedConnection_closed() {
        `when`(mMockBluetoothManager.openGattServer(eq(mMockContext),
                ArgumentMatchers.any(BluetoothGattServerCallback::class.java)))
                .thenReturn(mMockBluetoothGattServer)
        val testServiceInfo = ServiceInfo(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"), arrayListOf())
        val testSubject = PublishSubject.create<RxBleServerConnection>()
        var testSubscriber = TestSubscriber<RxBleServerConnection>()
        testSubject.subscribe(testSubscriber)
        testSubject.subscribe({ t -> t.closeServer().subscribe() }, {})
        val expectedConnection = RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext)
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(testSubject)
        testSubscriber.assertValue(expectedConnection)
        testSubscriber = TestSubscriber()
        rxBleServer.establishedConnection().subscribe(testSubscriber)
        testSubscriber.assertNoValues()
    }

    @Test
    fun test_establishedConnection_ok() {
        `when`(mMockBluetoothManager.openGattServer(eq(mMockContext),
                ArgumentMatchers.any(BluetoothGattServerCallback::class.java)))
                .thenReturn(mMockBluetoothGattServer)
        val testServiceInfo = ServiceInfo(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"), arrayListOf())
        var testSubscriber = TestSubscriber<RxBleServerConnection>()
        val expectedConnection = RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext)
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(testSubscriber)
        testSubscriber.assertValue(expectedConnection)
        testSubscriber = TestSubscriber()
        rxBleServer.establishedConnection().subscribe(testSubscriber)
        testSubscriber.assertValue(expectedConnection)
    }

    @Test
    fun test_establishedConnection_changed() {
        `when`(mMockBluetoothManager.openGattServer(eq(mMockContext),
                ArgumentMatchers.any(BluetoothGattServerCallback::class.java)))
                .thenReturn(mMockBluetoothGattServer)
        val testServiceInfo = ServiceInfo(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"), arrayListOf())
        var testSubscriber = TestSubscriber<RxBleServerConnection>()
        val expectedConnection = RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext)
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(testSubscriber)
        testSubscriber.assertValue(expectedConnection)
        testSubscriber = TestSubscriber()
        rxBleServer.establishedConnection().subscribe(testSubscriber)
        testSubscriber.assertValue(expectedConnection)
        rxBleServer.stopGattServer().subscribe()
        val newSubscriber = TestSubscriber<RxBleServerConnection>()
        rxBleServer.establishedConnection().subscribe(newSubscriber)
        newSubscriber.assertNoValues()
        val connectSubscriber = TestSubscriber<RxBleServerConnection>()
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(connectSubscriber)
        connectSubscriber.assertValue(expectedConnection)
        testSubscriber.assertValues(expectedConnection, expectedConnection)
    }


    @Test
    fun test_stopGattServer() {
        clearInvocations(mMockBluetoothGattServer)
        `when`(mMockBluetoothManager.openGattServer(eq(mMockContext),
                ArgumentMatchers.any(BluetoothGattServerCallback::class.java)))
                .thenReturn(mMockBluetoothGattServer)
        var testSubscriber = TestSubscriber<Unit>()
        rxBleServer.stopGattServer().subscribe(testSubscriber)
        testSubscriber.assertValue(Unit)
        val testServiceInfo = ServiceInfo(UUID.fromString("21e0379d-e4b2-bf80-b35e-ab2f13bd1015"), arrayListOf())
        val connectionSubscriber = TestSubscriber<RxBleServerConnection>()
        val connectionSubject = PublishSubject.create<RxBleServerConnection>()
        var actualConnection: RxBleServerConnection? = null
        connectionSubject.subscribe(connectionSubscriber)
        connectionSubject.subscribe({ actualConnection = it })
        rxBleServer.startGattServer(arrayListOf(testServiceInfo)).subscribe(connectionSubject)
        connectionSubscriber.assertValue(RxBleServerConnectionImpl(mMockBluetoothManager, mMockContext))
        testSubscriber = TestSubscriber()
        rxBleServer.stopGattServer().subscribe(testSubscriber)
        testSubscriber.assertValue(Unit)
        verify(mMockBluetoothGattServer).close()
        val isClosedSubscriber = TestSubscriber<Boolean>()
        actualConnection?.isClosed()?.subscribe(isClosedSubscriber)
        isClosedSubscriber.assertValue(true)
    }
}
