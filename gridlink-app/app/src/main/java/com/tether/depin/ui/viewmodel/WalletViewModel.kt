package com.tether.depin.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tether.depin.TetherApplication
import com.tether.depin.data.local.TrafficLog
import com.tether.depin.data.remote.ApiClient
import com.tether.depin.data.repository.NodeRepository
import com.tether.depin.wallet.SolanaRpcClient
import com.tether.depin.wallet.WalletConnectionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WalletViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = NodeRepository(
        database = (application as TetherApplication).database,
        api = ApiClient.matchmakerApi
    )

    val walletState = WalletConnectionManager.walletState

    val transactionHistory: StateFlow<List<TrafficLog>> = repository.trafficLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _airdropState = MutableStateFlow<AirdropState>(AirdropState.Idle)
    val airdropState: StateFlow<AirdropState> = _airdropState.asStateFlow()

    private val _settlementResult = MutableStateFlow<SettlementResult?>(null)
    val settlementResult: StateFlow<SettlementResult?> = _settlementResult.asStateFlow()

    private val _liveSolBalance = MutableStateFlow<Double?>(null)
    val liveSolBalance: StateFlow<Double?> = _liveSolBalance.asStateFlow()

    init {
        try {
            WalletConnectionManager.initialize(application)
            // Auto-fetch real balance on startup
            refreshBalance()
        } catch (e: Exception) {
            Log.e("WalletVM", "init failed: ${e.message}", e)
        }
    }

    fun requestAirdrop() {
        val pubKey = try { WalletConnectionManager.getStoredPublicKey() } catch (_: Exception) { null }
            ?: WalletConnectionManager.walletState.value.publicKey.ifBlank { null }

        if (pubKey.isNullOrBlank()) {
            _airdropState.value = AirdropState.Error("No wallet found")
            return
        }

        Log.d("WalletVM", "Requesting airdrop for: $pubKey")
        _airdropState.value = AirdropState.Loading
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SolanaRpcClient.requestAirdrop(pubKey)
                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        _airdropState.value = AirdropState.Success(result.getOrThrow())
                    } else {
                        // SHOW THE REAL ERROR — no masking
                        val realError = result.exceptionOrNull()?.message ?: "Airdrop failed (unknown)"
                        Log.e("WalletVM", "Airdrop failed: $realError")
                        _airdropState.value = AirdropState.Error("Airdrop: $realError")
                    }
                }
                // Poll balance multiple times for reactive UI
                if (result.isSuccess) {
                    for (i in 1..3) {
                        kotlinx.coroutines.delay(2000)
                        refreshBalance()
                    }
                }
            } catch (e: Exception) {
                Log.e("WalletVM", "Airdrop exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    _airdropState.value = AirdropState.Error(
                        "Airdrop: ${e.message ?: "Connection error"}"
                    )
                }
            }
        }
    }

    fun refreshBalance() {
        val pubKey = try { WalletConnectionManager.getStoredPublicKey() } catch (_: Exception) { null }
            ?: WalletConnectionManager.walletState.value.publicKey.ifBlank { null }
        if (pubKey.isNullOrBlank()) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val result = SolanaRpcClient.getBalance(pubKey)
                if (result.isSuccess) {
                    val bal = result.getOrThrow()
                    Log.d("WalletVM", "Balance refreshed: $bal SOL")
                    withContext(Dispatchers.Main) {
                        _liveSolBalance.value = bal
                        // Also sync into walletState so both sources agree
                        WalletConnectionManager.updateBalance(bal)
                    }
                }
            } catch (e: Exception) {
                Log.e("WalletVM", "Balance refresh failed: ${e.message}")
            }
        }
    }

    fun executeWithdrawal() {
        val pubKey = WalletConnectionManager.getStoredPublicKey()
        if (pubKey.isNullOrBlank()) {
            _settlementResult.value = SettlementResult(false, error = "No wallet connected")
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Real on-chain SOL transfer (self-transfer as proof-of-transaction)
                val result = WalletConnectionManager.signAndSendSolTransfer(
                    amountSol = 0.001,
                    recipientBase58 = pubKey
                )

                withContext(Dispatchers.Main) {
                    if (result.isSuccess) {
                        val txSig = result.getOrThrow()
                        _settlementResult.value = SettlementResult(
                            success = true,
                            txSignature = txSig,
                            explorerUrl = "https://explorer.solana.com/tx/$txSig?cluster=devnet"
                        )
                    } else {
                        _settlementResult.value = SettlementResult(
                            false,
                            error = result.exceptionOrNull()?.localizedMessage ?: "Transaction failed"
                        )
                    }
                }

                // Refresh balance after tx confirms
                kotlinx.coroutines.delay(3000)
                refreshBalance()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _settlementResult.value = SettlementResult(false, error = e.localizedMessage ?: "TX failed")
                }
            }
        }
    }

    fun resetAirdropState() { _airdropState.value = AirdropState.Idle }
    fun resetSettlementResult() { _settlementResult.value = null }

    sealed class AirdropState {
        object Idle : AirdropState()
        object Loading : AirdropState()
        data class Success(val txSignature: String) : AirdropState()
        data class Error(val message: String) : AirdropState()
    }

    data class SettlementResult(
        val success: Boolean,
        val txSignature: String = "",
        val explorerUrl: String = "",
        val error: String = ""
    )
}
