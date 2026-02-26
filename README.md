# 🌐 GridLink Node

**Share your bandwidth. Earn USDC. Run a real DePIN node from your pocket.**

GridLink is a decentralized physical infrastructure network (DePIN) that turns Android devices into bandwidth-sharing nodes. Earn micropayments for every megabyte you route, settled on-chain via Solana Devnet.

---

## 🚀 Quick Start

### Prerequisites

| Tool | Version |
|------|---------|
| Android Studio | Hedgehog+ |
| JDK | 17 |
| Python | 3.10+ |
| ADB | Latest |

### Build & Run

```bash
# Android app
cd tether
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk

# FastAPI Matchmaker Oracle
pip install fastapi uvicorn pydantic
uvicorn main:app --host 0.0.0.0 --port 8000
```

---

## 📱 App Screens

| Screen | What It Does |
|--------|-------------|
| **Dashboard** | Live bandwidth dial, USDC earnings counter, recent traffic feed |
| **Wallet** | Embedded Ed25519 keystore, SOL balance, airdrop, withdraw/stake with Explorer links |
| **Network** | Globe visualization, active peers, latency, signal quality |
| **Settings** | Private key export, data limits, background service toggle |
| **Invite** | Referral QR code generation & sharing |

---

## ⚙️ Core Mechanics

```
┌─────────────┐      HTTP Proxy       ┌──────────────────┐
│  Internet    │◄────────────────────► │  LocalProxyServer │
│  Traffic     │      Port 8080        │  (Netty)          │
└─────────────┘                        └────────┬─────────┘
                                                │
                                       BandwidthTracker
                                                │
                                    ┌───────────▼──────────┐
                                    │   Room DB (SQLite)    │
                                    │  • NodeStats (agg)    │
                                    │  • TrafficLog (feed)  │
                                    └───────────┬──────────┘
                                                │
                                        StateFlow → UI
                                                │
                              ┌─────────────────▼────────────────┐
                              │        Jetpack Compose UI         │
                              │  Dashboard • Wallet • Network     │
                              └──────────────────────────────────┘
```

- **TetherNodeService** — Foreground service managing the proxy and bandwidth tracking
- **BandwidthTracker** — Monitors bytes routed, calculates USDC earnings at `$0.003/MB`
- **Room DB** — Persists aggregate stats (`NodeStats`) and per-session traffic logs (`TrafficLog`)
- **FastAPI Matchmaker** — Lightweight oracle for node registration and network status polling

---

## 🔐 Solana Integration

| Component | Details |
|-----------|---------|
| **Keypair** | Ed25519 via BouncyCastle, stored in `EncryptedSharedPreferences` |
| **Encoding** | Native Base58 encode/decode (no external lib) |
| **RPC Client** | OkHttp direct JSON-RPC to `api.devnet.solana.com` |
| **Airdrop** | 1 SOL Devnet faucet via `requestAirdrop` RPC |
| **Balance** | Live on-chain balance via `getBalance` RPC |
| **Settlement** | Withdrawal flow with blockhash fetch → Explorer link |
| **Key Backup** | Persistent modal dialog — user must acknowledge before proceeding |

---

## 📂 Project Structure

```
monolith/
├── main.py                          # FastAPI Matchmaker Oracle
├── README.md
└── tether/                          # Android App
    ├── build.gradle.kts
    └── app/src/main/java/com/tether/depin/
        ├── MainActivity.kt
        ├── TetherApplication.kt
        ├── data/
        │   ├── local/
        │   │   ├── AppDatabase.kt       # Room DB definition
        │   │   ├── NodeStats.kt         # Aggregate bandwidth entity
        │   │   ├── NodeStatsDao.kt
        │   │   ├── TrafficLog.kt        # Per-session traffic entity
        │   │   └── TrafficLogDao.kt
        │   ├── remote/
        │   │   ├── ApiClient.kt         # Retrofit + OkHttp setup
        │   │   └── MatchmakerApi.kt     # /node/register, /node/status
        │   └── repository/
        │       └── NodeRepository.kt    # Data layer coordinator
        ├── service/
        │   ├── BandwidthTracker.kt      # Bytes → USDC conversion
        │   ├── LocalProxyServer.kt      # HTTP proxy on :8080
        │   └── TetherNodeService.kt     # Foreground service
        ├── wallet/
        │   ├── WalletConnectionManager.kt  # Singleton Ed25519 keystore
        │   ├── SolanaRpcClient.kt          # Native Devnet JSON-RPC
        │   └── SolanaTransactionBuilder.kt # TX construction
        ├── ui/
        │   ├── components/
        │   │   └── BottomNavBar.kt
        │   ├── navigation/
        │   │   └── TetherNavGraph.kt
        │   ├── screens/
        │   │   ├── DashboardScreen.kt
        │   │   ├── WalletScreen.kt
        │   │   ├── WalletOnboardingScreen.kt
        │   │   ├── NetworkScreen.kt
        │   │   ├── SettingsScreen.kt
        │   │   └── InviteScreen.kt
        │   ├── viewmodel/
        │   │   ├── DashboardViewModel.kt
        │   │   ├── WalletViewModel.kt
        │   │   ├── NetworkViewModel.kt
        │   │   └── SettingsViewModel.kt
        │   └── theme/
        │       └── TetherTheme.kt
        └── AndroidManifest.xml
```

---

## 🛠 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Language** | Kotlin 1.9, Python 3.10 |
| **UI Framework** | Jetpack Compose (BOM 2024.06) + Material3 |
| **Navigation** | Compose Navigation 2.7 |
| **Database** | Room 2.6 (SQLite) |
| **Networking** | Retrofit 2.9, OkHttp 4.12, Moshi |
| **Crypto** | BouncyCastle Ed25519, EncryptedSharedPreferences |
| **Blockchain** | Solana Devnet JSON-RPC, Base58, SPL Token |
| **Backend** | FastAPI + Uvicorn |
| **QR Codes** | ZXing 3.5 |
| **Permissions** | Accompanist 0.34 |
| **Location** | Google Play Services Location 21.1 |
| **Data** | DataStore Preferences, Kotlin Coroutines + Flow |
| **Build** | Gradle KTS, KSP |

---

## 📄 License

# MIT Licnse
Built by **Danyal** — GridLink DePIN, Monolith 2026.
