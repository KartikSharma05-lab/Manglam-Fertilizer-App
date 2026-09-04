package com.manglamfertilizer.app.ui.ai

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun AgriculturalAIScreen(
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var cropName by remember { mutableStateOf("") }
  var symptoms by remember { mutableStateOf("") }
  var landSize by remember { mutableStateOf("") }
  var isConsulting by remember { mutableStateOf(false) }
  var aiAdviceResult by remember { mutableStateOf<String?>(null) }

  BackHandler(enabled = aiAdviceResult != null) {
    aiAdviceResult = null
  }

  val scrollState = rememberScrollState()

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
              text = "Krishi AI Agronomist",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = TextPrimaryDark
            )
            Text(
              text = "Instant crop nutrition & fertilizer dosage advisory",
              style = MaterialTheme.typography.labelSmall,
              color = GoldAmber
            )
          }
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .navigationBarsPadding()
        .imePadding()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = Emerald900, modifier = Modifier.size(36.dp)) {
              Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Emerald400, modifier = Modifier.size(18.dp))
              }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text("Crop Diagnostic Query", fontWeight = FontWeight.Bold, color = TextPrimaryDark)
          }

          Spacer(modifier = Modifier.height(14.dp))

          OutlinedTextField(
            value = cropName,
            onValueChange = { cropName = it },
            label = { Text("Crop Name (e.g. Wheat, Mustard, Paddy)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = landSize,
            onValueChange = { landSize = it },
            label = { Text("Land Area (e.g. 2 Acres, 5 Bigha)") },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = symptoms,
            onValueChange = { symptoms = it },
            label = { Text("Symptoms / Pest / Fertilizer need") },
            placeholder = { Text("e.g. Yellow leaves, stem borer, basal fertilizer recommendation") },
            minLines = 3,
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          )

          Spacer(modifier = Modifier.height(16.dp))

          Button(
            onClick = {
              if (cropName.isBlank()) return@Button
              isConsulting = true
              aiAdviceResult = "For $cropName ($landSize):\n1. Recommend Nitrogen application (Urea 45kg/acre) during vegetative phase.\n2. Add Zinc Sulphate (21%) @ 5kg/acre for chlorophyll enhancement.\n3. Keep field moisture optimal and avoid water-logging."
              isConsulting = false
            },
            enabled = !isConsulting && cropName.isNotBlank(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
          ) {
            if (isConsulting) {
              CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Default.Psychology, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Consult Krishi AI", fontWeight = FontWeight.Bold)
            }
          }
        }
      }

      aiAdviceResult?.let { advice ->
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = DarkCard,
          border = androidx.compose.foundation.BorderStroke(1.dp, Emerald400),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text("AI Agronomist Recommendation", fontWeight = FontWeight.Bold, color = Emerald400)
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = advice, color = TextPrimaryDark, style = MaterialTheme.typography.bodyMedium)
          }
        }
      }
    }
  }
}
