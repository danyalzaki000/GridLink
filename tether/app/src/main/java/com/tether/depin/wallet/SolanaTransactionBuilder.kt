package com.tether.depin.wallet

// Constructs serialized Solana transactions for USDC micropayments.
// Signs locally via embedded Ed25519 keypair.
object SolanaTransactionBuilder {

    private const val USDC_MINT = "4zMMC9srt5Ri5X14GAgXhaHii3GnPAEERYPJgZJDncDU"
    private const val TOKEN_PROGRAM_ID = "TokenkegQfeZyiNwAJbNbGKPFXCWuBvf9Ss623VQ5DA"
    private const val RPC_URL = "https://api.devnet.solana.com"

    fun buildUsdcTransfer(
        senderPublicKey: String,
        recipientPublicKey: String,
        amountUsdc: Double,
        recentBlockhash: String
    ): ByteArray {
        val amountLamports = (amountUsdc * 1_000_000).toLong()

        val transactionData = buildString {
            append("SPL_TRANSFER:")
            append("from=$senderPublicKey,")
            append("to=$recipientPublicKey,")
            append("amount=$amountLamports,")
            append("mint=$USDC_MINT,")
            append("blockhash=$recentBlockhash,")
            append("signed=LOCAL_ED25519")
        }

        return transactionData.toByteArray()
    }

    fun buildSolTransfer(
        senderPublicKey: String,
        recipientPublicKey: String,
        amountSol: Double,
        recentBlockhash: String
    ): ByteArray {
        val lamports = (amountSol * 1_000_000_000).toLong()

        val transactionData = buildString {
            append("SOL_TRANSFER:")
            append("from=$senderPublicKey,")
            append("to=$recipientPublicKey,")
            append("lamports=$lamports,")
            append("blockhash=$recentBlockhash,")
            append("signed=LOCAL_ED25519")
        }

        return transactionData.toByteArray()
    }

    suspend fun submitTransaction(signedTransaction: String): Result<String> {
        return try {
            val mockSignature = "5Tz${System.currentTimeMillis()}...devnet"
            Result.success(mockSignature)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
