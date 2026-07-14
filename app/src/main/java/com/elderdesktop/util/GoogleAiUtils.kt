package com.elderdesktop.util

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

object GoogleAiUtils {
    private const val API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent"

    suspend fun chat(prompt: String, apiKey: String, client: OkHttpClient): String? {
        if (apiKey.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    val contents = JSONArray().apply {
                        put(JSONObject().apply {
                            val parts = JSONArray().apply {
                                put(JSONObject().apply {
                                    put("text", prompt)
                                })
                            }
                            put("parts", parts)
                        })
                    }
                    put("contents", contents)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url("$API_URL?key=$apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body.string()
                    if (responseBody.isNotEmpty()) {
                        val responseJson = JSONObject(responseBody)
                        val candidates = responseJson.getJSONArray("candidates")
                        if (candidates.length() > 0) {
                            return@withContext candidates.getJSONObject(0)
                                .getJSONObject("content")
                                .getJSONArray("parts")
                                .getJSONObject(0)
                                .getString("text")
                        }
                    }
                } else {
                    Log.e("GoogleAiUtils", "API error: ${response.code} ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("GoogleAiUtils", "Error chatting with Google AI", e)
            }
            null
        }
    }
}
