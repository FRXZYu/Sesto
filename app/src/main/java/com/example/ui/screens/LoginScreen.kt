package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.bouncyScale
import com.example.ui.components.rememberBounceInteractionSource
import com.example.ui.theme.EditorialSerif
import com.example.ui.theme.GoogleBrandBlue
import com.example.ui.theme.GoogleBrandGreen
import com.example.ui.theme.GoogleBrandRed
import com.example.ui.theme.GoogleBrandYellow
import com.example.ui.theme.equilibrioColors
import kotlinx.coroutines.delay
import android.app.Activity
import android.util.Log
import androidx.compose.material3.CircularProgressIndicator
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.compose.ui.platform.LocalContext
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.GoogleAuthProvider
import com.example.R
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
  onContinueWithGoogle: () -> Unit,
  modifier: Modifier = Modifier
) {
  val eqColors = MaterialTheme.equilibrioColors
  val haptic = LocalHapticFeedback.current
  val context = LocalContext.current
  val credentialManager = remember { CredentialManager.create(context) }
  val scope = rememberCoroutineScope()

  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  val handleGoogleSignIn: () -> Unit = {
    isLoading = true
    errorMessage = null
    scope.launch {
      try {
        val clientId = context.getString(R.string.default_web_client_id)
        val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()
        val result = credentialManager.getCredential(context as Activity, request)
        val credential = result.credential
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
          val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
          val googleCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
          com.google.firebase.auth.FirebaseAuth.getInstance().signInWithCredential(googleCredential).await()
          isLoading = false
          onContinueWithGoogle()
        } else {
          isLoading = false
          errorMessage = "Tipo de credencial inesperado"
        }
      } catch (e: GetCredentialCancellationException) {
        isLoading = false
        Log.w("Login", "Google Sign-In cancelled", e)
      } catch (e: Exception) {
        isLoading = false
        errorMessage = e.localizedMessage ?: "Error al iniciar sesión con Google"
        Log.e("Login", "Google Sign-In failed", e)
      }
    }
  }

  var showHeader by remember { mutableStateOf(false) }
  var showSlogan by remember { mutableStateOf(false) }
  var showButton by remember { mutableStateOf(false) }
  var showLegal by remember { mutableStateOf(false) }

  LaunchedEffect(Unit) {
    showHeader = true
    delay(110)
    showSlogan = true
    delay(110)
    showButton = true
    delay(90)
    showLegal = true
  }

  val infiniteTransition = rememberInfiniteTransition(label = "login_squares_float")
  val floatTopRight by infiniteTransition.animateFloat(
    initialValue = -5f,
    targetValue = 5f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "float_top_right"
  )
  val floatMidLeft by infiniteTransition.animateFloat(
    initialValue = 6f,
    targetValue = -6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "float_mid_left"
  )
  val floatLowerRight by infiniteTransition.animateFloat(
    initialValue = -4f,
    targetValue = 6f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 4500, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "float_lower_right"
  )

  val buttonInteraction = rememberBounceInteractionSource()
  val isButtonPressed by buttonInteraction.collectIsPressedAsState()
  val arrowNudge by animateDpAsState(
    targetValue = if (isButtonPressed) 4.dp else 0.dp,
    animationSpec = spring(
      dampingRatio = Spring.DampingRatioMediumBouncy,
      stiffness = Spring.StiffnessMedium
    ),
    label = "login_arrow_nudge"
  )

  BoxWithConstraints(
    modifier = modifier
      .fillMaxSize()
      .background(eqColors.loginCanvas)
      .testTag("login_screen")
  ) {
    val isCompactWidth = maxWidth < 400.dp
    val squareSizeTopRight = if (isCompactWidth) 156.dp else 184.dp
    val squareSizeMidLeft = if (isCompactWidth) 144.dp else 168.dp
    val squareSizeLowerRight = if (isCompactWidth) 150.dp else 174.dp

    // 3 cuadrados grandes de esquinas muy redondeadas en gris (#F8F8F8) rotados sutilmente en el fondo
    FloatingEditorialSquare(
      size = squareSizeTopRight,
      baseRotation = 16f,
      entranceDelayMs = 40L,
      ambientOffsetY = floatTopRight,
      squareColor = eqColors.loginSquare,
      onBounce = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
      modifier = Modifier
        .align(Alignment.TopEnd)
        .offset(x = 46.dp, y = maxHeight * 0.16f)
        .testTag("login_bg_square_1")
    )

    FloatingEditorialSquare(
      size = squareSizeMidLeft,
      baseRotation = -14f,
      entranceDelayMs = 130L,
      ambientOffsetY = floatMidLeft,
      squareColor = eqColors.loginSquare,
      onBounce = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
      modifier = Modifier
        .align(Alignment.CenterStart)
        .offset(x = (-42).dp, y = (-maxHeight * 0.05f))
        .testTag("login_bg_square_2")
    )

    FloatingEditorialSquare(
      size = squareSizeLowerRight,
      baseRotation = -11f,
      entranceDelayMs = 220L,
      ambientOffsetY = floatLowerRight,
      squareColor = eqColors.loginSquare,
      onBounce = { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
      modifier = Modifier
        .align(Alignment.CenterEnd)
        .offset(x = 40.dp, y = maxHeight * 0.11f)
        .testTag("login_bg_square_3")
    )

    // Contenido editorial principal
    Column(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .padding(horizontal = 28.dp, vertical = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Nombre de la app arriba a la izquierda en Serif negra
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 520.dp)
      ) {
        androidx.compose.animation.AnimatedVisibility(
          visible = showHeader,
          enter = fadeIn(tween(340)) + slideInVertically(
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioMediumBouncy,
              stiffness = Spring.StiffnessMediumLow
            ),
            initialOffsetY = { -it / 2 }
          ),
          modifier = Modifier.align(Alignment.TopStart)
        ) {
          Text(
            text = "Equilibrio",
            fontFamily = EditorialSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            letterSpacing = (-0.4).sp,
            color = eqColors.loginInk,
            modifier = Modifier
              .padding(top = 6.dp)
              .testTag("login_brand_title")
          )
        }
      }

      // Espacio editorial amplio superior
      Spacer(modifier = Modifier.weight(1f))

      // Mitad inferior centrada: Eslogan Serif grande + botón píldora + texto legal
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .widthIn(max = 420.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        AnimatedVisibility(
          visible = showSlogan,
          enter = fadeIn(tween(380)) +
            scaleIn(
              initialScale = 0.90f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
              )
            ) +
            slideInVertically(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
              ),
              initialOffsetY = { it / 3 }
            )
        ) {
          Text(
            text = "Tu propio\nritmo de equilibrio",
            fontFamily = EditorialSerif,
            fontWeight = FontWeight.Medium,
            fontSize = 35.sp,
            lineHeight = 42.sp,
            letterSpacing = (-0.5).sp,
            textAlign = TextAlign.Center,
            color = eqColors.loginInk,
            modifier = Modifier
              .padding(horizontal = 8.dp)
              .testTag("login_slogan")
          )
        }

        Spacer(modifier = Modifier.height(34.dp))

        AnimatedVisibility(
          visible = showButton,
          enter = fadeIn(tween(340)) +
            scaleIn(
              initialScale = 0.88f,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              )
            ) +
            slideInVertically(
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
              ),
              initialOffsetY = { it / 2 }
            )
        ) {
          Column(modifier = Modifier.fillMaxWidth()) {
            Button(
              onClick = {
                if (!isLoading) {
                  haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                  handleGoogleSignIn()
                }
              },
              enabled = !isLoading,
              interactionSource = buttonInteraction,
              shape = CircleShape,
              colors = ButtonDefaults.buttonColors(
                containerColor = eqColors.loginInk,
                contentColor = eqColors.loginOnInk
              ),
              elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 2.dp,
                pressedElevation = 0.dp
              ),
              contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .minimumInteractiveComponentSize()
                .bouncyScale(buttonInteraction, scaleDown = 0.94f)
                .testTag("login_google_button")
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                if (isLoading) {
                  CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = eqColors.loginOnInk,
                    strokeWidth = 2.5.dp
                  )
                } else {
                  GoogleGMark(modifier = Modifier.size(19.dp))
                  Spacer(modifier = Modifier.width(12.dp))
                  Text(
                    text = "Continuar con Google",
                    style = MaterialTheme.typography.titleMedium.copy(
                      fontWeight = FontWeight.SemiBold,
                      fontSize = 15.sp,
                      letterSpacing = 0.1.sp
                    ),
                    color = eqColors.loginOnInk
                  )
                  Spacer(modifier = Modifier.width(8.dp))
                  Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = eqColors.loginOnInk.copy(alpha = 0.82f),
                    modifier = Modifier
                      .size(17.dp)
                      .offset(x = arrowNudge)
                  )
                }
              }
            }

            AnimatedVisibility(visible = errorMessage != null) {
              Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(top = 12.dp)
                  .testTag("login_error_banner")
              ) {
                Text(
                  text = errorMessage.orEmpty(),
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                  color = MaterialTheme.colorScheme.onErrorContainer,
                  modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedVisibility(
          visible = showLegal,
          enter = fadeIn(tween(400)) + slideInVertically(
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioNoBouncy,
              stiffness = Spring.StiffnessLow
            ),
            initialOffsetY = { it / 2 }
          )
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier
              .padding(horizontal = 12.dp)
              .testTag("login_legal_text")
          ) {
            Icon(
              imageVector = Icons.Outlined.VerifiedUser,
              contentDescription = null,
              tint = eqColors.loginMuted,
              modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "Al usar Equilibrio aceptas nuestros Términos y Política de Privacidad",
              style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 11.5.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal
              ),
              color = eqColors.loginMuted,
              textAlign = TextAlign.Center
            )
          }
        }

        Spacer(modifier = Modifier.height(22.dp))
      }
    }
  }
}

@Composable
private fun FloatingEditorialSquare(
  size: Dp,
  baseRotation: Float,
  entranceDelayMs: Long,
  ambientOffsetY: Float,
  squareColor: androidx.compose.ui.graphics.Color,
  onBounce: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scope = rememberCoroutineScope()
  val entranceScale = remember { Animatable(0.72f) }
  val entranceAlpha = remember { Animatable(0f) }
  val tapRotationPulse = remember { Animatable(0f) }
  val tapScalePulse = remember { Animatable(1f) }
  val interactionSource = rememberBounceInteractionSource()

  LaunchedEffect(Unit) {
    delay(entranceDelayMs)
    launch {
      entranceAlpha.animateTo(1f, animationSpec = tween(durationMillis = 420))
    }
    entranceScale.animateTo(
      targetValue = 1f,
      animationSpec = spring(
        dampingRatio = 0.56f,
        stiffness = Spring.StiffnessLow
      )
    )
  }

  Surface(
    onClick = {
      onBounce()
      scope.launch {
        launch {
          tapScalePulse.snapTo(0.92f)
          tapScalePulse.animateTo(
            targetValue = 1f,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioHighBouncy,
              stiffness = Spring.StiffnessMediumLow
            )
          )
        }
        launch {
          tapRotationPulse.snapTo(if (baseRotation >= 0f) 7f else -7f)
          tapRotationPulse.animateTo(
            targetValue = 0f,
            animationSpec = spring(
              dampingRatio = Spring.DampingRatioHighBouncy,
              stiffness = Spring.StiffnessMediumLow
            )
          )
        }
      }
    },
    interactionSource = interactionSource,
    shape = RoundedCornerShape(34.dp),
    color = squareColor,
    modifier = modifier
      .size(size)
      .bouncyScale(interactionSource, scaleDown = 0.95f)
      .graphicsLayer {
        val totalScale = entranceScale.value * tapScalePulse.value
        scaleX = totalScale
        scaleY = totalScale
        alpha = entranceAlpha.value
        rotationZ = baseRotation + tapRotationPulse.value + (ambientOffsetY * 0.25f)
        translationY = ambientOffsetY * density
      }
  ) {}
}

@Composable
private fun GoogleGMark(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier) {
    val diameter = size.minDimension
    val strokeWidth = diameter * 0.20f
    val arcInset = strokeWidth / 2f
    val arcSize = Size(diameter - strokeWidth, diameter - strokeWidth)
    val topLeft = Offset(arcInset, arcInset)
    val radius = diameter / 2f
    val center = Offset(radius, radius)

    // Red (top)
    drawArc(
      color = GoogleBrandRed,
      startAngle = -142f,
      sweepAngle = 97f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )
    // Yellow (left)
    drawArc(
      color = GoogleBrandYellow,
      startAngle = 140f,
      sweepAngle = 80f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )
    // Green (bottom)
    drawArc(
      color = GoogleBrandGreen,
      startAngle = 38f,
      sweepAngle = 104f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )
    // Blue (right lower arc + horizontal crossbar)
    drawArc(
      color = GoogleBrandBlue,
      startAngle = -4f,
      sweepAngle = 44f,
      useCenter = false,
      topLeft = topLeft,
      size = arcSize,
      style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
    )
    drawLine(
      color = GoogleBrandBlue,
      start = Offset(center.x, center.y),
      end = Offset(diameter - arcInset * 0.25f, center.y),
      strokeWidth = strokeWidth,
      cap = StrokeCap.Butt
    )
  }
}
