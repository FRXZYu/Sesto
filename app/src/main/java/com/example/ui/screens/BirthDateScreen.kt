package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AllZodiacSigns
import com.example.model.ZodiacSign
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.equilibrioColors

fun calculateZodiacFromDate(day: Int, month: Int): ZodiacSign {
  return when (month) {
    1 -> if (day <= 19) AllZodiacSigns[9] else AllZodiacSigns[10] // Capricornio / Acuario
    2 -> if (day <= 18) AllZodiacSigns[10] else AllZodiacSigns[11] // Acuario / Piscis
    3 -> if (day <= 20) AllZodiacSigns[11] else AllZodiacSigns[0] // Piscis / Aries
    4 -> if (day <= 19) AllZodiacSigns[0] else AllZodiacSigns[1] // Aries / Tauro
    5 -> if (day <= 20) AllZodiacSigns[1] else AllZodiacSigns[2] // Tauro / Géminis
    6 -> if (day <= 20) AllZodiacSigns[2] else AllZodiacSigns[3] // Géminis / Cáncer
    7 -> if (day <= 22) AllZodiacSigns[3] else AllZodiacSigns[4] // Cáncer / Leo
    8 -> if (day <= 22) AllZodiacSigns[4] else AllZodiacSigns[5] // Leo / Virgo
    9 -> if (day <= 22) AllZodiacSigns[5] else AllZodiacSigns[6] // Virgo / Libra
    10 -> if (day <= 22) AllZodiacSigns[6] else AllZodiacSigns[7] // Libra / Escorpio
    11 -> if (day <= 21) AllZodiacSigns[7] else AllZodiacSigns[8] // Escorpio / Sagitario
    12 -> if (day <= 21) AllZodiacSigns[8] else AllZodiacSigns[9] // Sagitario / Capricornio
    else -> AllZodiacSigns[0]
  }
}

@Composable
fun BirthDateScreen(
  googleName: String,
  googleEmail: String,
  onBirthDateSubmitted: (day: Int, month: Int, year: Int, zodiacSign: String, zodiacSymbol: String) -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current

  var dayInput by remember { mutableStateOf("15") }
  var monthInput by remember { mutableStateOf("6") }
  var yearInput by remember { mutableStateOf("1998") }

  val day = dayInput.toIntOrNull() ?: 15
  val month = monthInput.toIntOrNull() ?: 6
  val year = yearInput.toIntOrNull() ?: 1998

  val calculatedZodiac = remember(day, month) {
    calculateZodiacFromDate(day.coerceIn(1, 31), month.coerceIn(1, 12))
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(eqColors.canvasBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column {
      Spacer(modifier = Modifier.height(24.dp))

      Surface(
        color = eqColors.accentPrimary.copy(alpha = 0.08f),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.size(56.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.Cake,
            contentDescription = null,
            tint = eqColors.accentPrimary,
            modifier = Modifier.size(28.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "¿Cuándo naciste?",
        style = MaterialTheme.typography.displayMedium.copy(
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold
        ),
        color = eqColors.textPrimary
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Calcularemos tu signo zodiacal automáticamente para acompañar tus ciclos de bienestar.",
        style = MaterialTheme.typography.bodyLarge,
        color = eqColors.textSecondary
      )

      Spacer(modifier = Modifier.height(32.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedTextField(
          value = dayInput,
          onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) dayInput = it },
          label = { Text("Día") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = eqColors.surfaceCard,
            unfocusedContainerColor = eqColors.surfaceCard,
            focusedBorderColor = eqColors.accentPrimary,
            unfocusedBorderColor = eqColors.border,
            focusedTextColor = eqColors.textPrimary,
            unfocusedTextColor = eqColors.textPrimary
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("input_birth_day")
        )

        OutlinedTextField(
          value = monthInput,
          onValueChange = { if (it.length <= 2 && it.all { c -> c.isDigit() }) monthInput = it },
          label = { Text("Mes") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = eqColors.surfaceCard,
            unfocusedContainerColor = eqColors.surfaceCard,
            focusedBorderColor = eqColors.accentPrimary,
            unfocusedBorderColor = eqColors.border,
            focusedTextColor = eqColors.textPrimary,
            unfocusedTextColor = eqColors.textPrimary
          ),
          modifier = Modifier
            .weight(1f)
            .testTag("input_birth_month")
        )

        OutlinedTextField(
          value = yearInput,
          onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) yearInput = it },
          label = { Text("Año") },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = eqColors.surfaceCard,
            unfocusedContainerColor = eqColors.surfaceCard,
            focusedBorderColor = eqColors.accentPrimary,
            unfocusedBorderColor = eqColors.border,
            focusedTextColor = eqColors.textPrimary,
            unfocusedTextColor = eqColors.textPrimary
          ),
          modifier = Modifier
            .weight(1.3f)
            .testTag("input_birth_year")
        )
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Calculated Zodiac preview card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = eqColors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, eqColors.accentPrimary.copy(alpha = 0.3f)),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("zodiac_preview_card")
      ) {
        Row(
          modifier = Modifier.padding(20.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Surface(
            shape = CircleShape,
            color = eqColors.accentPrimary.copy(alpha = 0.12f),
            modifier = Modifier.size(52.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Text(
                text = calculatedZodiac.symbol,
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 26.sp),
                color = eqColors.accentPrimary
              )
            }
          }
          Spacer(modifier = Modifier.width(16.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Signo detectado",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
              color = eqColors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "${calculatedZodiac.name} (${calculatedZodiac.dates})",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = eqColors.textPrimary
            )
          }
        }
      }
    }

    val continueInteraction = rememberBounceInteractionSource()
    Button(
      onClick = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onBirthDateSubmitted(
          day.coerceIn(1, 31),
          month.coerceIn(1, 12),
          year.coerceIn(1920, 2025),
          calculatedZodiac.name,
          calculatedZodiac.symbol
        )
      },
      interactionSource = continueInteraction,
      shape = RoundedCornerShape(18.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = eqColors.accentPrimary,
        contentColor = eqColors.onAccentPrimary
      ),
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .bouncyScale(continueInteraction)
        .testTag("btn_continue_birthdate")
    ) {
      Text(
        text = "Confirmar y continuar",
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
      )
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        modifier = Modifier.size(20.dp)
      )
    }
  }
}
