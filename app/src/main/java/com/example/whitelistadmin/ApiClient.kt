package com.example.whitelistadmin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

class ApiClient(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("admin_config", Context.MODE_PRIVATE)
    private val executor = Executors.newSingleThreadExecutor()

    fun getServerUrl(): String = prefs.getString("server_url", "") ?: ""
    fun setServerUrl(url: String) = prefs.edit().putString("server_url", url).apply()
    fun getToken(): String = prefs.getString("admin_token", "") ?: ""
    fun setToken(token: String) = prefs.edit().putString("admin_token", token).apply()

    interface Callback<T> {
        fun onSuccess(result: T)
        fun onError(message: String)
    }

    fun login(password: String, callback: Callback<String>) {
        val serverUrl = getServerUrl().trimEnd('/')
        if (serverUrl.isEmpty()) {
            callback.onError("请输入服务器地址")
            return
        }

        executor.execute {
            try {
                val url = URL("$serverUrl/api/admin/login")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.doOutput = true

                val body = JSONObject().apply { put("password", password) }
                conn.outputStream.use { it.write(body.toString().toByteArray()) }

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val token = JSONObject(resp).getString("token")
                    setToken(token)
                    callback.onSuccess(token)
                } else {
                    callback.onError("密码错误")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ApiClient", "login failed", e)
                callback.onError("网络错误")
            }
        }
    }

    fun getPendingRequests(callback: Callback<List<RequestItem>>) {
        val serverUrl = getServerUrl().trimEnd('/')
        val token = getToken()
        if (serverUrl.isEmpty() || token.isEmpty()) {
            callback.onError("未登录")
            return
        }

        executor.execute {
            try {
                val url = URL("$serverUrl/api/admin/requests?status=pending")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("X-Admin-Token", token)

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(resp)
                    val arr = json.getJSONArray("requests")
                    val list = mutableListOf<RequestItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            RequestItem(
                                id = obj.getInt("id"),
                                domain = obj.getString("domain"),
                                requestedBy = obj.optString("requested_by", ""),
                                createdAt = obj.getLong("created_at")
                            )
                        )
                    }
                    callback.onSuccess(list)
                } else if (conn.responseCode == 401) {
                    callback.onError("登录已过期")
                } else {
                    callback.onError("获取失败")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ApiClient", "getPendingRequests failed", e)
                callback.onError("网络错误")
            }
        }
    }

    fun getWhitelist(callback: Callback<List<WhitelistItem>>) {
        val serverUrl = getServerUrl().trimEnd('/')
        val token = getToken()
        if (serverUrl.isEmpty() || token.isEmpty()) {
            callback.onError("未登录")
            return
        }

        executor.execute {
            try {
                val url = URL("$serverUrl/api/admin/whitelist")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.setRequestProperty("X-Admin-Token", token)

                if (conn.responseCode == 200) {
                    val resp = conn.inputStream.bufferedReader().readText()
                    val json = JSONObject(resp)
                    val arr = json.getJSONArray("whitelist")
                    val list = mutableListOf<WhitelistItem>()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        list.add(
                            WhitelistItem(
                                id = obj.getInt("id"),
                                domain = obj.getString("domain")
                            )
                        )
                    }
                    callback.onSuccess(list)
                } else {
                    callback.onError("获取失败")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ApiClient", "getWhitelist failed", e)
                callback.onError("网络错误")
            }
        }
    }

    fun approveRequest(requestId: Int, callback: Callback<String>) {
        val serverUrl = getServerUrl().trimEnd('/')
        val token = getToken()
        if (serverUrl.isEmpty() || token.isEmpty()) {
            callback.onError("未登录")
            return
        }

        executor.execute {
            try {
                val url = URL("$serverUrl/api/admin/requests/$requestId/approve")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "POST"
                conn.setRequestProperty("X-Admin-Token", token)

                if (conn.responseCode == 200) {
                    callback.onSuccess("已同意")
                } else {
                    callback.onError("操作失败")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ApiClient", "approveRequest failed", e)
                callback.onError("网络错误")
            }
        }
    }

    fun rejectRequest(requestId: Int, callback: Callback<String>) {
        val serverUrl = getServerUrl().trimEnd('/')
        val token = getToken()
        if (serverUrl.isEmpty() || token.isEmpty()) {
            callback.onError("未登录")
            return
        }

        executor.execute {
            try {
                val url = URL("$serverUrl/api/admin/requests/$requestId/reject")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "POST"
                conn.setRequestProperty("X-Admin-Token", token)

                if (conn.responseCode == 200) {
                    callback.onSuccess("已拒绝")
                } else {
                    callback.onError("操作失败")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ApiClient", "rejectRequest failed", e)
                callback.onError("网络错误")
            }
        }
    }

    fun removeWhitelist(id: Int, callback: Callback<String>) {
        val serverUrl = getServerUrl().trimEnd('/')
        val token = getToken()
        if (serverUrl.isEmpty() || token.isEmpty()) {
            callback.onError("未登录")
            return
        }

        executor.execute {
            try {
                val url = URL("$serverUrl/api/admin/whitelist/$id")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                conn.requestMethod = "DELETE"
                conn.setRequestProperty("X-Admin-Token", token)

                if (conn.responseCode == 200) {
                    callback.onSuccess("已删除")
                } else {
                    callback.onError("操作失败")
                }
                conn.disconnect()
            } catch (e: Exception) {
                Log.e("ApiClient", "removeWhitelist failed", e)
                callback.onError("网络错误")
            }
        }
    }
}

data class RequestItem(
    val id: Int,
    val domain: String,
    val requestedBy: String,
    val createdAt: Long
)

data class WhitelistItem(
    val id: Int,
    val domain: String
)
