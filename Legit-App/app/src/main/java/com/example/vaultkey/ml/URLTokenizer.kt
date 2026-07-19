package com.example.vaultkey.ml

class URLTokenizer(private val maxLength: Int = 200) {
    private val charToIdx = mutableMapOf<Char, Int>()
    
    init {
        buildVocabulary()
    }
    
    private fun buildVocabulary() {
        var idx = 2 // 0 for PAD, 1 for UNK
        
        // Lowercase letters (2-27)
        for (c in 'a'..'z') charToIdx[c] = idx++
        
        // Uppercase letters (28-53)
        for (c in 'A'..'Z') charToIdx[c] = idx++
        
        // Digits (54-63)
        for (c in '0'..'9') charToIdx[c] = idx++
        
        // Special characters common in URLs (64-75)
        val specialChars = charArrayOf(
            '.', '/', '-', '_', ':', '?', '=', '&', 
            '#', '%', '@', '+'
        )
        for (c in specialChars) charToIdx[c] = idx++
    }
    
    fun tokenize(url: String): IntArray {
        val tokens = IntArray(maxLength) { 0 }
        val limitedUrl = url.take(maxLength)
        
        for (i in limitedUrl.indices) {
            tokens[i] = charToIdx[limitedUrl[i]] ?: 1 // 1 for UNK
        }
        
        return tokens
    }
    
    val vocabSize: Int
        get() = charToIdx.size + 2
}
