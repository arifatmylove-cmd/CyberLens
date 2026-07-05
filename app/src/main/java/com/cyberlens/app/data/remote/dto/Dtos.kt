package com.cyberlens.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class IpInfoDto(
    val ip: String?,
    val city: String?,
    val region: String?,
    val country: String?,
    val org: String?,
    val timezone: String?,
    val hostname: String?,
    val loc: String?
)

data class IpApiDto(
    val ip: String?,
    val city: String?,
    val region: String?,
    @SerializedName("country_name") val countryName: String?,
    val org: String?,
    val asn: String?,
    val timezone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val isp: String?
)

data class ShodanInternetDbDto(
    val ip: String?,
    val ports: List<Int>?,
    val tags: List<String>?,
    val cpes: List<String>?,
    val vulns: List<String>?,
    val hostnames: List<String>?
)

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
    val resolutions: List<VtResolution>?
)

data class VtDomainReportDto(
    @SerializedName("response_code") val responseCode: Int?,
    val categories: Map<String, String>?,
    @SerializedName("detected_urls") val detectedUrls: List<VtDetectedUrl>?,
    val resolutions: List<VtResolution>?
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

data class CveDto(
    val id: String?,
    val summary: String?,
    @SerializedName("cvss") val cvss: Double?,
    @SerializedName("Published") val published: String?,
    val references: List<String>?
)

data class UsernameCheckDto(
    val platform: String?,
    val url: String?,
    val exists: Boolean?,
    val confidence: Int?
)
