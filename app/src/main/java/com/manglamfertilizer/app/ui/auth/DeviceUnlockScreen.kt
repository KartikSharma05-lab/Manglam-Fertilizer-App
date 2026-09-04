package com.manglamfertilizer.app.ui.auth

import android.widget.Toast
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.data.util.BiometricAuthManager
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.util.Locale

@Composable
fun DeviceUnlockScreen(
  user: User,
  onUnlockSuccess: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var hasPromptedAuto by remember { mutableStateOf(false) }

  // Resolve display name for the Welcome Back greeting
  val displayName = remember(user) {
    val rawName = user.name.trim()
    if (rawName.isNotBlank()) {
      rawName
    } else {
      val emailPrefix = user.email.substringBefore('@').trim()
      if (emailPrefix.isNotBlank()) {
        emailPrefix.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
      } else {
        "User"
      }
    }
  }

  fun triggerBiometricAuth(showErrorToast: Boolean = true) {
    val activity = context as? FragmentActivity
    if (activity != null) {
      BiometricAuthManager.authenticate(
        activity = activity,
        title = "Unlock ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME}",
        subtitle = "Verify your identity to access inventory and billing",
        onSuccess = {
          onUnlockSuccess()
        },
        onError = { msg ->
          // Show brief non-intrusive toast instead of permanent red error banner
          if (showErrorToast && msg.isNotBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
          }
        }
      )
    } else {
      if (showErrorToast) {
        Toast.makeText(context, "Biometric service is not ready. Please try again.", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // Automatically request authentication on initial launch
  LaunchedEffect(Unit) {
    if (!hasPromptedAuto) {
      hasPromptedAuto = true
      triggerBiometricAuth(showErrorToast = false)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg)
      .statusBarsPadding()
      .navigationBarsPadding()
      .padding(horizontal = 24.dp, vertical = 20.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.SpaceBetween
  ) {
    // 1. Top Section: Shop Logo & Branding
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(top = 16.dp)
    ) {
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
            contentDescription = "Mangalam Fertilizer Logo",
            tint = Emerald400,
            modifier = Modifier.size(38.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "MANGALAM FERTILIZER",
        style = MaterialTheme.typography.titleLarge.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.2.sp
        ),
        color = TextPrimaryDark
      )

      Spacer(modifier = Modifier.height(3.dp))

      Text(
        text = "Inventory & Billing",
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        color = GoldAmber
      )
    }

    // 2. Middle Section: Clean Welcome Back Card with Biometric Visual
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = DarkCard,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Surface(
          shape = CircleShape,
          color = DarkSurfaceElevated,
          border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f)),
          modifier = Modifier.size(72.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = Icons.Default.Fingerprint,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(38.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
          text = "Welcome Back, $displayName",
          style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          ),
          color = TextPrimaryDark,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Verify your identity to open your dashboard",
          style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
          color = TextSecondaryDark,
          textAlign = TextAlign.Center
        )
      }
    }

    // 3. Bottom Action Buttons
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = { triggerBiometricAuth(showErrorToast = true) },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = Emerald500,
          contentColor = DarkBg
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("unlock_biometric_button")
      ) {
        Icon(
          imageVector = Icons.Default.Fingerprint,
          contentDescription = null,
          modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Unlock with Biometric / Screen Lock",
          fontWeight = FontWeight.Bold,
          fontSize = 14.5.sp
        )
      }

      OutlinedButton(
        onClick = onLogout,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
          contentColor = TextSecondaryDark
        ),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .height(48.dp)
          .testTag("unlock_signout_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Logout,
          contentDescription = null,
          modifier = Modifier.size(17.dp),
          tint = TextSecondaryDark
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Sign in with another account",
          fontSize = 13.5.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }
  }
}
