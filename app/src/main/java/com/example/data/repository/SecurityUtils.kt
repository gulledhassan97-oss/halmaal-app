package com.example.data.repository

import java.security.MessageDigest
import java.security.SecureRandom

object SecurityUtils {
    private val secureRandom = SecureRandom()

    fun generateSixDigitCode(): String {
        val code = 100000 + secureRandom.nextInt(900000)
        return code.toString()
    }

    fun generateSifaloOrderId(): String {
        val rand = 1000 + secureRandom.nextInt(9000)
        return "ORD-${System.currentTimeMillis() / 1000L}-$rand"
    }

    fun hashSecret(raw: String): String {
        val saltBytes = ByteArray(16)
        secureRandom.nextBytes(saltBytes)
        val saltHex = saltBytes.joinToString("") { "%02x".format(it) }
        val digest = sha256("$saltHex:$raw")
        return "$saltHex:$digest"
    }

    fun verifySecret(raw: String, storedHash: String): Boolean {
        val parts = storedHash.split(":")
        if (parts.size != 2) return false
        val salt = parts[0]
        val expectedDigest = parts[1]
        val actualDigest = sha256("$salt:$raw")
        return MessageDigest.isEqual(
            expectedDigest.toByteArray(Charsets.UTF_8),
            actualDigest.toByteArray(Charsets.UTF_8)
        )
    }

    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.contains("@") && trimmed.contains(".") && trimmed.length >= 5
    }

    /**
     * Standardizes customer wallet numbers for Sifalo Pay:
     * strips non-digits and leading zeros, and ensures the 252 country prefix.
     */
    fun standardizeSomaliPhone(phone: String): String {
        var digits = phone.filter { it.isDigit() }
        if (digits.startsWith("00")) {
            digits = digits.drop(2)
        }
        if (digits.startsWith("2520")) {
            digits = "252" + digits.drop(4)
        }
        if (digits.startsWith("0")) {
            digits = digits.drop(1)
        }
        if (digits.length == 9 && !digits.startsWith("252")) {
            digits = "252$digits"
        }
        return digits
    }

    fun isValidSomaliMobilePhone(phone: String): Boolean {
        val standardized = standardizeSomaliPhone(phone)
        return standardized.startsWith("252") && standardized.length in 11..13
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
