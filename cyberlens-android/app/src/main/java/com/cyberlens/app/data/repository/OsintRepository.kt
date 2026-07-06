package com.cyberlens.app.data.repository

import com.cyberlens.app.data.local.AppDatabase
import com.cyberlens.app.data.local.ScanEntity
import com.cyberlens.app.data.remote.*
import com.cyberlens.app.data.remote.dto.*
import com.cyberlens.app.domain.model.*
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.cert.X509Certificate
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import javax.net.ssl.HttpsURLConnection

@Singleton
class OsintRepository @Inject constructor(
    private val ipGeoService: IpGeoApiService,
    private val shodanInternetDb: ShodanInternetDbService,
    private val shodanApiService: ShodanApiService,
    private val hackerTargetService: HackerTargetApiService,
    private val virusTotalService: VirusTotalApiService,
    private val cveService: CveSearchApiService,
    private val whoisXmlService: WhoisXmlApiService,
    private val database: AppDatabase,
    private val gson: Gson,
    private val okHttpClient: OkHttpClient,
    @Named("vt_api_key")       private val vtApiKey: String,
    @Named("ipgeo_api_key")    private val ipGeoApiKey: String,
    @Named("shodan_api_key")   private val shodanApiKey: String,
    @Named("whoisxml_api_key") private val whoisXmlApiKey: String
) {

    // ─── IP Intelligence ─────────────────────────────────────────────────────
    suspend fun analyzeIp(ip: String): Result<IpInfo> = runCatching {
        // Handle private / reserved IPs gracefully
        if (isPrivateIp(ip)) {
            val info = IpInfo(
                ip = ip, city = "Private Network", region = "LAN",
                country = "Local", org = "Private Address Space",
                asn = detectPrivateRange(ip), timezone = null,
                latitude = null, longitude = null, hostname = null,
                isp = "Local Network", riskLevel = RiskLevel.UNKNOWN,
                openPorts = emptyList(), tags = listOf("private", "rfc1918"),
                cpes = emptyList(), vulns = emptyList()
            )
            saveToHistory(ScanType.IP_INTEL, ip, info, RiskLevel.UNKNOWN.name)
            return@runCatching info
        }

        // 1. IPGeolocation.io — primary geo source
        val geo = withContext(Dispatchers.IO) {
            runCatching { ipGeoService.getIpGeo(ipGeoApiKey, ip).body() }.getOrNull()
        }

        // 2. Shodan full API — ports, vulns, banners
        val shodan = withContext(Dispatchers.IO) {
            runCatching { shodanApiService.getHost(ip, shodanApiKey).body() }.getOrNull()
        }

        // 3. Fallback to InternetDB if Shodan API fails
        val inetDb = if (shodan == null) withContext(Dispatchers.IO) {
            runCatching { shodanInternetDb.getInternetDbInfo(ip).body() }.getOrNull()
        } else null

        val vulns = shodan?.vulns?.keys?.toList() ?: inetDb?.vulns ?: emptyList()
        val ports = shodan?.ports ?: inetDb?.ports ?: emptyList()
        val tags  = shodan?.tags ?: inetDb?.tags ?: emptyList()

        val riskLevel = when {
            vulns.isNotEmpty()                                          -> RiskLevel.DANGEROUS
            tags.any { it in listOf("tor", "vpn", "proxy", "botnet") } -> RiskLevel.SUSPICIOUS
            ports.any { it in listOf(23, 445, 3389, 5900) }           -> RiskLevel.SUSPICIOUS
            else                                                        -> RiskLevel.SAFE
        }

        val info = IpInfo(
            ip        = ip,
            city      = geo?.city ?: shodan?.city,
            region    = geo?.stateProv ?: shodan?.regionCode,
            country   = geo?.countryName ?: shodan?.countryName,
            org       = geo?.organization ?: shodan?.org,
            asn       = geo?.asn ?: shodan?.asn,
            timezone  = geo?.timeZone?.name,
            latitude  = geo?.latitude?.toDoubleOrNull() ?: shodan?.latitude,
            longitude = geo?.longitude?.toDoubleOrNull() ?: shodan?.longitude,
            hostname  = shodan?.hostnames?.firstOrNull() ?: inetDb?.hostnames?.firstOrNull(),
            isp       = geo?.isp ?: shodan?.isp,
            riskLevel = riskLevel,
            openPorts = ports,
            tags      = tags,
            cpes      = inetDb?.cpes ?: emptyList(),
            vulns     = vulns
        )
        saveToHistory(ScanType.IP_INTEL, ip, info, riskLevel.name)
        info
    }

    // ─── Domain Analysis ─────────────────────────────────────────────────────
    suspend fun analyzeDomain(domain: String): Result<DomainInfo> = runCatching {
        val clean = domain.removePrefix("https://").removePrefix("http://")
            .trimEnd('/').split("/").first().split("?").first()

        // 1. Direct HTTPS request — real response headers + SSL cert (runs on IO dispatcher)
        val (headers, statusCode, sslExpiry, sslValid) = directHttpsHeaders("https://$clean")

        // 2. DNS lookup via HackerTarget (plain-text, free)
        val dnsRaw = withContext(Dispatchers.IO) {
            runCatching { hackerTargetService.dnsLookup(clean).body() ?: "" }.getOrDefault("")
        }
        val dnsRecords = parseDnsRecords(dnsRaw)

        // 3. WHOIS via WhoisXML
        val whoisResp = withContext(Dispatchers.IO) {
            runCatching { whoisXmlService.getWhois(whoisXmlApiKey, clean).body() }.getOrNull()
        }
        val whoisData = formatWhoisData(whoisResp?.whoisRecord)

        // 4. Evaluate security headers from real response
        val hasHttps  = statusCode in 200..499
        val hasHsts   = headers.keys.any { it.equals("strict-transport-security", ignoreCase = true) }
        val hasCsp    = headers.keys.any { it.equals("content-security-policy", ignoreCase = true) }
        val hasXFrame = headers.keys.any { it.equals("x-frame-options", ignoreCase = true) }
        val serverInfo = headers.entries.find { it.key.equals("server", ignoreCase = true) }?.value
        val technologies = detectTechnologies(headers, serverInfo)

        var score = 0
        if (hasHttps)         score += 25
        if (sslValid)         score += 15
        if (hasHsts)          score += 20
        if (hasCsp)           score += 20
        if (hasXFrame)        score += 10
        if (dnsRecords.isNotEmpty()) score += 5
        if (whoisData != null) score += 5

        val ipRecord = dnsRecords.find { it.type == "A" }?.value
        val domainInfo = DomainInfo(
            domain           = clean,
            ipAddress        = ipRecord,
            reverseDns       = null,
            dnsRecords       = dnsRecords,
            httpHeaders      = headers,
            sslValid         = sslValid,
            sslExpiry        = sslExpiry,
            whoisData        = whoisData,
            securityScore    = score,
            hasHttps         = hasHttps,
            hasHsts          = hasHsts,
            hasCsp           = hasCsp,
            hasXFrameOptions = hasXFrame,
            serverInfo       = serverInfo,
            technologies     = technologies
        )
        saveToHistory(ScanType.DOMAIN_ANALYSIS, clean, domainInfo,
            if (score >= 70) "SAFE" else if (score >= 40) "SUSPICIOUS" else "DANGEROUS")
        domainInfo
    }

    // ─── Username OSINT ──────────────────────────────────────────────────────
    suspend fun checkUsername(username: String): Result<UsernameOsintResult> = runCatching {
        // Platforms with reliable 200/404 status-code detection
        data class Platform(val name: String, val url: String, val useGet: Boolean = false, val notFoundBody: String? = null)

        val platforms = listOf(
            // API endpoints — most reliable
            Platform("GitHub",      "https://api.github.com/users/$username", useGet = true),
            Platform("Reddit",      "https://www.reddit.com/user/$username/about.json", useGet = true),
            Platform("HackerNews",  "https://hacker-news.firebaseio.com/v0/user/$username.json", useGet = true, notFoundBody = "null"),
            Platform("Dev.to",      "https://dev.to/api/users/by_username?url=$username", useGet = true),
            // Social platforms — reliable 404
            Platform("GitLab",      "https://gitlab.com/$username"),
            Platform("DeviantArt",  "https://www.deviantart.com/$username"),
            Platform("SoundCloud",  "https://soundcloud.com/$username"),
            Platform("Vimeo",       "https://vimeo.com/$username"),
            Platform("Medium",      "https://medium.com/@$username"),
            Platform("Twitch",      "https://www.twitch.tv/$username"),
            Platform("Pinterest",   "https://www.pinterest.com/$username/"),
            Platform("Keybase",     "https://keybase.io/$username"),
            Platform("Replit",      "https://replit.com/@$username"),
            Platform("Codepen",     "https://codepen.io/$username"),
            Platform("Mastodon",    "https://mastodon.social/@$username"),
            Platform("Flickr",      "https://www.flickr.com/people/$username/"),
            Platform("Bitbucket",   "https://bitbucket.org/$username/"),
            Platform("Steam",       "https://steamcommunity.com/id/$username"),
            Platform("Gravatar",    "https://en.gravatar.com/$username"),
            Platform("Npmjs",       "https://www.npmjs.com/~$username")
        )

        val results = platforms.map { platform ->
            withContext(Dispatchers.IO) {
                val (code, body) = if (platform.useGet) httpGet(platform.url) else httpHead(platform.url)
                val notFoundByBody = platform.notFoundBody != null &&
                        body?.trim().equals(platform.notFoundBody, ignoreCase = true)
                val found = when {
                    notFoundByBody    -> false
                    code == 200       -> true
                    code == 404       -> false
                    code in 301..302  -> false  // redirect usually means user not found on this platform
                    code == 403       -> true   // forbidden — profile exists but we can't read it
                    else              -> false
                }
                UsernameResult(
                    platform   = platform.name,
                    url        = platform.url,
                    found      = found,
                    confidence = when {
                        found && code == 200 -> 95
                        found && code == 403 -> 70
                        else                 -> 0
                    }
                )
            }
        }

        val foundCount = results.count { it.found }
        val osintResult = UsernameOsintResult(
            username     = username,
            results      = results,
            foundCount   = foundCount,
            totalChecked = platforms.size
        )
        saveToHistory(ScanType.USERNAME_OSINT, username, osintResult,
            if (foundCount > 5) "SUSPICIOUS" else "UNKNOWN")
        osintResult
    }

    // ─── Threat Intelligence ─────────────────────────────────────────────────
    suspend fun checkThreat(target: String): Result<ThreatInfo> = runCatching {
        val isIp = target.matches(Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$"))
        val threatInfo = if (vtApiKey.isNotBlank()) {
            if (isIp) {
                val body = withContext(Dispatchers.IO) {
                    runCatching { virusTotalService.getIpReport(vtApiKey, target).body() }.getOrNull()
                }
                val malicious = body?.detectedUrls?.count { (it.positives ?: 0) > 0 } ?: 0
                ThreatInfo(
                    target       = target,
                    isIp         = true,
                    malicious    = malicious,
                    country      = body?.country,
                    riskLevel    = if (malicious > 0) RiskLevel.DANGEROUS else RiskLevel.SAFE
                )
            } else {
                val body = withContext(Dispatchers.IO) {
                    runCatching { virusTotalService.getDomainReport(vtApiKey, target).body() }.getOrNull()
                }
                val malicious = body?.detectedUrls?.count { (it.positives ?: 0) > 0 } ?: 0
                ThreatInfo(
                    target      = target,
                    isIp        = false,
                    malicious   = malicious,
                    categories  = body?.categories ?: emptyMap(),
                    riskLevel   = if (malicious > 0) RiskLevel.DANGEROUS else RiskLevel.SAFE,
                    tags        = body?.subdomains?.take(5) ?: emptyList()
                )
            }
        } else {
            ThreatInfo(target = target, isIp = isIp, riskLevel = RiskLevel.UNKNOWN)
        }
        saveToHistory(ScanType.THREAT_INTEL, target, threatInfo, threatInfo.riskLevel.name)
        threatInfo
    }

    // ─── Red Team: Nmap via HackerTarget ─────────────────────────────────────
    suspend fun nmapScan(target: String, scanType: String): Result<NmapScanResult> = runCatching {
        val start = System.currentTimeMillis()
        val raw = withContext(Dispatchers.IO) {
            runCatching { hackerTargetService.nmapScan(target).body() ?: "" }.getOrDefault("")
        }
        if (raw.startsWith("error") || raw.startsWith("API count")) {
            throw Exception("HackerTarget: $raw")
        }
        val ports = parseNmapOutput(raw)
        val result = NmapScanResult(
            target       = target,
            scanType     = scanType,
            ports        = ports,
            osGuess      = extractOsGuess(raw),
            rawOutput    = raw,
            scanDuration = System.currentTimeMillis() - start
        )
        saveToHistory(ScanType.NMAP, target, result, "UNKNOWN")
        result
    }

    // ─── Red Team: Port Scanner (TCP connect) ────────────────────────────────
    suspend fun portScan(target: String, ports: List<Int>): Result<NmapScanResult> = runCatching {
        val start = System.currentTimeMillis()
        val results = withContext(Dispatchers.IO) {
            ports.map { port ->
                val open = runCatching {
                    val sock = java.net.Socket()
                    sock.connect(java.net.InetSocketAddress(target, port), 1500)
                    sock.close()
                    true
                }.getOrDefault(false)
                PortResult(port, open, getCommonService(port), null)
            }
        }
        val result = NmapScanResult(
            target       = target,
            scanType     = "TCP Connect",
            ports        = results,
            osGuess      = null,
            rawOutput    = results.joinToString("\n") {
                "${it.port}/tcp  ${if (it.open) "open  " else "closed"} ${it.service ?: ""}"
            },
            scanDuration = System.currentTimeMillis() - start
        )
        saveToHistory(ScanType.PORT_SCAN, target, result, "UNKNOWN")
        result
    }

    // ─── Red Team: Banner Grab ───────────────────────────────────────────────
    suspend fun bannerGrab(target: String, port: Int): Result<PortResult> = runCatching {
        val banner = withContext(Dispatchers.IO) {
            runCatching {
                val sock = java.net.Socket()
                sock.connect(java.net.InetSocketAddress(target, port), 4000)
                sock.soTimeout = 4000
                // For HTTP/HTTPS ports, send a probe request
                if (port == 80 || port == 8080 || port == 8000) {
                    sock.getOutputStream().write("HEAD / HTTP/1.0\r\nHost: $target\r\n\r\n".toByteArray())
                    sock.getOutputStream().flush()
                }
                val banner = sock.getInputStream().bufferedReader().readLine() ?: "No banner"
                sock.close()
                banner
            }.getOrElse { e ->
                // Fallback: try OkHttp for HTTP ports
                if (port == 80 || port == 443 || port == 8080 || port == 8443) {
                    val scheme = if (port == 443 || port == 8443) "https" else "http"
                    runCatching {
                        val req = Request.Builder().url("$scheme://$target:$port/")
                            .addHeader("User-Agent", "CyberLens/1.0").head().build()
                        val resp = okHttpClient.newCall(req).execute()
                        val server = resp.header("Server") ?: resp.header("x-powered-by") ?: "Unknown"
                        "HTTP/${resp.protocol} ${resp.code} — Server: $server".also { resp.close() }
                    }.getOrDefault("Connection refused: ${e.message}")
                } else {
                    "Connection refused or timeout: ${e.message}"
                }
            }
        }
        val result = PortResult(port, true, getCommonService(port), banner)
        saveToHistory(ScanType.BANNER_GRAB, "$target:$port", result, "UNKNOWN")
        result
    }

    // ─── Red Team: WAF Detection ─────────────────────────────────────────────
    suspend fun detectWaf(target: String): Result<WafDetectResult> = runCatching {
        val url = if (target.startsWith("http")) target else "https://$target"

        // Make a direct real request — no HackerTarget proxy that distorts headers
        val headers = withContext(Dispatchers.IO) {
            runCatching {
                val req = Request.Builder().url(url)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .get().build()
                val resp = okHttpClient.newCall(req).execute()
                val hdrs = resp.headers.toMap()
                resp.close()
                hdrs
            }.getOrDefault(emptyMap())
        }

        val fingerprints = mutableListOf<String>()
        var wafName: String? = null

        val wafSignatures = mapOf(
            "Cloudflare"       to listOf("cf-ray", "cf-cache-status", "cf-request-id", "cf-ipcountry"),
            "AWS WAF"          to listOf("x-amzn-requestid", "x-amz-cf-id", "x-amz-apigw-id"),
            "Akamai"           to listOf("x-akamai-transformed", "x-check-cacheable", "akamai"),
            "Sucuri"           to listOf("x-sucuri-id", "x-sucuri-cache", "sucuri"),
            "Imperva/Incapsula" to listOf("x-iinfo", "incap_ses", "visid_incap"),
            "Fastly"           to listOf("x-fastly-request-id", "x-served-by", "fastly"),
            "F5 BIG-IP"        to listOf("bigipserver", "x-cnection", "f5-trafficshield"),
            "ModSecurity"      to listOf("mod_security", "x-modsec-rule"),
            "Barracuda"        to listOf("barra_counter_session", "barracuda"),
            "nginx"            to listOf("x-nginx-cache", "nginx"),
            "Varnish"          to listOf("x-varnish", "via: 1.1 varnish"),
            "Cloudfront"       to listOf("x-amz-cf-id", "x-cache: hit from cloudfront"),
            "Azure Front Door" to listOf("x-azure-requestid", "x-azure-ref")
        )

        headers.forEach { (k, v) ->
            val kLower = k.lowercase()
            val vLower = v.lowercase()
            wafSignatures.forEach { (waf, sigs) ->
                if (sigs.any { sig -> kLower.contains(sig) || vLower.contains(sig) }) {
                    if (!fingerprints.any { it.startsWith(waf) }) {
                        fingerprints.add("$waf: $k = ${v.take(60)}")
                        if (wafName == null) wafName = waf
                    }
                }
            }
        }

        val wafResult = WafDetectResult(
            target      = target,
            wafDetected = fingerprints.isNotEmpty(),
            wafName     = wafName,
            confidence  = if (fingerprints.size > 2) 95 else if (fingerprints.isNotEmpty()) 70 else 0,
            fingerprints = fingerprints
        )
        saveToHistory(ScanType.WAF_DETECT, target, wafResult,
            if (wafResult.wafDetected) "SAFE" else "UNKNOWN")
        wafResult
    }

    // ─── CVE Lookup ──────────────────────────────────────────────────────────
    suspend fun searchCve(vendor: String, product: String): Result<List<CveInfo>> = runCatching {
        val resp = withContext(Dispatchers.IO) { cveService.searchCve(vendor, product) }
        (resp.body() ?: emptyList()).map { dto ->
            CveInfo(
                id            = dto.id ?: "Unknown",
                summary       = dto.summary ?: "No summary",
                cvss          = dto.cvss,
                publishedDate = dto.published,
                references    = dto.references ?: emptyList()
            )
        }
    }

    // ─── History ─────────────────────────────────────────────────────────────
    fun getAllScans(): Flow<List<ScanEntity>> = database.scanDao().getAllScans()
    fun searchScans(query: String): Flow<List<ScanEntity>> = database.scanDao().searchScans(query)
    suspend fun deleteScan(id: Long) = database.scanDao().deleteScanById(id)
    suspend fun clearHistory() = database.scanDao().deleteAllScans()

    // ─── Private: HTTP helpers (all run on IO dispatcher) ────────────────────

    /** HEAD request — returns (statusCode, null body). Uses IO dispatcher. */
    private suspend fun httpHead(url: String): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .head().build()
            val resp = okHttpClient.newCall(req).execute()
            val code = resp.code
            resp.close()
            Pair(code, null)
        } catch (e: Exception) { Pair(0, null) }
    }

    /** GET request — returns (statusCode, body). Uses IO dispatcher. */
    private suspend fun httpGet(url: String): Pair<Int, String?> = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .get().build()
            val resp = okHttpClient.newCall(req).execute()
            val code = resp.code
            val body = resp.body?.string()
            resp.close()
            Pair(code, body)
        } catch (e: Exception) { Pair(0, null) }
    }

    data class HttpsResult(
        val headers: Map<String, String>,
        val statusCode: Int,
        val sslExpiry: String?,
        val sslValid: Boolean
    )

    /** Makes a real HTTPS request and extracts security info from OkHttp response. */
    private suspend fun directHttpsHeaders(url: String): HttpsResult = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder().url(url)
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                .head().build()
            val resp = okHttpClient.newCall(req).execute()
            val headers = mutableMapOf<String, String>()
            for (i in 0 until resp.headers.size) {
                headers[resp.headers.name(i)] = resp.headers.value(i)
            }
            val sslExpiry = runCatching {
                val certs = resp.handshake?.peerCertificates ?: emptyList()
                (certs.firstOrNull() as? X509Certificate)?.notAfter?.toString()
            }.getOrNull()
            val sslValid = sslExpiry != null
            resp.close()
            HttpsResult(headers, resp.code, sslExpiry, sslValid)
        } catch (e: Exception) {
            HttpsResult(emptyMap(), 0, null, false)
        }
    }

    // ─── Private: Parsing helpers ────────────────────────────────────────────

    private fun parseDnsRecords(raw: String): List<DnsRecord> {
        // HackerTarget DNS output: "hostname. TTL IN TYPE value"
        return raw.lines().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith(";")) return@mapNotNull null
            val parts = trimmed.split(Regex("\\s+"))
            if (parts.size >= 4) {
                // parts[0]=name, parts[1]=TTL, parts[2]=IN/class, parts[3]=type, parts[4..]=value
                val typeIdx = if (parts[2].equals("IN", ignoreCase = true)) 3 else 2
                val type = parts.getOrNull(typeIdx) ?: return@mapNotNull null
                val value = parts.drop(typeIdx + 1).joinToString(" ").trimEnd('.')
                if (value.isNotBlank()) DnsRecord(type = type.uppercase(), value = value) else null
            } else null
        }.distinctBy { "${it.type}:${it.value}" }
    }

    private fun detectTechnologies(headers: Map<String, String>, server: String?): List<String> {
        val techs = mutableListOf<String>()
        server?.let { s ->
            when {
                s.contains("nginx",      ignoreCase = true) -> techs.add("Nginx")
                s.contains("apache",     ignoreCase = true) -> techs.add("Apache")
                s.contains("cloudflare", ignoreCase = true) -> techs.add("Cloudflare")
                s.contains("iis",        ignoreCase = true) -> techs.add("Microsoft IIS")
                s.contains("litespeed",  ignoreCase = true) -> techs.add("LiteSpeed")
                s.contains("caddy",      ignoreCase = true) -> techs.add("Caddy")
                s.contains("vercel",     ignoreCase = true) -> techs.add("Vercel")
                s.contains("openresty",  ignoreCase = true) -> techs.add("OpenResty")
            }
        }
        headers.entries.find { it.key.equals("x-powered-by", ignoreCase = true) }
            ?.value?.let { techs.add(it) }
        headers.entries.find { it.key.equals("x-generator",  ignoreCase = true) }
            ?.value?.let { techs.add(it) }
        if (headers.keys.any { it.equals("cf-ray", ignoreCase = true) })   techs.add("Cloudflare CDN")
        if (headers.keys.any { it.equals("x-vercel-id", ignoreCase = true) }) techs.add("Vercel Edge")
        return techs.filter { it.isNotEmpty() }.distinct()
    }

    private fun parseNmapOutput(raw: String): List<PortResult> {
        return raw.lines().mapNotNull { line ->
            val match = Regex("(\\d+)/(tcp|udp)\\s+(open|closed|filtered)\\s*(\\S*)").find(line)
            match?.let {
                val port  = it.groupValues[1].toIntOrNull() ?: return@let null
                val state = it.groupValues[3]
                val svc   = it.groupValues[4].ifBlank { null }
                PortResult(port, state == "open", svc ?: getCommonService(port), null)
            }
        }
    }

    private fun extractOsGuess(raw: String): String? {
        val line = raw.lines().firstOrNull { it.contains("OS guess", ignoreCase = true) || it.contains("Running:", ignoreCase = true) }
        return line?.substringAfter(":")?.trim()?.take(80)
    }

    private fun formatWhoisData(record: WhoisRecordDto?): String? {
        record ?: return null
        return buildString {
            record.domainName?.let   { appendLine("Domain:       $it") }
            record.registrarName?.let { appendLine("Registrar:    $it") }
            record.createdDate?.let  { appendLine("Created:      ${it.take(10)}") }
            record.updatedDate?.let  { appendLine("Updated:      ${it.take(10)}") }
            record.expiresDate?.let  { appendLine("Expires:      ${it.take(10)}") }
            record.status?.let       { appendLine("Status:       ${it.take(80)}") }
            record.nameServers?.hostNames?.let { ns ->
                appendLine("Nameservers:  ${ns.joinToString(", ")}")
            }
        }.trim().ifBlank { null }
    }

    private fun getCommonService(port: Int): String = when (port) {
        20 -> "FTP-Data"; 21 -> "FTP"; 22 -> "SSH"; 23 -> "Telnet"
        25 -> "SMTP"; 53 -> "DNS"; 80 -> "HTTP"; 110 -> "POP3"
        143 -> "IMAP"; 443 -> "HTTPS"; 445 -> "SMB"; 587 -> "SMTP-TLS"
        993 -> "IMAPS"; 995 -> "POP3S"; 1433 -> "MSSQL"; 3306 -> "MySQL"
        3389 -> "RDP"; 5432 -> "PostgreSQL"; 5900 -> "VNC"; 6379 -> "Redis"
        8080 -> "HTTP-Alt"; 8443 -> "HTTPS-Alt"; 8888 -> "HTTP-Dev"
        27017 -> "MongoDB"; 9200 -> "Elasticsearch"; 9300 -> "ES-Transport"
        else -> "Unknown"
    }

    private suspend fun <T> saveToHistory(type: ScanType, target: String, data: T, risk: String) {
        runCatching {
            database.scanDao().insertScan(
                ScanEntity(
                    scanType   = type,
                    target     = target,
                    resultJson = gson.toJson(data),
                    riskLevel  = risk,
                    summary    = generateSummary(type, target)
                )
            )
        }
    }

    private fun generateSummary(type: ScanType, target: String): String = when (type) {
        ScanType.IP_INTEL       -> "IP analysis of $target"
        ScanType.DOMAIN_ANALYSIS -> "Domain analysis of $target"
        ScanType.USERNAME_OSINT  -> "OSINT lookup for @$target"
        ScanType.THREAT_INTEL    -> "Threat check for $target"
        ScanType.NMAP            -> "Nmap scan of $target"
        ScanType.PORT_SCAN       -> "Port scan of $target"
        ScanType.BANNER_GRAB     -> "Banner grab of $target"
        ScanType.WAF_DETECT      -> "WAF detection for $target"
        else                     -> "$type scan of $target"
    }

    // ─── Private: IP range helpers ───────────────────────────────────────────

    private fun isPrivateIp(ip: String): Boolean {
        return runCatching {
            val parts = ip.split(".").map { it.toInt() }
            if (parts.size != 4) return@runCatching false
            when {
                parts[0] == 10                                   -> true  // 10.0.0.0/8
                parts[0] == 172 && parts[1] in 16..31           -> true  // 172.16.0.0/12
                parts[0] == 192 && parts[1] == 168              -> true  // 192.168.0.0/16
                parts[0] == 127                                  -> true  // loopback
                parts[0] == 169 && parts[1] == 254              -> true  // link-local
                parts[0] == 0                                    -> true  // 0.x.x.x
                else                                             -> false
            }
        }.getOrDefault(false)
    }

    private fun detectPrivateRange(ip: String): String {
        val parts = ip.split(".").map { it.toIntOrNull() ?: 0 }
        return when {
            parts[0] == 10  -> "RFC1918 Class A (10.0.0.0/8)"
            parts[0] == 172 -> "RFC1918 Class B (172.16.0.0/12)"
            parts[0] == 192 -> "RFC1918 Class C (192.168.0.0/16)"
            parts[0] == 127 -> "Loopback (127.0.0.0/8)"
            parts[0] == 169 -> "Link-Local (169.254.0.0/16)"
            else            -> "Reserved"
        }
    }
}
