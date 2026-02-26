package com.tether.depin.wallet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

// Native JSON-RPC client for Solana Devnet operations via OkHttp.
object SolanaRpcClient {

    private const val RPC_URL = "https://api.devnet.solana.com"
    private val JSON_MEDIA = "application/json".toMediaType()

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun requestAirdrop(publicKeyBase58: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "requestAirdrop")
                put("params", org.json.JSONArray().apply {
                    put(publicKeyBase58)
                    put(1_000_000_000) // 1 SOL
                })
            }

            val request = Request.Builder()
                .url(RPC_URL)
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val json = JSONObject(responseBody)

            if (json.has("error")) {
                Result.failure<String>(Exception(json.getJSONObject("error").getString("message")))
            } else {
                Result.success(json.getString("result"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBalance(publicKeyBase58: String): Result<Double> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getBalance")
                put("params", org.json.JSONArray().apply {
                    put(publicKeyBase58)
                })
            }

            val request = Request.Builder()
                .url(RPC_URL)
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val json = JSONObject(responseBody)

            if (json.has("error")) {
                Result.failure<Double>(Exception(json.getJSONObject("error").getString("message")))
            } else {
                val lamports = json.getJSONObject("result").getLong("value")
                Result.success(lamports.toDouble() / 1_000_000_000.0)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRecentBlockhash(): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "getLatestBlockhash")
                put("params", org.json.JSONArray().apply {
                    put(JSONObject().apply { put("commitment", "finalized") })
                })
            }

            val request = Request.Builder()
                .url(RPC_URL)
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val json = JSONObject(responseBody)

            if (json.has("error")) {
                Result.failure<String>(Exception(json.getJSONObject("error").getString("message")))
            } else {
                val blockhash = json.getJSONObject("result")
                    .getJSONObject("value")
                    .getString("blockhash")
                Result.success(blockhash)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendTransaction(signedTxBase64: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("jsonrpc", "2.0")
                put("id", 1)
                put("method", "sendTransaction")
                put("params", org.json.JSONArray().apply {
                    put(signedTxBase64)
                    put(JSONObject().apply {
                        put("encoding", "base64")
                        put("preflightCommitment", "confirmed")
                    })
                })
            }

            val request = Request.Builder()
                .url(RPC_URL)
                .post(body.toString().toRequestBody(JSON_MEDIA))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string() ?: throw Exception("Empty response")
            val json = JSONObject(responseBody)

            if (json.has("error")) {
                Result.failure<String>(Exception(json.getJSONObject("error").getString("message")))
            } else {
                Result.success(json.getString("result"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
