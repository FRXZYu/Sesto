package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.CompletionRecord
import com.example.domain.DayKey
import com.example.domain.HelpfulnessRating
import com.example.model.BalanceMission
import com.example.model.DimensionsProvider
import com.example.ui.theme.equilibrioColors
import java.text.SimpleDateFormat
import java.util.Locale

fun formatReadableReflectionDate(dateKey: String, dayKey: DayKey = DayKey.DEFAULT): String {
  return when (dateKey) {
    dayKey.today() -> "Hoy"
    dayKey.yesterday() -> "Ayer"
    else -> {
      val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      val out = SimpleDateFormat("d MMM yyyy", Locale("es", "ES"))
      val parsed = runCatching { parser.parse(dateKey) }.getOrNull()
      if (parsed != null) out.format(parsed) else dateKey
    }
  }
}

@Composable
fun ReflectionDiaryEntryCard(
  record: CompletionRecord,
  mission: BalanceMission?,
  onEditReflection: (CompletionRecord) -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current
  val interaction = rememberBounceInteractionSource()
  val dim = DimensionsProvider.dimensions.getOrNull(record.dimensionIndex)
  val accentColor = dim?.accentColor ?: eqColors.accentPrimary
  val readableDate = remember(record.date) { formatReadableReflectionDate(record.date) }
  val title = mission?.title ?: (dim?.title?.let { "Acción de $it" } ?: "Acción de equilibrio")

  val ratingStyle = when (record.helpfulness) {
    HelpfulnessRating.HELPFUL -> Triple("Me ayudó", Icons.Outlined.ThumbUp, eqColors.successText)
    HelpfulnessRating.NEUTRAL -> Triple("Neutro", Icons.Outlined.Remove, eqColors.accentPrimary)
    HelpfulnessRating.HEAVY -> Triple("Pesado", Icons.Outlined.HourglassEmpty, eqColors.warningAccent)
    null -> null
  }

  Surface(
    onClick = {
      haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onEditReflection(record)
    },
    interactionSource = interaction,
    shape = RoundedCornerShape(18.dp),
    color = eqColors.surfaceCard,
    border = BorderStroke(1.dp, eqColors.border),
    shadowElevation = 1.dp,
    modifier = modifier
      .fillMaxWidth()
      .bouncyScale(interaction, scaleDown = 0.98f)
      .testTag("reflection_entry_${record.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(accentColor)
          )
          Text(
            text = readableDate,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = eqColors.textSecondary
          )
          Text(
            text = "· ${dim?.title ?: ""}",
            style = MaterialTheme.typography.labelSmall,
            color = eqColors.textTertiary
          )
        }

        if (ratingStyle != null) {
          val (label, icon, color) = ratingStyle
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = color.copy(alpha = 0.14f)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = color
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
        color = eqColors.textPrimary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = record.reflectionNote.ifBlank { "Toca para añadir una nota breve sobre cómo te sentiste" },
        style = MaterialTheme.typography.bodySmall.copy(lineHeight = 18.sp),
        color = if (record.reflectionNote.isNotBlank()) eqColors.textSecondary else eqColors.textTertiary
      )
    }
  }
}

@Composable
fun EmptyReflectionsCard(modifier: Modifier = Modifier) {
  val eqColors = MaterialTheme.equilibrioColors
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = eqColors.surfaceVariant,
    border = BorderStroke(1.dp, eqColors.border),
    modifier = modifier
      .fillMaxWidth()
      .testTag("empty_reflections_card")
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(38.dp)
          .clip(CircleShape)
          .background(eqColors.accentPrimary.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Outlined.EditNote,
          contentDescription = null,
          tint = eqColors.accentPrimary,
          modifier = Modifier.size(20.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Text(
        text = "Cuando valora una acción, aparecerá aquí",
        style = MaterialTheme.typography.bodyMedium,
        color = eqColors.textSecondary
      )
    }
  }
}
