package com.jusou.app

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Space
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.AccessibilityDelegateCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.jusou.app.databinding.ActivityMainBinding
import com.jusou.app.databinding.ItemHistoryRowBinding
import com.jusou.app.databinding.ItemPlatformBinding
import com.jusou.app.databinding.ViewHistoryPopupBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var historyBinding: ViewHistoryPopupBinding
    private lateinit var historyPopup: PopupWindow
    private val platformCards = mutableMapOf<Platform, View>()
    private var hideDeleteNotice: Runnable? = null
    private var historyRequested = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        historyBinding = ViewHistoryPopupBinding.inflate(layoutInflater)
        historyPopup = PopupWindow(
            historyBinding.root,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT,
            false,
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            inputMethodMode = PopupWindow.INPUT_METHOD_NEEDED
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
            elevation = dp(8).toFloat()
        }
        setupViews()
        renderPlatforms()
        renderHistory()
    }

    override fun onDestroy() {
        hideHistory()
        super.onDestroy()
    }

    private fun setupViews() {
        binding.etQuery.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (!s.isNullOrBlank()) binding.searchInput.error = null
                if (historyRequested) showHistory()
            }
        })
        binding.etQuery.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) showHistory()
        }
        binding.etQuery.setOnClickListener { showHistory() }

        // A keyboard action never guesses which platform the user intended.
        binding.etQuery.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (validateQuery()) {
                    hideKeyboard()
                    platformCards.values.firstOrNull()?.requestFocus()
                    Toast.makeText(this, R.string.choose_platform_prompt, Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }

        historyBinding.tvClearHistory.setOnClickListener {
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

        binding.btnManagePlatforms.setOnClickListener { showPlatformManager() }
        binding.btnGithub.setOnClickListener { openGithub(R.string.github_repo_url) }
        binding.btnSuggestPlatform.setOnClickListener { openGithub(R.string.github_new_issue_url) }

    }

    private fun showHistory() {
        historyRequested = true
        renderHistory()
        if (historyBinding.historyList.childCount == 0) return
        binding.searchInput.post {
            if (!historyRequested || historyBinding.historyList.childCount == 0 || isFinishing || isDestroyed) return@post
            val anchor = binding.searchInput
            binding.scrollView.scrollTo(0, maxOf(0, anchor.top - dp(16)))
            historyPopup.width = anchor.width.takeIf { it > 0 }
                ?: (minOf(resources.configuration.screenWidthDp - 48, 560) * resources.displayMetrics.density).toInt()
            historyPopup.height = historyPopupHeight()
            if (historyPopup.isShowing) {
                historyPopup.update(anchor, 0, dp(4), historyPopup.width, historyPopup.height)
            } else {
                historyPopup.showAsDropDown(anchor, 0, dp(4))
            }
        }
    }

    private fun hideHistory() {
        historyRequested = false
        historyPopup.dismiss()
    }

    private fun historyPopupHeight(): Int = dp(minOf(245, 49 + historyBinding.historyList.childCount * 49))

    private fun renderPlatforms() {
        val enabled = Platform.values().filter { it in PlatformPreferences.enabled(this) }
        binding.platformContainer.removeAllViews()
        platformCards.clear()
        enabled.chunked(2).forEachIndexed { rowIndex, platforms ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setBaselineAligned(false)
                if (rowIndex > 0) setPadding(0, dp(12), 0, 0)
            }
            binding.platformContainer.addView(row)
            platforms.forEachIndexed { index, platform ->
                if (index > 0) row.addView(Space(this), LinearLayout.LayoutParams(dp(12), 1))
                val card = ItemPlatformBinding.inflate(layoutInflater, row, false)
                val (description, background, icon) = when (platform) {
                    Platform.DOUYIN -> Triple(R.string.douyin_description, R.drawable.bg_avatar_douyin, R.drawable.ic_douyin)
                    Platform.XIAOHONGSHU -> Triple(R.string.xhs_description, R.drawable.bg_avatar_xhs, null)
                    Platform.ZHIHU -> Triple(R.string.zhihu_description, R.drawable.bg_avatar_zhihu, null)
                    Platform.BILIBILI -> Triple(R.string.bilibili_description, R.drawable.bg_avatar_bilibili, R.drawable.ic_bilibili)
                    Platform.X -> Triple(R.string.x_description, R.drawable.bg_avatar_x, null)
                    Platform.REDDIT -> Triple(R.string.reddit_description, R.drawable.bg_avatar_reddit, null)
                    Platform.YOUTUBE -> Triple(R.string.youtube_description, R.drawable.bg_avatar_youtube, null)
                }
                setupPlatform(card, platform, description, background, icon)
                card.root.id = when (platform) {
                    Platform.DOUYIN -> R.id.btnDouyin
                    Platform.XIAOHONGSHU -> R.id.btnXhs
                    Platform.ZHIHU -> R.id.btnZhihu
                    Platform.BILIBILI -> R.id.btnBilibili
                    Platform.X -> R.id.btnX
                    Platform.REDDIT -> R.id.btnReddit
                    Platform.YOUTUBE -> R.id.btnYoutube
                }
                row.addView(card.root, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
                platformCards[platform] = card.root
            }
            if (platforms.size == 1) row.addView(Space(this), LinearLayout.LayoutParams(dp(12), 1))
            if (platforms.size == 1) row.addView(Space(this), LinearLayout.LayoutParams(0, 1, 1f))
        }
    }

    private fun showPlatformManager() {
        val platforms = Platform.values()
        val selected = PlatformPreferences.enabled(this).toMutableSet()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.manage_platforms_title)
            .setMultiChoiceItems(platforms.map { it.title }.toTypedArray(),
                platforms.map { it in selected }.toBooleanArray()) { dialog, which, checked ->
                if (checked) selected.add(platforms[which]) else selected.remove(platforms[which])
                if (selected.isEmpty()) {
                    selected.add(platforms[which])
                    (dialog as androidx.appcompat.app.AlertDialog).listView.setItemChecked(which, true)
                    Toast.makeText(this, R.string.manage_platforms_message, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save) { _, _ ->
                PlatformPreferences.save(this, selected)
                renderPlatforms()
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun openGithub(@StringRes url: Int) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(getString(url)))
        if (runCatching { startActivity(intent) }.isFailure) {
            Snackbar.make(binding.root, R.string.github_unavailable, Snackbar.LENGTH_LONG).show()
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
        hideHistory()
        WindowCompat.getInsetsController(window, binding.root).hide(WindowInsetsCompat.Type.ime())
        binding.etQuery.clearFocus()
    }

    private fun search(platform: Platform) {
        if (!validateQuery()) return
        val q = query()
        hideKeyboard()
        if (SearchEngine.open(this, platform, q)) {
            SearchHistoryManager.add(this, q)
            binding.etQuery.text?.clear()
            renderHistory()
        } else {
            Snackbar.make(binding.root, R.string.search_unavailable, Snackbar.LENGTH_LONG).show()
        }
    }

    private fun showDeleteNotice(item: String) {
        hideDeleteNotice?.let { binding.deleteNotice.removeCallbacks(it) }
        binding.deleteNotice.text = getString(R.string.history_removed, item)
        binding.deleteNotice.visibility = View.VISIBLE
        binding.deleteNotice.announceForAccessibility(binding.deleteNotice.text)
        val dismiss = Runnable { binding.deleteNotice.visibility = View.GONE }
        hideDeleteNotice = dismiss
        binding.deleteNotice.postDelayed(dismiss, 1800L)
    }

    private fun renderHistory() {
        val query = query()
        val items = SearchHistoryManager.load(this).filter { query.isEmpty() || it.contains(query, ignoreCase = true) }
        historyBinding.historyList.removeAllViews()
        for (item in items) {
            val row = ItemHistoryRowBinding.inflate(layoutInflater, historyBinding.historyList, false)
            row.historyQuery.text = item
            row.historyQuery.contentDescription = getString(R.string.history_fill, item)
            row.historyQuery.setOnClickListener {
                hideHistory()
                binding.etQuery.setText(item)
                binding.etQuery.setSelection(item.length)
                hideKeyboard()
                binding.scrollView.smoothScrollTo(0, 0)
            }
            row.btnDeleteHistory.contentDescription = getString(R.string.history_remove, item)
            row.btnDeleteHistory.setOnClickListener {
                SearchHistoryManager.remove(this, item)
                renderHistory()
                showDeleteNotice(item)
            }
            historyBinding.historyList.addView(row.root)
        }
        if (items.isEmpty()) {
            historyPopup.dismiss()
        } else if (historyPopup.isShowing) {
            historyPopup.height = historyPopupHeight()
            historyPopup.update(historyPopup.width, historyPopup.height)
        }
    }
}
