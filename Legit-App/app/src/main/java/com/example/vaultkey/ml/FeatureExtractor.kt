package com.example.vaultkey.ml

import java.net.URI
import java.util.Locale
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

class FeatureExtractor {
    private val suspiciousTlds = setOf(
        "xyz", "top", "club", "work", "click", "link", "loan", "win", "bid",
        "download", "racing", "review", "cricket", "party", "gq", "ml", "cf",
        "tk", "ga", "icu", "buzz", "rest", "cyou", "monster", "surf"
    )
    private val misleadingTlds = setOf("cm", "co", "om", "vip", "info")
    private val urlShorteners = setOf(
        "bit.ly", "tinyurl.com", "goo.gl", "t.co", "ow.ly", "is.gd", "buff.ly",
        "rebrand.ly", "cutt.ly", "shorte.st", "tiny.cc", "rb.gy", "s.id"
    )
    private val suspiciousFileExtensions = setOf(
        "exe", "scr", "bat", "cmd", "com", "pif", "vbs", "js", "jar", "msi",
        "apk", "dmg", "zip", "rar", "7z", "ps1"
    )
    private val knownBrands = setOf(
        "paypal", "amazon", "apple", "microsoft", "google", "facebook", "netflix",
        "bankofamerica", "wellsfargo", "chase", "citibank", "americanexpress",
        "instagram", "linkedin", "twitter", "ebay", "dropbox", "adobe", "docusign",
        "coinbase", "binance", "outlook", "office365", "icloud", "steam", "spotify"
    )
    private val loginKeywords = setOf("login", "signin", "log-in", "sign-in")
    private val secureKeywords = setOf("secure", "security", "protected", "safe")
    private val accountKeywords = setOf("account", "acct", "profile")
    private val updateKeywords = setOf("update", "renew", "restore")
    private val verifyKeywords = setOf("verify", "verification", "confirm", "validate")
    private val bankKeywords = setOf("bank", "banking", "wallet", "billing", "payment")
    private val structuralChars = setOf(':', '/', '.', '-', '_', '?', '&', '=', '%', '#', '@', '~', '+')
    private val multiPartSuffixes = setOf(
        "co.uk", "org.uk", "ac.uk", "gov.uk", "co.in", "firm.in", "net.in",
        "org.in", "com.au", "net.au", "org.au", "co.jp", "com.br", "com.cn"
    )
    private val hexEncodedRegex = Regex("%[0-9a-fA-F]{2}")
    private val nestedUrlRegex = Regex("(url|redirect|next|dest|continue)=https?", RegexOption.IGNORE_CASE)
    private val homoglyphRegex = Regex("[\\u0400-\\u04FF\\u0370-\\u03FF]")

    fun extract(urlStr: String): FloatArray {
        val url = urlStr.filter { it.isDefined() || it.isWhitespace() }
        val uri = parseUri(url)
        val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
        val hostname = uri.host?.lowercase(Locale.ROOT).orEmpty()
        val path = uri.rawPath.orEmpty()
        val query = uri.rawQuery.orEmpty()
        val fragment = uri.rawFragment.orEmpty()
        val domainParts = splitDomain(hostname)
        val registeredDomain = domainParts.registeredDomain
        val subdomain = domainParts.subdomain
        val tld = domainParts.tld
        val subdomainLabels = subdomain.split('.').filter { it.isNotEmpty() }
        val pathSegments = path.split('/').filter { it.isNotEmpty() }
        val queryParams = if (query.isBlank()) emptyList() else query.split('&')
        val hasPort = uri.port != -1
        val portIsStandard = !hasPort || uri.port == 80 || uri.port == 443
        val urlLower = url.lowercase(Locale.ROOT)

        val features = FloatArray(41)
        features[0] = url.length.toFloat()
        features[1] = url.count { it == '.' }.toFloat()
        features[2] = url.count { it == '-' }.toFloat()
        features[3] = url.count { it == '_' }.toFloat()
        features[4] = url.count { it.isDigit() }.toFloat()
        features[5] = url.count { !it.isLetterOrDigit() && it !in structuralChars }.toFloat()
        features[6] = if ("@" in url) 1.0f else 0.0f
        features[7] = pathSegments.size.toFloat()
        features[8] = queryParams.size.toFloat()
        features[9] = if (fragment.isNotEmpty()) 1.0f else 0.0f
        features[10] = shannonEntropy(url)
        features[11] = maxConsecutiveConsonants(url).toFloat()
        features[12] = hostname.length.toFloat()
        features[13] = subdomainLabels.size.toFloat()
        features[14] = if (isIpAddress(hostname)) 1.0f else 0.0f
        features[15] = if (urlShorteners.any { hostname == it || hostname.endsWith(".$it") }) 1.0f else 0.0f
        features[16] = if (tld in suspiciousTlds) 1.0f else 0.0f
        features[17] = if ("xn--" in hostname) 1.0f else 0.0f
        features[18] = digitRatio(registeredDomain)
        features[19] = tld.length.toFloat()
        features[20] = if (scheme == "https") 1.0f else 0.0f
        features[21] = if (hasPort) 1.0f else 0.0f
        features[22] = if (portIsStandard) 1.0f else 0.0f
        features[23] = if ("//" in path) 1.0f else 0.0f
        features[24] = hexEncodedRegex.findAll(url).count().toFloat()
        features[25] = if (hasSuspiciousExtension(path)) 1.0f else 0.0f
        features[26] = if (loginKeywords.any { it in urlLower }) 1.0f else 0.0f
        features[27] = if (secureKeywords.any { it in urlLower }) 1.0f else 0.0f
        features[28] = if (accountKeywords.any { it in urlLower }) 1.0f else 0.0f
        features[29] = if (updateKeywords.any { it in urlLower }) 1.0f else 0.0f
        features[30] = if (verifyKeywords.any { it in urlLower }) 1.0f else 0.0f
        features[31] = if (bankKeywords.any { it in urlLower }) 1.0f else 0.0f
        features[32] = brandImpersonationScore(urlLower, registeredDomain)
        features[33] = if (nestedUrlRegex.containsMatchIn(query)) 1.0f else 0.0f
        features[34] = if (subdomainLabels.size > 3) 1.0f else 0.0f
        features[35] = randomLookingDomainScore(registeredDomain)
        features[36] = if (subdomainLabels.any { it.length > 20 }) 1.0f else 0.0f
        features[37] = if (url.isNotEmpty()) path.length.toFloat() / url.length else 0.0f
        features[38] = if (knownBrands.any { it in subdomain } && registeredDomain !in knownBrands) 1.0f else 0.0f
        features[39] = if (tld in misleadingTlds) 1.0f else 0.0f
        features[40] = homoglyphScore(hostname)
        return features
    }

    private fun parseUri(url: String): URI {
        val candidate = if ("://" in url) url else "http://$url"
        return try {
            URI(candidate)
        } catch (_: Exception) {
            URI("http://invalid.url")
        }
    }

    private data class DomainParts(val registeredDomain: String, val subdomain: String, val tld: String)

    private fun splitDomain(hostname: String): DomainParts {
        val labels = hostname.trim('.').split('.').filter { it.isNotEmpty() }
        if (labels.isEmpty()) return DomainParts("", "", "")
        if (isIpAddress(hostname)) return DomainParts(hostname, "", "")
        val suffixLength = if (labels.size >= 3 && labels.takeLast(2).joinToString(".") in multiPartSuffixes) 2 else 1
        val tld = labels.takeLast(suffixLength).joinToString(".")
        val domainIndex = labels.size - suffixLength - 1
        if (domainIndex < 0) return DomainParts(labels.first(), "", tld)
        val registeredDomain = labels[domainIndex]
        val subdomain = labels.take(domainIndex).joinToString(".")
        return DomainParts(registeredDomain, subdomain, tld)
    }

    private fun shannonEntropy(text: String): Float {
        if (text.isEmpty()) return 0.0f
        val counts = text.groupingBy { it }.eachCount()
        val length = text.length.toFloat()
        var entropy = 0.0
        for (count in counts.values) {
            val p = count / length
            entropy -= p * log2(p.toDouble())
        }
        return entropy.toFloat()
    }

    private fun maxConsecutiveConsonants(text: String): Int {
        val consonants = "bcdfghjklmnpqrstvwxyzBCDFGHJKLMNPQRSTVWXYZ"
        var best = 0
        var run = 0
        for (char in text) {
            if (char in consonants) {
                run += 1
                best = max(best, run)
            } else {
                run = 0
            }
        }
        return best
    }

    private fun isIpAddress(hostname: String): Boolean {
        val parts = hostname.split('.')
        return parts.size == 4 && parts.all { part ->
            part.toIntOrNull()?.let { it in 0..255 } == true
        }
    }

    private fun digitRatio(text: String): Float {
        if (text.isEmpty()) return 0.0f
        return text.count { it.isDigit() }.toFloat() / text.length
    }

    private fun hasSuspiciousExtension(path: String): Boolean {
        val extension = path.substringAfterLast('.', missingDelimiterValue = "").lowercase(Locale.ROOT)
        return extension.isNotEmpty() && extension in suspiciousFileExtensions
    }

    private fun brandImpersonationScore(urlLower: String, registeredDomain: String): Float {
        val hits = knownBrands.count { brand -> brand in urlLower && brand != registeredDomain }
        return min(hits / 3.0f, 1.0f)
    }

    private fun randomLookingDomainScore(domainLabel: String): Float {
        if (domainLabel.length < 4) return 0.0f
        var score = 0.0f
        if (shannonEntropy(domainLabel) > 3.5f) score += 0.4f
        if (maxConsecutiveConsonants(domainLabel) >= 5) score += 0.3f
        if (digitRatio(domainLabel) > 0.3f) score += 0.3f
        return min(score, 1.0f)
    }

    private fun homoglyphScore(hostname: String): Float {
        val matches = homoglyphRegex.findAll(hostname).count()
        val hasAsciiLetters = hostname.any { it.code < 128 && it.isLetter() }
        if (matches > 0 && hasAsciiLetters) {
            return min(matches / 5.0f, 1.0f)
        }
        return 0.0f
    }
}
