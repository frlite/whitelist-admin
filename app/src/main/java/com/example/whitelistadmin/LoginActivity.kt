package com.example.whitelistadmin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.whitelistadmin.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var apiClient: ApiClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient(this)

        // 如果已有 token，直接进入主界面
        if (apiClient.getToken().isNotEmpty()) {
            navigateToMain()
            return
        }

        binding.btnLogin.setOnClickListener {
            val serverUrl = binding.etServer.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (serverUrl.isEmpty()) {
                Toast.makeText(this, "请输入服务器地址", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            apiClient.setServerUrl(serverUrl)

            apiClient.login(password, object : ApiClient.Callback<String> {
                override fun onSuccess(result: String) {
                    runOnUiThread {
                        navigateToMain()
                    }
                }

                override fun onError(message: String) {
                    runOnUiThread {
                        Toast.makeText(this@LoginActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            })
        }
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
