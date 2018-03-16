package com.petriyov.rxandroidble

import android.content.Context
import com.petriyov.rxandroidble.internal.DaggerServerComponent
import com.petriyov.rxandroidble.internal.ServerComponent
import rx.Observable
import java.util.*

interface RxBleServer {

    /**
     * @exception GattServerAlreadyStartedBleException
     * @exception EmptyServicesBleException
     * @exception GattServerBleException
     */
    fun startGattServer(services: List<ServiceInfo>): Observable<RxBleServerConnection>

    /**
     * @exception EmptyServicesBleException
     * @exception AdvertisementBleException
     * @exception BluetoothNotAvailableException
     * @exception LeNotAvailableBleException
     */
    fun startAdvertising(deviceName: String, serviceUuids: List<UUID>): Observable<Unit>

    fun stopAdvertising(): Observable<Unit>

    fun stopGattServer(): Observable<Unit>

    fun checkBluetoothSupportLevel(): Observable<BluetoothSupportLevel>

    fun establishedConnection(): Observable<RxBleServerConnection>

    companion object {
        fun getInstance(context: Context): RxBleServer {
            return DaggerServerComponent
                    .builder()
                    .serverModule(ServerComponent.ServerModule(context))
                    .build()
                    .rxBleServer()
        }
    }
}
