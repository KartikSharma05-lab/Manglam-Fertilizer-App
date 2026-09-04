package com.manglamfertilizer.app.ui.ai

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.InfoSky
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun VoiceAIScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var isListening by remember { mutableStateOf(false) }
  var transcript by remember { mutableStateOf("Tap the microphone to speak billing or stock commands in Hindi or English") }

  val infiniteTransition = rememberInfiniteTransition(label = "pulse")
  val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = if (isListening) 1.25f else 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(800),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulseScale"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
  ) {
    // Header
    Surface(
      color = DarkSurface,
      modifier = Modifier.fillMaxWidth(),
      tonalElevation = 4.dp
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .statusBarsPadding()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimaryDark)
          }
          Spacer(modifier = Modifier.width(8.dp))
          Column {
            Text(
              text = "Voice AI Assistant",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimaryDark
            )
            Text(
              text = "Voice command for fast billing and stock check",
              style = MaterialTheme.typography.labelSmall,
              color = InfoSky
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .navigationBarsPadding()
        .padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = if (isListening) "Listening to voice input..." else "Tap to Speak",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = if (isListening) Emerald400 else TextSecondaryDark
      )

      Spacer(modifier = Modifier.height(30.dp))

      // Animated Mic Button
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(120.dp)
          .scale(if (isListening) pulseScale else 1f)
      ) {
        Surface(
          shape = CircleShape,
          color = if (isListening) Emerald500 else Emerald900,
          border = androidx.compose.foundation.BorderStroke(2.dp, Emerald400),
          modifier = Modifier
            .size(90.dp)
            .clickable {
              isListening = !isListening
              if (isListening) {
                transcript = "Listening: \"Urea 2 bag Ramesh Kumar ko bill karo\""
              }
            }
            .testTag("voice_mic_button")
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
              contentDescription = "Microphone",
              tint = if (isListening) DarkBg else Emerald400,
              modifier = Modifier.size(40.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(30.dp))

      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(16.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Text(
            text = "RECOGNIZED COMMAND",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
            color = TextSecondaryDark
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = transcript,
            style = MaterialTheme.typography.bodyMedium,
            color = TextPrimaryDark,
            textAlign = TextAlign.Center
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Examples: \"DAP stock kitna bacha hai?\", \"Ramesh Kumar ka udhaar check karo\", \"Create bill for NPK 10:26:26\"",
        style = MaterialTheme.typography.bodySmall,
        color = TextMutedDark,
        textAlign = TextAlign.Center
      )
    }
  }
}
