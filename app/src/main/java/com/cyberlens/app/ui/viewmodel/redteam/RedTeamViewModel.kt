package com.cyberlens.app.ui.viewmodel.redteam

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cyberlens.app.data.repository.OsintRepository
import com.cyberlens.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RedTeamViewModel @Inject constructor(
    private val repository: OsintRepository
) : ViewModel() {

    // Nmap
    private val _nmapState = MutableStateFlow<UiState<NmapScanResult>>(UiState.Idle)
    val nmapState: StateFlow<UiState<NmapScanResult>> = _nmapState

    // Port Scan
    private val _portState = MutableStateFlow<UiState<NmapScanResult>>(UiState.Idle)
    val portState: StateFlow<UiState<NmapScanResult>> = _portState

    // Banner Grab
    private val _bannerState = MutableStateFlow<UiState<PortResult>>(UiState.Idle)
    val bannerState: StateFlow<UiState<PortResult>> = _bannerState

    // WAF Detect
    private val _wafState = MutableStateFlow<UiState<WafDetectResult>>(UiState.Idle)
    val wafState: StateFlow<UiState<WafDetectResult>> = _wafState

    // CVE Lookup
    private val _cveState = MutableStateFlow<UiState<List<CveInfo>>>(UiState.Idle)
    val cveState: StateFlow<UiState<List<CveInfo>>> = _cveState

    private val _consentGiven = MutableStateFlow(false)
    val consentGiven: StateFlow<Boolean> = _consentGiven
    fun onConsentChange(v: Boolean) { _consentGiven.value = v }

    fun nmapScan(target: String, scanType: String = "Quick") {
        if (!_consentGiven.value) return
        viewModelScope.launch {
            _nmapState.value = UiState.Loading
            _nmapState.value = repository.nmapScan(target, scanType).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Nmap failed") }
            )
        }
    }

    fun portScan(target: String, portsInput: String) {
        if (!_consentGiven.value) return
        val ports = parsePorts(portsInput)
        if (ports.isEmpty()) return
        viewModelScope.launch {
            _portState.value = UiState.Loading
            _portState.value = repository.portScan(target, ports).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Port scan failed") }
            )
        }
    }

    fun bannerGrab(target: String, port: Int) {
        if (!_consentGiven.value) return
        viewModelScope.launch {
            _bannerState.value = UiState.Loading
            _bannerState.value = repository.bannerGrab(target, port).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Banner grab failed") }
            )
        }
    }

    fun detectWaf(target: String) {
        if (!_consentGiven.value) return
        viewModelScope.launch {
            _wafState.value = UiState.Loading
            _wafState.value = repository.detectWaf(target).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "WAF detect failed") }
            )
        }
    }

    fun lookupCve(vendor: String, product: String) {
        viewModelScope.launch {
            _cveState.value = UiState.Loading
            _cveState.value = repository.searchCve(vendor, product).fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "CVE lookup failed") }
            )
        }
    }

    private fun parsePorts(input: String): List<Int> {
        val ports = mutableListOf<Int>()
        input.split(",").forEach { token ->
            val trimmed = token.trim()
            if (trimmed.contains("-")) {
                val (start, end) = trimmed.split("-")
                val s = start.trim().toIntOrNull() ?: return@forEach
                val e = end.trim().toIntOrNull() ?: return@forEach
                ports.addAll((s..minOf(e, s + 1000)).toList())
            } else {
                trimmed.toIntOrNull()?.let { ports.add(it) }
            }
        }
        return ports.filter { it in 1..65535 }.take(200)
    }
}
