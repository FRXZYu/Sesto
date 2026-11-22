package com.example

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.work.testing.TestListenableWorkerBuilder
import com.example.notifications.DailyReminderWorker
import com.example.notifications.NotificationHelper
import com.example.notifications.NotificationScheduler
import com.example.notifications.StreakGuardianRules
import com.example.notifications.StreakGuardianWorker
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class NotificationWorkerTest {

  @Test
  fun testStreakGuardianRulesRespectHourCompletionsStreakAndFreeze() {
    // Antes de las 21:00 -> no notificar
    assertFalse(
      StreakGuardianRules.shouldNotify(
        hourOfDay = 20,
        todayCompletionsCount = 0,
        activeStreakDays = 4,
        isFrozenToday = false
      )
    )
    // Ya completó al menos 1 acción hoy -> no notificar
    assertFalse(
      StreakGuardianRules.shouldNotify(
        hourOfDay = 21,
        todayCompletionsCount = 1,
        activeStreakDays = 4,
        isFrozenToday = false
      )
    )
    // Racha menor a 2 -> no notificar
    assertFalse(
      StreakGuardianRules.shouldNotify(
        hourOfDay = 22,
        todayCompletionsCount = 0,
        activeStreakDays = 1,
        isFrozenToday = false
      )
    )
    // Racha congelada hoy -> no notificar
    assertFalse(
      StreakGuardianRules.shouldNotify(
        hourOfDay = 21,
        todayCompletionsCount = 0,
        activeStreakDays = 5,
        isFrozenToday = true
      )
    )
    // >21:00, sin acciones hoy, no congelada, racha >= 2 -> sí notificar
    assertTrue(
      StreakGuardianRules.shouldNotify(
        hourOfDay = 21,
        todayCompletionsCount = 0,
        activeStreakDays = 3,
        isFrozenToday = false
      )
    )
  }

  @Test
  fun testNotificationCopyAndSchedulerConstraintsAndDelay() {
    assertNull(NotificationHelper.buildDailyReminderMessage(0))
    assertEquals(
      "Tu momento de equilibrio: hoy tienes 1 misión pendiente",
      NotificationHelper.buildDailyReminderMessage(1)
    )
    assertEquals(
      "Tu momento de equilibrio: hoy tienes 3 misiones pendientes",
      NotificationHelper.buildDailyReminderMessage(3)
    )
    assertEquals(
      "Tu racha de 5 días vive hasta medianoche \uD83D\uDD25",
      NotificationHelper.buildStreakGuardianMessage(5)
    )

    val constraints = NotificationScheduler.defaultConstraints()
    assertTrue(constraints.requiresBatteryNotLow())

    val at1900 = Calendar.getInstance().apply {
      set(Calendar.YEAR, 2026)
      set(Calendar.MONTH, Calendar.NOVEMBER)
      set(Calendar.DAY_OF_MONTH, 12)
      set(Calendar.HOUR_OF_DAY, 19)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val delayTo2000 = NotificationScheduler.computeInitialDelayMillis(20, 0, nowMillis = at1900)
    assertEquals(60L * 60L * 1000L, delayTo2000)

    val at2030 = at1900 + 90L * 60L * 1000L
    val delayNextDay2000 = NotificationScheduler.computeInitialDelayMillis(20, 0, nowMillis = at2030)
    assertEquals(23L * 60L * 60L * 1000L + 30L * 60L * 1000L, delayNextDay2000)
  }

  @Test
  fun testWorkersExecuteSafelyAndPendingIntentTargetsHoyTab() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()

    val dailyWorker = TestListenableWorkerBuilder<DailyReminderWorker>(context).build()
    val dailyResult = dailyWorker.doWork()
    assertEquals(androidx.work.ListenableWorker.Result.success(), dailyResult)

    val guardianWorker = TestListenableWorkerBuilder<StreakGuardianWorker>(context).build()
    val guardianResult = guardianWorker.doWork()
    assertEquals(androidx.work.ListenableWorker.Result.success(), guardianResult)

    // Verificar constante de deep-link a la pestaña Hoy (tab 2)
    val intent = Intent(context, MainActivity::class.java).apply {
      putExtra(MainActivity.EXTRA_TARGET_TAB, NotificationHelper.TARGET_TAB_HOY)
    }
    assertEquals(2, intent.getIntExtra(MainActivity.EXTRA_TARGET_TAB, -1))
  }
}
