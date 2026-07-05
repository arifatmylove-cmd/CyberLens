package com.cyberlens.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberlens.app.data.repository.OsintRepository
import com.cyberlens.app.domain.model.IpInfo
import com.cyberlens.app.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class IpIntelViewModel @Inject constructor(
    private val repository: OsintRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<IpInfo>>(UiState.Idle)
    val uiState: StateFlow<UiState<IpInfo>> = _uiState

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _consentGiven = MutableStateFlow(false)
    val consentGiven: StateFlow<Boolean> = _consentGiven

    fun onInputChange(value: String) { _inputText.value = value }
    fun onConsentChange(value: Boolean) { _consentGiven.value = value }

    fun analyze() {
        if (!_consentGiven.value) return
        val ip = _inputText.value.trim()
        if (ip.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = repository.analyzeIp(ip).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Analysis failed") }
            )
        }
    }

    fun reset() { _uiState.value = UiState.Idle }
}
