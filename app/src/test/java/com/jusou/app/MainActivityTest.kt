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
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
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

    @Before fun resetHistory() = SearchHistoryManager.clear(context)
    @After fun clearHistory() = SearchHistoryManager.clear(context)

    @Test fun emptyHistoryShowsExplanationAndHidesClearAction() {
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.historyEmpty).visibility)
            assertEquals(View.GONE, activity.findViewById<View>(R.id.tvClearHistory).visibility)
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
            val chip = activity.findViewById<ChipGroup>(R.id.chipGroup).getChildAt(0) as Chip
            assertTrue(chip.maxWidth > 0)
            chip.performClick()
            val input = activity.findViewById<EditText>(R.id.etQuery)
            assertEquals(query, input.text.toString())
            assertEquals(query.length, input.selectionStart)
            assertNull(shadowOf(activity).nextStartedActivity)
        }
    }

    @Test fun clearHistoryRequiresConfirmationAndCancelPreservesIt() {
        SearchHistoryManager.add(context, "徒步路线")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val clear = activity.findViewById<View>(R.id.tvClearHistory)
            clear.performClick()
            val cancelDialog: Dialog = ShadowDialog.getLatestDialog()
            cancelDialog.findViewById<Button>(android.R.id.button2).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertEquals(listOf("徒步路线"), SearchHistoryManager.load(context))
            clear.performClick()
            ShadowDialog.getLatestDialog().findViewById<Button>(android.R.id.button1).performClick()
            shadowOf(Looper.getMainLooper()).idle()
            assertTrue(SearchHistoryManager.load(context).isEmpty())
            assertEquals(View.VISIBLE, activity.findViewById<View>(R.id.historyEmpty).visibility)
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
            activity.findViewById<EditText>(R.id.etQuery).setText("  徒步路线  ")
            listOf(
                R.id.btnDouyin to "snssdk1128",
                R.id.btnXhs to "xhsdiscover",
                R.id.btnZhihu to "zhihu",
                R.id.btnBilibili to "bilibili",
            ).forEach { (id, scheme) ->
                activity.findViewById<View>(id).performClick()
                assertEquals(scheme, shadowOf(activity).nextStartedActivity.data?.scheme)
            }
            assertEquals(listOf("徒步路线"), SearchHistoryManager.load(context))
        }
    }
}
