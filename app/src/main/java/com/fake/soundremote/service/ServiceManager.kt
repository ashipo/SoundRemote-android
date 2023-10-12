package com.fake.soundremote.service

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.fake.soundremote.SoundRemoteApplication
import com.fake.soundremote.data.Keystroke
import com.fake.soundremote.util.ConnectionStatus
import com.fake.soundremote.util.SystemMessage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

data class ServiceState(
    val connectionStatus: ConnectionStatus,
    val isMuted: Boolean
)

@Singleton
internal class ServiceManager(
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    @Inject
    constructor(application: Application) : this(
        scope = (application as SoundRemoteApplication).applicationScope,
        dispatcher = Dispatchers.Default,
    )

    private lateinit var service: WeakReference<MainService>
    private var bound: Boolean = false
    private var stateCollect: Job? = null
    private var messageCollect: Job? = null
    private var _serviceState = MutableStateFlow(ServiceState(ConnectionStatus.DISCONNECTED, false))
    val serviceState: StateFlow<ServiceState>
        get() = _serviceState

    private val _systemMessages: Channel<SystemMessage> = Channel(5, BufferOverflow.DROP_OLDEST)
    val systemMessages: ReceiveChannel<SystemMessage>
        get() = _systemMessages

    fun bind(context: Context) {
        Intent(context, MainService::class.java).also { intent ->
            context.bindService(intent, serviceConnection, 0)
        }
    }

    fun unbind(context: Context) {
        stopCollect()
        context.unbindService(serviceConnection)
    }

    fun connect(address: String) {
        if (!bound) return
        service.get()?.connect(address)
    }

    fun disconnect() {
        if (!bound) return
        service.get()?.disconnect()
    }

    fun sendKeystroke(keystroke: Keystroke) {
        if (!bound) return
        service.get()?.sendKeystroke(keystroke)
    }

    fun setMuted(value: Boolean) {
        if (!bound) return
        service.get()?.setMuted(value)
    }

    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            val localBinder = binder as MainService.LocalBinder
            service = WeakReference(localBinder.getService())
            startCollect()
            bound = true
        }

        override fun onServiceDisconnected(name: ComponentName) {
            bound = false
            stopCollect()
        }
    }

    private fun startCollect() {
        service.get()?.let { service ->
            stateCollect = scope.launch(dispatcher) {
                combine(service.connectionStatus, service.isMuted) { connectionStatus, isMuted ->
                    ServiceState(connectionStatus, isMuted)
                }.collect { _serviceState.value = it }
            }
            messageCollect = scope.launch(dispatcher) {
                while (isActive) {
                    val message = service.systemMessages.receive()
                    _systemMessages.send(message)
                }
            }
        }
    }

    private fun stopCollect() {
        stateCollect?.cancel()
        stateCollect = null
        messageCollect?.cancel()
        messageCollect = null
    }
}