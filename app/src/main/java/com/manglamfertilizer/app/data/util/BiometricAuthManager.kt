package com.manglamfertilizer.app.data.util

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuthManager {
  private const val TAG = "BiometricAuthManager"

  fun isBiometricOrDeviceLockAvailable(context: Context): Boolean {
    val biometricManager = BiometricManager.from(context)
    val authenticators = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    } else {
      BIOMETRIC_WEAK or DEVICE_CREDENTIAL
    }

    val canAuth = biometricManager.canAuthenticate(authenticators)
    Log.d(TAG, "Biometric/DeviceLock availability code: $canAuth")
    return canAuth == BiometricManager.BIOMETRIC_SUCCESS ||
        canAuth == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED
  }

  fun authenticate(
    activity: FragmentActivity,
    title: String = "Unlock ${AppConstants.OFFICIAL_SHOP_NAME}",
    subtitle: String = "Authenticate to access your inventory and billing",
    onSuccess: () -> Unit,
    onError: (String) -> Unit
  ) {
    val executor = ContextCompat.getMainExecutor(activity)

    val promptCallback = object : BiometricPrompt.AuthenticationCallback() {
      override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
        super.onAuthenticationSucceeded(result)
        Log.i(TAG, "Biometric authentication succeeded.")
        onSuccess()
      }

      override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
        super.onAuthenticationError(errorCode, errString)
        Log.w(TAG, "Biometric authentication error $errorCode: $errString")
        // Don't treat user cancellation as a fatal failure, just pass message for retry
        onError(errString.toString())
      }

      override fun onAuthenticationFailed() {
        super.onAuthenticationFailed()
        Log.w(TAG, "Biometric authentication failed (unrecognized).")
        onError("Authentication not recognized. Please try again.")
      }
    }

    try {
      val biometricPrompt = BiometricPrompt(activity, executor, promptCallback)

      val promptInfoBuilder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)

      // Support Biometric + Device PIN/Pattern/Password
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_STRONG or BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
      } else {
        @Suppress("DEPRECATION")
        promptInfoBuilder.setAllowedAuthenticators(BIOMETRIC_WEAK or DEVICE_CREDENTIAL)
      }

      val promptInfo = promptInfoBuilder.build()
      biometricPrompt.authenticate(promptInfo)
    } catch (e: Exception) {
      Log.e(TAG, "Failed to launch BiometricPrompt: ${e.message}", e)
      onError(e.message ?: "Unable to launch device authentication.")
    }
  }
}
