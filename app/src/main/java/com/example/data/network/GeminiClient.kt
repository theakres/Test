package com.example.data.network

import android.util.Log
import com.example.BuildConfig
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * General text generation function using Gemini 3.5 Flash.
     */
    suspend fun generateText(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.w(TAG, "Gemini API Key is empty or placeholder!")
            return@withContext "API_KEY_MISSING"
        }

        val url = "$BASE_URL/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
        
        try {
            // Build request JSON
            val requestJson = JSONObject()
            
            // Contents
            val contentsArray = org.json.JSONArray()
            val contentObj = JSONObject()
            val partsArray = org.json.JSONArray()
            val partObj = JSONObject().put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            // System Instruction if provided
            if (systemInstruction != null) {
                val sysObj = JSONObject()
                val sysParts = org.json.JSONArray().put(JSONObject().put("text", systemInstruction))
                sysObj.put("parts", sysParts)
                requestJson.put("systemInstruction", sysObj)
            }

            // Generation Config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.2) // Low temperature for deterministic/reliable math or data formatting
            requestJson.put("generationConfig", genConfig)

            val mediaType = "application/json; charset=utf-8".toMediaType()
            val body = requestJson.toString().toRequestBody(mediaType)
            
            val request = Request.Builder()
                .url(url)
                .post(body)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorBody = response.body?.string() ?: "Empty body"
                    Log.e(TAG, "API error: ${response.code} - $errorBody")
                    return@withContext "ERROR: Server returned code ${response.code}"
                }
                
                val responseStr = response.body?.string()
                if (responseStr.isNullOrEmpty()) {
                    return@withContext "ERROR: Empty response from Gemini"
                }

                // Parse standard Gemini JSON response structure
                val root = JSONObject(responseStr)
                val candidates = root.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    if (content != null) {
                        val parts = content.optJSONArray("parts")
                        if (parts != null && parts.length() > 0) {
                            return@withContext parts.getJSONObject(0).optString("text")
                        }
                    }
                }
                "ERROR: Response format mismatch"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API call", e)
            "ERROR: ${e.localizedMessage ?: "Unknown connection error"}"
        }
    }

    /**
     * Special function to fetch live currency rates relative to USD from Gemini.
     */
    suspend fun fetchLiveRates(): Map<String, Double>? {
        val prompt = """
            Please return standard estimated currency exchange rates relative to USD (1 USD = ... ) as a raw JSON object.
            Do not include any Markdown tags, code block wrappers like ```json or any other text before or after the JSON. Just return the raw JSON text.
            The JSON MUST contain these exact keys for currency codes, with standard reasonable approximate values:
            USD, EUR, GBP, JPY, CAD, AUD, CNY, INR, BRL, RUB, BYN, UAH, KZT.
            Example response format:
            {
              "USD": 1.0,
              "EUR": 0.92,
              "GBP": 0.78,
              "JPY": 158.5,
              "CAD": 1.37,
              "AUD": 1.50,
              "CNY": 7.26,
              "INR": 83.5,
              "BRL": 5.45,
              "RUB": 90.0,
              "BYN": 3.25,
              "UAH": 41.0,
              "KZT": 475.0
            }
        """.trimIndent()

        val systemInstruction = "You are a reliable, up-to-date currency rate service that only outputs raw JSON. Do not write markdown, code fences, or natural language comments."
        val result = generateText(prompt, systemInstruction)
        
        if (result == "API_KEY_MISSING" || result.startsWith("ERROR")) {
            return null
        }

        return try {
            // Clean any potential markdown wrapper if the model didn't obey instructions
            val cleanedResult = result.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
                
            val json = JSONObject(cleanedResult)
            val ratesMap = mutableMapOf<String, Double>()
            val keys = listOf("USD", "EUR", "GBP", "JPY", "CAD", "AUD", "CNY", "INR", "BRL", "RUB", "BYN", "UAH", "KZT")
            for (key in keys) {
                if (json.has(key)) {
                    ratesMap[key] = json.getDouble(key)
                }
            }
            if (ratesMap.isNotEmpty()) ratesMap else null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse live rates: $result", e)
            null
        }
    }
}
