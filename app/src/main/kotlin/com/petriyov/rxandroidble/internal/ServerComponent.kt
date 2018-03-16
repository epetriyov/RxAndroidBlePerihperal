package com.petriyov.rxandroidble.internal

import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Context
import com.petriyov.rxandroidble.RxBleServer
import dagger.Binds
import dagger.Component
import dagger.Module
import dagger.Provides

/**
 * Created by Eugene on 1/29/2018.
 */
@Component(modules = [(ServerComponent.ServerModule::class), (ServerComponent.ServerModuleBinder::class)])
internal interface ServerComponent {
    @Module
    class ServerModule(private val context: Context) {

        @Provides
        fun provideContext(): Context {
            return context
        }

        @Provides
        fun provideAdvertiseModule(): AdvertiseModule {
            return AdvertiseModule()
        }
    }

    @Module
    abstract class ServerModuleBinder {
        @Binds
        abstract fun bindRxBleServer(rxBleServer: RxBleServerImpl): RxBleServer
    }

    fun rxBleServer(): RxBleServer
}

open class AdvertiseModule {
    fun provideAdvertiseSetting(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()
    }

    fun provideAdvertiseDataBuilder(): AdvertiseData.Builder {
        return AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
    }
}
