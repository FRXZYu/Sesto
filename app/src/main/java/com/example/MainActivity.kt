package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.example.data.AppThemeMode
import com.example.data.BalanceRepository
import com.example.ui.AppScreenState
import com.example.ui.MainViewModel
import com.example.ui.screens.BirthDateScreen
import com.example.ui.screens.LoadingScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MainHomeScreen
import com.example.ui.screens.NameInputScreen
import com.example.ui.screens.ProfilePhotoScreen
import com.example.ui.screens.QuestionnaireScreen
import com.example.ui.screens.ReportScreen
import com.example.ui.screens.TransitionScreen
import com.example.ui.screens.ZodiacAndGoalScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.EquilibrioWidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  companion object {
    const val EXTRA_TARGET_TAB = "extra_target_tab"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    applyTargetTabFromIntent(intent)

    setContent {
      val appPrefs by viewModel.appPreferences.collectAsStateWithLifecycle()
      val useDarkTheme = when (appPrefs.themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
      }
      MyApplicationTheme(darkTheme = useDarkTheme) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          EquilibrioApp(viewModel = viewModel)
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    applyTargetTabFromIntent(intent)
  }

  override fun onResume() {
    super.onResume()
    val appCtx = applicationContext
    lifecycleScope.launch(Dispatchers.IO) {
      BalanceRepository(appCtx).refreshDayIfNeeded()
      EquilibrioWidgetUpdater.requestUpdate(appCtx)
    }
  }

  private fun applyTargetTabFromIntent(intent: Intent?) {
    val targetTab = intent?.getIntExtra(EXTRA_TARGET_TAB, -1) ?: -1
    if (targetTab in 0..4) {
      viewModel.selectHomeTab(targetTab)
      if (viewModel.uiState.value !is AppScreenState.Home) {
        viewModel.backToHomeFromReport()
      }
    }
  }
}

@Composable
fun EquilibrioApp(viewModel: MainViewModel) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val selectedHomeTab by viewModel.selectedHomeTab.collectAsStateWithLifecycle()

  Crossfade(
    targetState = uiState,
    animationSpec = tween(350),
    label = "screen_crossfade"
  ) { state ->
    when (state) {
      is AppScreenState.Login -> {
        LoginScreen(
          onContinueWithGoogle = {
            viewModel.onContinueWithGoogle()
          }
        )
      }

      is AppScreenState.BirthDateSelection -> {
        BackHandler {
          // Stay or handle back
        }
        BirthDateScreen(
          googleName = state.googleName,
          googleEmail = state.googleEmail,
          onBirthDateSubmitted = { day, month, year, zodiacSign, zodiacSymbol ->
            viewModel.onBirthDateSubmitted(day, month, year, zodiacSign, zodiacSymbol, state.googleName)
          }
        )
      }

      is AppScreenState.Questionnaire -> {
        BackHandler(enabled = state.currentStep > 0) {
          viewModel.previousQuestion()
        }

        QuestionnaireScreen(
          currentStep = state.currentStep,
          scores = state.scores,
          onScoreSelected = { dimIndex, score ->
            viewModel.onScoreChanged(dimIndex, score)
          },
          onNext = {
            viewModel.nextQuestion()
          },
          onPrevious = {
            viewModel.previousQuestion()
          },
          onJumpToStep = { step ->
            viewModel.jumpToQuestion(step)
          }
        )
      }

      is AppScreenState.TransitioningToReport -> {
        TransitionScreen(
          scores = state.scores,
          onComplete = {
            viewModel.onTransitionComplete(state.scores)
          }
        )
      }

      is AppScreenState.Report -> {
        BackHandler {
          viewModel.backToHomeFromReport()
        }

        ReportScreen(
          report = state.report,
          selectedDimensionIndex = state.selectedDimensionIndex,
          filter = state.filter,
          onDimensionSelect = { dimIndex ->
            viewModel.selectDimensionInReport(dimIndex)
          },
          onFilterChange = { filter ->
            viewModel.setReportFilter(filter)
          },
          onRetake = {
            viewModel.retakeAssessment()
          },
          onContinue = {
            viewModel.continueFromReport()
          },
          isProfileCompleted = state.isProfileCompleted
        )
      }

      is AppScreenState.Loading -> {
        LoadingScreen(
          message = state.quote,
          onFinished = {
            viewModel.onLoadingFinished()
          }
        )
      }

      is AppScreenState.NameInput -> {
        BackHandler {
          viewModel.backFromNameInput()
        }

        NameInputScreen(
          initialName = state.currentName,
          onNameSubmitted = { name ->
            viewModel.onNameSubmitted(name)
          }
        )
      }

      is AppScreenState.ProfilePhoto -> {
        BackHandler {
          viewModel.backToNameInput()
        }

        ProfilePhotoScreen(
          currentPhotoUri = state.photoUri,
          onPhotoSelected = { uri ->
            viewModel.onPhotoSelected(uri)
          },
          onContinue = { uri ->
            viewModel.onPhotoConfirmed(uri)
          }
        )
      }

      is AppScreenState.ZodiacAndGoal -> {
        BackHandler {
          viewModel.backToProfilePhoto()
        }

        ZodiacAndGoalScreen(
          initialZodiac = state.zodiacSign,
          initialGoal = state.monthlyGoal,
          onFinished = { zodiacSign, zodiacSymbol, monthlyGoal ->
            viewModel.onZodiacAndGoalFinished(zodiacSign, zodiacSymbol, monthlyGoal)
          }
        )
      }

      is AppScreenState.Home -> {
        MainHomeScreen(
          userProfile = state.userProfile,
          report = state.report,
          selectedTab = selectedHomeTab,
          onSelectTab = { tab ->
            viewModel.selectHomeTab(tab)
          },
          onOpenReport = {
            viewModel.openDetailedReportFromHome()
          },
          onUpdateGoal = { newGoal ->
            viewModel.onUpdateGoal(newGoal)
          },
          onUpdateGoalWithReason = { newGoal, reason ->
            viewModel.onUpdateGoal(newGoal, reason)
          },
          onUpdateProfileIdentity = { name, zodiacSign, zodiacSymbol ->
            viewModel.onUpdateProfileIdentity(name, zodiacSign, zodiacSymbol)
          }
        )
      }
    }
  }
}
