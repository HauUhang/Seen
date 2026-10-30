package com.jusou.app

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import com.google.android.material.chip.ChipGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Render the actual Android views, rather than maintaining a separate UI mockup. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w393dp-h852dp-port-mdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class MainActivityRenderingTest {
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

    private fun render(name: String, width: Int, height: Int, withHistory: Boolean = false) {
        val context = RuntimeEnvironment.getApplication()
        SearchHistoryManager.clear(context)
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
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            root.draw(Canvas(bitmap))
            val output = File("build/reports/ui/$name.png")
            output.parentFile?.mkdirs()
            output.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            bitmap.recycle()
            // Also retain the scrollable content so visual review includes the footer.
            val content = (root as ViewGroup).getChildAt(0)
            val fullBitmap = Bitmap.createBitmap(width, content.height, Bitmap.Config.ARGB_8888)
            val fullCanvas = Canvas(fullBitmap)
            fullCanvas.drawColor(activity.getColor(R.color.background))
            content.draw(fullCanvas)
            File(output.parentFile, "$name-full.png").outputStream().use {
                fullBitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            fullBitmap.recycle()
            val card = activity.findViewById<View>(R.id.btnDouyin)
            assertTrue("Platform card size: ${card.width} x ${card.height}; content: ${activity.findViewById<View>(R.id.content).width}", card.width >= 48 && card.height >= 48)
            assertTrue("Tablet content must be bounded", activity.findViewById<View>(R.id.content).width <= 560)
            assertEquals("No unsolicited keyboard focus", false, activity.findViewById<EditText>(R.id.etQuery).hasFocus())
            val chips = activity.findViewById<ChipGroup>(R.id.chipGroup)
            for (index in 0 until chips.childCount) {
                assertTrue("Long history must fit the content", chips.getChildAt(index).width <= chips.width)
            }
        }
    }
}
