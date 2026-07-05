package com.cyberlens.app.domain.model

import com.google.gson.annotations.SerializedName

enum class RiskLevel { SAFE, SUSPICIOUS, DANGEROUS, UNKNOWN }
enum class ScanType {
    IP_INTEL, DOMAIN_ANALYSIS, USERNAME_OSINT,
    IMAGE_REVERSE, WEB_SCANNER, THREAT_INTEL,
    NMAP, PORT_SCAN, BANNER_GRAB, WAF_DETECT
}

data class IpInfo(
    val ip: String,
    val city: String?,
    val region: String?,
    val country: String?,
    val org: String?,
    val asn: String?,
    val timezone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val hostname: String?,
    val isp: String?,
    val riskLevel: RiskLevel = RiskLevel.UNKNOWN,
    val openPorts: List<Int> = emptyList(),
    val tags: List<String> = emptyList(),
    val cpes: List<String> = emptyList(),
    val vulns: List<String> = emptyList()
)

data class DnsRecord(val type: String, val value: String)

data class DomainInfo(
    val domain: String,
    val ipAddress: String?,
    val reverseDns: String?,
    val dnsRecords: List<DnsRecord> = emptyList(),
    val httpHeaders: Map<String, String> = emptyMap(),
    val sslValid: Boolean?,
    val sslExpiry: String?,
    val whoisData: String?,
    val securityScore: Int = 0,
    val hasHttps: Boolean = false,
    val hasHsts: Boolean = false,
    val hasCsp: Boolean = false,
    val hasXFrameOptions: Boolean = false,
    val serverInfo: String?,
    val technologies: List<String> = emptyList()
)

data class UsernameResult(
    val platform: String,
    val url: String,
    val found: Boolean,
    val confidence: Int
)

data class UsernameOsintResult(
    val username: String,
    val results: List<UsernameResult> = emptyList(),
    val foundCount: Int = 0,
    val totalChecked: Int = 0
)

data class ThreatInfo(
    val target: String,
    val isIp: Boolean,
    val malicious: Int = 0,
    val suspicious: Int = 0,
    val harmless: Int = 0,
    val undetected: Int = 0,
    val riskLevel: RiskLevel = RiskLevel.UNKNOWN,
    val categories: Map<String, String> = emptyMap(),
    val lastAnalysisDate: String? = null,
    val country: String? = null,
    val tags: List<String> = emptyList()
)

data class PortResult(val port: Int, val open: Boolean, val service: String?, val banner: String?)

data class NmapScanResult(
    val target: String,
    val scanType: String,
    val ports: List<PortResult> = emptyList(),
    val osGuess: String?,
    val rawOutput: String,
    val scanDuration: Long
)

data class WafDetectResult(
    val target: String,
    val wafDetected: Boolean,
    val wafName: String?,
    val confidence: Int,
    val fingerprints: List<String> = emptyList()
)

data class CveInfo(
    val id: String,
    val summary: String,
    val cvss: Double?,
    val publishedDate: String?,
    val references: List<String> = emptyList()
)

sealed class UiState<out T> {
    object Idle : UiState<Nothing>()
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
