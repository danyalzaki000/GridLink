// Author: Danyal | GridLink DePIN - Monolith 2026
package com.tether.depin.wallet

import android.util.Base64
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

// Builds and signs real Solana transactions for on-chain devnet submission.
// Uses native binary serialization — no external Solana SDK required.
object SolanaTransactionBuilder {

    // System Program ID = 32 zero bytes ("11111111111111111111111111111111" in base58)
    private val SYSTEM_PROGRAM_ID = ByteArray(32)

    /**
     * Encodes a value as Solana's compact-u16 (short-u16) variable-length integer.
     * Most values in our case are < 128, so they fit in a single byte.
     */
    private fun encodeCompactU16(value: Int): ByteArray {
        return when {
            value < 0x80 -> byteArrayOf(value.toByte())
            value < 0x4000 -> byteArrayOf(
                ((value and 0x7F) or 0x80).toByte(),
                ((value shr 7) and 0x7F).toByte()
            )
            else -> byteArrayOf(
                ((value and 0x7F) or 0x80).toByte(),
                (((value shr 7) and 0x7F) or 0x80).toByte(),
                ((value shr 14) and 0x03).toByte()
            )
        }
    }

    /**
     * Builds a SOL transfer transaction message (unsigned).
     *
     * Account layout:
     *   [0] sender   — signer, writable
     *   [1] recipient — writable
     *   [2] System Program — readonly, unsigned
     *
     * Instruction: SystemProgram.Transfer (index 2), data = u32 LE (type=2) + u64 LE (lamports)
     *
     * @param senderPubkey    32-byte Ed25519 public key of the sender
     * @param recipientPubkey 32-byte Ed25519 public key of the recipient
     * @param lamports        Amount to transfer in lamports (1 SOL = 1_000_000_000 lamports)
     * @param recentBlockhash 32-byte recent blockhash
     * @return The serialized message bytes ready for signing
     */
    fun buildSolTransferMessage(
        senderPubkey: ByteArray,
        recipientPubkey: ByteArray,
        lamports: Long,
        recentBlockhash: ByteArray
    ): ByteArray {
        require(senderPubkey.size == 32) { "Sender pubkey must be 32 bytes" }
        require(recipientPubkey.size == 32) { "Recipient pubkey must be 32 bytes" }
        require(recentBlockhash.size == 32) { "Blockhash must be 32 bytes" }
        require(lamports > 0) { "Lamports must be positive" }

        val isSelfTransfer = senderPubkey.contentEquals(recipientPubkey)
        val out = ByteArrayOutputStream()

        if (isSelfTransfer) {
            // Self-transfer: only 2 unique accounts [sender, system_program]
            out.write(1)  // num_required_signatures
            out.write(0)  // num_readonly_signed_accounts
            out.write(1)  // num_readonly_unsigned_accounts (System Program)

            out.write(encodeCompactU16(2))   // 2 accounts
            out.write(senderPubkey)           // [0] sender/recipient (signer + writable)
            out.write(SYSTEM_PROGRAM_ID)      // [1] System Program (readonly)

            out.write(recentBlockhash)

            out.write(encodeCompactU16(1))   // 1 instruction
            out.write(1)  // program_id_index: account[1] = System Program

            out.write(encodeCompactU16(2))   // 2 account references
            out.write(0)                      // source = account[0]
            out.write(0)                      // destination = account[0] (same)
        } else {
            // Normal transfer: 3 unique accounts [sender, recipient, system_program]
            out.write(1)  // num_required_signatures
            out.write(0)  // num_readonly_signed_accounts
            out.write(1)  // num_readonly_unsigned_accounts (System Program)

            out.write(encodeCompactU16(3))   // 3 accounts
            out.write(senderPubkey)           // [0] sender (signer + writable)
            out.write(recipientPubkey)        // [1] recipient (writable)
            out.write(SYSTEM_PROGRAM_ID)      // [2] System Program (readonly)

            out.write(recentBlockhash)

            out.write(encodeCompactU16(1))   // 1 instruction
            out.write(2)  // program_id_index: account[2] = System Program

            out.write(encodeCompactU16(2))   // 2 account references
            out.write(0)                      // source = account[0]
            out.write(1)                      // destination = account[1]
        }

        // Instruction data: u32 LE (type=2 for Transfer) + u64 LE (lamports)
        val instructionData = ByteBuffer.allocate(12)
            .order(ByteOrder.LITTLE_ENDIAN)
            .putInt(2)             // Transfer instruction discriminator
            .putLong(lamports)     // amount in lamports
            .array()

        out.write(encodeCompactU16(instructionData.size))
        out.write(instructionData)

        return out.toByteArray()
    }

    /**
     * Signs a serialized message with Ed25519 and assembles the complete transaction.
     *
     * Transaction wire format:
     *   [compact-u16: num_signatures] [signature_0: 64 bytes] [message bytes]
     *
     * @param messageBytes   The unsigned message from buildSolTransferMessage()
     * @param privateKeySeed 32-byte Ed25519 private key seed
     * @return Base64-encoded signed transaction ready for sendTransaction RPC
     */
    fun signAndSerialize(
        messageBytes: ByteArray,
        privateKeySeed: ByteArray
    ): String {
        require(privateKeySeed.size == 32) { "Private key seed must be 32 bytes" }

        // Ed25519 sign the message
        val privateKey = Ed25519PrivateKeyParameters(privateKeySeed, 0)
        val signer = Ed25519Signer()
        signer.init(true, privateKey)
        signer.update(messageBytes, 0, messageBytes.size)
        val signature = signer.generateSignature()  // 64 bytes

        // Assemble full transaction
        val out = ByteArrayOutputStream()
        out.write(encodeCompactU16(1))   // 1 signature
        out.write(signature)              // 64-byte Ed25519 signature
        out.write(messageBytes)           // message

        return Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
    }
}
