package com.cyberlens.app.data.repository

import com.cyberlens.app.data.local.AppDatabase
import com.cyberlens.app.data.local.ScanEntity
import com.cyberlens.app.data.remote.*
import com.cyberlens.app.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import javax.inject.Named

@Singleton
class OsintRepository @Inject constructor(
    private val ipInfoService: IpInfoApiService,
    private val ipApiService: IpApiService,
    private val shodanService: ShodanInternetDbService,
    private val hackerTargetService: HackerTargetApiService,
    private val cveService: CveSearchApiService,
    private val virusTotalService: VirusTotalApiService,
    private val database: AppDatabase,
    private val gson: Gson,
    @Named("vt_api_key") private val vtApiKey: String
) {
    // ─── IP Intelligence ────────────────────────────────────────────
    suspend fun analyzeIp(ip: String): Result<IpInfo> = runCatching {
        val ipApiResp = ipApiService.getIpDetails(ip)
        val shodanResp = runCatching { shodanService.getInternetDbInfo(ip).body() }.getOrNull()
        val ipApiBody = ipApiResp.body()

        val riskLevel = when {
            (shodanResp?.vulns?.isNotEmpty() == true) -> RiskLevel.DANGEROUS
            (shodanResp?.tags?.contains("tor") == true ||
             shodanResp?.tags?.contains("vpn") == true) -> RiskLevel.SUSPICIOUS
            else -> RiskLevel.UNKNOWN
        }

        val org = ipApiBody?.org ?: ""
        val asnPart = ipApiBody?.asn ?: ""
        val ipInfo = IpInfo(
            ip = ip,
            city = ipApiBody?.city,
            region = ipApiBody?.region,
            country = ipApiBody?.countryName,
            org = org,
            asn = asnPart,
            timezone = ipApiBody?.timezone,
            latitude = ipApiBody?.latitude,
            longitude = ipApiBody?.longitude,
            hostname = shodanResp?.hostnames?.firstOrNull(),
            isp = ipApiBody?.isp,
            riskLevel = riskLevel,
            openPorts = shodanResp?.ports ?: emptyList(),
            tags = shodanResp?.tags ?: emptyList(),
            cpes = shodanResp?.cpes ?: emptyList(),
            vulns = shodanResp?.vulns ?: emptyList()
        )
        saveToHistory(ScanType.IP_INTEL, ip, ipInfo, riskLevel.name)
        ipInfo
    }

    // ─── Domain Analysis ─────────────────────────────────────────────
    suspend fun analyzeDomain(domain: String): Result<DomainInfo> = runCatching {
        val cleanDomain = domain.removePrefix("https://").removePrefix("http://").split("/").first()
        val dnsRaw = runCatching { hackerTargetService.dnsLookup(cleanDomain).body() ?: "" }.getOrDefault("")
        val headersRaw = runCatching { hackerTargetService.httpHeaders(cleanDomain).body() ?: "" }.getOrDefault("")
        val whoisRaw = runCatching { hackerTargetService.whois(cleanDomain).body() ?: "" }.getOrDefault("")

        val dnsRecords = parseDnsRecords(dnsRaw)
        val headers = parseHeaders(headersRaw)
        val hasHttps = checkHttps(cleanDomain)
        val hasHsts = headers.keys.any { it.equals("strict-transport-security", ignoreCase = true) }
        val hasCsp = headers.keys.any { it.equals("content-security-policy", ignoreCase = true) }
        val hasXFrame = headers.keys.any { it.equals("x-frame-options", ignoreCase = true) }
        val serverInfo = headers.entries.find { it.key.equals("server", ignoreCase = true) }?.value
        val technologies = detectTechnologies(headers, serverInfo)
        val sslExpiry = if (hasHttps) checkSslExpiry(cleanDomain) else null

        var score = 0
        if (hasHttps) score += 25
        if (hasHsts) score += 20
        if (hasCsp) score += 20
        if (hasXFrame) score += 15
        if (dnsRecords.isNotEmpty()) score += 10
        if (whoisRaw.isNotEmpty()) score += 10

        val ipRecord = dnsRecords.find { it.type == "A" }?.value
        val domainInfo = DomainInfo(
            domain = cleanDomain,
            ipAddress = ipRecord,
            reverseDns = null,
            dnsRecords = dnsRecords,
            httpHeaders = headers,
            sslValid = hasHttps,
            sslExpiry = sslExpiry,
            whoisData = whoisRaw.take(2000),
            securityScore = score,
            hasHttps = hasHttps,
            hasHsts = hasHsts,
            hasCsp = hasCsp,
            hasXFrameOptions = hasXFrame,
            serverInfo = serverInfo,
            technologies = technologies
        )
        saveToHistory(ScanType.DOMAIN_ANALYSIS, cleanDomain, domainInfo,
            if (score >= 60) "SAFE" else if (score >= 30) "SUSPICIOUS" else "DANGEROUS")
        domainInfo
    }

    // ─── Username OSINT ───────────────────────────────────────────────
    suspend fun checkUsername(username: String): Result<UsernameOsintResult> = runCatching {
        val platforms = listOf(
            Triple("GitHub", "https://github.com/%s", "github.com"),
            Triple("Twitter/X", "https://twitter.com/%s", "twitter.com"),
            Triple("Instagram", "https://instagram.com/%s", "instagram.com"),
            Triple("Reddit", "https://reddit.com/user/%s", "reddit.com"),
            Triple("TikTok", "https://tiktok.com/@%s", "tiktok.com"),
            Triple("LinkedIn", "https://linkedin.com/in/%s", "linkedin.com"),
            Triple("Pinterest", "https://pinterest.com/%s", "pinterest.com"),
            Triple("Twitch", "https://twitch.tv/%s", "twitch.tv"),
            Triple("YouTube", "https://youtube.com/@%s", "youtube.com"),
            Triple("Facebook", "https://facebook.com/%s", "facebook.com"),
            Triple("DeviantArt", "https://deviantart.com/%s", "deviantart.com"),
            Triple("GitLab", "https://gitlab.com/%s", "gitlab.com"),
            Triple("Medium", "https://medium.com/@%s", "medium.com"),
            Triple("Snapchat", "https://snapchat.com/add/%s", "snapchat.com"),
            Triple("Spotify", "https://open.spotify.com/user/%s", "open.spotify.com"),
            Triple("SoundCloud", "https://soundcloud.com/%s", "soundcloud.com"),
            Triple("Vimeo", "https://vimeo.com/%s", "vimeo.com"),
            Triple("Tumblr", "https://%s.tumblr.com", "tumblr.com"),
            Triple("HackerNews", "https://news.ycombinator.com/user?id=%s", "ycombinator.com"),
            Triple("ProductHunt", "https://producthunt.com/@%s", "producthunt.com")
        )

        val results = platforms.map { (platform, urlTemplate, _) ->
            val url = urlTemplate.format(username)
            val code = runCatching {
                val conn = URL(url).openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.instanceFollowRedirects = false
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (compatible)")
                conn.connectTimeout = 5000
                conn.readTimeout = 5000
                val code = conn.responseCode
                conn.disconnect()
                code
            }.getOrDefault(0)

            UsernameResult(
                platform = platform,
                url = url,
                found = code == 200,
                confidence = if (code == 200) 90 else if (code == 301 || code == 302) 50 else 0
            )
        }

        val foundCount = results.count { it.found }
        val osintResult = UsernameOsintResult(
            username = username,
            results = results,
            foundCount = foundCount,
            totalChecked = platforms.size
        )
        saveToHistory(ScanType.USERNAME_OSINT, username, osintResult,
            if (foundCount > 5) "SUSPICIOUS" else "UNKNOWN")
        osintResult
    }

    // ─── Threat Intelligence ─────────────────────────────────────────
    suspend fun checkThreat(target: String): Result<ThreatInfo> = runCatching {
        val isIp = target.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))
        val threatInfo = if (vtApiKey.isNotBlank()) {
            if (isIp) {
                val resp = virusTotalService.getIpReport(vtApiKey, target)
                val body = resp.body()
                ThreatInfo(
                    target = target, isIp = true,
                    country = body?.country,
                    riskLevel = if ((body?.detectedUrls?.size ?: 0) > 0) RiskLevel.DANGEROUS else RiskLevel.SAFE,
                    lastAnalysisDate = null
                )
            } else {
                val resp = virusTotalService.getDomainReport(vtApiKey, target)
                val body = resp.body()
                ThreatInfo(
                    target = target, isIp = false,
                    categories = body?.categories ?: emptyMap(),
                    riskLevel = if ((body?.detectedUrls?.size ?: 0) > 0) RiskLevel.DANGEROUS else RiskLevel.SAFE,
                    lastAnalysisDate = null
                )
            }
        } else {
            ThreatInfo(target = target, isIp = isIp, riskLevel = RiskLevel.UNKNOWN, lastAnalysisDate = null)
        }
        saveToHistory(ScanType.THREAT_INTEL, target, threatInfo, threatInfo.riskLevel.name)
        threatInfo
    }

    // ─── Red Team: Nmap via HackerTarget ─────────────────────────────
    suspend fun nmapScan(target: String, scanType: String): Result<NmapScanResult> = runCatching {
        val start = System.currentTimeMillis()
        val raw = hackerTargetService.nmapScan(target).body() ?: "No response"
        val ports = parseNmapOutput(raw)
        val result = NmapScanResult(
            target = target,
            scanType = scanType,
            ports = ports,
            osGuess = null,
            rawOutput = raw,
            scanDuration = System.currentTimeMillis() - start
        )
        saveToHistory(ScanType.NMAP, target, result, "UNKNOWN")
        result
    }

    // ─── Red Team: Port Scanner ───────────────────────────────────────
    suspend fun portScan(target: String, ports: List<Int>): Result<NmapScanResult> = runCatching {
        val start = System.currentTimeMillis()
        val results = mutableListOf<PortResult>()
        for (port in ports) {
            val open = runCatching {
                val sock = java.net.Socket()
                sock.connect(java.net.InetSocketAddress(target, port), 1500)
                sock.close()
                true
            }.getOrDefault(false)
            results.add(PortResult(port, open, getCommonService(port), null))
        }
        val result = NmapScanResult(
            target = target,
            scanType = "TCP Connect",
            ports = results,
            osGuess = null,
            rawOutput = results.joinToString("\n") { "${it.port}/tcp ${if (it.open) "open" else "closed"} ${it.service ?: ""}" },
            scanDuration = System.currentTimeMillis() - start
        )
        saveToHistory(ScanType.PORT_SCAN, target, result, "UNKNOWN")
        result
    }

    // ─── Red Team: Banner Grab ─────────────────────────────────────────
    suspend fun bannerGrab(target: String, port: Int): Result<PortResult> = runCatching {
        val banner = runCatching {
            val sock = java.net.Socket()
            sock.connect(java.net.InetSocketAddress(target, port), 3000)
            sock.soTimeout = 3000
            val reader = sock.getInputStream().bufferedReader()
            val banner = reader.readLine() ?: "No banner"
            sock.close()
            banner
        }.getOrDefault("Connection refused or no banner")
        PortResult(port, true, getCommonService(port), banner)
    }

    // ─── Red Team: WAF Detection ──────────────────────────────────────
    suspend fun detectWaf(target: String): Result<WafDetectResult> = runCatching {
        val url = if (target.startsWith("http")) target else "https://$target"
        val headersRaw = runCatching { hackerTargetService.httpHeaders(target.removePrefix("https://").removePrefix("http://")).body() ?: "" }.getOrDefault("")
        val headers = parseHeaders(headersRaw)

        val fingerprints = mutableListOf<String>()
        var wafName: String? = null

        val wafSignatures = mapOf(
            "Cloudflare" to listOf("cf-ray", "cloudflare", "__cfduid"),
            "AWS WAF" to listOf("x-amzn-requestid", "x-amz-cf-id"),
            "Akamai" to listOf("akamai", "x-akamai-transformed"),
            "Sucuri" to listOf("x-sucuri-id", "sucuri"),
            "Imperva/Incapsula" to listOf("x-iinfo", "incap_ses"),
            "Fastly" to listOf("x-fastly-request-id", "fastly"),
            "F5 BIG-IP" to listOf("bigipserver", "f5"),
            "ModSecurity" to listOf("mod_security", "modsec"),
            "Barracuda" to listOf("barra_counter_session"),
            "nginx" to listOf("x-nginx-cache", "nginx")
        )

        headers.forEach { (k, v) ->
            wafSignatures.forEach { (waf, sigs) ->
                if (sigs.any { sig -> k.contains(sig, ignoreCase = true) || v.contains(sig, ignoreCase = true) }) {
                    fingerprints.add("$waf header detected: $k")
                    if (wafName == null) wafName = waf
                }
            }
        }

        WafDetectResult(
            target = target,
            wafDetected = fingerprints.isNotEmpty(),
            wafName = wafName,
            confidence = if (fingerprints.size > 2) 95 else if (fingerprints.isNotEmpty()) 70 else 0,
            fingerprints = fingerprints
        )
    }

    // ─── CVE Lookup ───────────────────────────────────────────────────
    suspend fun searchCve(vendor: String, product: String): Result<List<CveInfo>> = runCatching {
        val resp = cveService.searchCve(vendor, product)
        (resp.body() ?: emptyList()).map { dto ->
            CveInfo(
                id = dto.id ?: "Unknown",
                summary = dto.summary ?: "No summary",
                cvss = dto.cvss,
                publishedDate = dto.published,
                references = dto.references ?: emptyList()
            )
        }
    }

    // ─── History ──────────────────────────────────────────────────────
    fun getAllScans(): Flow<List<ScanEntity>> = database.scanDao().getAllScans()
    fun searchScans(query: String): Flow<List<ScanEntity>> = database.scanDao().searchScans(query)
    suspend fun deleteScan(id: Long) = database.scanDao().deleteScanById(id)
    suspend fun clearHistory() = database.scanDao().deleteAllScans()

    // ─── Private Helpers ──────────────────────────────────────────────
    private suspend fun <T> saveToHistory(type: ScanType, target: String, data: T, risk: String) {
        runCatching {
            database.scanDao().insertScan(
                ScanEntity(
                    scanType = type,
                    target = target,
                    resultJson = gson.toJson(data),
                    riskLevel = risk,
                    summary = generateSummary(type, target)
                )
            )
        }
    }

    private fun generateSummary(type: ScanType, target: String): String = when (type) {
        ScanType.IP_INTEL -> "IP analysis of $target"
        ScanType.DOMAIN_ANALYSIS -> "Domain analysis of $target"
        ScanType.USERNAME_OSINT -> "OSINT lookup for @$target"
        ScanType.THREAT_INTEL -> "Threat check for $target"
        ScanType.NMAP -> "Nmap scan of $target"
        ScanType.PORT_SCAN -> "Port scan of $target"
        ScanType.BANNER_GRAB -> "Banner grab of $target"
        ScanType.WAF_DETECT -> "WAF detection for $target"
        else -> "$type scan of $target"
    }

    private fun parseDnsRecords(raw: String): List<DnsRecord> {
        return raw.lines().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size >= 4) {
                DnsRecord(type = parts[2], value = parts[3])
            } else null
        }
    }

    private fun parseHeaders(raw: String): Map<String, String> {
        val map = mutableMapOf<String, String>()
        raw.lines().forEach { line ->
            val colonIdx = line.indexOf(':')
            if (colonIdx > 0) {
                val key = line.substring(0, colonIdx).trim()
                val value = line.substring(colonIdx + 1).trim()
                map[key] = value
            }
        }
        return map
    }

    private fun checkHttps(domain: String): Boolean = runCatching {
        val conn = URL("https://$domain").openConnection() as HttpURLConnection
        conn.connectTimeout = 5000
        conn.readTimeout = 5000
        val code = conn.responseCode
        conn.disconnect()
        code < 400
    }.getOrDefault(false)

    private fun checkSslExpiry(domain: String): String? = runCatching {
        val conn = URL("https://$domain").openConnection() as javax.net.ssl.HttpsURLConnection
        conn.connectTimeout = 5000
        conn.connect()
        val cert = conn.serverCertificates.firstOrNull() as? java.security.cert.X509Certificate
        conn.disconnect()
        cert?.notAfter?.toString()
    }.getOrNull()

    private fun detectTechnologies(headers: Map<String, String>, server: String?): List<String> {
        val techs = mutableListOf<String>()
        server?.let { s ->
            when {
                s.contains("nginx", ignoreCase = true) -> techs.add("Nginx")
                s.contains("apache", ignoreCase = true) -> techs.add("Apache")
                s.contains("cloudflare", ignoreCase = true) -> techs.add("Cloudflare")
                s.contains("iis", ignoreCase = true) -> techs.add("IIS")
            }
        }
        if (headers.keys.any { it.equals("x-powered-by", ignoreCase = true) }) {
            techs.add(headers.entries.find { it.key.equals("x-powered-by", ignoreCase = true) }?.value ?: "")
        }
        return techs.filter { it.isNotEmpty() }
    }

    private fun parseNmapOutput(raw: String): List<PortResult> {
        return raw.lines().mapNotNull { line ->
            val match = Regex("(\\d+)/(tcp|udp)\\s+(open|closed|filtered)\\s*(\\S*)").find(line)
            match?.let {
                val port = it.groupValues[1].toIntOrNull() ?: return@let null
                val state = it.groupValues[3]
                val service = it.groupValues[4].ifBlank { null }
                PortResult(port, state == "open", service ?: getCommonService(port), null)
            }
        }
    }

    private fun getCommonService(port: Int): String = when (port) {
        21 -> "FTP"; 22 -> "SSH"; 23 -> "Telnet"; 25 -> "SMTP"
        53 -> "DNS"; 80 -> "HTTP"; 110 -> "POP3"; 143 -> "IMAP"
        443 -> "HTTPS"; 445 -> "SMB"; 3306 -> "MySQL"; 3389 -> "RDP"
        5432 -> "PostgreSQL"; 6379 -> "Redis"; 8080 -> "HTTP-Alt"
        8443 -> "HTTPS-Alt"; 27017 -> "MongoDB"; 9200 -> "Elasticsearch"
        else -> "Unknown"
    }
}
