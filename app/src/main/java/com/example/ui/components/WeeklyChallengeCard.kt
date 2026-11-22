package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BalanceMission
import com.example.model.DimensionsProvider
import com.example.ui.theme.equilibrioColors

/**
 * Tarjeta destacada "Reto de la semana" para la sección Misiones.
 * Enfocada en la dimensión real más baja, 45-60 min, 3-4 pasos y +80 EXP fuera del tope diario.
 */
@Composable
fun WeeklyChallengeCard(
  challenge: BalanceMission,
  isAccepted: Boolean,
  isCompleted: Boolean,
  onAcceptChallenge: () -> Unit,
  onViewDetails: () -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current
  val dim = DimensionsProvider.dimensions.getOrNull(challenge.dimensionIndex)
  val accentColor = dim?.accentColor ?: eqColors.accentPrimary
  val acceptInteraction = rememberBounceInteractionSource()
  val detailsInteraction = rememberBounceInteractionSource()

  val cardBg by animateColorAsState(
    targetValue = if (isCompleted) eqColors.completedCard else eqColors.surfaceCard,
    animationSpec = tween(250),
    label = "weekly_card_bg"
  )

  Surface(
    onClick = onViewDetails,
    shape = RoundedCornerShape(24.dp),
    color = cardBg,
    border = BorderStroke(
      width = 1.5.dp,
      color = if (isCompleted) eqColors.completedBorder else accentColor.copy(alpha = 0.45f)
    ),
    shadowElevation = if (isCompleted) 0.dp else 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .testTag("weekly_challenge_card")
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = accentColor.copy(alpha = 0.14f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = "RETO DE LA SEMANA · ${challenge.dimensionTitle.uppercase()}",
                style = MaterialTheme.typography.labelSmall.copy(
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.4.sp
                ),
                color = accentColor
              )
            }
          }
        }

        Surface(
          shape = RoundedCornerShape(10.dp),
          color = if (isCompleted) eqColors.completedBadge else eqColors.warningContainer
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (isCompleted) Icons.Filled.CheckCircle else Icons.Default.Bolt,
              contentDescription = null,
              tint = if (isCompleted) eqColors.completedContent else eqColors.warningText,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
              text = if (isCompleted) "Completado" else "+${challenge.expReward} EXP · Extra",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (isCompleted) eqColors.completedContent else eqColors.warningText
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = challenge.title,
        style = MaterialTheme.typography.titleMedium.copy(
          fontWeight = FontWeight.Bold,
          fontSize = 18.sp
        ),
        color = if (isCompleted) eqColors.completedTitle else eqColors.textPrimary
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = challenge.summary,
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 19.sp),
        color = if (isCompleted) eqColors.completedSubtitle else eqColors.textSecondary
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Vista previa de pasos (3-4 pasos)
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = eqColors.surfaceVariant.copy(alpha = 0.65f),
        border = BorderStroke(1.dp, eqColors.border),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              text = "Paso a paso (${challenge.steps.size} pasos)",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = eqColors.textPrimary
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = null,
                tint = eqColors.textSecondary,
                modifier = Modifier.size(13.dp)
              )
              Spacer(modifier = Modifier.width(3.dp))
              Text(
                text = "${challenge.durationLabel} · Sin tope diario",
                style = MaterialTheme.typography.labelSmall,
                color = eqColors.textSecondary
              )
            }
          }

          challenge.steps.forEachIndexed { idx, step ->
            Row(
              verticalAlignment = Alignment.Top,
              modifier = Modifier.fillMaxWidth()
            ) {
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "${idx + 1}",
                  style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                  ),
                  color = accentColor
                )
              }
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = step,
                style = MaterialTheme.typography.bodySmall.copy(lineHeight = 17.sp),
                color = eqColors.textSecondary,
                modifier = Modifier.weight(1f)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        if (!isAccepted) {
          Button(
            onClick = {
              haptic.performHapticFeedback(HapticFeedbackType.LongPress)
              onAcceptChallenge()
            },
            interactionSource = acceptInteraction,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = eqColors.accentPrimary,
              contentColor = eqColors.onAccentPrimary
            ),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            modifier = Modifier
              .weight(1f)
              .height(44.dp)
              .bouncyScale(acceptInteraction, scaleDown = 0.94f)
              .testTag("btn_accept_weekly_challenge")
          ) {
            Text(
              text = "Aceptar reto",
              style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowForward,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
          }
        } else {
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isCompleted) eqColors.completedBadge else eqColors.successBadge,
            modifier = Modifier.weight(1f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = if (isCompleted) eqColors.completedContent else eqColors.successText,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isCompleted) "Reto completado" else "En Hoy y Plan",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = if (isCompleted) eqColors.completedContent else eqColors.successText
              )
            }
          }
        }

        OutlinedButton(
          onClick = onViewDetails,
          interactionSource = detailsInteraction,
          shape = RoundedCornerShape(14.dp),
          border = BorderStroke(1.dp, eqColors.border),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = eqColors.accentPrimary),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
          modifier = Modifier
            .height(44.dp)
            .bouncyScale(detailsInteraction, scaleDown = 0.93f)
            .testTag("btn_details_weekly_challenge")
        ) {
          Text(
            text = "Ver detalle",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
          )
        }
      }
    }
  }
}
