package com.cyberlens.app.data.remote.dto

import com.google.gson.annotations.SerializedName

// ─── IPGeolocation.io ─────────────────────────────────────────────────────────
data class IpGeoDto(
    val ip: String?,
    val city: String?,
    @SerializedName("state_prov") val stateProv: String?,
    @SerializedName("country_name") val countryName: String?,
    val latitude: String?,
    val longitude: String?,
    val isp: String?,
    val organization: String?,
    val asn: String?,
    @SerializedName("time_zone") val timeZone: IpGeoTimezoneDto?,
    @SerializedName("district") val district: String?,
    val zipcode: String?
)

data class IpGeoTimezoneDto(
    val name: String?,
    val offset: Int?,
    @SerializedName("current_time") val currentTime: String?
)

// ─── Shodan InternetDB (free, no key) ─────────────────────────────────────────
data class ShodanInternetDbDto(
    val ip: String?,
    val ports: List<Int>?,
    val tags: List<String>?,
    val cpes: List<String>?,
    val vulns: List<String>?,
    val hostnames: List<String>?
)

// ─── Shodan full API (api.shodan.io) ─────────────────────────────────────────
data class ShodanHostDto(
    @SerializedName("ip_str") val ipStr: String?,
    val ports: List<Int>?,
    val tags: List<String>?,
    val vulns: Map<String, ShodanVulnDetailDto>?,
    val hostnames: List<String>?,
    val org: String?,
    val isp: String?,
    val asn: String?,
    val os: String?,
    val city: String?,
    @SerializedName("country_name") val countryName: String?,
    @SerializedName("region_code") val regionCode: String?,
    val latitude: Double?,
    val longitude: Double?,
    val data: List<ShodanServiceDataDto>?
)

data class ShodanVulnDetailDto(
    val cvss: Double?,
    val summary: String?,
    val references: List<String>?
)

data class ShodanServiceDataDto(
    val port: Int?,
    val transport: String?,
    val product: String?,
    val version: String?,
    val data: String?
)

// ─── WhoisXML API ─────────────────────────────────────────────────────────────
data class WhoisXmlResponseDto(
    @SerializedName("WhoisRecord") val whoisRecord: WhoisRecordDto?
)

data class WhoisRecordDto(
    val domainName: String?,
    val registrarName: String?,
    val registrantEmail: String?,
    val createdDate: String?,
    val updatedDate: String?,
    val expiresDate: String?,
    val registrantName: String?,
    val nameServers: WhoisNameServersDto?,
    val status: String?
)

data class WhoisNameServersDto(
    val hostNames: List<String>?
)

// ─── VirusTotal v2 ────────────────────────────────────────────────────────────
data class VtUrlReportDto(
    @SerializedName("response_code") val responseCode: Int?,
    val positives: Int?,
    val total: Int?,
    @SerializedName("scan_date") val scanDate: String?,
    val url: String?,
    val permalink: String?
)

data class VtIpReportDto(
    @SerializedName("response_code") val responseCode: Int?,
    val country: String?,
    @SerializedName("detected_urls") val detectedUrls: List<VtDetectedUrl>?,
    val resolutions: List<VtResolution>?,
    val asn: Long?,
    @SerializedName("as_owner") val asOwner: String?
)

data class VtDomainReportDto(
    @SerializedName("response_code") val responseCode: Int?,
    val categories: Map<String, String>?,
    @SerializedName("detected_urls") val detectedUrls: List<VtDetectedUrl>?,
    val resolutions: List<VtResolution>?,
    val subdomains: List<String>?
)

data class VtDetectedUrl(
    val url: String?,
    val positives: Int?,
    val total: Int?,
    @SerializedName("scan_date") val scanDate: String?
)

data class VtResolution(
    @SerializedName("ip_address") val ipAddress: String?,
    @SerializedName("last_resolved") val lastResolved: String?
)

// ─── CIRCL CVE Search ─────────────────────────────────────────────────────────
data class CveDto(
    val id: String?,
    val summary: String?,
    @SerializedName("cvss") val cvss: Double?,
    @SerializedName("Published") val published: String?,
    val references: List<String>?
)

// ─── SauceNAO ─────────────────────────────────────────────────────────────────
// (defined in SauceNaoDto.kt)
