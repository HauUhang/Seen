package com.jusou.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.jusou.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupViews()
        renderHistory()
    }

    private fun setupViews() {
        binding.btnClear.setOnClickListener { binding.etQuery.setText("") }

        binding.btnDouyin.setOnClickListener { search(Platform.DOUYIN) }
        binding.btnXhs.setOnClickListener { search(Platform.XIAOHONGSHU) }
        binding.btnZhihu.setOnClickListener { search(Platform.ZHIHU) }
        binding.btnBilibili.setOnClickListener { search(Platform.BILIBILI) }

        binding.etQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                binding.btnClear.visibility =
                    if (s.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
            }
        })

        binding.tvClearHistory.setOnClickListener {
            SearchHistoryManager.clear(this)
            renderHistory()
        }
    }

    private fun query(): String = binding.etQuery.text.toString().trim()

    private fun search(platform: Platform) {
        val q = query()
        if (q.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_query, Toast.LENGTH_SHORT).show()
            return
        }
        SearchHistoryManager.add(this, q)
        renderHistory()
        SearchEngine.open(this, platform, q)
    }

    private fun renderHistory() {
        val items = SearchHistoryManager.load(this)
        binding.chipGroup.removeAllViews()
        binding.historySection.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        for (item in items) {
            val chip = Chip(this).apply {
                text = item
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    binding.etQuery.setText(item)
                    binding.etQuery.setSelection(item.length)
                }
            }
            binding.chipGroup.addView(chip)
        }
    }
}
