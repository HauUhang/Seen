package com.jusou.app

import android.app.Dialog
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.EditText
import android.widget.Button
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowDialog

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class MainActivityTest {
    private val context: Context get() = RuntimeEnvironment.getApplication()

    private fun historyPopup(activity: MainActivity): PopupWindow =
        MainActivity::class.java.getDeclaredField("historyPopup").apply { isAccessible = true }
            .get(activity) as PopupWindow

    private fun historyList(activity: MainActivity): LinearLayout =
        historyPopup(activity).contentView.findViewById(R.id.historyList)

    private fun openHistory(activity: MainActivity) {
        activity.findViewById<EditText>(R.id.etQuery).requestFocus()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Before fun resetHistory() {
        SearchHistoryManager.clear(context)
        context.getSharedPreferences("jusou_prefs", Context.MODE_PRIVATE).edit()
            .remove("enabled_platforms").commit()
    }
    @After fun clearHistory() = resetHistory()

    @Test fun emptyHistoryStaysHiddenWhenSearchFieldIsClicked() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertFalse(historyPopup(activity).isShowing)
            openHistory(activity)
            assertFalse(historyPopup(activity).isShowing)
        }
    }

    @Test fun savedHistoryOpensOnFirstFocusWithoutMovingPlatformCards() {
        SearchHistoryManager.add(context, "咖啡")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val platforms = activity.findViewById<View>(R.id.platformContainer)
            assertFalse(historyPopup(activity).isShowing)
            openHistory(activity)
            assertTrue(historyPopup(activity).isShowing)
            assertEquals("清空历史记录",
                historyPopup(activity).contentView.findViewById<TextView>(R.id.tvClearHistory).text.toString())
            assertEquals(1, historyList(activity).childCount)
            assertEquals(platforms.parent, activity.findViewById<View>(R.id.content))
        }
    }

    @Test fun savedHistoryOpensOnFirstClick() {
        SearchHistoryManager.add(context, "咖啡")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertFalse(historyPopup(activity).isShowing)
            activity.findViewById<EditText>(R.id.etQuery).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(historyPopup(activity).isShowing)
            assertEquals(1, historyList(activity).childCount)
        }
    }

    @Test fun typingFiltersHistoryAndClearingInputReopensSuggestions() {
        SearchHistoryManager.add(context, "咖啡")
        SearchHistoryManager.add(context, "徒步")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val input = activity.findViewById<EditText>(R.id.etQuery)
            openHistory(activity)
            assertEquals(2, historyList(activity).childCount)
            input.setText("咖")
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(1, historyList(activity).childCount)
            input.setText("不存在")
            shadowOf(Looper.getMainLooper()).idle()
            assertFalse(historyPopup(activity).isShowing)
            input.setText("")
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(historyPopup(activity).isShowing)
            assertEquals(2, historyList(activity).childCount)
        }
    }

    @Test fun blankSearchShowsInlineErrorWithoutLaunchingAnything() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val input = activity.findViewById<EditText>(R.id.etQuery)
            input.setText("   ")
            activity.findViewById<View>(R.id.btnDouyin).performClick()
            assertNotNull(activity.findViewById<TextInputLayout>(R.id.searchInput).error)
            assertNull(shadowOf(activity).nextStartedActivity)
            input.setText("相机")
            assertNull(activity.findViewById<TextInputLayout>(R.id.searchInput).error)
        }
    }

    @Test fun historyRefillsFullQueryWithoutOpeningAPlatform() {
        val query = "很长的搜索关键词".repeat(30)
        SearchHistoryManager.add(context, query)
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val input = activity.findViewById<EditText>(R.id.etQuery)
            openHistory(activity)
            val row = historyList(activity).getChildAt(0)
            row.findViewById<TextView>(R.id.historyQuery).performClick()
            assertEquals(query, input.text.toString())
            assertEquals(query.length, input.selectionStart)
            assertFalse(historyPopup(activity).isShowing)
            assertNull(shadowOf(activity).nextStartedActivity)
        }
    }

    @Test fun clearHistoryRequiresConfirmationAndCancelPreservesIt() {
        SearchHistoryManager.add(context, "徒步路线")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            openHistory(activity)
            val clear = historyPopup(activity).contentView.findViewById<View>(R.id.tvClearHistory)
            clear.performClick()
            val cancelDialog: Dialog = ShadowDialog.getLatestDialog()
            cancelDialog.findViewById<Button>(android.R.id.button2).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(listOf("徒步路线"), SearchHistoryManager.load(context))
            clear.performClick()
            ShadowDialog.getLatestDialog().findViewById<Button>(android.R.id.button1).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(SearchHistoryManager.load(context).isEmpty())
            assertFalse(historyPopup(activity).isShowing)
        }
    }

    @Test fun oneHistoryItemCanBeDeletedWithoutClearingTheOthers() {
        SearchHistoryManager.add(context, "咖啡")
        SearchHistoryManager.add(context, "徒步")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            openHistory(activity)
            val list = historyList(activity)
            list.getChildAt(0).findViewById<View>(R.id.btnDeleteHistory).performClick()
            assertEquals(listOf("咖啡"), SearchHistoryManager.load(context))
            assertEquals(1, list.childCount)
            assertTrue(historyPopup(activity).isShowing)
            val notice = activity.findViewById<android.widget.TextView>(R.id.deleteNotice)
            assertEquals(View.VISIBLE, notice.visibility)
            assertEquals("已删除“徒步”", notice.text.toString())
        }
    }

    @Test fun deletingLastHistoryItemClosesThePanel() {
        SearchHistoryManager.add(context, "咖啡")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            openHistory(activity)
            historyPopup(activity).contentView.findViewById<View>(R.id.btnDeleteHistory).performClick()
            assertTrue(SearchHistoryManager.load(context).isEmpty())
            assertFalse(historyPopup(activity).isShowing)
        }
    }

    @Test fun githubLinksOpenRepositoryAndNewIssuePage() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.findViewById<View>(R.id.btnGithub).performClick()
            assertEquals("https://github.com/HauUhang/Seen",
                shadowOf(activity).nextStartedActivity.data.toString())
            activity.findViewById<View>(R.id.btnSuggestPlatform).performClick()
            assertEquals("https://github.com/HauUhang/Seen/issues/new",
                shadowOf(activity).nextStartedActivity.data.toString())
        }
    }

    @Test fun enabledPlatformsArePersistedAndRendered() {
        PlatformPreferences.save(context, setOf(Platform.X, Platform.YOUTUBE))
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertNull(activity.findViewById<View>(R.id.btnDouyin))
            assertNotNull(activity.findViewById<View>(R.id.btnX))
            assertNotNull(activity.findViewById<View>(R.id.btnYoutube))
        }
        assertEquals(setOf(Platform.X, Platform.YOUTUBE), PlatformPreferences.enabled(context))
    }

    @Test fun newPlatformsOpenEncodedSearchResults() {
        val urls = mapOf(
            Platform.X to "https://x.com/search?q=A%20B%26%E4%B8%AD%E6%96%87",
            Platform.REDDIT to "https://www.reddit.com/search/?q=A%20B%26%E4%B8%AD%E6%96%87",
            Platform.YOUTUBE to "https://www.youtube.com/results?search_query=A%20B%26%E4%B8%AD%E6%96%87",
        )
        urls.forEach { (platform, expected) ->
            val attempted = mutableListOf<Intent>()
            val launcher = object : ContextWrapper(context) {
                override fun startActivity(intent: Intent) { attempted.add(intent) }
            }
            assertTrue(SearchEngine.open(launcher, platform, "A B&中文"))
            assertEquals(expected, attempted.single().data.toString())
        }
    }

    @Test fun querySurvivesActivityRecreation() {
        val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
        controller.get().findViewById<EditText>(R.id.etQuery).setText("周末去哪里")
        val state = Bundle()
        controller.pause().saveInstanceState(state).stop().destroy()
        Robolectric.buildActivity(MainActivity::class.java).setup(state).use { recreated ->
            assertEquals("周末去哪里", recreated.get().findViewById<EditText>(R.id.etQuery).text.toString())
        }
    }

    @Test fun unavailableAppsFallBackToAnEncodedWebSearch() {
        val attempted = mutableListOf<Intent>()
        val browserOnly = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) {
                attempted.add(intent)
                if (intent.data?.scheme != "https") throw ActivityNotFoundException()
            }
        }
        assertTrue(SearchEngine.open(browserOnly, Platform.BILIBILI, "A B&中文"))
        assertEquals("tv.danmaku.bili", attempted[0].`package`)
        assertEquals("com.bilibili.app.in", attempted[1].`package`)
        assertEquals("https://search.bilibili.com/all?keyword=A%20B%26%E4%B8%AD%E6%96%87", attempted.last().data.toString())
    }

    @Test fun missingBrowserReturnsFailureInsteadOfSilentlySucceeding() {
        val unavailable = object : ContextWrapper(context) {
            override fun startActivity(intent: Intent) { throw ActivityNotFoundException() }
        }
        assertFalse(SearchEngine.open(unavailable, Platform.ZHIHU, "天气"))
    }

    @Test fun allFourCardsLaunchTheirPlatformAndSaveTheTrimmedQueryOnce() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val input = activity.findViewById<EditText>(R.id.etQuery)
            listOf(
                R.id.btnDouyin to "snssdk1128",
                R.id.btnXhs to "xhsdiscover",
                R.id.btnZhihu to "zhihu",
                R.id.btnBilibili to "bilibili",
            ).forEach { (id, scheme) ->
                input.setText("  徒步路线  ")
                activity.findViewById<View>(id).performClick()
                assertEquals(scheme, shadowOf(activity).nextStartedActivity.data?.scheme)
                assertEquals("", input.text.toString())
            }
            assertEquals(listOf("徒步路线"), SearchHistoryManager.load(context))
        }
    }
}
