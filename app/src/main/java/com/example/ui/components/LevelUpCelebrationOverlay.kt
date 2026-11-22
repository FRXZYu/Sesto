package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.HexagonCalculator
import com.example.model.GamificationStats
import com.example.model.MissionRewardResult
import com.example.ui.theme.ColorAutoestima
import com.example.ui.theme.ColorDescanso
import com.example.ui.theme.ColorEjercicio
import com.example.ui.theme.ColorExcelencia
import com.example.ui.theme.ColorHobbies
import com.example.ui.theme.ColorRelaciones
import com.example.ui.theme.equilibrioColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

private data class ConfettiParticle(
  val xFraction: Float,
  val startYFraction: Float,
  val speedY: Float,
  val driftX: Float,
  val sizeDp: Float,
  val color: Color,
  val rotationSpeed: Float,
  val initialAngle: Float,
  val isCircle: Boolean
)

/**
 * Overlay sereno a pantalla completa de 2 segundos para celebrar una subida de nivel,
 * respetando el lenguaje visual (#FAFAFC, carbón #1E2024 y acentos de dimensiones).
 */
@Composable
fun LevelUpCelebrationOverlay(
  reward: MissionRewardResult,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  val haptic = LocalHapticFeedback.current
  val scaleAnim = remember { Animatable(0.84f) }
  val alphaAnim = remember { Animatable(0f) }
  val progressBarAnim = remember { Animatable(0f) }
  val confettiProgress = remember { Animatable(0f) }

  val expInLevel = reward.newTotalExp % GamificationStats.EXP_PER_LEVEL
  val targetBarFraction = if (expInLevel == 0) {
    0.05f
  } else {
    (expInLevel.toFloat() / GamificationStats.EXP_PER_LEVEL.toFloat()).coerceIn(0.06f, 1f)
  }
  val newRank = remember(reward.newLevel) {
    HexagonCalculator.calculateRank(reward.newLevel)
  }

  val eqColors = MaterialTheme.equilibrioColors
  val accentPrimary = eqColors.accentPrimary
  val particles = remember(reward.newLevel, accentPrimary) {
    val palette = listOf(
      accentPrimary,
      ColorAutoestima,
      ColorEjercicio,
      ColorDescanso,
      ColorRelaciones,
      ColorHobbies,
      ColorExcelencia
    )
    List(36) { idx ->
      val seed = (idx * 73 + reward.newLevel * 19) % 100
      ConfettiParticle(
        xFraction = (0.06f + ((seed * 0.88f) / 100f)),
        startYFraction = -0.05f - ((idx % 6) * 0.045f),
        speedY = 0.55f + ((idx % 5) * 0.14f),
        driftX = (((idx % 7) - 3) * 0.045f),
        sizeDp = 5f + (idx % 4) * 2.5f,
        color = palette[idx % palette.size].copy(alpha = 0.82f),
        rotationSpeed = (if (idx % 2 == 0) 1f else -1f) * (140f + (idx % 5) * 45f),
        initialAngle = (idx * 37f) % 360f,
        isCircle = idx % 3 == 0
      )
    }
  }

  LaunchedEffect(reward) {
    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    alphaAnim.snapTo(0f)
    scaleAnim.snapTo(0.84f)
    progressBarAnim.snapTo(0f)
    confettiProgress.snapTo(0f)

    launch {
      alphaAnim.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
    }
    launch {
      scaleAnim.animateTo(
        1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
      )
    }
    launch {
      delay(140)
      progressBarAnim.animateTo(
        targetValue = targetBarFraction,
        animationSpec = tween(durationMillis = 850, easing = FastOutSlowInEasing)
      )
    }
    launch {
      confettiProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 1950, easing = LinearEasing)
      )
    }

    delay(2000L)
    onDismiss()
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(eqColors.canvasBackground.copy(alpha = 0.95f))
      .alpha(alphaAnim.value)
      .clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = onDismiss
      )
      .testTag("level_up_celebration_overlay"),
    contentAlignment = Alignment.Center
  ) {
    // Confeti sutil en el palette actual
    Canvas(modifier = Modifier.fillMaxSize()) {
      val t = confettiProgress.value
      particles.forEach { p ->
        val cx = size.width * (p.xFraction + sin(t * 6f + p.initialAngle) * p.driftX)
        val cy = size.height * (p.startYFraction + t * p.speedY)
        if (cy in -40f..(size.height + 40f)) {
          val pxSize = p.sizeDp.dp.toPx()
          val angle = p.initialAngle + t * p.rotationSpeed
          val fade = if (t > 0.78f) ((1f - t) / 0.22f).coerceIn(0f, 1f) else 1f
          val drawColor = p.color.copy(alpha = p.color.alpha * fade)

          rotate(degrees = angle, pivot = Offset(cx, cy)) {
            if (p.isCircle) {
              drawCircle(
                color = drawColor,
                radius = pxSize * 0.45f,
                center = Offset(cx, cy)
              )
            } else {
              drawRoundRect(
                color = drawColor,
                topLeft = Offset(cx - pxSize / 2f, cy - pxSize * 0.3f),
                size = Size(pxSize, pxSize * 0.6f),
                cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
              )
            }
          }
        }
      }
    }

    Surface(
      shape = RoundedCornerShape(28.dp),
      color = eqColors.surfaceCard,
      border = BorderStroke(1.dp, eqColors.border),
      shadowElevation = 10.dp,
      modifier = Modifier
        .padding(horizontal = 28.dp)
        .widthIn(max = 360.dp)
        .fillMaxWidth()
        .scale(scaleAnim.value)
    ) {
      Column(
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          shape = CircleShape,
          color = eqColors.accentPrimary,
          modifier = Modifier.size(64.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Outlined.WorkspacePremium,
              contentDescription = null,
              tint = eqColors.onAccentPrimary,
              modifier = Modifier.size(32.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = eqColors.textSecondary,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "NUEVO NIVEL ALCANZADO",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp
            ),
            color = eqColors.textSecondary
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Nivel ${reward.newLevel}",
          style = MaterialTheme.typography.displayMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp
          ),
          color = eqColors.textPrimary,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = eqColors.surfaceVariant
        ) {
          Text(
            text = newRank,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = eqColors.accentPrimary,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Progreso de nivel",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = eqColors.textSecondary
          )
          Text(
            text = "$expInLevel / ${GamificationStats.EXP_PER_LEVEL} EXP",
            style = MaterialTheme.typography.labelSmall.copy(
              fontWeight = FontWeight.Bold,
              fontFeatureSettings = "tnum"
            ),
            color = eqColors.accentPrimary
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(eqColors.surfaceVariant)
        ) {
          Box(
            modifier = Modifier
              .fillMaxWidth(progressBarAnim.value)
              .height(8.dp)
              .clip(RoundedCornerShape(4.dp))
              .background(eqColors.accentPrimary)
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
          text = "Tu constancia amable está expandiendo tu equilibrio.",
          style = MaterialTheme.typography.bodySmall,
          color = eqColors.textSecondary,
          textAlign = TextAlign.Center
        )
      }
    }
  }
}
