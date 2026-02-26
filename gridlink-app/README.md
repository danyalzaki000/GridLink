# 🌐 GridLink

**A native Android DePIN application that turns your phone into a decentralized bandwidth proxy node, earning USDC micropayments on Solana Devnet.**

GridLink is a mobile-first Decentralized Physical Infrastructure Network (DePIN) built with Kotlin and Jetpack Compose. Users rent out their mobile bandwidth through a local HTTP proxy, receive micropayments in USDC via the Solana Mobile Wallet Adapter, and monitor their node through a premium dark-themed dashboard.

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog 2023.1+ |
| JDK | 17+ |
| DigitalOcean VPS | Ubuntu 22.04 (for Matchmaker Oracle) |
| Python | 3.10+ (for Oracle) |

### Build the Android App

```bash
cd tether
./gradlew assembleDebug
```

**APK Output:** `app/build/outputs/apk/debug/app-debug.apk`

### Deploy the Matchmaker Oracle (VPS)

```bash
# On your DigitalOcean VPS
pip install fastapi uvicorn
pm2 start "uvicorn main:app --host 0.0.0.0 --port 8000" --name gridlink-oracle
```

The Oracle runs at `http://<VPS_IP>:8000` and serves:
- `POST /node/register` — Register a new bandwidth node
- `GET /node/status` — Live peer count, latency, and active status

---

## 📱 App Screens

| Screen | Description |
|--------|-------------|
| **Dashboard** | Live bandwidth stats, USDC earnings, proxy toggle (START/STOP), recent traffic logs with relative timestamps |
| **Wallet** | Solana Devnet balance, WITHDRAW (ModalBottomSheet with spinner), STAKE (100 USDC @ 12% APY dialog), transaction history with filter (All/Scraping/AI Inference) |
| **Network** | Animated geographic Canvas map with topographic contours, radial zone rings, data-flow particles, live VPS-polled stats (latency, peers, signal quality, uptime), clickable proxy traffic feed with detail dialogs |
| **Settings** | Persistent Background toggle, Wi-Fi Only, Battery Saver, Daily Data Limit slider, Earning Alerts notification toggle, Private Key Export dialog, Help & Support URL, Sign Out confirmation |
| **Invite** | Real ZXing QR code encoding invite URL, `TETHER-NODE-88X` referral code, native Android share sheet via `Intent.ACTION_SEND`, referral stats |
| **Onboarding** | Animated logo with pulsing rings, IMPORT EXISTING WALLET / CREATE NEW WALLET buttons triggering MWA flow, Devnet badge |

---

## ⚙️ Core Mechanics

### Background Proxy Engine
- **Port 8080** local HTTP CONNECT proxy runs as an Android `ForegroundService`
- Persistent notification with a **"Stop Node"** kill-switch button
- `START_STICKY` ensures the service restarts if killed by the OS

### Bandwidth Billing
- Traffic is metered in **10 MB chunks** ($0.001 USDC per chunk)
- `BandwidthTracker` accumulates bytes and fires billing events at each threshold
- All stats persisted to **Room Database** (`NodeStats`, `TrafficLog` entities)

### Real-Time VPS Polling
- `NetworkViewModel` polls `GET /node/status` every **10 seconds**
- Live latency, peer count, signal quality, and uptime displayed on Network screen

---

## 🔐 Solana Integration

| Component | Detail |
|-----------|--------|
| **Wallet Adapter** | Solana Mobile Wallet Adapter (MWA) v2.0.3 |
| **Cluster** | `devnet` |
| **RPC URL** | `https://api.devnet.solana.com` |
| **USDC Mint** | `4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU` (Devnet) |
| **Transaction Builder** | `SolanaTransactionBuilder` constructs SPL Token transfer instructions |
| **Oracle Endpoint** | `http://64.227.168.228:8000` (DigitalOcean) |

---

## 📂 Project Structure

```
tether/
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── res/values/
│       │   ├── strings.xml
│       │   └── themes.xml
│       └── java/com/tether/depin/
│           ├── TetherApplication.kt        # App + Notification Channel
│           ├── MainActivity.kt             # Entry + Permission Gate
│           ├── data/
│           │   ├── local/
│           │   │   ├── AppDatabase.kt      # Room DB (NodeStats, TrafficLog)
│           │   │   ├── NodeStatsDao.kt
│           │   │   ├── TrafficLogDao.kt
│           │   │   ├── NodeStats.kt
│           │   │   └── TrafficLog.kt
│           │   └── remote/
│           │       ├── ApiClient.kt        # Retrofit + Moshi (→ VPS Oracle)
│           │       └── MatchmakerApi.kt    # POST /node/register, GET /node/status
│           ├── repository/
│           │   └── NodeRepository.kt       # Single source of truth
│           ├── service/
│           │   ├── TetherNodeService.kt    # Foreground proxy service
│           │   ├── LocalProxyServer.kt     # HTTP CONNECT proxy (port 8080)
│           │   └── BandwidthTracker.kt     # 10MB chunk billing
│           ├── wallet/
│           │   ├── WalletConnectionManager.kt  # MWA Devnet flow
│           │   └── SolanaTransactionBuilder.kt # USDC transfer builder
│           └── ui/
│               ├── theme/TetherTheme.kt    # M3 dark theme + Inter font
│               ├── components/BottomNavBar.kt  # FAB → Invite screen
│               ├── navigation/TetherNavGraph.kt
│               ├── viewmodel/
│               │   ├── DashboardViewModel.kt
│               │   ├── NetworkViewModel.kt # VPS polling every 10s
│               │   ├── WalletViewModel.kt
│               │   └── SettingsViewModel.kt
│               └── screens/
│                   ├── DashboardScreen.kt
│                   ├── WalletScreen.kt     # Withdraw + Stake + Filter
│                   ├── NetworkScreen.kt    # Canvas geo-map + traffic feed
│                   ├── SettingsScreen.kt   # Functional toggles + dialogs
│                   ├── InviteScreen.kt     # ZXing QR + share sheet
│                   └── WalletOnboardingScreen.kt
├── build.gradle.kts                        # Root Gradle config
├── settings.gradle.kts
└── gradle/wrapper/
main.py                                     # FastAPI Matchmaker Oracle
```

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 1.9 |
| **UI** | Jetpack Compose (Material Design 3) |
| **Architecture** | MVVM + Clean Architecture |
| **Local DB** | Room Database + KSP |
| **Networking** | Retrofit2 + Moshi |
| **Concurrency** | Kotlin Coroutines + StateFlow |
| **Web3** | Solana Mobile Wallet Adapter 2.0.3 |
| **Location** | Google Play Services FusedLocation + Geocoder |
| **QR Code** | ZXing Core 3.5.3 |
| **Preferences** | Jetpack DataStore |
| **Permissions** | Accompanist Permissions |
| **Backend** | FastAPI + Uvicorn (Python) |
| **Build** | Gradle Kotlin DSL |

---

## 📄 License

MIT License. Built for the Solana DePIN Hackathon.
