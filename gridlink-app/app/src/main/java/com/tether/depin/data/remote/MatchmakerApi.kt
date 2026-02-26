package com.tether.depin.data.remote

import com.squareup.moshi.JsonClass
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

@JsonClass(generateAdapter = true)
data class RegisterRequest(
    val ipAddress: String,
    val walletAddress: String,
    val port: Int = 8080
)

@JsonClass(generateAdapter = true)
data class RegisterResponse(
    val nodeId: String = "",
    val status: String = ""
)

@JsonClass(generateAdapter = true)
data class NodeStatusResponse(
    val isActive: Boolean = false,
    val connectedPeers: Int = 0,
    val latencyMs: Int = 0
)

interface MatchmakerApi {

    @POST("/node/register")
    suspend fun registerNode(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("/node/status")
    suspend fun getNodeStatus(): Response<NodeStatusResponse>
}
