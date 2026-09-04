package com.manglamfertilizer.app.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Circle
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun DisplayNameSetupScreen(
  googleDisplayName: String?,
  onSaveDisplayName: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val hasGoogleName = !googleDisplayName.isNullOrBlank()
  val focusManager = LocalFocusManager.current

  // If googleDisplayName exists, default to option 1, else option 2
  var selectedOption by remember { mutableStateOf(if (hasGoogleName) 1 else 2) }
  var customNameInput by remember { mutableStateOf("") }
  var validationError by remember { mutableStateOf<String?>(null) }

  fun handleSave() {
    focusManager.clearFocus()
    if (selectedOption == 1 && hasGoogleName && googleDisplayName != null) {
      onSaveDisplayName(googleDisplayName.trim())
    } else {
      val trimmed = customNameInput.trim()
      if (trimmed.isEmpty()) {
        validationError = "Please enter your preferred name."
        return
      }
      if (trimmed.length > 50) {
        validationError = "Name should be within 50 characters."
        return
      }
      validationError = null
      onSaveDisplayName(trimmed)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .padding(24.dp)
      .verticalScroll(rememberScrollState()),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.fillMaxWidth()
    ) {
      Spacer(modifier = Modifier.height(16.dp))

      // Logo Icon
      Surface(
        shape = RoundedCornerShape(18.dp),
        color = Emerald900,
        modifier = Modifier
          .size(68.dp)
          .border(1.5.dp, Emerald400, RoundedCornerShape(18.dp))
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Eco,
            contentDescription = null,
            tint = Emerald400,
            modifier = Modifier.size(38.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "Welcome to ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME}",
        style = MaterialTheme.typography.headlineSmall.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        ),
        color = TextPrimaryDark
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Choose the name you'd like us to use for your greeting.",
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondaryDark
      )

      Spacer(modifier = Modifier.height(28.dp))

      // Option 1: Same as Google Account
      if (hasGoogleName) {
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (selectedOption == 1) DarkSurfaceElevated else DarkCard,
          border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (selectedOption == 1) Emerald400 else DarkBorder
          ),
          modifier = Modifier
            .fillMaxWidth()
            .clickable {
              selectedOption = 1
              validationError = null
            }
            .testTag("option_google_name")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = if (selectedOption == 1) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
              contentDescription = null,
              tint = if (selectedOption == 1) Emerald400 else TextMutedDark,
              modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Same as Google Account",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimaryDark
              )
              Text(
                text = googleDisplayName ?: "",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Emerald400
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))
      }

      // Option 2: Use another name
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (selectedOption == 2) DarkSurfaceElevated else DarkCard,
        border = androidx.compose.foundation.BorderStroke(
          1.5.dp,
          if (selectedOption == 2) Emerald400 else DarkBorder
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clickable {
            selectedOption = 2
            validationError = null
          }
          .testTag("option_custom_name")
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(
              imageVector = if (selectedOption == 2) Icons.Filled.CheckCircle else Icons.Outlined.Circle,
              contentDescription = null,
              tint = if (selectedOption == 2) Emerald400 else TextMutedDark,
              modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column {
              Text(
                text = "Use another name",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimaryDark
              )
              Text(
                text = "Enter any preferred greeting name",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondaryDark
              )
            }
          }

          if (selectedOption == 2) {
            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
              value = customNameInput,
              onValueChange = {
                customNameInput = it
                if (validationError != null) validationError = null
              },
              placeholder = { Text("Enter your name (e.g. Kartik)", color = TextMutedDark) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.Person,
                  contentDescription = null,
                  tint = Emerald400
                )
              },
              singleLine = true,
              keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done
              ),
              keyboardActions = KeyboardActions(onDone = { handleSave() }),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkBg,
                unfocusedContainerColor = DarkBg
              ),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("custom_name_input")
            )
          }
        }
      }

      if (validationError != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = validationError ?: "",
          color = SoftRed,
          style = MaterialTheme.typography.bodySmall,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }

    // Bottom Action
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 24.dp)
    ) {
      Button(
        onClick = { handleSave() },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Emerald500,
          contentColor = Color.Black
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("save_display_name_button")
      ) {
        Text(
          text = "Continue to Dashboard",
          fontWeight = FontWeight.Bold,
          fontSize = 15.sp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Icon(
          imageVector = Icons.Default.ArrowForward,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
