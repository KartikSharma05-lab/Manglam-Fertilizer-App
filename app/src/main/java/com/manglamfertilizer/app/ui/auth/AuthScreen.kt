package com.manglamfertilizer.app.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.GreetingInfo
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

@Composable
fun AuthScreen(
  greetingInfo: GreetingInfo,
  onLogin: (email: String, pass: String, onResult: (Boolean, String?) -> Unit) -> Unit,
  onForgotPassword: (email: String, onDone: (Boolean, String) -> Unit) -> Unit,
  modifier: Modifier = Modifier
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var isPasswordVisible by remember { mutableStateOf(false) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var showForgotDialog by remember { mutableStateOf(false) }

  val focusManager = LocalFocusManager.current
  val scrollState = rememberScrollState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .statusBarsPadding()
      .navigationBarsPadding()
      .imePadding()
      .padding(horizontal = 24.dp)
      .verticalScroll(scrollState),
    contentAlignment = Alignment.Center
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Manglam Fertilizer Logo Header
      Surface(
        shape = CircleShape,
        color = Emerald900,
        modifier = Modifier
          .size(76.dp)
          .border(2.dp, Emerald400, CircleShape)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.Eco,
            contentDescription = "Manglam Fertilizer Logo",
            tint = Emerald400,
            modifier = Modifier.size(42.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "MANGALAM FERTILIZER",
        style = MaterialTheme.typography.headlineMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp
        ),
        color = TextPrimaryDark,
        textAlign = TextAlign.Center
      )

      Text(
        text = "Inventory & Billing",
        style = MaterialTheme.typography.bodyMedium.copy(
          fontWeight = FontWeight.SemiBold
        ),
        color = GoldAmber,
        textAlign = TextAlign.Center
      )

      Spacer(modifier = Modifier.height(18.dp))

      // Dynamic Time-Based Greeting (05:00-11:59 Good Morning, 12:00-16:59 Good Afternoon, 17:00-20:59 Good Evening, 21:00-04:59 Good Night)
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, DarkBorder)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          val icon = when (greetingInfo.iconName) {
            "wb_sunny" -> Icons.Default.WbSunny
            "wb_twilight" -> Icons.Default.WbTwilight
            "nights_stay" -> Icons.Default.NightsStay
            else -> Icons.Default.Bedtime
          }
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GoldAmber,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = greetingInfo.greeting,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
            color = TextPrimaryDark
          )
        }
      }

      Spacer(modifier = Modifier.height(28.dp))

      // Login Card
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(22.dp)
        ) {
          Text(
            text = "Secure Merchant Login",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimaryDark
          )

          Spacer(modifier = Modifier.height(18.dp))

          // Email Field
          OutlinedTextField(
            value = email,
            onValueChange = {
              email = it
              errorMessage = null
            },
            label = { Text("Email") },
            placeholder = { Text("Enter registered email") },
            leadingIcon = {
              Icon(Icons.Default.Email, contentDescription = null, tint = Emerald400)
            },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Email,
              imeAction = ImeAction.Next
            ),
            keyboardActions = KeyboardActions(
              onNext = { focusManager.moveFocus(FocusDirection.Down) }
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedLabelColor = Emerald400,
              unfocusedLabelColor = TextSecondaryDark,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated,
              cursorColor = Emerald400
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_email_input")
          )

          Spacer(modifier = Modifier.height(16.dp))

          // Password Field
          OutlinedTextField(
            value = password,
            onValueChange = {
              password = it
              errorMessage = null
            },
            label = { Text("Password") },
            placeholder = { Text("Enter password") },
            leadingIcon = {
              Icon(Icons.Default.Lock, contentDescription = null, tint = Emerald400)
            },
            trailingIcon = {
              IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                Icon(
                  imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                  contentDescription = if (isPasswordVisible) "Hide password" else "Show password",
                  tint = TextSecondaryDark
                )
              }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
              keyboardType = KeyboardType.Password,
              imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
              onDone = {
                focusManager.clearFocus()
                if (email.isNotBlank() && password.isNotBlank() && !isLoading) {
                  isLoading = true
                  errorMessage = null
                  onLogin(email, password) { success, msg ->
                    isLoading = false
                    if (!success) {
                      errorMessage = msg
                      password = ""
                    }
                  }
                }
              }
            ),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = Emerald400,
              unfocusedBorderColor = DarkBorder,
              focusedLabelColor = Emerald400,
              unfocusedLabelColor = TextSecondaryDark,
              focusedTextColor = TextPrimaryDark,
              unfocusedTextColor = TextPrimaryDark,
              focusedContainerColor = DarkSurfaceElevated,
              unfocusedContainerColor = DarkSurfaceElevated,
              cursorColor = Emerald400
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("login_password_input")
          )

          // Forgot Password?
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
          ) {
            TextButton(
              onClick = { showForgotDialog = true },
              enabled = !isLoading,
              modifier = Modifier.testTag("forgot_password_button")
            ) {
              Text(
                text = "Forgot Password?",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = Emerald400
              )
            }
          }

          AnimatedVisibility(
            visible = errorMessage != null,
            enter = fadeIn(),
            exit = fadeOut()
          ) {
            Text(
              text = errorMessage ?: "",
              color = SoftRed,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier
                .padding(bottom = 12.dp)
                .testTag("login_error_message")
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          // Login Button
          Button(
            onClick = {
              if (email.isBlank() || password.isBlank()) {
                errorMessage = "Please enter your email and password"
                return@Button
              }
              isLoading = true
              errorMessage = null
              onLogin(email, password) { success, msg ->
                isLoading = false
                if (!success) {
                  errorMessage = msg
                  password = ""
                }
              }
            },
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = Emerald500,
              contentColor = DarkBg,
              disabledContainerColor = Emerald900,
              disabledContentColor = TextSecondaryDark
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp)
              .testTag("login_submit_button")
          ) {
            if (isLoading) {
              CircularProgressIndicator(
                color = DarkBg,
                strokeWidth = 2.dp,
                modifier = Modifier.size(22.dp)
              )
            } else {
              Text(
                text = "Login to Dashboard",
                style = MaterialTheme.typography.labelLarge.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
              )
            }
          }
        }
      }
    }
  }

  // Forgot Password Dialog
  if (showForgotDialog) {
    ForgotPasswordDialog(
      initialEmail = email,
      onDismiss = { showForgotDialog = false },
      onSubmit = { resetEmail, onComplete ->
        onForgotPassword(resetEmail, onComplete)
      }
    )
  }
}

@Composable
private fun ForgotPasswordDialog(
  initialEmail: String,
  onDismiss: () -> Unit,
  onSubmit: (email: String, onComplete: (Boolean, String) -> Unit) -> Unit
) {
  var resetEmail by remember { mutableStateOf(initialEmail) }
  var isSending by remember { mutableStateOf(false) }
  var feedbackMessage by remember { mutableStateOf<String?>(null) }
  var isSuccess by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isSending) onDismiss() },
    title = {
      Text(
        text = "Reset Password",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = TextPrimaryDark
      )
    },
    text = {
      Column {
        Text(
          text = "Enter your registered email address to receive password reset instructions.",
          style = MaterialTheme.typography.bodyMedium,
          color = TextSecondaryDark
        )
        Spacer(modifier = Modifier.height(14.dp))
        OutlinedTextField(
          value = resetEmail,
          onValueChange = {
            resetEmail = it
            feedbackMessage = null
          },
          label = { Text("Registered Email") },
          placeholder = { Text("e.g. merchant@example.com") },
          singleLine = true,
          enabled = !isSending && !isSuccess,
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Emerald400,
            unfocusedBorderColor = DarkBorder,
            focusedLabelColor = Emerald400,
            unfocusedLabelColor = TextSecondaryDark,
            focusedTextColor = TextPrimaryDark,
            unfocusedTextColor = TextPrimaryDark,
            focusedContainerColor = DarkSurfaceElevated,
            unfocusedContainerColor = DarkSurfaceElevated
          ),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        )

        feedbackMessage?.let { msg ->
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = msg,
            color = if (isSuccess) Emerald400 else SoftRed,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
          )
        }
      }
    },
    confirmButton = {
      if (!isSuccess) {
        Button(
          onClick = {
            if (resetEmail.isBlank()) return@Button
            isSending = true
            onSubmit(resetEmail) { success, msg ->
              isSending = false
              isSuccess = success
              feedbackMessage = msg
            }
          },
          enabled = !isSending && resetEmail.isNotBlank(),
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
        ) {
          if (isSending) {
            CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          } else {
            Text("Send Reset Link")
          }
        }
      } else {
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg)
        ) {
          Text("Done")
        }
      }
    },
    dismissButton = {
      if (!isSuccess) {
        TextButton(
          onClick = onDismiss,
          enabled = !isSending
        ) {
          Text("Cancel", color = TextSecondaryDark)
        }
      }
    },
    containerColor = DarkCard,
    shape = RoundedCornerShape(16.dp)
  )
}
