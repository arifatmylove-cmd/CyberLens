package com.cyberlens.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberlens.app.data.repository.OsintRepository
import com.cyberlens.app.domain.model.ThreatInfo
import com.cyberlens.app.domain.model.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ThreatViewModel @Inject constructor(
    private val repository: OsintRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ThreatInfo>>(UiState.Idle)
    val uiState: StateFlow<UiState<ThreatInfo>> = _uiState

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText

    private val _consentGiven = MutableStateFlow(false)
    val consentGiven: StateFlow<Boolean> = _consentGiven

    fun onInputChange(value: String) { _inputText.value = value }
    fun onConsentChange(value: Boolean) { _consentGiven.value = value }

    fun check() {
        if (!_consentGiven.value) return
        val target = _inputText.value.trim()
        if (target.isBlank()) return
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _uiState.value = repository.checkThreat(target).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Threat check failed") }
            )
        }
    }

    fun reset() { _uiState.value = UiState.Idle }
}
