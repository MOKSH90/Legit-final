package com.example.vaultkey.ml

import android.net.Uri
import java.util.Locale
import kotlin.math.log2
import kotlin.math.max
import kotlin.math.min

class FeatureExtractor {
    
    private val SUSPICIOUS_TLDS = setOf(".xyz", ".top", ".buzz", ".club", ".tk", ".ml", ".ga")
    private val URL_SHORTENERS = setOf("bit.ly", "tinyurl.com", "goo.gl", "t.co")
    private val COMMON_BRANDS = setOf(
        "paypal", "apple", "google", "microsoft", "amazon", "facebook",
        "netflix", "instagram", "twitter", "linkedin", "dropbox", "adobe",
        "spotify", "walmart", "ebay", "chase", "wellsfargo", "bankofamerica",
        "citibank", "usbank", "capitalone", "americanexpress", "discover",
        "apex", "apexmotors", "apex-motors", "legit", "vaultkey", "legitpipeline", "legit-estates"
    )

    fun extract(urlStr: String): FloatArray {
        val features = FloatArray(41)

        // Clean URL
        val url = urlStr.filter { it.isDefined() || it.isWhitespace() }
        val uri = try {
            val parsed = Uri.parse(url.lowercase(Locale.ROOT))
            if (parsed.scheme == null) Uri.parse("http://$url") else parsed
        } catch (e: Exception) {
            Uri.parse("http://invalid.url")
        }

        val domain = uri.host ?: ""
        val path = uri.path ?: ""
        val query = uri.query ?: ""

        // === 1. Lexical Features (12) ===
        features[0] = normalize(url.length.toFloat(), 0f, 500f)
        features[1] = normalize(url.count { it == '.' }.toFloat(), 0f, 20f)
        features[2] = normalize(url.count { it == '-' }.toFloat(), 0f, 20f)
        features[3] = normalize(url.count { it == '_' }.toFloat(), 0f, 20f)
        features[4] = normalize(url.count { it.isDigit() }.toFloat(), 0f, 50f)
        features[5] = normalize(url.count { "!@#$%^&*()+=[]{}|;:,<>?".contains(it) }.toFloat(), 0f, 20f)
        features[6] = if (url.contains('@')) 1.0f else 0.0f
        features[7] = normalize(path.count { it == '/' }.toFloat(), 0f, 15f)
        features[8] = normalize(uri.queryParameterNames.size.toFloat(), 0f, 20f)
        features[9] = if (uri.fragment != null) 1.0f else 0.0f
        features[10] = calculateEntropy(url)
        features[11] = consecutiveConsonants(url)

        // === 2. Domain Intelligence Features (8) ===
        features[12] = normalize(domain.length.toFloat(), 0f, 100f)
        features[13] = normalize(domain.count { it == '.' }.toFloat(), 0f, 10f)
        features[14] = if (isIpAddress(domain)) 1.0f else 0.0f
        features[15] = if (usesUrlShortener(domain)) 1.0f else 0.0f
        features[16] = if (hasSuspiciousTld(domain)) 1.0f else 0.0f
        features[17] = if (domain.startsWith("xn--") || domain.contains("xn--")) 1.0f else 0.0f
        features[18] = digitRatio(domain)
        features[19] = normalize(getTld(domain).length.toFloat(), 0f, 10f)

        // === 3. Security Features (6) ===
        features[20] = if (uri.scheme == "https") 1.0f else 0.0f
        features[21] = if (domain.contains(':')) 1.0f else 0.0f
        features[22] = if (uri.port in listOf(-1, 80, 443)) 1.0f else 0.0f
        features[23] = if (path.contains("//")) 1.0f else 0.0f
        features[24] = normalize(url.count { it == '%' }.toFloat(), 0f, 20f)
        features[25] = if (hasSuspiciousExtension(path)) 1.0f else 0.0f

        // === 4. Content Indicator Features (7) ===
        val urlLower = url.lowercase(Locale.ROOT)
        features[26] = if (listOf("login", "signin", "log-in", "sign-in").any { urlLower.contains(it) }) 1.0f else 0.0f
        features[27] = if (listOf("secure", "security", "ssl").any { urlLower.contains(it) }) 1.0f else 0.0f
        features[28] = if (listOf("account", "myaccount", "profile").any { urlLower.contains(it) }) 1.0f else 0.0f
        features[29] = if (listOf("update", "upgrade", "renew").any { urlLower.contains(it) }) 1.0f else 0.0f
        features[30] = if (listOf("verify", "confirm", "validate").any { urlLower.contains(it) }) 1.0f else 0.0f
        features[31] = if (listOf("bank", "banking", "financial").any { urlLower.contains(it) }) 1.0f else 0.0f
        features[32] = brandImpersonationScore(domain)

        // === 5. Behavioral Features (5) ===
        features[33] = urlShorteningChainScore(url)
        features[34] = if (domain.count { it == '.' } > 4) 1.0f else 0.0f
        features[35] = randomLookingScore(domain)
        features[36] = if (hasLongSubdomain(domain)) 1.0f else 0.0f
        features[37] = if (url.isNotEmpty()) normalize(path.length.toFloat(), 0f, 200f) else 0.0f

        // === 6. Reputation Features (3) ===
        features[38] = if (brandInSubdomain(domain)) 1.0f else 0.0f
        features[39] = misleadingTldScore(domain)
        features[40] = homoglyphScore(domain)

        return features
    }

    private fun normalize(value: Float, minVal: Float, maxVal: Float): Float {
        return min(1.0f, max(0.0f, (value - minVal) / (maxVal - minVal + 1e-8f)))
    }

    private fun calculateEntropy(text: String): Float {
        if (text.isEmpty()) return 0.0f
        val counts = text.groupingBy { it }.eachCount()
        val len = text.length.toFloat()
        var entropy = 0.0
        for (count in counts.values) {
            val p = count / len
            entropy -= p * log2(p + 1e-10)
        }
        return normalize(entropy.toFloat(), 0f, 6f)
    }

    private fun consecutiveConsonants(url: String): Float {
        val consonants = "bcdfghjklmnpqrstvwxyz"
        var maxCount = 0
        var current = 0
        for (char in url.lowercase(Locale.ROOT)) {
            if (consonants.contains(char)) {
                current++
                maxCount = max(maxCount, current)
            } else {
                current = 0
            }
        }
        return normalize(maxCount.toFloat(), 0f, 10f)
    }

    private fun isIpAddress(domain: String): Boolean {
        val cleanDomain = domain.split(':')[0]
        val parts = cleanDomain.split('.')
        if (parts.size == 4) {
            return try {
                parts.all { it.toInt() in 0..255 }
            } catch (e: NumberFormatException) {
                false
            }
        }
        return false
    }

    private fun usesUrlShortener(domain: String): Boolean {
        return URL_SHORTENERS.any { domain.contains(it) }
    }

    private fun hasSuspiciousTld(domain: String): Boolean {
        return SUSPICIOUS_TLDS.any { domain.endsWith(it) }
    }

    private fun getTld(domain: String): String {
        val parts = domain.split('.')
        return if (parts.isNotEmpty()) parts.last() else ""
    }

    private fun digitRatio(domain: String): Float {
        if (domain.isEmpty()) return 0.0f
        return domain.count { it.isDigit() }.toFloat() / domain.length
    }

    private fun hasSuspiciousExtension(path: String): Boolean {
        val suspicious = listOf(".exe", ".zip", ".rar", ".js", ".php", ".asp", ".scr", ".bat", ".cmd")
        val pathLower = path.lowercase(Locale.ROOT)
        return suspicious.any { pathLower.endsWith(it) }
    }

    private fun brandImpersonationScore(domain: String): Float {
        val domainLower = domain.lowercase(Locale.ROOT)
        var score = 0.0f
        for (brand in COMMON_BRANDS) {
            if (domainLower.contains(brand)) {
                val actualDomains = listOf(
                    "$brand.com", "$brand.net", "$brand.org", "www.$brand.com",
                    "$brand.io", "$brand.in", "$brand-pipeline.com"
                )
                if (!actualDomains.any { domainLower == it || domainLower.endsWith("." + it.removePrefix("www.")) }) {
                    score = max(score, 0.8f)
                }
                // Check for typosquatting (brand+digit or digit+brand)
                if (Regex("$brand\\d").containsMatchIn(domainLower) || Regex("\\d$brand").containsMatchIn(domainLower)) {
                    score = max(score, 0.95f)
                }
            }
        }
        return score
    }

    private fun urlShorteningChainScore(url: String): Float {
        val count = URL_SHORTENERS.count { url.lowercase(Locale.ROOT).contains(it) }
        return normalize(count.toFloat(), 0f, 3f)
    }

    private fun randomLookingScore(domain: String): Float {
        val parts = domain.split('.')
        val mainDomain = if (parts.size > 1) parts[parts.size - 2] else domain
        if (mainDomain.isEmpty()) return 0.0f

        var score = 0.0f
        val consonants = mainDomain.count { "bcdfghjklmnpqrstvwxyz".contains(it) }
        if (consonants.toFloat() / mainDomain.length > 0.7) score += 0.3f

        if (mainDomain.any { it.isDigit() } && mainDomain.any { it.isLetter() }) score += 0.3f

        if (mainDomain.length > 15) score += 0.4f

        return min(1.0f, score)
    }

    private fun hasLongSubdomain(domain: String): Boolean {
        val parts = domain.split('.')
        if (parts.size > 2) {
            val subdomains = parts.take(parts.size - 2)
            return subdomains.any { it.length > 20 }
        }
        return false
    }

    private fun brandInSubdomain(domain: String): Boolean {
        val parts = domain.split('.')
        if (parts.size > 2) {
            val subdomains = parts.take(parts.size - 2).joinToString(".").lowercase(Locale.ROOT)
            return COMMON_BRANDS.any { subdomains.contains(it) }
        }
        return false
    }

    private fun misleadingTldScore(domain: String): Float {
        val misleadingPatterns = mapOf(
            ".com-" to 0.9f,
            "-com." to 0.9f,
            ".org-" to 0.8f,
            "-secure" to 0.7f,
            "-login" to 0.8f,
            "-verify" to 0.8f
        )
        val domainLower = domain.lowercase(Locale.ROOT)
        for ((pattern, score) in misleadingPatterns) {
            if (domainLower.contains(pattern)) return score
        }
        return 0.0f
    }

    private fun homoglyphScore(domain: String): Float {
        val domainLower = domain.lowercase(Locale.ROOT)
        val homoglyphs = mapOf(
            'o' to listOf('0', 'ó', 'ò', 'ö'),
            'l' to listOf('1', 'i', '|'),
            'i' to listOf('1', 'l', '!', 'í', 'ì'),
            'a' to listOf('4', '@', 'á', 'à'),
            'e' to listOf('3', 'é', 'è', 'ë'),
            's' to listOf('5', '$'),
            't' to listOf('+', '7'),
            'b' to listOf('8'),
            'g' to listOf('9'),
            'n' to listOf('ñ')
        )

        var maxScore = 0.0f
        for (brand in COMMON_BRANDS) {
            // Check if domain contains the brand with any character replaced by a homoglyph
            for (i in brand.indices) {
                val char = brand[i]
                val subs = homoglyphs[char] ?: continue
                for (sub in subs) {
                    val modifiedBrand = brand.substring(0, i) + sub + brand.substring(i + 1)
                    if (domainLower.contains(modifiedBrand)) {
                        maxScore = max(maxScore, 0.95f)
                        break
                    }
                }
            }

            // Also check for general homoglyph presence in domains that look like the brand
            if (domainLower.contains(brand)) {
                val hasOtherHomoglyphs = homoglyphs.values.flatten().any { it in domainLower && it !in brand }
                if (hasOtherHomoglyphs) {
                    maxScore = max(maxScore, 0.85f)
                }
            }
        }
        return maxScore
    }}
