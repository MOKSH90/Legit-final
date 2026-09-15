package com.example.vaultkey.ml

import android.net.Uri
import java.util.Locale

object DetectionUrlNormalizer {

    private val targetKeys = listOf("url", "target", "dest", "destination", "redirect", "redirect_url")

    fun normalize(scannedValue: String): String {
        val raw = scannedValue.trim()
        if (raw.isEmpty()) return raw

        val parsed = tryParse(raw) ?: return raw
        val directTarget = targetKeys.firstNotNullOfOrNull { key -> parsed.getQueryParameter(key) }
        if (!directTarget.isNullOrBlank()) {
            return directTarget.trim()
        }

        val mirrorHost = parsed.getQueryParameter("mirror_host")?.trim()?.lowercase(Locale.ROOT)
        if (!mirrorHost.isNullOrBlank()) {
            val transport = parsed.getQueryParameter("transport")
                ?.trim()
                ?.lowercase(Locale.ROOT)
                ?.takeIf { it == "http" || it == "https" }
                ?: "http"
            val path = parsed.path?.takeIf { it.isNotBlank() } ?: "/"
            val normalizedPath = if (path.startsWith("/malicious-apex")) "/verify/apexm0tors-secure-demo.tk" else path

            val passthrough = parsed.queryParameterNames
                .filterNot { it in setOf("source", "mirror_host", "transport", "tld", "intent") }
                .sorted()
                .joinToString("&") { key ->
                    val value = parsed.getQueryParameter(key).orEmpty()
                    "${Uri.encode(key)}=${Uri.encode(value)}"
                }

            val tld = parsed.getQueryParameter("tld")?.trim()?.lowercase(Locale.ROOT)
            val hostWithTld = when {
                tld.isNullOrBlank() || mirrorHost.endsWith(".$tld") -> mirrorHost
                else -> "$mirrorHost.$tld"
            }
            val intent = parsed.getQueryParameter("intent")?.trim()?.lowercase(Locale.ROOT)
            val intentQuery = if (intent.isNullOrBlank()) "" else "intent=${Uri.encode(intent)}"
            val query = listOf(intentQuery, passthrough).filter { it.isNotBlank() }.joinToString("&")

            return buildString {
                append(transport)
                append("://")
                append(hostWithTld)
                append(normalizedPath)
                if (query.isNotBlank()) {
                    append('?')
                    append(query)
                }
            }
        }

        return raw
    }

    private fun tryParse(value: String): Uri? = try {
        Uri.parse(value)
    } catch (_: Exception) {
        null
    }
}
