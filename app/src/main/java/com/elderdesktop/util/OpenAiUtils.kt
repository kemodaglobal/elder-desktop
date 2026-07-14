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

object OpenAiUtils {
    private const val API_URL = "https://api.openai.com/v1/chat/completions"

    suspend fun chat(prompt: String, apiKey: String, client: OkHttpClient): String? {
        if (apiKey.isEmpty()) return null

        return withContext(Dispatchers.IO) {
            try {
                val json = JSONObject().apply {
                    put("model", "gpt-3.5-turbo")
                    val messages = JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("content", prompt)
                        })
                    }
                    put("messages", messages)
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer $apiKey")
                    .post(body)
                    .build()

                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body.string()
                    if (responseBody.isNotEmpty()) {
                        val responseJson = JSONObject(responseBody)
                        val choices = responseJson.getJSONArray("choices")
                        if (choices.length() > 0) {
                            return@withContext choices.getJSONObject(0)
                                .getJSONObject("message")
                                .getString("content")
                        }
                    }
                } else {
                    Log.e("OpenAiUtils", "API error: ${response.code} ${response.message}")
                }
            } catch (e: Exception) {
                Log.e("OpenAiUtils", "Error chatting with OpenAI", e)
            }
            null
        }
    }
}
