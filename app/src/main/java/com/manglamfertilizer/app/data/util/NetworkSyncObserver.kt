package com.manglamfertilizer.app.data.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.manglamfertilizer.app.data.model.CloudSyncState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.random.Random

/**
 * Authoritative Network & Cloud Synchronization Observer for MANGALAM FERTILIZER.
 *
 * Deterministic Cloud State Machine & Single-Flight Guard:
 * 1. GREEN (CONNECTED_SYNCED):
 *    - Authenticated (FirebaseAuth.currentUser != null)
 *    - Android network available & validated
 *    - Firestore server probe succeeds (Source.SERVER)
 *    - Firestore server communication confirmed
 *    - Zero pending writes in local queue
 *    - Status: "Synced with Cloud"
 *
 * 2. YELLOW (SYNCING_OR_WEAK):
 *    - Server probe running (bounded, max 6s) -> "Connecting to Cloud..."
 *    - Active cloud sync in progress -> "Syncing with Cloud..."
 *    - Pending offline writes queue uploading -> "Uploading pending changes..."
 *
 * 3. RED (OFFLINE_OR_ERROR):
 *    - User unauthenticated / expired -> "Sign in required"
 *    - Device is offline -> "Offline (Saved locally)"
 *    - Firestore returns PERMISSION_DENIED -> "Cloud permission/configuration error"
 *    - Firestore server unavailable or probe timed out -> "Cloud unavailable"
 *
 * Single-Flight Guard:
 * - `probeInProgress` prevents concurrent/duplicate probes when user repeatedly taps the cloud icon.
 * - PERMISSION_DENIED stops automated rapid retries and awaits manual user action.
 * - Transient errors use exponential backoff with jitter (1s -> 30s).
 */
class NetworkSyncObserver(
  private val context: Context,
  private val scope: CoroutineScope
) {
  private val tag = "NetworkSyncObserver"

  private val connectivityManager =
    context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

  // Authoritative StateFlows observed by UI
  private val _syncState = MutableStateFlow(CloudSyncState.SYNCING_OR_WEAK)
  val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

  private val _syncStatusText = MutableStateFlow("Connecting to Cloud...")
  val syncStatusText: StateFlow<String> = _syncStatusText.asStateFlow()

  // Fine-grained state flags
  @Volatile private var isInternetConnected = false
  @Volatile private var isNetworkValidated = false
  @Volatile private var isFirebaseReachable = false
  @Volatile private var isSyncInProgress = false
  @Volatile private var hasPendingWrites = false
  @Volatile private var hasFirebaseError = false
  @Volatile private var isPermissionDenied = false
  @Volatile private var hasVerifiedCloudConnection = false
  @Volatile private var lastErrorMessage: String? = null

  // Single-flight guard to prevent multiple concurrent Firestore probes
  private val probeInProgress = AtomicBoolean(false)

  private var debounceJob: Job? = null
  private var connectivityCheckJob: Job? = null
  private var periodicReachabilityJob: Job? = null
  private var authStateListener: FirebaseAuth.AuthStateListener? = null

  // Exponential backoff parameters for transient network failures
  private var currentBackoffMs = 1000L
  private val maxBackoffMs = 30_000L

  init {
    checkInitialNetworkState()
    registerNetworkCallback()
    registerAuthStateListener()
    startPeriodicReachabilityCheck()
    triggerFirestoreReachabilityCheck(isImmediate = true)
  }

  private fun registerAuthStateListener() {
    try {
      authStateListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        Log.d(tag, "[SYNC_DIAGNOSTIC] AUTH_STATE_CHANGED => user: ${user?.email}")
        if (user != null) {
          isPermissionDenied = false
          hasFirebaseError = false
          lastErrorMessage = null
          currentBackoffMs = 1000L
          triggerFirestoreReachabilityCheck(isImmediate = true)
        } else {
          isFirebaseReachable = false
          hasVerifiedCloudConnection = false
          lastErrorMessage = "Sign in required"
          recalculateState(immediate = true)
        }
      }
      FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)
    } catch (e: Exception) {
      Log.w(tag, "[SYNC_DIAGNOSTIC] Unable to register AuthStateListener: ${e.message}")
    }
  }

  private fun checkInitialNetworkState() {
    try {
      val activeNetwork = connectivityManager?.activeNetwork
      val capabilities = connectivityManager?.getNetworkCapabilities(activeNetwork)
      updateCapabilities(capabilities)
      recalculateState(immediate = true)
    } catch (e: Exception) {
      Log.e(tag, "[SYNC_DIAGNOSTIC] Failed to check initial network state: ${e.message}", e)
      isInternetConnected = false
      recalculateState(immediate = true)
    }
  }

  private fun registerNetworkCallback() {
    val request = NetworkRequest.Builder()
      .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
      .build()

    try {
      connectivityManager?.registerNetworkCallback(
        request,
        object : ConnectivityManager.NetworkCallback() {
          override fun onAvailable(network: Network) {
            Log.d(tag, "[SYNC_DIAGNOSTIC] NETWORK = AVAILABLE")
            val capabilities = connectivityManager.getNetworkCapabilities(network)
            updateCapabilities(capabilities)
            recalculateState(immediate = false)
            // On network restore, clear transient backoff and re-probe
            if (!isPermissionDenied) {
              currentBackoffMs = 1000L
              triggerFirestoreReachabilityCheck(isImmediate = false)
            }
          }

          override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            updateCapabilities(capabilities)
            recalculateState(immediate = false)
          }

          override fun onLost(network: Network) {
            Log.d(tag, "[SYNC_DIAGNOSTIC] NETWORK = LOST (OFFLINE)")
            isInternetConnected = false
            isNetworkValidated = false
            isFirebaseReachable = false
            hasVerifiedCloudConnection = false
            lastErrorMessage = "Offline (Saved locally)"
            recalculateState(immediate = true)
          }

          override fun onUnavailable() {
            Log.d(tag, "[SYNC_DIAGNOSTIC] NETWORK = UNAVAILABLE")
            isInternetConnected = false
            isNetworkValidated = false
            isFirebaseReachable = false
            hasVerifiedCloudConnection = false
            lastErrorMessage = "Offline (Saved locally)"
            recalculateState(immediate = true)
          }
        }
      )
    } catch (e: Exception) {
      Log.w(tag, "[SYNC_DIAGNOSTIC] Unable to register network callback: ${e.message}")
    }
  }

  private fun updateCapabilities(capabilities: NetworkCapabilities?) {
    if (capabilities == null) {
      isInternetConnected = false
      isNetworkValidated = false
      return
    }

    val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val isValidated = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

    isInternetConnected = hasInternet
    isNetworkValidated = isValidated
  }

  /**
   * Bounded active Firestore reachability verification with single-flight guard.
   * Probes Firestore server with a strict timeout (6 seconds).
   */
  fun triggerFirestoreReachabilityCheck(isImmediate: Boolean = false) {
    // Single-flight guard: If a probe is already running, skip launching a duplicate request
    if (!probeInProgress.compareAndSet(false, true)) {
      Log.d(tag, "[SYNC_DIAGNOSTIC] Probe already in progress; ignoring duplicate trigger request")
      return
    }

    connectivityCheckJob?.cancel()
    connectivityCheckJob = scope.launch(Dispatchers.IO) {
      try {
        if (!isImmediate) {
          delay(200) // Brief debounce for rapid network state flutters
        }

        // Step 1: Immediately perform Firebase Authentication verification
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
          Log.i(tag, "[SYNC_DIAGNOSTIC] AUTH = UNAUTHENTICATED | SYNC = OFFLINE_OR_ERROR")
          isFirebaseReachable = false
          hasVerifiedCloudConnection = false
          hasFirebaseError = true
          isPermissionDenied = false
          lastErrorMessage = "Sign in required"
          recalculateState(immediate = true)
          return@launch
        }

        // Step 2: Verify Android physical network availability
        if (!isInternetConnected) {
          Log.i(tag, "[SYNC_DIAGNOSTIC] NETWORK = UNAVAILABLE | SYNC = OFFLINE_OR_ERROR")
          isFirebaseReachable = false
          hasVerifiedCloudConnection = false
          hasFirebaseError = true
          isPermissionDenied = false
          lastErrorMessage = "Offline (Saved locally)"
          recalculateState(immediate = true)
          return@launch
        }

        // Step 3: Fast Socket Ping to verify raw internet routing
        val socketReachable = performSocketPing()
        if (!socketReachable) {
          Log.w(tag, "[SYNC_DIAGNOSTIC] NETWORK = SOCKET_PING_FAILED")
        }

        // Step 4: Transition to YELLOW: "Connecting to Cloud..." during active probe
        _syncState.value = CloudSyncState.SYNCING_OR_WEAK
        _syncStatusText.value = "Connecting to Cloud..."
        Log.i(tag, "[SYNC_DIAGNOSTIC] FIRESTORE = PROBE_STARTED | User: ${currentUser.email}")

        var caughtPermissionDenied = false
        var caughtTransientError: String? = null

        // Step 5: Bounded Firestore Source.SERVER probe (Max 6 seconds timeout)
        val probeResult = withTimeoutOrNull(6000L) {
          try {
            val db = FirestoreProvider.get()
            if (db != null && FirebaseAuth.getInstance().currentUser != null) {
              val businessId = FirestoreProvider.BUSINESS_ID
              // Primary probe: business system health ping
              val docRef = db.collection("businesses").document(businessId)
                .collection("system_health").document("connectivity_ping")
              val snap = docRef.get(Source.SERVER).await()
              snap != null
            } else {
              false
            }
          } catch (e: FirebaseFirestoreException) {
            Log.w(tag, "[SYNC_DIAGNOSTIC] FIRESTORE_PROBE_ERROR = Code: ${e.code.name} | Msg: ${e.message}")
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
              caughtPermissionDenied = true
            } else if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                       e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
              caughtTransientError = "Cloud unavailable"
            }
            false
          } catch (e: Exception) {
            Log.w(tag, "[SYNC_DIAGNOSTIC] FIRESTORE_PROBE_EXCEPTION = ${e.message}")
            caughtTransientError = "Cloud unavailable"
            false
          }
        }

        if (probeResult == true) {
          // 6. Firestore server responds successfully: GREEN + "Synced with Cloud"
          Log.i(tag, "[SYNC_DIAGNOSTIC] FIRESTORE = SERVER_REACHABLE | SYNC = CONNECTED_SYNCED")
          isFirebaseReachable = true
          hasVerifiedCloudConnection = true
          hasFirebaseError = false
          isPermissionDenied = false
          lastErrorMessage = null
          currentBackoffMs = 1000L // Reset backoff on success
        } else {
          isFirebaseReachable = false
          if (caughtPermissionDenied) {
            // 7. PERMISSION_DENIED: RED + "Cloud permission/configuration error"
            Log.e(tag, "[SYNC_DIAGNOSTIC] FIRESTORE = PERMISSION_DENIED | Stopping auto-retry loop")
            hasFirebaseError = true
            isPermissionDenied = true
            lastErrorMessage = "Cloud permission/configuration error"
          } else {
            // 8. Unavailable or timed out: RED + "Cloud unavailable"
            Log.w(tag, "[SYNC_DIAGNOSTIC] FIRESTORE = UNREACHABLE_OR_TIMEOUT")
            isPermissionDenied = false
            hasFirebaseError = true
            lastErrorMessage = caughtTransientError ?: "Cloud unavailable"

            // Exponential backoff with jitter for transient errors: 1s, 2s, 4s, 8s, 16s, 30s
            val jitter = Random.nextLong(100, 500)
            currentBackoffMs = ((currentBackoffMs * 2) + jitter).coerceAtMost(maxBackoffMs)
          }
        }
      } finally {
        probeInProgress.set(false)
        recalculateState(immediate = true)
      }
    }
  }

  private fun startPeriodicReachabilityCheck() {
    periodicReachabilityJob?.cancel()
    periodicReachabilityJob = scope.launch(Dispatchers.IO) {
      while (true) {
        val delayInterval = if (isFirebaseReachable && hasVerifiedCloudConnection) {
          60_000L // Check every 60s when connected
        } else if (isPermissionDenied) {
          // Do NOT auto-poll when permission is denied; wait for user manual retry
          delay(10_000L)
          continue
        } else {
          currentBackoffMs // Exponential backoff for transient errors
        }
        delay(delayInterval)

        if (isInternetConnected && FirebaseAuth.getInstance().currentUser != null && !isPermissionDenied && !probeInProgress.get()) {
          triggerFirestoreReachabilityCheck(isImmediate = true)
        }
      }
    }
  }

  private fun performSocketPing(): Boolean {
    return try {
      Socket().use { socket ->
        socket.connect(InetSocketAddress("8.8.8.8", 53), 2500)
        true
      }
    } catch (e: Exception) {
      false
    }
  }

  /**
   * Called by user interaction (e.g. clicking cloud status indicator) to trigger an immediate retry.
   * Uses single-flight guard to avoid duplicate Firestore probes.
   */
  fun retryConnection() {
    Log.i(tag, "[SYNC_DIAGNOSTIC] RETRY_REQUESTED by user")
    hasFirebaseError = false
    isPermissionDenied = false
    lastErrorMessage = null
    currentBackoffMs = 1000L
    triggerFirestoreReachabilityCheck(isImmediate = true)
  }

  /**
   * Called by repositories when starting or completing cloud data transmissions.
   */
  fun setSyncInProgress(inProgress: Boolean) {
    isSyncInProgress = inProgress
    recalculateState(immediate = false)
  }

  /**
   * Called by snapshot listeners when metadata confirms whether local changes are pending in write queue.
   */
  fun setPendingWrites(pending: Boolean) {
    hasPendingWrites = pending
    if (!pending) {
      hasFirebaseError = false
    }
    recalculateState(immediate = false)
  }

  /**
   * Called by repositories when Firestore write or sync succeeds.
   * Marks cloud connection confirmed to allow Green state.
   */
  fun setFirebaseSuccess() {
    Log.d(tag, "[SYNC_DIAGNOSTIC] setFirebaseSuccess() received from repository")
    hasVerifiedCloudConnection = true
    isFirebaseReachable = true
    hasFirebaseError = false
    isPermissionDenied = false
    lastErrorMessage = null
    recalculateState(immediate = false)
  }

  /**
   * Called when Firestore encounters a network/permission/timeout error.
   */
  fun setSyncError(isError: Boolean = true, errorMessage: String? = null) {
    Log.w(tag, "[SYNC_DIAGNOSTIC] setSyncError(isError=$isError, msg=$errorMessage)")
    hasFirebaseError = isError
    lastErrorMessage = errorMessage
    if (isError) {
      isFirebaseReachable = false
      if (errorMessage?.contains("permission", ignoreCase = true) == true) {
        isPermissionDenied = true
      }
    }
    recalculateState(immediate = true)
  }

  /**
   * Called when user explicitly signs out.
   */
  fun onUserSignedOut() {
    isFirebaseReachable = false
    hasVerifiedCloudConnection = false
    hasPendingWrites = false
    isSyncInProgress = false
    lastErrorMessage = "Sign in required"
    recalculateState(immediate = true)
  }

  /**
   * Recalculates and emits the consolidated sync status state.
   */
  private fun recalculateState(immediate: Boolean = false) {
    debounceJob?.cancel()
    if (immediate) {
      applyState()
    } else {
      debounceJob = scope.launch(Dispatchers.Main) {
        delay(150)
        applyState()
      }
    }
  }

  private fun applyState() {
    val authUser = FirebaseAuth.getInstance().currentUser

    when {
      // 1. Red State: Unauthenticated, Offline, Permission Denied, or Hard Firestore Failure
      authUser == null -> {
        _syncState.value = CloudSyncState.OFFLINE_OR_ERROR
        _syncStatusText.value = "Sign in required"
      }
      !isInternetConnected -> {
        _syncState.value = CloudSyncState.OFFLINE_OR_ERROR
        _syncStatusText.value = "Offline (Saved locally)"
      }
      isPermissionDenied -> {
        _syncState.value = CloudSyncState.OFFLINE_OR_ERROR
        _syncStatusText.value = "Cloud permission/configuration error"
      }
      hasFirebaseError || (!isFirebaseReachable && !probeInProgress.get() && !hasVerifiedCloudConnection) -> {
        _syncState.value = CloudSyncState.OFFLINE_OR_ERROR
        _syncStatusText.value = lastErrorMessage ?: "Cloud unavailable"
      }

      // 2. Green State: Authenticated + Verified Reachable + First Cloud Op Confirmed + Zero Pending Writes + No Active Sync
      hasVerifiedCloudConnection && isFirebaseReachable && !hasPendingWrites && !hasFirebaseError && !isSyncInProgress && !probeInProgress.get() -> {
        _syncState.value = CloudSyncState.CONNECTED_SYNCED
        _syncStatusText.value = "Synced with Cloud"
      }

      // 3. Yellow State: Initial Bounded Connecting Probe OR Active Sync OR Pending Offline Writes
      probeInProgress.get() -> {
        _syncState.value = CloudSyncState.SYNCING_OR_WEAK
        _syncStatusText.value = "Connecting to Cloud..."
      }
      isSyncInProgress -> {
        _syncState.value = CloudSyncState.SYNCING_OR_WEAK
        _syncStatusText.value = "Syncing with Cloud..."
      }
      hasPendingWrites -> {
        _syncState.value = CloudSyncState.SYNCING_OR_WEAK
        _syncStatusText.value = "Uploading pending changes..."
      }

      // Fallback: If cloud connection was verified, show Synced; otherwise error
      hasVerifiedCloudConnection && isFirebaseReachable -> {
        _syncState.value = CloudSyncState.CONNECTED_SYNCED
        _syncStatusText.value = "Synced with Cloud"
      }
      else -> {
        _syncState.value = CloudSyncState.OFFLINE_OR_ERROR
        _syncStatusText.value = lastErrorMessage ?: "Cloud unavailable"
      }
    }

    Log.d(
      tag,
      "[SYNC_DIAGNOSTIC] Consolidated State => State: ${_syncState.value.name} | Text: \"${_syncStatusText.value}\" | Auth: ${authUser != null} | Net: $isInternetConnected | FB_Reachable: $isFirebaseReachable | Verified: $hasVerifiedCloudConnection | PendingWrites: $hasPendingWrites"
    )
  }
}

