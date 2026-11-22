package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Badge
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.equilibrioColors

@Composable
fun NameInputScreen(
  initialName: String = "",
  onNameSubmitted: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  var name by remember { mutableStateOf(initialName) }

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
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.size(52.dp)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Outlined.Badge,
            contentDescription = null,
            tint = eqColors.accentPrimary,
            modifier = Modifier.size(28.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "¿Cómo te llamas?",
        style = MaterialTheme.typography.displayMedium.copy(
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold
        ),
        color = eqColors.textPrimary
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Personalizaremos tu panel de inicio y tu camino de motivación con tu nombre.",
        style = MaterialTheme.typography.bodyLarge,
        color = eqColors.textSecondary
      )

      Spacer(modifier = Modifier.height(32.dp))

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        placeholder = { Text("Escribe tu nombre (ej. Kirenia)", color = eqColors.textSecondary) },
        singleLine = true,
        shape = RoundedCornerShape(18.dp),
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = eqColors.surfaceCard,
          unfocusedContainerColor = eqColors.surfaceCard,
          focusedBorderColor = eqColors.accentPrimary,
          unfocusedBorderColor = eqColors.border,
          focusedTextColor = eqColors.textPrimary,
          unfocusedTextColor = eqColors.textPrimary
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("input_user_name")
      )
    }

    val continueInteraction = rememberBounceInteractionSource()
    Button(
      onClick = {
        val trimmed = name.trim().ifEmpty { "Kirenia" }
        onNameSubmitted(trimmed)
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
        .testTag("btn_continue_name")
    ) {
      Text(
        text = "Continuar",
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
