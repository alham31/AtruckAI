package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response models ---

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String? = null,
    val inlineData: InlineData? = null
)

data class InlineData(
    val mimeType: String,
    val data: String // Base64 string without net line breaks
)

data class ResponseFormat(
    val responseMimeType: String
)

data class GenerationConfig(
    val responseMimeType: String? = "application/json",
    val temperature: Float? = 0.1f
)

data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: ResponseContent?
)

data class ResponseContent(
    val parts: List<ResponsePart>?
)

data class ResponsePart(
    val text: String?
)

// --- Structuring the OCR result ---

data class ExtractedReceipt(
    val merchantName: String,
    val transactionDate: String, // Format: YYYY-MM-DD
    val totalAmount: Double,
    val items: List<ExtractedItem>
)

data class ExtractedItem(
    val name: String,
    val quantity: Double,
    val price: Double,
    val totalPrice: Double
)

// --- Retrofit Setup ---

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun extractReceipt(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val service: GeminiApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApiService::class.java)
    }

    // Helper to serialize extracted receipt
    fun parseExtractedReceipt(jsonStr: String): ExtractedReceipt? {
        return try {
            val adapter = moshi.adapter(ExtractedReceipt::class.java)
            // Strip markdown block markers (e.g. ```json ... ```) if Gemini returns them
            val cleanedJson = jsonStr.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            adapter.fromJson(cleanedJson)
        } catch (e: Exception) {
            Log.e("GeminiApiClient", "Failed to parse JSON: ${e.message}", e)
            null
        }
    }
}

// Utility to convert bitmap to Base64
fun Bitmap.toBase64(): String {
    val outputStream = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
    return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
}
