package com.fake.soundremote.ui.home

import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fake.soundremote.R
import com.fake.soundremote.data.KeystrokeRepository
import com.fake.soundremote.data.preferences.UserPreferencesRepository
import com.fake.soundremote.service.ServiceManager
import com.fake.soundremote.util.ConnectionStatus
import com.fake.soundremote.util.generateDescription
import com.google.common.net.InetAddresses
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUIState(
    val keystrokes: List<HomeKeystrokeUIState> = emptyList(),
    val serverAddress: String = "",
    val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
    val isMuted: Boolean = false,
)

data class HomeKeystrokeUIState(
    val id: Int,
    val name: String,
    val description: String,
)

@HiltViewModel
internal class HomeViewModel @Inject constructor(
    private val userPreferencesRepo: UserPreferencesRepository,
    private val keystrokeRepository: KeystrokeRepository,
    private val serviceManager: ServiceManager,
) : ViewModel() {

    private val _homeUIState = MutableStateFlow(HomeUIState())
    val homeUIState: StateFlow<HomeUIState>
        get() = _homeUIState
    var messageState by mutableStateOf<Int?>(null)
        private set

    init {
        viewModelScope.launch {
            combine(
                keystrokeRepository.getFavouredOrdered(true),
                userPreferencesRepo.serverAddressFlow,
                serviceManager.serviceState,
            ) { keystrokes, address, serviceState ->
                val keystrokeStates = keystrokes.map { keystroke ->
                    HomeKeystrokeUIState(
                        id = keystroke.id,
                        name = keystroke.name,
                        description = generateDescription(keystroke),
                    )
                }
                HomeUIState(
                    keystrokes = keystrokeStates,
                    serverAddress = address,
                    connectionStatus = serviceState.connectionStatus,
                    isMuted = serviceState.isMuted,
                )
            }.collect { _homeUIState.value = it }
        }
    }

    private fun setServerAddress(address: String) {
        viewModelScope.launch {
            userPreferencesRepo.setServerAddress(address)
        }
    }

    private fun setMessage(@StringRes messageId: Int) {
        messageState = messageId
    }

    fun messageShown() {
        messageState = null
    }

    fun connect(address: String) {
        val newAddress = address.trim()
        if (InetAddresses.isInetAddress(newAddress)) {
            setServerAddress(newAddress)
            serviceManager.connect(newAddress)
        } else {
            setMessage(R.string.message_invalid_address)
        }
    }

    fun disconnect() {
        serviceManager.disconnect()
    }

    fun sendKeystroke(keystrokeId: Int) {
        viewModelScope.launch {
            keystrokeRepository.getById(keystrokeId)?.let {
                serviceManager.sendKeystroke(it)
            }
        }
    }

    fun setMuted(value: Boolean) {
        serviceManager.setMuted(value)
    }
}
