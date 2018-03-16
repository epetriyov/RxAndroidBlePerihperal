package com.petriyov.rxandroidble.internal

import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.pm.PackageManager
import android.os.ParcelUuid
import com.petriyov.rxandroidble.*
import rx.Emitter
import rx.Observable
import rx.subjects.BehaviorSubject
import java.util.*
import javax.inject.Inject


/**
 * Created by Eugene on 1/26/2018.
 */
internal class RxBleServerImpl @Inject constructor(private val context: Context,
                                                   private val advertiseModule: AdvertiseModule) : RxBleServer {

    private val mBluetoothManager: BluetoothManager = context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

    private var rxBleServerConnection: RxBleServerConnection? = null

    private var advertiseEmitter: Emitter<in Unit>? = null

    private val connectionChangedSubject = BehaviorSubject.create<Unit>()

    private val mAdvertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            advertiseEmitter?.onNext(Unit)
        }

        override fun onStartFailure(errorCode: Int) {
            val errorMessage = when (errorCode) {
                ADVERTISE_FAILED_DATA_TOO_LARGE -> "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes."
                ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Failed to start advertising because no advertising instance is available."
                ADVERTISE_FAILED_ALREADY_STARTED -> "Failed to start advertising as the advertising is already started."
                ADVERTISE_FAILED_INTERNAL_ERROR -> "Operation failed due to an internal error."
                ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "This feature is not supported on this platform."
                else -> "Unknown advertise start error"
            }
            advertiseEmitter?.onError(AdvertisementBleException(errorMessage, errorCode))
            advertiseEmitter = null
        }
    }

    private fun getLeAdvertiser(): BluetoothLeAdvertiser {
        val bluetoothAdapter = mBluetoothManager.adapter
                ?: throw BluetoothNotAvailableException("Bluetooth not available")
        return bluetoothAdapter.bluetoothLeAdvertiser
                ?: throw LeNotAvailableBleException("Failed to create advertiser")
    }

    override fun establishedConnection(): Observable<RxBleServerConnection> =
            connectionChangedSubject
                    .filter({ rxBleServerConnection != null })
                    .flatMap { rxBleServerConnection!!.isClosed() }
                    .filter({ !it })
                    .map { rxBleServerConnection }

    override fun startGattServer(services: List<ServiceInfo>): Observable<RxBleServerConnection> {
        return Observable.create<RxBleServerConnection>({ t ->
            var isClosed = false
            if (rxBleServerConnection != null) {
                rxBleServerConnection!!.isClosed().subscribe({
                    if (!it) {
                        t.onError(GattServerAlreadyStartedBleException("Gatt server already started"))
                        isClosed = true
                    }
                })
            }
            if (isClosed) {
                return@create
            }
            if (services.isEmpty()) {
                throw EmptyServicesBleException("No services to create GATT server")
            }
            rxBleServerConnection = RxBleServerConnectionImpl(mBluetoothManager, context)
            rxBleServerConnection!!.addServices(services).subscribe()
            t.onNext(rxBleServerConnection)
            connectionChangedSubject.onNext(Unit)
        }, Emitter.BackpressureMode.NONE)
    }

    override fun startAdvertising(deviceName: String, serviceUuids: List<UUID>): Observable<Unit> {
        return Observable.create({ t ->
            if (serviceUuids.isEmpty()) {
                throw EmptyServicesBleException("No services for advertisement")
            }
            val bluetoothLeAdvertiser = getLeAdvertiser()
            mBluetoothManager.adapter.name = deviceName
            val advertiseDataBuilder = advertiseModule.provideAdvertiseDataBuilder()
            for (serviceUuid in serviceUuids) {
                advertiseDataBuilder.addServiceUuid(ParcelUuid(serviceUuid))
            }

            advertiseEmitter = t
            bluetoothLeAdvertiser.startAdvertising(advertiseModule.provideAdvertiseSetting(), advertiseDataBuilder.build(), mAdvertiseCallback)
            t.setCancellation { advertiseEmitter = null }
        }, Emitter.BackpressureMode.NONE)

    }

    override fun stopAdvertising(): Observable<Unit> {
        return Observable.fromCallable {
            try {
                val bluetoothLeAdvertiser = getLeAdvertiser()
                bluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback)
            } catch (exception: BleServerException) {
                // do nothing
            }
            advertiseEmitter = null
        }
    }

    override fun checkBluetoothSupportLevel(): Observable<BluetoothSupportLevel> {
        return Observable.just(
                if (mBluetoothManager.adapter == null) {
                    BluetoothSupportLevel.NO_BLUETOOTH
                } else if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                    BluetoothSupportLevel.NO_BLE
                } else if (!mBluetoothManager.adapter.isMultipleAdvertisementSupported) {
                    BluetoothSupportLevel.NO_BLE_PERIPHERAL
                } else if (!mBluetoothManager.adapter.isEnabled) {
                    BluetoothSupportLevel.DISABLED
                } else {
                    BluetoothSupportLevel.OK
                })
    }

    override fun stopGattServer(): Observable<Unit> {
        return Observable.fromCallable {
            rxBleServerConnection?.closeServer()?.subscribe()
            rxBleServerConnection = null
            Unit
        }
    }
}
