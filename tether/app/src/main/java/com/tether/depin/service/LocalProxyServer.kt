package com.tether.depin.service

import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket

class LocalProxyServer(
    private val port: Int = 8080,
    private val onBytesTransferred: (Long) -> Unit = {}
) {

    private var serverSocket: ServerSocket? = null
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val isRunning: Boolean
        get() = serverSocket?.isClosed == false

    fun start() {
        serverJob = scope.launch {
            try {
                serverSocket = ServerSocket(port)
                while (isActive) {
                    val clientSocket = serverSocket?.accept() ?: break
                    launch { handleConnection(clientSocket) }
                }
            } catch (e: Exception) {
                if (isActive) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun stop() {
        serverJob?.cancel()
        try {
            serverSocket?.close()
        } catch (_: Exception) { }
        serverSocket = null
    }

    private suspend fun handleConnection(clientSocket: Socket) {
        withContext(Dispatchers.IO) {
            try {
                val input = clientSocket.getInputStream()
                val output = clientSocket.getOutputStream()

                // Read the HTTP request
                val requestBuffer = ByteArray(8192)
                val bytesRead = input.read(requestBuffer)
                if (bytesRead <= 0) {
                    clientSocket.close()
                    return@withContext
                }

                val request = String(requestBuffer, 0, bytesRead)
                val firstLine = request.lines().firstOrNull() ?: ""

                if (firstLine.startsWith("CONNECT")) {
                    // HTTPS tunnel (CONNECT method)
                    handleConnectTunnel(clientSocket, firstLine, input, output)
                } else {
                    // HTTP proxy
                    handleHttpProxy(clientSocket, request, firstLine, output)
                }

                onBytesTransferred(bytesRead.toLong())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { clientSocket.close() } catch (_: Exception) { }
            }
        }
    }

    private fun handleConnectTunnel(
        clientSocket: Socket,
        connectLine: String,
        clientIn: InputStream,
        clientOut: OutputStream
    ) {
        // Parse host:port from CONNECT host:port HTTP/1.1
        val parts = connectLine.split(" ")
        if (parts.size < 2) return

        val hostPort = parts[1].split(":")
        val host = hostPort[0]
        val port = hostPort.getOrNull(1)?.toIntOrNull() ?: 443

        try {
            val remoteSocket = Socket(host, port)
            // Send 200 Connection Established
            clientOut.write("HTTP/1.1 200 Connection Established\r\n\r\n".toByteArray())
            clientOut.flush()

            // Bidirectional relay
            val job1 = scope.launch { relay(clientIn, remoteSocket.getOutputStream()) }
            val job2 = scope.launch { relay(remoteSocket.getInputStream(), clientOut) }

            runBlocking {
                job1.join()
                job2.join()
            }

            remoteSocket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun handleHttpProxy(
        clientSocket: Socket,
        request: String,
        firstLine: String,
        clientOut: OutputStream
    ) {
        // Parse: GET http://host:port/path HTTP/1.1
        val parts = firstLine.split(" ")
        if (parts.size < 3) return

        try {
            val url = java.net.URL(parts[1])
            val host = url.host
            val port = if (url.port > 0) url.port else 80

            val remoteSocket = Socket(host, port)
            val remoteOut = remoteSocket.getOutputStream()

            // Rewrite the request line to relative path
            val path = if (url.path.isNullOrEmpty()) "/" else url.path +
                    (if (url.query != null) "?${url.query}" else "")
            val rewrittenFirstLine = "${parts[0]} $path ${parts[2]}"
            val rewrittenRequest = request.replaceFirst(firstLine, rewrittenFirstLine)

            remoteOut.write(rewrittenRequest.toByteArray())
            remoteOut.flush()

            // Relay response back
            val totalBytes = relay(remoteSocket.getInputStream(), clientOut)
            onBytesTransferred(totalBytes)

            remoteSocket.close()
        } catch (e: Exception) {
            val errorResponse = "HTTP/1.1 502 Bad Gateway\r\nContent-Length: 0\r\n\r\n"
            clientOut.write(errorResponse.toByteArray())
            clientOut.flush()
        }
    }

    private fun relay(input: InputStream, output: OutputStream): Long {
        var totalBytes = 0L
        try {
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
                output.flush()
                totalBytes += bytesRead
            }
        } catch (_: Exception) { }
        return totalBytes
    }
}
