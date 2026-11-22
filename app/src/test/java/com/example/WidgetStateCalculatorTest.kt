package com.example

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.example.widget.EquilibrioWidgetCalculator
import com.example.widget.EquilibrioWidgetHexagonRenderer
import com.example.widget.EquilibrioWidgetProvider
import com.example.widget.WidgetLayoutMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class WidgetStateCalculatorTest {

  @Test
  fun `calculateSnapshot formats planned completions and streak labels correctly`() {
    val planIds = listOf("m1", "m2", "m3")
    val scores = mapOf(0 to 4f, 1 to 6f, 2 to 2f, 3 to 8f, 4 to 0f, 5 to 12f)

    val emptySnapshot = EquilibrioWidgetCalculator.calculateSnapshot(
      todayPlanMissionIds = planIds,
      todayCompletedMissionIds = emptySet(),
      realScoresFloat = scores,
      activeStreakDays = 0
    )
    assertEquals(0, emptySnapshot.completedTodayOfThree)
    assertEquals("0 de 3 misiones hoy", emptySnapshot.missionsLabel)
    assertEquals("0 días", emptySnapshot.streakLabel)
    assertEquals(10f, emptySnapshot.realScoresFloat[5]!!, 0.001f)

    val oneSnapshot = EquilibrioWidgetCalculator.calculateSnapshot(
      todayPlanMissionIds = planIds,
      todayCompletedMissionIds = setOf("m2"),
      realScoresFloat = scores,
      activeStreakDays = 1
    )
    assertEquals(1, oneSnapshot.completedTodayOfThree)
    assertEquals("1 de 3 misiones hoy", oneSnapshot.missionsLabel)
    assertEquals("1 día", oneSnapshot.streakLabel)

    val clampedSnapshot = EquilibrioWidgetCalculator.calculateSnapshot(
      todayPlanMissionIds = planIds,
      todayCompletedMissionIds = setOf("m1", "m2", "m3", "extra1", "extra2"),
      realScoresFloat = scores,
      activeStreakDays = 7
    )
    assertEquals(3, clampedSnapshot.completedTodayOfThree)
    assertEquals("3 de 3 misiones hoy", clampedSnapshot.missionsLabel)
    assertEquals("7 días", clampedSnapshot.streakLabel)
  }

  @Test
  fun `calculateSnapshot falls back to completed ids when daily plan is not yet created`() {
    val snapshot = EquilibrioWidgetCalculator.calculateSnapshot(
      todayPlanMissionIds = emptyList(),
      todayCompletedMissionIds = setOf("custom_1", "custom_2"),
      realScoresFloat = mapOf(0 to 2f),
      activeStreakDays = 3
    )

    assertEquals(2, snapshot.completedTodayOfThree)
    assertEquals("2 de 3 misiones hoy", snapshot.missionsLabel)
    assertEquals("3 días", snapshot.streakLabel)
    assertEquals(0f, snapshot.realScoresFloat[4]!!, 0.001f)
  }

  @Test
  fun `resolveLayoutMode switches cleanly between 4x1 and 4x2`() {
    assertEquals(
      WidgetLayoutMode.COMPACT_4X1,
      EquilibrioWidgetCalculator.resolveLayoutMode(minWidthDp = 250, minHeightDp = 56)
    )
    assertEquals(
      WidgetLayoutMode.COMPACT_4X1,
      EquilibrioWidgetCalculator.resolveLayoutMode(minWidthDp = 250, minHeightDp = 99)
    )
    assertEquals(
      WidgetLayoutMode.EXPANDED_4X2,
      EquilibrioWidgetCalculator.resolveLayoutMode(minWidthDp = 250, minHeightDp = 100)
    )
    assertEquals(
      WidgetLayoutMode.EXPANDED_4X2,
      EquilibrioWidgetCalculator.resolveLayoutMode(minWidthDp = 250, minHeightDp = 140)
    )
    assertEquals(
      WidgetLayoutMode.EXPANDED_4X2,
      EquilibrioWidgetCalculator.resolveLayoutMode(minWidthDp = 0, minHeightDp = 0)
    )
  }

  @Test
  @GraphicsMode(GraphicsMode.Mode.NATIVE)
  fun `hexagon renderer draws visible pixels in both light and dark themes`() {
    val scores = mapOf(0 to 6f, 1 to 8f, 2 to 4f, 3 to 10f, 4 to 2f, 5 to 7f)
    val lightBitmap = EquilibrioWidgetHexagonRenderer.renderBitmap(
      sizePx = 120,
      realScores = scores,
      gridColor = 0xFFD1D5DB.toInt(),
      fillColor = 0x381E2024.toInt(),
      strokeColor = 0xFF1E2024.toInt(),
      vertexRingColor = 0xFFFAFAFC.toInt()
    )
    val darkBitmap = EquilibrioWidgetHexagonRenderer.renderBitmap(
      sizePx = 120,
      realScores = scores,
      gridColor = 0xFF374151.toInt(),
      fillColor = 0x45F3F4F6.toInt(),
      strokeColor = 0xFFF3F4F6.toInt(),
      vertexRingColor = 0xFF1A1D24.toInt()
    )
    val minClampedBitmap = EquilibrioWidgetHexagonRenderer.renderBitmap(
      sizePx = 16,
      realScores = emptyMap(),
      gridColor = 0xFFD1D5DB.toInt(),
      fillColor = 0x381E2024.toInt(),
      strokeColor = 0xFF1E2024.toInt(),
      vertexRingColor = 0xFFFAFAFC.toInt()
    )

    assertEquals(120, lightBitmap.width)
    assertEquals(120, darkBitmap.height)
    assertEquals(64, minClampedBitmap.width)
    assertEquals(Bitmap.Config.ARGB_8888, lightBitmap.config)
    assertFalse(lightBitmap.isRecycled)
    assertTrue((lightBitmap.getPixel(60, 60) ushr 24) > 0)
    assertTrue((darkBitmap.getPixel(60, 60) ushr 24) > 0)
  }

  @Test
  fun `buildRemoteViews chooses expected layout and navigates to Hoy tab on click`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val snapshot = EquilibrioWidgetCalculator.calculateSnapshot(
      todayPlanMissionIds = listOf("a", "b", "c"),
      todayCompletedMissionIds = setOf("a", "b"),
      realScoresFloat = mapOf(0 to 4f, 1 to 6f, 2 to 8f, 3 to 4f, 4 to 2f, 5 to 6f),
      activeStreakDays = 4
    )

    val compactViews = EquilibrioWidgetProvider.buildRemoteViews(
      context = context,
      snapshot = snapshot,
      layoutMode = WidgetLayoutMode.COMPACT_4X1
    )
    assertEquals(R.layout.widget_equilibrio_4x1, compactViews.layoutId)

    val expandedViews = EquilibrioWidgetProvider.buildRemoteViews(
      context = context,
      snapshot = snapshot,
      layoutMode = WidgetLayoutMode.EXPANDED_4X2
    )
    assertEquals(R.layout.widget_equilibrio_4x2, expandedViews.layoutId)

    val appliedView = expandedViews.apply(context, null)
    val missionsText = appliedView.findViewById<android.widget.TextView>(R.id.widget_missions_text)
    val streakText = appliedView.findViewById<android.widget.TextView>(R.id.widget_streak_text)
    assertEquals("2 de 3 misiones hoy", missionsText.text.toString())
    assertEquals("4 días", streakText.text.toString())

    // Verify PendingIntent opens MainActivity directly on Hoy tab (index 2)
    appliedView.performClick()
    val shadowApp = Shadows.shadowOf(context as android.app.Application)
    val startedIntent: Intent? = shadowApp.nextStartedActivity
    assertNotNull(startedIntent)
    assertEquals(MainActivity::class.java.name, startedIntent?.component?.className)
    assertEquals(2, startedIntent?.getIntExtra(MainActivity.EXTRA_TARGET_TAB, -1))
    assertNotNull(AppWidgetManager.getInstance(context))
  }
}
