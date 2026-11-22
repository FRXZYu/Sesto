package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.analysis.LifeAnalysisEngine
import com.example.data.AppPreferences
import com.example.data.AssessmentRepository
import com.example.data.CloudSyncHelper
import com.example.model.AssessmentReport
import com.example.model.DimensionsProvider
import com.example.model.ReportFilter
import com.example.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppScreenState {
  data object Login : AppScreenState

  data class BirthDateSelection(
    val googleName: String,
    val googleEmail: String
  ) : AppScreenState

  data class NameInput(
    val currentName: String = ""
  ) : AppScreenState

  data class ProfilePhoto(
    val name: String,
    val photoUri: String? = null
  ) : AppScreenState

  data class ZodiacAndGoal(
    val name: String,
    val photoUri: String?,
    val zodiacSign: String = "Aries",
    val monthlyGoal: String = "Quiero ser mi mejor versión"
  ) : AppScreenState

  data class Questionnaire(
    val currentStep: Int = 0,
    val scores: Map<Int, Int> = emptyMap()
  ) : AppScreenState

  data class TransitioningToReport(
    val scores: Map<Int, Int>,
    val previousScores: Map<Int, Int>? = null
  ) : AppScreenState

  data class Report(
    val report: AssessmentReport,
    val selectedDimensionIndex: Int? = null,
    val filter: ReportFilter = ReportFilter.ALL,
    val isProfileCompleted: Boolean = false
  ) : AppScreenState

  data class Loading(
    val quote: String,
    val nextScreen: AppScreenState
  ) : AppScreenState

  data class Home(
    val userProfile: UserProfile,
    val report: AssessmentReport
  ) : AppScreenState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val repository = AssessmentRepository(application)

  private val _uiState = MutableStateFlow<AppScreenState>(AppScreenState.Login)
  val uiState: StateFlow<AppScreenState> = _uiState.asStateFlow()

  private val _selectedHomeTab = MutableStateFlow(2)
  val selectedHomeTab: StateFlow<Int> = _selectedHomeTab.asStateFlow()

  val appPreferences: StateFlow<AppPreferences> = repository.appPrefsFlow

  // Track onboarding state in progress
  private var pendingName: String = ""
  private var pendingPhotoUri: String? = null
  private var pendingZodiacSign: String = "Aries"
  private var pendingZodiacSymbol: String = "♈"
  private var pendingEmail: String = ""

  init {
    loadInitialState()
  }

  private fun loadInitialState() {
    val hasSignedIn = repository.hasSignedIn()
    val isProfileCompleted = repository.isProfileCompleted()
    val currentScores = repository.loadCurrentScores()
    val previousScores = repository.loadPreviousScores()

    if (!hasSignedIn) {
      _uiState.value = AppScreenState.Login
      return
    }

    if (!isProfileCompleted) {
      if (currentScores != null && currentScores.size == 6) {
        val report = LifeAnalysisEngine.generateReport(currentScores, previousScores)
        _uiState.value = AppScreenState.Report(
          report = report,
          selectedDimensionIndex = report.lowestDimension.index,
          isProfileCompleted = false
        )
      } else {
        val existingProfile = repository.loadProfile()
        pendingName = existingProfile.name
        pendingPhotoUri = existingProfile.photoUri
        _uiState.value = AppScreenState.NameInput(currentName = existingProfile.name)
      }
      return
    }

    if (currentScores == null || currentScores.size < 6) {
      _uiState.value = AppScreenState.Questionnaire(
        currentStep = 0,
        scores = emptyMap()
      )
      return
    }

    val report = LifeAnalysisEngine.generateReport(currentScores, previousScores)
    val profile = repository.loadProfile()
    _uiState.value = AppScreenState.Home(userProfile = profile, report = report)
  }

  fun onContinueWithGoogle() {
    val currentUser = FirebaseAuth.getInstance().currentUser
    val displayName = currentUser?.displayName ?: "Kirenia"
    val email = currentUser?.email ?: "kirenia@example.com"
    pendingEmail = email
    pendingPhotoUri = currentUser?.photoUrl?.toString()

    repository.saveAccountEmail(email)
    repository.markSignedIn()

    val nextScreen = AppScreenState.BirthDateSelection(
      googleName = displayName,
      googleEmail = email
    )
    showLoadingTransition(
      quote = "Comencemos a trazar tu camino hacia el equilibrio.",
      nextScreen = nextScreen
    )
  }

  fun onBirthDateSubmitted(day: Int, month: Int, year: Int, zodiacSign: String, zodiacSymbol: String, googleName: String) {
    pendingZodiacSign = zodiacSign
    pendingZodiacSymbol = zodiacSymbol
    pendingName = googleName
    val nextScreen = AppScreenState.NameInput(currentName = googleName)
    showLoadingTransition(
      quote = "Tu signo es $zodiacSign $zodiacSymbol. Ahora confirmemos tu nombre.",
      nextScreen = nextScreen
    )
  }

  fun onNameSubmitted(name: String) {
    pendingName = name.trim().ifEmpty { "Kirenia" }
    val nextScreen = AppScreenState.ProfilePhoto(name = pendingName, photoUri = pendingPhotoUri)
    showLoadingTransition(
      quote = "Reconocer tu identidad es el primer paso hacia una autoestima inquebrantable.",
      nextScreen = nextScreen
    )
  }

  fun onPhotoSelected(photoUri: String?) {
    pendingPhotoUri = photoUri
  }

  fun onPhotoConfirmed(photoUri: String?) {
    pendingPhotoUri = photoUri
    val nextScreen = AppScreenState.ZodiacAndGoal(
      name = pendingName,
      photoUri = pendingPhotoUri,
      zodiacSign = pendingZodiacSign
    )
    showLoadingTransition(
      quote = "La autenticidad es tu mayor superpoder.",
      nextScreen = nextScreen
    )
  }

  fun onZodiacAndGoalFinished(zodiacSign: String, zodiacSymbol: String, monthlyGoal: String) {
    pendingZodiacSign = zodiacSign
    pendingZodiacSymbol = zodiacSymbol
    val updatedProfile = UserProfile(
      name = pendingName,
      photoUri = pendingPhotoUri,
      zodiacSign = zodiacSign,
      zodiacSymbol = zodiacSymbol,
      monthlyGoal = monthlyGoal
    )
    repository.saveProfile(updatedProfile)
    syncUserDataToCloud()

    val currentScores = repository.loadCurrentScores()
    if (currentScores != null && currentScores.size == 6) {
      val previousScores = repository.loadPreviousScores()
      val report = LifeAnalysisEngine.generateReport(currentScores, previousScores)
      val homeScreen = AppScreenState.Home(userProfile = updatedProfile, report = report)
      showLoadingTransition(
        quote = "Tus cimientos están listos. Es hora de conquistar tu mejor versión.",
        nextScreen = homeScreen
      )
    } else {
      showLoadingTransition(
        quote = "Ahora evaluaremos tus 6 dimensiones para trazar tu hexágono inicial.",
        nextScreen = AppScreenState.Questionnaire(currentStep = 0, scores = emptyMap())
      )
    }
  }

  private fun syncUserDataToCloud() {
    viewModelScope.launch {
      val currentUser = FirebaseAuth.getInstance().currentUser
      val uid = currentUser?.uid ?: "test_user_uid"
      val profile = repository.loadProfile()
      val currentScores = repository.loadCurrentScores()
      val previousScores = repository.loadPreviousScores()
      CloudSyncHelper.saveUserDataToCloud(
        uid = uid,
        email = currentUser?.email ?: pendingEmail,
        profile = profile,
        currentScores = currentScores,
        previousScores = previousScores,
        isProfileCompleted = repository.isProfileCompleted(),
        isAssessmentCompleted = repository.hasCompletedAssessment()
      )
    }
  }

  fun onScoreChanged(dimensionIndex: Int, score: Int) {
    val current = _uiState.value
    if (current is AppScreenState.Questionnaire) {
      val updated = current.scores.toMutableMap()
      updated[dimensionIndex] = score
      _uiState.update { current.copy(scores = updated) }
    }
  }

  fun nextQuestion() {
    val current = _uiState.value
    if (current is AppScreenState.Questionnaire) {
      val maxSteps = DimensionsProvider.dimensions.size
      val currentScores = current.scores.toMutableMap()
      if (!currentScores.containsKey(current.currentStep)) {
        currentScores[current.currentStep] = 7
      }

      if (current.currentStep < maxSteps - 1) {
        _uiState.update {
          current.copy(
            currentStep = current.currentStep + 1,
            scores = currentScores
          )
        }
      } else {
        finishAssessment(currentScores)
      }
    }
  }

  fun previousQuestion() {
    val current = _uiState.value
    if (current is AppScreenState.Questionnaire && current.currentStep > 0) {
      _uiState.update { current.copy(currentStep = current.currentStep - 1) }
    }
  }

  fun jumpToQuestion(step: Int) {
    val current = _uiState.value
    if (current is AppScreenState.Questionnaire && step in 0 until DimensionsProvider.dimensions.size) {
      _uiState.update { current.copy(currentStep = step) }
    }
  }

  private fun finishAssessment(scores: Map<Int, Int>) {
    repository.saveScores(scores)
    syncUserDataToCloud()
    val previousScores = repository.loadPreviousScores()
    _uiState.value = AppScreenState.TransitioningToReport(
      scores = scores,
      previousScores = previousScores
    )
  }

  fun onTransitionComplete(scores: Map<Int, Int>) {
    val previousScores = repository.loadPreviousScores()
    val report = LifeAnalysisEngine.generateReport(scores, previousScores)
    _uiState.value = AppScreenState.Report(
      report = report,
      selectedDimensionIndex = report.lowestDimension.index,
      isProfileCompleted = repository.isProfileCompleted()
    )
  }

  fun selectDimensionInReport(index: Int) {
    val current = _uiState.value
    if (current is AppScreenState.Report) {
      _uiState.update { current.copy(selectedDimensionIndex = index) }
    }
  }

  fun setReportFilter(filter: ReportFilter) {
    val current = _uiState.value
    if (current is AppScreenState.Report) {
      _uiState.update { current.copy(filter = filter) }
    }
  }

  fun continueFromReport() {
    if (repository.isProfileCompleted()) {
      backToHomeFromReport()
    } else {
      startProfileSetup()
    }
  }

  fun selectHomeTab(tabIndex: Int) {
    _selectedHomeTab.value = tabIndex.coerceIn(0, 4)
  }

  fun startProfileSetup() {
    val existingProfile = repository.loadProfile()
    pendingName = existingProfile.name
    pendingPhotoUri = existingProfile.photoUri

    val nextScreen = AppScreenState.NameInput(currentName = existingProfile.name)
    showLoadingTransition(
      quote = "Cada gran transformación comienza con un nombre y una decisión clara.",
      nextScreen = nextScreen
    )
  }

  fun onUpdateGoal(newGoal: String, reason: String = "") {
    repository.updateMonthlyGoal(newGoal, reason)
    syncUserDataToCloud()
    val current = _uiState.value
    if (current is AppScreenState.Home) {
      val updatedProfile = current.userProfile.copy(monthlyGoal = newGoal)
      _uiState.value = current.copy(userProfile = updatedProfile)
    }
  }

  fun onUpdateProfileIdentity(name: String, zodiacSign: String, zodiacSymbol: String) {
    repository.updateProfileIdentity(name, zodiacSign, zodiacSymbol)
    syncUserDataToCloud()
    val current = _uiState.value
    if (current is AppScreenState.Home) {
      val updatedProfile = current.userProfile.copy(
        name = name.trim(),
        zodiacSign = zodiacSign.trim().ifBlank { current.userProfile.zodiacSign },
        zodiacSymbol = zodiacSymbol.trim().ifBlank { current.userProfile.zodiacSymbol }
      )
      _uiState.value = current.copy(userProfile = updatedProfile)
    }
  }

  fun openDetailedReportFromHome() {
    val current = _uiState.value
    if (current is AppScreenState.Home) {
      _uiState.value = AppScreenState.Report(
        report = current.report,
        selectedDimensionIndex = current.report.lowestDimension.index,
        isProfileCompleted = repository.isProfileCompleted()
      )
    }
  }

  fun openSavedReport() {
    val currentScores = repository.loadCurrentScores() ?: return
    val previousScores = repository.loadPreviousScores()
    val report = LifeAnalysisEngine.generateReport(currentScores, previousScores)
    _uiState.value = AppScreenState.Report(
      report = report,
      selectedDimensionIndex = report.lowestDimension.index,
      isProfileCompleted = repository.isProfileCompleted()
    )
  }

  fun backFromNameInput() {
    if (repository.hasCompletedAssessment()) {
      openSavedReport()
    } else {
      _uiState.value = AppScreenState.Login
    }
  }

  fun backToNameInput() {
    _uiState.value = AppScreenState.NameInput(currentName = pendingName)
  }

  fun backToProfilePhoto() {
    _uiState.value = AppScreenState.ProfilePhoto(name = pendingName, photoUri = pendingPhotoUri)
  }

  fun backToHomeFromReport() {
    if (repository.hasSignedIn() && repository.isProfileCompleted()) {
      val profile = repository.loadProfile()
      val currentScores = repository.loadCurrentScores() ?: return
      val previousScores = repository.loadPreviousScores()
      val report = LifeAnalysisEngine.generateReport(currentScores, previousScores)
      _uiState.value = AppScreenState.Home(userProfile = profile, report = report)
    }
  }

  private fun showLoadingTransition(quote: String, nextScreen: AppScreenState) {
    _uiState.value = AppScreenState.Loading(
      quote = quote,
      nextScreen = nextScreen
    )
  }

  fun onLoadingFinished() {
    val current = _uiState.value
    if (current is AppScreenState.Loading) {
      _uiState.value = current.nextScreen
    }
  }

  fun retakeAssessment() {
    _uiState.value = AppScreenState.Questionnaire(
      currentStep = 0,
      scores = emptyMap()
    )
  }
}
