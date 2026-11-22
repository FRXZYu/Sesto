package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.components.bouncyClick
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.equilibrioColors

@Composable
fun ProfilePhotoScreen(
  currentPhotoUri: String?,
  onPhotoSelected: (String?) -> Unit,
  onContinue: (String?) -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  var selectedUri by remember { mutableStateOf<Uri?>(currentPhotoUri?.let { Uri.parse(it) }) }

  val photoPickerLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      selectedUri = uri
      onPhotoSelected(uri.toString())
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(eqColors.canvasBackground)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 20.dp),
    verticalArrangement = Arrangement.SpaceBetween,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Spacer(modifier = Modifier.height(24.dp))

      Text(
        text = "Foto de perfil",
        style = MaterialTheme.typography.displayMedium.copy(
          fontSize = 28.sp,
          fontWeight = FontWeight.Bold
        ),
        color = eqColors.textPrimary
      )

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = "Elige una foto para tu avatar o sáltalo por ahora si prefieres.",
        style = MaterialTheme.typography.bodyLarge,
        color = eqColors.textSecondary
      )

      Spacer(modifier = Modifier.height(48.dp))

      // Circular Photo Frame with Refined Material 3 Tonal Surface (No more harsh orange)
      Box(
        modifier = Modifier
          .size(170.dp)
          .shadow(6.dp, CircleShape)
          .clip(CircleShape)
          .background(eqColors.surfaceVariant)
          .border(3.dp, eqColors.accentPrimary.copy(alpha = 0.35f), CircleShape)
          .bouncyClick {
            photoPickerLauncher.launch(
              PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
          }
          .testTag("circular_profile_avatar"),
        contentAlignment = Alignment.Center
      ) {
        if (selectedUri != null) {
          AsyncImage(
            model = selectedUri,
            contentDescription = "Foto de perfil",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
          )
        } else {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Icon(
              imageVector = Icons.Outlined.Person,
              contentDescription = null,
              tint = eqColors.accentPrimary,
              modifier = Modifier.size(72.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Toca para elegir",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = eqColors.textSecondary
            )
          }
        }

        // Camera badge icon overlay
        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(10.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(eqColors.accentPrimary)
            .border(2.dp, eqColors.surfaceCard, CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = eqColors.onAccentPrimary,
            modifier = Modifier.size(18.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      val galleryBtnInteraction = rememberBounceInteractionSource()
      OutlinedButton(
        onClick = {
          photoPickerLauncher.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
          )
        },
        interactionSource = galleryBtnInteraction,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, eqColors.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = eqColors.textPrimary),
        modifier = Modifier
          .bouncyScale(galleryBtnInteraction)
          .testTag("btn_pick_gallery")
      ) {
        Icon(
          imageVector = Icons.Default.AddPhotoAlternate,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (selectedUri == null) "Elegir de la galería" else "Cambiar foto",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
        )
      }
    }

    // Bottom buttons: Continue and Saltar (Skip)
    Column(
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      val continueInteraction = rememberBounceInteractionSource()
      Button(
        onClick = {
          onContinue(selectedUri?.toString())
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
          .testTag("btn_continue_photo")
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

      Spacer(modifier = Modifier.height(10.dp))

      val skipInteraction = rememberBounceInteractionSource()
      OutlinedButton(
        onClick = {
          onContinue(null)
        },
        interactionSource = skipInteraction,
        shape = RoundedCornerShape(18.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, eqColors.border.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = eqColors.textSecondary),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .bouncyScale(skipInteraction)
          .testTag("btn_skip_photo")
      ) {
        Text(
          text = "Saltar",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium)
        )
      }
    }
  }
}
