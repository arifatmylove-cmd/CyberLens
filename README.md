# CyberLens — Cybersecurity OSINT & Intelligence Android App

A professional mobile cybersecurity intelligence toolkit built with Kotlin + Jetpack Compose.

[![Build APK](https://github.com/arifatmylove-cmd/CyberLens/actions/workflows/build-apk.yml/badge.svg)](https://github.com/arifatmylove-cmd/CyberLens/actions/workflows/build-apk.yml)

---

## 📱 Features

### OSINT Modules
| Module | Description | APIs Used |
|--------|-------------|-----------|
| 🌐 IP Intelligence | Geo, ISP, ASN, risk level, open ports, vulns | ipapi.co, Shodan InternetDB |
| 🌍 Domain Analysis | WHOIS, DNS records, SSL, security headers | HackerTarget, direct checks |
| 🕵️ Username OSINT | Check 20+ platforms for username existence | HTTP probing |
| 🖼️ Image Reverse | Reverse image search (API key required) | Google Vision / TinEye |
| 🔐 Website Scanner | Security score, headers, TLS, tech stack | HackerTarget, direct checks |
| ☣️ Threat Intel | Reputation, blacklist, malware status | VirusTotal (free key) |

### Red Team Tools (Authorized Use Only)
| Tool | Description |
|------|-------------|
| 🗺️ Nmap Scanner | Top-port TCP scan via HackerTarget free API |
| 🔌 Port Scanner | TCP connect scan on custom port ranges |
| 📋 Banner Grab | Service banner/version fingerprinting |
| 🛡️ WAF Detect | Detect Cloudflare, AWS WAF, Akamai, Imperva & more |
| 🔍 CVE Lookup | Search known vulnerabilities via CIRCL CVE DB |

### Platform Features
- 📊 Scan History — Room DB, searchable, timestamped
- 🌑 Dark cyberpunk UI with neon blue/green accents
- ✅ Authorization consent on all scan operations
- 🏗️ MVVM + Hilt DI + Coroutines + Flow

---

## 🔑 API Keys

Most features work **without any API key** using free public APIs:

| Service | Key Required | Free Tier |
|---------|-------------|-----------|
| ipapi.co | No | 1000 req/day |
| Shodan InternetDB | No | Unlimited |
| HackerTarget | No | 100 req/day |
| CIRCL CVE Search | No | Unlimited |
| VirusTotal | Optional | 500 req/day |
| Google Cloud Vision | For Image Search | 1000 req/month |
| TinEye | For Image Search | Trial available |

To add VirusTotal: edit `app/src/main/java/com/cyberlens/app/di/AppModule.kt`, find `provideVtApiKey()` and add your key.

---

## 🏗️ Build

### Prerequisites
- Android Studio Ladybug or newer
- JDK 17
- Android SDK 35

### Local Build
```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### GitHub Actions (Automatic APK)
Every push to `main` triggers the workflow. Download the APK:
1. Go to **Actions** tab on GitHub
2. Click the latest **Build CyberLens APK** run
3. Download **CyberLens-Debug-APK** from the Artifacts section

---

## ⚠️ Legal Notice

This tool is for **authorized cybersecurity research only**. You must:
- Obtain written authorization before scanning any target
- Only scan systems you own or have explicit permission to test
- Comply with all applicable laws (CFAA, Computer Misuse Act, etc.)

The authors are not responsible for unauthorized or illegal use.

---

## 🏛️ Architecture

```
app/
├── di/           — Hilt dependency injection modules
├── data/
│   ├── local/    — Room database (ScanEntity, ScanDao)
│   ├── remote/   — Retrofit API services + DTOs
│   └── repository/ — OsintRepository (single source of truth)
├── domain/
│   └── model/    — Domain models (IpInfo, DomainInfo, etc.)
└── ui/
    ├── theme/    — Cyberpunk Material3 theme
    ├── navigation/ — NavGraph
    ├── components/ — Reusable Compose components
    ├── screens/  — All feature screens
    └── viewmodel/ — HiltViewModels per feature
```

## Tech Stack
- **Kotlin** + **Jetpack Compose** + **Material 3**
- **Hilt** — Dependency Injection
- **Retrofit** + **OkHttp** — Network
- **Room** — Local database
- **Coroutines** + **Flow** — Async
- **Coil** — Image loading
