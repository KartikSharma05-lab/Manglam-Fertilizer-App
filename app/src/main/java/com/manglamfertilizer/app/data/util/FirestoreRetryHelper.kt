package com.manglamfertilizer.app.data.util

import android.util.Log
import kotlinx.coroutines.delay
import kotlin.math.min
import kotlin.math.pow

/**
 * Controlled Cloud Sync Retry Engine for Firestore and Network operations.
 *
 * Pattern:
 * - 1st attempt: Immediate
 * - 1st retry: 1000ms delay
 * - 2nd retry: 2000ms delay
 * - 3rd retry: 4000ms delay
 * - Subsequent retries: Exponential backoff with jitter up to maxBackoffMs (capped at 30s)
 * - Prevents infinite tight loops and respects Firestore Spark quota efficiency.
 */
object FirestoreRetryHelper {
  private const val TAG = "FirestoreRetryHelper"
  const val DEFAULT_MAX_RETRIES = 3
  const val DEFAULT_INITIAL_DELAY_MS = 1000L
  const val DEFAULT_MAX_DELAY_MS = 30_000L

  /**
   * Executes a block with controlled retries and exponential backoff.
   */
  suspend fun <T> withRetry(
    tag: String = TAG,
    operationName: String = "operation",
    maxRetries: Int = DEFAULT_MAX_RETRIES,
    initialDelayMs: Long = DEFAULT_INITIAL_DELAY_MS,
    maxDelayMs: Long = DEFAULT_MAX_DELAY_MS,
    shouldRetry: (Throwable) -> Boolean = { true },
    onRetry: ((attempt: Int, delayMs: Long, error: Throwable) -> Unit)? = null,
    block: suspend () -> T
  ): Result<T> {
    var currentDelay = initialDelayMs
    var attempt = 0

    while (true) {
      attempt++
      try {
        val result = block()
        return Result.success(result)
      } catch (t: Throwable) {
        val isLastAttempt = attempt > maxRetries
        if (isLastAttempt || !shouldRetry(t)) {
          Log.e(tag, "Operation '$operationName' failed after $attempt attempt(s): ${t.message}")
          return Result.failure(t)
        }

        // Calculate backoff: 1st retry = 1s, 2nd = 2s, 3rd = 4s, etc.
        val exponentialMultiplier = 2.0.pow((attempt - 1).toDouble()).toLong()
        val computedDelay = min(initialDelayMs * exponentialMultiplier, maxDelayMs)
        val jitter = (0..200).random()
        val finalDelay = computedDelay + jitter

        Log.w(tag, "Retry attempt #$attempt for '$operationName' in ${finalDelay}ms. Cause: ${t.message}")
        onRetry?.invoke(attempt, finalDelay, t)
        delay(finalDelay)
      }
    }
  }
}
