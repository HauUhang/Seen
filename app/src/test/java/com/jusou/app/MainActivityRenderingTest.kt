package com.jusou.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import kotlin.math.abs

/** Render the actual Android views, rather than maintaining a separate UI mockup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityRenderingTest {
    private fun historyPopup(activity: MainActivity): PopupWindow =
        MainActivity::class.java.getDeclaredField("historyPopup").apply { isAccessible = true }
            .get(activity) as PopupWindow

    @Test fun lightHome() = render("home-light", 393, 852)

    @Test
    @Config(qualifiers = "w393dp-h852dp-port-night-mdpi")
    fun darkHome() = render("home-dark", 393, 852)

    @Test
    @Config(qualifiers = "w320dp-h640dp-port-mdpi")
    fun narrowScreenWithLongHistory() = render("home-compact-history", 320, 640, true)

    @Test
    @Config(qualifiers = "w800dp-h1000dp-port-mdpi")
    fun tabletContentStaysReadable() = render("home-tablet", 800, 1000, true)

    @Test fun deletionNoticeIsCentered() {
        val context = RuntimeEnvironment.getApplication()
        SearchHistoryManager.clear(context)
        SearchHistoryManager.add(context, "周末徒步路线")
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            activity.findViewById<EditText>(R.id.etQuery).requestFocus()
            shadowOf(Looper.getMainLooper()).idle()
            historyPopup(activity).contentView.findViewById<View>(R.id.btnDeleteHistory).performClick()
            val root = activity.findViewById<ViewGroup>(android.R.id.content).getChildAt(0)
            val width = 393
            val height = 852
            root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, width, height)
            val notice = activity.findViewById<TextView>(R.id.deleteNotice)
            assertTrue(abs(notice.left + notice.width / 2 - width / 2) <= 1)
            assertTrue(abs(notice.top + notice.height / 2 - height / 2) <= 1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            val output = File("build/reports/ui/delete-notice.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
        }
    }

    private fun render(name: String, width: Int, height: Int, withHistory: Boolean = false) {
        val context = RuntimeEnvironment.getApplication()
        SearchHistoryManager.clear(context)
        context.getSharedPreferences("jusou_prefs", android.content.Context.MODE_PRIVATE)
            .edit().remove("enabled_platforms").commit()
        if (withHistory) {
            listOf("周末徒步路线", "咖啡怎么冲", "一个很长的关键词".repeat(20)).forEach {
                SearchHistoryManager.add(context, it)
            }
        }
        Robolectric.buildActivity(MainActivity::class.java).setup().use { controller ->
            val activity = controller.get()
            val root = activity.findViewById<View>(R.id.scrollView)
            root.measure(
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY),
            )
            root.layout(0, 0, width, height)
            val platformTop = activity.findViewById<View>(R.id.platformContainer).top
            if (withHistory) {
                activity.findViewById<EditText>(R.id.etQuery).requestFocus()
                shadowOf(Looper.getMainLooper()).idle()
            }
            val content = activity.findViewById<View>(R.id.content)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(activity.getColor(R.color.background))
            // Draw unscrolled content so the popup and its anchor share one coordinate space.
            canvas.save()
            canvas.translate(content.left.toFloat(), content.top.toFloat())
            content.draw(canvas)
            canvas.restore()
            if (withHistory) {
                val popup = historyPopup(activity)
                assertTrue("History popup must open on first focus", popup.isShowing)
                assertEquals("Platform cards must stay in place", platformTop,
                    activity.findViewById<View>(R.id.platformContainer).top)
                val popupView = popup.contentView
                popupView.measure(
                    View.MeasureSpec.makeMeasureSpec(popup.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(popup.height, View.MeasureSpec.EXACTLY),
                )
                popupView.layout(0, 0, popup.width, popup.height)
                val anchor = activity.findViewById<View>(R.id.searchInput)
                canvas.save()
                canvas.translate((content.left + anchor.left).toFloat(),
                    (content.top + anchor.bottom + 4).toFloat())
                popupView.draw(canvas)
                canvas.restore()
                val rows = popupView.findViewById<LinearLayout>(R.id.historyList)
                for (index in 0 until rows.childCount) {
                    assertTrue("History row must fit the popup", rows.getChildAt(index).width <= rows.width)
                }
            }
            val output = File("build/reports/ui/$name.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            // Also retain the scrollable content so visual review includes the footer.
            val fullContent = (root as ViewGroup).getChildAt(0)
            val fullBitmap = Bitmap.createBitmap(width, fullContent.height, Bitmap.Config.ARGB_8888)
            val fullCanvas = Canvas(fullBitmap)
            fullCanvas.drawColor(activity.getColor(R.color.background))
            fullContent.draw(fullCanvas)
            File(output.parentFile, "$name-full.png").outputStream().use {
                fullBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            fullBitmap.recycle()
            val card = activity.findViewById<View>(R.id.btnDouyin)
            assertTrue("Platform card size: ${card.width} x ${card.height}; content: ${activity.findViewById<View>(R.id.content).width}", card.width >= 48 && card.height >= 48)
            assertTrue("Tablet content must be bounded", activity.findViewById<View>(R.id.content).width <= 560)
            if (!withHistory) {
                assertEquals("No unsolicited keyboard focus", false, activity.findViewById<EditText>(R.id.etQuery).hasFocus())
                assertTrue("History must stay hidden on launch", !historyPopup(activity).isShowing)
            }
        }
    }
}
