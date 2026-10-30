package com.jusou.app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jusou.app.databinding.ActivityMainBinding
import com.jusou.app.databinding.ItemPlatformBinding

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
        setupPlatform(binding.btnDouyin, Platform.DOUYIN, R.string.douyin_description,
            R.drawable.bg_avatar_douyin, R.drawable.ic_douyin)
        setupPlatform(binding.btnXhs, Platform.XIAOHONGSHU, R.string.xhs_description,
            R.drawable.bg_avatar_xhs)
        setupPlatform(binding.btnZhihu, Platform.ZHIHU, R.string.zhihu_description,
            R.drawable.bg_avatar_zhihu)
        setupPlatform(binding.btnBilibili, Platform.BILIBILI, R.string.bilibili_description,
            R.drawable.bg_avatar_bilibili, R.drawable.ic_bilibili)

        binding.etQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) binding.searchInput.error = null
            }
        })

        // A keyboard action never guesses which platform the user intended.
        binding.etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validateQuery()) {
                    hideKeyboard()
                    binding.btnDouyin.root.requestFocus()
                    Toast.makeText(this, R.string.choose_platform_prompt, Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        binding.tvClearHistory.setOnClickListener {
            MaterialAlertDialogBuilder(this)
                .setTitle(R.string.clear_history_title)
                .setMessage(R.string.clear_history_message)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_history) { _, _ ->
                    SearchHistoryManager.clear(this)
                    renderHistory()
                }
                .show()
        }

        binding.chipGroup.addOnLayoutChangeListener { _, left, _, right, _, oldLeft, _, oldRight, _ ->
            if (right - left != oldRight - oldLeft) {
                for (index in 0 until binding.chipGroup.childCount) {
                    (binding.chipGroup.getChildAt(index) as Chip).maxWidth = right - left
                }
            }
        }
    }

    private fun setupPlatform(
        card: ItemPlatformBinding,
        platform: Platform,
        @StringRes description: Int,
        @DrawableRes background: Int,
        @DrawableRes icon: Int? = null,
    ) {
        card.platformTitle.text = platform.title
        card.platformDescription.setText(description)
        card.platformBadge.setBackgroundResource(background)
        if (icon != null) {
            card.platformIcon.setImageResource(icon)
            card.platformIcon.visibility = View.VISIBLE
            card.platformMonogram.visibility = View.GONE
        } else {
            card.platformMonogram.text = if (platform == Platform.XIAOHONGSHU) platform.title else platform.short
            if (platform == Platform.XIAOHONGSHU) card.platformMonogram.textSize = 11f
        }
        card.root.contentDescription = getString(R.string.search_on_platform, platform.title)
        ViewCompat.setAccessibilityDelegate(card.root, object : AccessibilityDelegateCompat() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfoCompat) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Button::class.java.name
            }
        })
        card.root.setOnClickListener { search(platform) }
    }

    private fun query(): String = binding.etQuery.text?.toString()?.trim().orEmpty()

    private fun validateQuery(): Boolean {
        if (query().isNotEmpty()) return true
        binding.searchInput.error = getString(R.string.toast_empty_query)
        binding.etQuery.requestFocus()
        WindowCompat.getInsetsController(window, binding.root).show(WindowInsetsCompat.Type.ime())
        return false
    }

    private fun hideKeyboard() {
        WindowCompat.getInsetsController(window, binding.root).hide(WindowInsetsCompat.Type.ime())
        binding.etQuery.clearFocus()
    }

    private fun search(platform: Platform) {
        if (!validateQuery()) return
        val q = query()
        hideKeyboard()
        if (SearchEngine.open(this, platform, q)) {
            SearchHistoryManager.add(this, q)
            renderHistory()
        } else {
            Snackbar.make(binding.root, R.string.search_unavailable, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun renderHistory() {
        val items = SearchHistoryManager.load(this)
        binding.chipGroup.removeAllViews()
        binding.tvClearHistory.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        binding.historyEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
        binding.chipGroup.visibility = if (items.isEmpty()) View.GONE else View.VISIBLE
        val availableWidth = binding.chipGroup.width.takeIf { it > 0 }
            ?: (minOf(resources.configuration.screenWidthDp - 48, 560) * resources.displayMetrics.density).toInt()
        for (item in items) {
            val chip = (layoutInflater.inflate(R.layout.item_history_chip, binding.chipGroup, false) as Chip).apply {
                text = item
                contentDescription = getString(R.string.history_fill, item)
                isCheckable = false
                isClickable = true
                isFocusable = true
                maxWidth = availableWidth
                setSingleLine(true)
                ellipsize = TextUtils.TruncateAt.END
                setOnClickListener {
                    binding.etQuery.setText(item)
                    binding.etQuery.setSelection(item.length)
                    hideKeyboard()
                    binding.scrollView.smoothScrollTo(0, 0)
                }
            }
            binding.chipGroup.addView(chip)
        }
    }
}
