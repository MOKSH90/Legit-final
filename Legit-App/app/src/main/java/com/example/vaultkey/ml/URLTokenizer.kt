package com.example.vaultkey.ml

class URLTokenizer(private val maxLength: Int = 200) {
    private val printableLow = 32
    private val printableHigh = 126
    private val modelVocabSize = 128
    
    fun tokenize(url: String): IntArray {
        val tokens = IntArray(maxLength) { 0 }
        val limitedUrl = url.take(maxLength)
        
        for (i in limitedUrl.indices) {
            tokens[i] = charToId(limitedUrl[i])
        }
        
        return tokens
    }

    private fun charToId(char: Char): Int {
        val code = char.code
        if (code in printableLow..printableHigh) {
            val id = code - printableLow + 2
            if (id < modelVocabSize) return id
        }
        return 1
    }
    
    val vocabSize: Int
        get() = modelVocabSize
}
