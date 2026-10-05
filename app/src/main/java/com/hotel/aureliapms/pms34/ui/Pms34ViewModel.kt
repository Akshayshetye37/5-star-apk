package com.hotel.aureliapms.pms34.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hotel.aureliapms.pms34.integration.Pms34IntegrationContract
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class Pms34ViewModel(
    private val integration: Pms34IntegrationContract
) : ViewModel() {

    private val _state = MutableStateFlow(Pms34State())
    val state: StateFlow<Pms34State> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { integration.dashboard() }
                .onSuccess {
                    _state.value = Pms34State(
                        loading = false,
                        dashboard = it
                    )
                }
                .onFailure {
                    _state.value = Pms34State(
                        loading = false,
                        error = it.message ?: "Unable to load PMS dashboard"
                    )
                }
        }
    }
}
