package com.example.whitelistadmin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.whitelistadmin.databinding.ActivityMainBinding
import com.example.whitelistadmin.databinding.ItemRequestBinding
import com.example.whitelistadmin.databinding.ItemWhitelistBinding
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var apiClient: ApiClient
    private var currentTab = 0 // 0=pending, 1=whitelist

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        apiClient = ApiClient(this)

        binding.rvList.layoutManager = LinearLayoutManager(this)

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                currentTab = tab.position
                refresh()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        // 下拉刷新效果：点击标题栏刷新
        binding.tabLayout.setOnClickListener { refresh() }

        refresh()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        if (currentTab == 0) {
            loadPendingRequests()
        } else {
            loadWhitelist()
        }
    }

    private fun loadPendingRequests() {
        apiClient.getPendingRequests(object : ApiClient.Callback<List<RequestItem>> {
            override fun onSuccess(result: List<RequestItem>) {
                runOnUiThread {
                    binding.rvList.adapter = RequestAdapter(result,
                        onApprove = { item -> approveItem(item) },
                        onReject = { item -> rejectItem(item) }
                    )
                    binding.tvEmpty.text = getString(R.string.empty_pending)
                    binding.tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    if (message == "登录已过期") {
                        showExpiredDialog()
                    } else {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun loadWhitelist() {
        apiClient.getWhitelist(object : ApiClient.Callback<List<WhitelistItem>> {
            override fun onSuccess(result: List<WhitelistItem>) {
                runOnUiThread {
                    binding.rvList.adapter = WhitelistAdapter(result,
                        onDelete = { item -> deleteItem(item) }
                    )
                    binding.tvEmpty.text = getString(R.string.empty_whitelist)
                    binding.tvEmpty.visibility = if (result.isEmpty()) View.VISIBLE else View.GONE
                }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    if (message == "登录已过期") {
                        showExpiredDialog()
                    } else {
                        Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun approveItem(item: RequestItem) {
        AlertDialog.Builder(this)
            .setTitle("确认同意")
            .setMessage("同意将 ${item.domain} 加入白名单？")
            .setPositiveButton("同意") { _, _ ->
                apiClient.approveRequest(item.id, object : ApiClient.Callback<String> {
                    override fun onSuccess(result: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, R.string.toast_approved, Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }

                    override fun onError(message: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun rejectItem(item: RequestItem) {
        AlertDialog.Builder(this)
            .setTitle("确认拒绝")
            .setMessage("拒绝 ${item.domain} 的访问请求？")
            .setPositiveButton("拒绝") { _, _ ->
                apiClient.rejectRequest(item.id, object : ApiClient.Callback<String> {
                    override fun onSuccess(result: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, R.string.toast_rejected, Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }

                    override fun onError(message: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun deleteItem(item: WhitelistItem) {
        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("从白名单中删除 ${item.domain}？")
            .setPositiveButton("删除") { _, _ ->
                apiClient.removeWhitelist(item.id, object : ApiClient.Callback<String> {
                    override fun onSuccess(result: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, R.string.toast_removed, Toast.LENGTH_SHORT).show()
                            refresh()
                        }
                    }

                    override fun onError(message: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showExpiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("登录已过期")
            .setMessage("请重新登录")
            .setPositiveButton("确定") { _, _ ->
                apiClient.setToken("")
                finish()
            }
            .setCancelable(false)
            .show()
    }
}

class RequestAdapter(
    private val items: List<RequestItem>,
    private val onApprove: (RequestItem) -> Unit,
    private val onReject: (RequestItem) -> Unit
) : RecyclerView.Adapter<RequestAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemRequestBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemRequestBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvDomain.text = item.domain
        val time = java.text.SimpleDateFormat("MM-dd HH:mm", java.util.Locale.getDefault())
            .format(java.util.Date(item.createdAt * 1000))
        holder.binding.tvInfo.text = "请求时间：$time"
        holder.binding.btnApprove.setOnClickListener { onApprove(item) }
        holder.binding.btnReject.setOnClickListener { onReject(item) }
    }

    override fun getItemCount() = items.size
}

class WhitelistAdapter(
    private val items: List<WhitelistItem>,
    private val onDelete: (WhitelistItem) -> Unit
) : RecyclerView.Adapter<WhitelistAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemWhitelistBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemWhitelistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvDomain.text = item.domain
        holder.binding.btnDelete.setOnClickListener { onDelete(item) }
    }

    override fun getItemCount() = items.size
}
