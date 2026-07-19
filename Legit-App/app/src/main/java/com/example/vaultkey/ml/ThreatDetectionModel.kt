package com.example.vaultkey.ml

import android.content.Context
import android.util.Log
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class ThreatDetectionModel(context: Context) {
    
    private var interpreter: Interpreter? = null
    private val tokenizer = URLTokenizer(200)
    private val extractor = FeatureExtractor()
    var isInitialized = false
        private set
    
    private val threatClasses = listOf("safe", "phishing", "malware", "data_leak", "scam")
    
    init {
        try {
            val modelBuffer = loadModelFile(context, "shieldnet_model.tflite")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            isInitialized = true
            Log.d("ThreatDetection", "ML Model initialized successfully")
        } catch (e: Exception) {
            Log.e("ThreatDetection", "Failed to initialize TFLite model", e)
            isInitialized = false
        }
    }
    
    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        context.assets.openFd(modelName).use { fileDescriptor ->
            FileInputStream(fileDescriptor.fileDescriptor).use { inputStream ->
                val fileChannel = inputStream.channel
                return fileChannel.map(
                    FileChannel.MapMode.READ_ONLY,
                    fileDescriptor.startOffset,
                    fileDescriptor.declaredLength
                )
            }
        }
    }
    
    data class Prediction(
        val threatClass: String,
        val confidence: Float,
        val isMalicious: Boolean,
        val analyzedUrl: String,
        val maliciousScore: Float
    )
    
    fun predict(url: String): Prediction {
        try {
            val analyzedUrl = DetectionUrlNormalizer.normalize(url)
            val features = extractor.extract(analyzedUrl)
            if (!isInitialized || interpreter == null) {
                return heuristicPrediction(analyzedUrl, features)
            }

            val tokens = tokenizer.tokenize(analyzedUrl)
            
            val urlInput = arrayOf(tokens)
            val featureInput = arrayOf(features)
            val inputs = arrayOf<Any>(urlInput, featureInput)
            
            val outputMap = mutableMapOf<Int, Any>()
            val result = Array(1) { FloatArray(5) }
            outputMap[0] = result
            
            interpreter?.runForMultipleInputsOutputs(inputs, outputMap)
            
            val probabilities = result[0]
            var maxIdx = 0
            var maxProb = 0.0f
            
            for (i in probabilities.indices) {
                if (probabilities[i] > maxProb) {
                    maxProb = probabilities[i]
                    maxIdx = i
                }
            }
            
            val threatClass = threatClasses[maxIdx]
            val safeScore = probabilities.getOrElse(0) { 0f }
            val maliciousScore = probabilities.drop(1).sum()
            val heuristicRisk = heuristicRiskScore(analyzedUrl, features)
            val isMalicious = maxIdx != 0 || maliciousScore >= 0.40f || heuristicRisk >= 0.55f || safeScore <= 0.35f

            Log.d(
                "ThreatDetection",
                "scan=$url analyzed=$analyzedUrl class=$threatClass maxProb=$maxProb maliciousScore=$maliciousScore heuristic=$heuristicRisk safe=$safeScore"
            )
            
            val effectiveClass = if (isMalicious && threatClass == "safe") "phishing" else threatClass
            val effectiveConfidence = maxOf(maxProb, maliciousScore, heuristicRisk)

            return Prediction(
                threatClass = effectiveClass,
                confidence = effectiveConfidence,
                isMalicious = isMalicious,
                analyzedUrl = analyzedUrl,
                maliciousScore = maxOf(maliciousScore, heuristicRisk)
            )
        } catch (e: Exception) {
            Log.e("ThreatDetection", "Inference failed", e)
            val analyzedUrl = DetectionUrlNormalizer.normalize(url)
            return heuristicPrediction(analyzedUrl, extractor.extract(analyzedUrl))
        }
    }

    private fun heuristicPrediction(url: String, features: FloatArray): Prediction {
        val heuristicRisk = heuristicRiskScore(url, features)
        val isMalicious = heuristicRisk >= 0.6f
        return Prediction(
            threatClass = if (isMalicious) "phishing" else "safe",
            confidence = heuristicRisk,
            isMalicious = isMalicious,
            analyzedUrl = url,
            maliciousScore = heuristicRisk
        )
    }

    private fun heuristicRiskScore(url: String, features: FloatArray): Float {
        val urlLower = url.lowercase()
        var score = 0f

        if ("legit" in urlLower || "vaultkey" in urlLower) {
            // Legit is a brand, if it's in a suspicious TLD it's very likely a clone
            if (".tk" in urlLower || ".ml" in urlLower || ".ga" in urlLower || ".xyz" in urlLower) score += 0.45f
        }
        if (".tk" in urlLower || ".ml" in urlLower) score += 0.35f
        if (urlLower.startsWith("http://")) score += 0.2f
        if ("verify" in urlLower || "secure" in urlLower || "update" in urlLower) score += 0.1f

        score += features.getOrElse(16) { 0f } * 0.2f
        score += features.getOrElse(27) { 0f } * 0.1f
        score += features.getOrElse(30) { 0f } * 0.1f
        score += features.getOrElse(32) { 0f } * 0.25f
        score += features.getOrElse(40) { 0f } * 0.3f

        return score.coerceIn(0f, 1f)
    }
    
    fun close() {
        interpreter?.close()
        interpreter = null
    }
}
