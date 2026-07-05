package com.cyberlens.app.data.remote

import com.cyberlens.app.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

// IpInfo.io — free tier, no key needed for basic fields
interface IpInfoApiService {
    @GET("{ip}/json")
    suspend fun getIpInfo(@Path("ip") ip: String): Response<IpInfoDto>
}

// ipapi.co — free tier
interface IpApiService {
    @GET("{ip}/json/")
    suspend fun getIpDetails(@Path("ip") ip: String): Response<IpApiDto>
}

// Shodan InternetDB — completely free, no key
interface ShodanInternetDbService {
    @GET("{ip}")
    suspend fun getInternetDbInfo(@Path("ip") ip: String): Response<ShodanInternetDbDto>
}

// HackerTarget — free recon APIs
interface HackerTargetApiService {
    @GET("dnslookup/")
    suspend fun dnsLookup(@Query("q") domain: String): Response<String>

    @GET("reverseiplookup/")
    suspend fun reverseIp(@Query("q") ip: String): Response<String>

    @GET("whois/")
    suspend fun whois(@Query("q") domain: String): Response<String>

    @GET("httpheaders/")
    suspend fun httpHeaders(@Query("q") target: String): Response<String>

    @GET("nmap/")
    suspend fun nmapScan(@Query("q") target: String): Response<String>

    @GET("hostsearch/")
    suspend fun hostSearch(@Query("q") domain: String): Response<String>

    @GET("pagelinks/")
    suspend fun pageLinks(@Query("q") url: String): Response<String>

    @GET("geoip/")
    suspend fun geoIp(@Query("q") ip: String): Response<String>
}

// CIRCL CVE Search — free, no key
interface CveSearchApiService {
    @GET("search/{vendor}/{product}")
    suspend fun searchCve(
        @Path("vendor") vendor: String,
        @Path("product") product: String
    ): Response<List<CveDto>>

    @GET("cve/{cveId}")
    suspend fun getCve(@Path("cveId") cveId: String): Response<CveDto>
}

// VirusTotal — free key (80 req/day)
interface VirusTotalApiService {
    @GET("url/report")
    suspend fun getUrlReport(
        @Query("apikey") apiKey: String,
        @Query("resource") resource: String
    ): Response<VtUrlReportDto>

    @GET("ip-address/report")
    suspend fun getIpReport(
        @Query("apikey") apiKey: String,
        @Query("resource") resource: String
    ): Response<VtIpReportDto>

    @GET("domain/report")
    suspend fun getDomainReport(
        @Query("apikey") apiKey: String,
        @Query("domain") domain: String
    ): Response<VtDomainReportDto>
}

// Sherlock-style username search via free APIs
interface UsernameCheckApiService {
    @GET("check")
    suspend fun checkUsername(@Query("username") username: String): Response<List<UsernameCheckDto>>
}
