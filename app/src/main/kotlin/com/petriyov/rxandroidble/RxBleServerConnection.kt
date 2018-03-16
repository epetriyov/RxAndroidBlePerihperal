package com.petriyov.rxandroidble

import rx.Observable
import java.util.*

/**
 * Created by Eugene on 1/26/2018.
 */
interface RxBleServerConnection {

    fun connectionStateChanges(): Observable<Pair<DeviceInfo, BleState>>

    fun characteristicReadRequests(): Observable<ReadRequest>

    fun characteristicWriteRequests(): Observable<WriteRequest>

    fun descriptorReadRequests(): Observable<ReadRequest>

    fun descriptorWriteRequests(): Observable<WriteRequest>

    fun closeServer(): Observable<Unit>

    /**
     *  @exception NotConnectedBleException
     */
    fun addServices(services: List<ServiceInfo>): Observable<Unit>

    fun isClosed(): Observable<Boolean>

    /**
     * @exception NotConnectedBleException
     * @exception WrongAddressBleException
     * @return true, if the response has been sent successfully
     */
    fun sendResponse(deviceAddress: String, requestId: Int, status: ResponseStatus, offset: Int, value: ByteArray?):
            Observable<Boolean>

    /**
     * @exception NotConnectedBleException
     * @return true, if the notification has been triggered successfully
     */
    fun notifyCharacteristicChanged(serviceUUID: UUID, characterisiticUUID: UUID, value: ByteArray, confirm: Boolean):
            Observable<Boolean>

    /**
     * @exception NotConnectedBleException
     * @return true, if the response has been sent successfully
     */
    fun sendDescriptorReadResponse(readRequest: ReadRequest, status: ResponseStatus): Observable<Boolean>

    /**
     * @exception NotConnectedBleException
     * @return true, if the response has been sent successfully
     */
    fun sendDescriptorWriteResponse(writeRequest: WriteRequest, status: ResponseStatus): Observable<Boolean>

    /**
     * @exception WrongAddressBleException
     */
    fun cancelConnection(deviceAddress: String): Observable<Unit>
}
