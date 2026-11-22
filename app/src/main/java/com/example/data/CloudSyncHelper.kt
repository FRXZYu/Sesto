package com.example.data

import android.util.Log
import com.example.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object CloudSyncHelper {
  private val db by lazy { FirebaseFirestore.getInstance() }

  suspend fun checkExistingUserInCloud(uid: String): Map<String, Any>? {
    return try {
      val doc = db.collection("users").document(uid).get().await()
      if (doc.exists()) {
        doc.data
      } else {
        null
      }
    } catch (e: Exception) {
      Log.e("CloudSync", "Error checking user in cloud", e)
      null
    }
  }

  suspend fun saveUserDataToCloud(
    uid: String,
    email: String,
    profile: UserProfile,
    currentScores: Map<Int, Int>?,
    previousScores: Map<Int, Int>?,
    isProfileCompleted: Boolean,
    isAssessmentCompleted: Boolean
  ) {
    try {
      val data = mapOf(
        "uid" to uid,
        "email" to email,
        "name" to profile.name,
        "photoUri" to profile.photoUri,
        "zodiacSign" to profile.zodiacSign,
        "zodiacSymbol" to profile.zodiacSymbol,
        "monthlyGoal" to profile.monthlyGoal,
        "chosenFocusDimensions" to profile.chosenFocusDimensions.toList(),
        "currentScores" to currentScores?.mapKeys { it.key.toString() },
        "previousScores" to previousScores?.mapKeys { it.key.toString() },
        "isProfileCompleted" to isProfileCompleted,
        "isAssessmentCompleted" to isAssessmentCompleted,
        "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
      )
      db.collection("users").document(uid).set(data).await()
      Log.d("CloudSync", "User data successfully saved to Firestore")
    } catch (e: Exception) {
      Log.e("CloudSync", "Error saving user data to cloud", e)
    }
  }
}
