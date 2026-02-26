// Author: Danyal | GridLink DePIN - Monolith 2026
package com.tether.depin.wallet

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import java.security.SecureRandom

// Singleton embedded Solana wallet — Ed25519 keypairs in EncryptedSharedPreferences.
// Single source of truth for wallet state across all ViewModels.
object WalletConnectionManager {

    const val SOLANA_RPC_URL = "https://api.devnet.solana.com"
    const val SOLANA_CLUSTER = "devnet"
    private const val PREF_PRIVATE_KEY = "solana_private_key"
    private const val PREF_PUBLIC_KEY = "solana_public_key"

    private val BASE58_ALPHABET = "123456789ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz"

    fun encodeBase58(bytes: ByteArray): String {
        val bi = java.math.BigInteger(1, bytes)
        val sb = StringBuilder()
        var num = bi
        while (num > java.math.BigInteger.ZERO) {
            val divmod = num.divideAndRemainder(java.math.BigInteger.valueOf(58))
            sb.append(BASE58_ALPHABET[divmod[1].toInt()])
            num = divmod[0]
        }
        for (b in bytes) {
            if (b.toInt() == 0) sb.append('1') else break
        }
        return sb.reverse().toString()
    }

    fun decodeBase58(input: String): ByteArray {
        var bi = java.math.BigInteger.ZERO
        for (c in input) {
            val idx = BASE58_ALPHABET.indexOf(c)
            if (idx < 0) throw IllegalArgumentException("Invalid Base58 character: $c")
            bi = bi.multiply(java.math.BigInteger.valueOf(58)).add(java.math.BigInteger.valueOf(idx.toLong()))
        }
        val bytes = bi.toByteArray()
        val stripped = if (bytes.size > 1 && bytes[0].toInt() == 0) bytes.copyOfRange(1, bytes.size) else bytes
        val leadingZeros = input.takeWhile { it == '1' }.length
        return ByteArray(leadingZeros) + stripped
    }

    private val _walletState = MutableStateFlow(WalletState())
    val walletState: StateFlow<WalletState> = _walletState.asStateFlow()

    private val _newlyCreatedKey = MutableStateFlow<CreatedKeyInfo?>(null)
    val newlyCreatedKey: StateFlow<CreatedKeyInfo?> = _newlyCreatedKey.asStateFlow()

    data class CreatedKeyInfo(val publicKeyBase58: String, val privateKeyBase58: String)

    private var encryptedPrefs: SharedPreferences? = null
    private var initialized = false

    data class WalletState(
        val isConnected: Boolean = false,
        val publicKey: String = "",
        val privateKeyBase58: String = "",
        val balanceUsdc: Double = 12.50,
        val balanceSol: Double = 0.14,
        val label: String = "GridLink Embedded Wallet",
        val cluster: String = SOLANA_CLUSTER,
        val rpcUrl: String = SOLANA_RPC_URL
    )

    fun initialize(context: Context) {
        if (initialized && encryptedPrefs != null) return
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        encryptedPrefs = EncryptedSharedPreferences.create(
            "gridlink_wallet_secure",
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        initialized = true
        loadWalletOnStartup()
    }

    fun createNewWallet(): String {
        val generator = Ed25519KeyPairGenerator()
        generator.init(Ed25519KeyGenerationParameters(SecureRandom()))
        val keyPair = generator.generateKeyPair()

        val privateParams = keyPair.private as Ed25519PrivateKeyParameters
        val publicParams = keyPair.public as Ed25519PublicKeyParameters

        // Solana 64-byte secret = 32-byte seed ++ 32-byte pubkey
        val secretBytes = privateParams.encoded + publicParams.encoded
        val publicBase58 = encodeBase58(publicParams.encoded)
        val secretBase58 = encodeBase58(secretBytes)

        encryptedPrefs?.edit()
            ?.putString(PREF_PRIVATE_KEY, secretBase58)
            ?.putString(PREF_PUBLIC_KEY, publicBase58)
            ?.apply()

        _walletState.value = WalletState(
            isConnected = true,
            publicKey = publicBase58,
            privateKeyBase58 = secretBase58,
            balanceUsdc = 0.0,
            balanceSol = 0.0
        )

        _newlyCreatedKey.value = CreatedKeyInfo(publicBase58, secretBase58)
        return secretBase58
    }

    fun importWallet(privateKeyBase58: String): Result<String> {
        return try {
            val keyBytes = decodeBase58(privateKeyBase58.trim())

            val publicBase58: String
            if (keyBytes.size == 64) {
                publicBase58 = encodeBase58(keyBytes.copyOfRange(32, 64))
            } else if (keyBytes.size == 32) {
                val privateParams = Ed25519PrivateKeyParameters(keyBytes, 0)
                publicBase58 = encodeBase58(privateParams.generatePublicKey().encoded)
            } else {
                return Result.failure(IllegalArgumentException("Invalid key length: ${keyBytes.size} bytes"))
            }

            encryptedPrefs?.edit()
                ?.putString(PREF_PRIVATE_KEY, privateKeyBase58.trim())
                ?.putString(PREF_PUBLIC_KEY, publicBase58)
                ?.apply()

            _walletState.value = WalletState(
                isConnected = true,
                publicKey = publicBase58,
                privateKeyBase58 = privateKeyBase58.trim(),
                balanceUsdc = 12.50,
                balanceSol = 0.14
            )

            Result.success(publicBase58)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getStoredPublicKey(): String? {
        return encryptedPrefs?.getString(PREF_PUBLIC_KEY, null)
            ?: _walletState.value.publicKey.ifBlank { null }
    }

    private fun loadWalletOnStartup() {
        val storedKey = encryptedPrefs?.getString(PREF_PRIVATE_KEY, null)
        val storedPubKey = encryptedPrefs?.getString(PREF_PUBLIC_KEY, null)

        if (storedKey != null && storedPubKey != null) {
            _walletState.value = WalletState(
                isConnected = true,
                publicKey = storedPubKey,
                privateKeyBase58 = storedKey,
                balanceUsdc = 12.50,
                balanceSol = 0.14
            )
        }
    }

    fun disconnectWallet() {
        encryptedPrefs?.edit()?.clear()?.apply()
        _walletState.value = WalletState()
        _newlyCreatedKey.value = null
    }

    fun acknowledgeKeyBackup() {
        _newlyCreatedKey.value = null
    }

    suspend fun signTransaction(amountUsdc: Double, recipientAddress: String): Result<String> {
        return try {
            val mockSignature = "5Tz${System.currentTimeMillis()}...devnet"
            Result.success(mockSignature)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
