package com.cyberlens.app.data.remote

import com.cyberlens.app.data.remote.dto.*
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.*

// ─── IPGeolocation.io — free 30k/month ───────────────────────────────────────
interface IpGeoApiService {
    @GET("ipgeo")
    suspend fun getIpGeo(
        @Query("apiKey") apiKey: String,
        @Query("ip") ip: String
    ): Response<IpGeoDto>
}

// ─── Shodan InternetDB — completely free, no key ──────────────────────────────
interface ShodanInternetDbService {
    @GET("{ip}")
    suspend fun getInternetDbInfo(@Path("ip") ip: String): Response<ShodanInternetDbDto>
}

// ─── Shodan full API — api.shodan.io ─────────────────────────────────────────
interface ShodanApiService {
    @GET("shodan/host/{ip}")
    suspend fun getHost(
        @Path("ip") ip: String,
        @Query("key") apiKey: String
    ): Response<ShodanHostDto>
}

// ─── HackerTarget — free recon APIs ──────────────────────────────────────────
interface HackerTargetApiService {
    @GET("dnslookup/")
    suspend fun dnsLookup(@Query("q") domain: String): Response<String>

    @GET("nmap/")
    suspend fun nmapScan(@Query("q") target: String): Response<String>

    @GET("hostsearch/")
    suspend fun hostSearch(@Query("q") domain: String): Response<String>

    @GET("geoip/")
    suspend fun geoIp(@Query("q") ip: String): Response<String>

    @GET("reverseiplookup/")
    suspend fun reverseIp(@Query("q") ip: String): Response<String>
}

// ─── WhoisXML API — 500 free credits/month ───────────────────────────────────
interface WhoisXmlApiService {
    @GET("whoisserver/WhoisService")
    suspend fun getWhois(
        @Query("apiKey") apiKey: String,
        @Query("domainName") domain: String,
        @Query("outputFormat") format: String = "JSON"
    ): Response<WhoisXmlResponseDto>
}

// ─── CIRCL CVE Search — free, no key ─────────────────────────────────────────
interface CveSearchApiService {
    @GET("search/{vendor}/{product}")
    suspend fun searchCve(
        @Path("vendor") vendor: String,
        @Path("product") product: String
    ): Response<List<CveDto>>

    @GET("cve/{cveId}")
    suspend fun getCve(@Path("cveId") cveId: String): Response<CveDto>
}

// ─── VirusTotal v2 — free key ─────────────────────────────────────────────────
interface VirusTotalApiService {
    @GET("url/report")
    suspend fun getUrlReport(
        @Query("apikey") apiKey: String,
        @Query("resource") resource: String
    ): Response<VtUrlReportDto>

    @GET("ip-address/report")
    suspend fun getIpReport(
        @Query("apikey") apiKey: String,
        @Query("ip") resource: String
    ): Response<VtIpReportDto>

    @GET("domain/report")
    suspend fun getDomainReport(
        @Query("apikey") apiKey: String,
        @Query("domain") domain: String
    ): Response<VtDomainReportDto>
}

// ─── SauceNAO — free 300/day ─────────────────────────────────────────────────
interface SauceNaoApiService {
    @GET("search.php")
    suspend fun searchByUrl(
        @Query("api_key") apiKey: String,
        @Query("url") imageUrl: String,
        @Query("output_type") outputType: Int = 2,
        @Query("numres") numResults: Int = 10,
        @Query("minsim") minSimilarity: Int = 50
    ): Response<com.cyberlens.app.data.remote.dto.SauceNaoResponseDto>

    @Multipart
    @POST("search.php")
    suspend fun searchByFile(
        @Query("api_key") apiKey: String,
        @Query("output_type") outputType: Int = 2,
        @Query("numres") numResults: Int = 10,
        @Part file: MultipartBody.Part
    ): Response<com.cyberlens.app.data.remote.dto.SauceNaoResponseDto>
}
