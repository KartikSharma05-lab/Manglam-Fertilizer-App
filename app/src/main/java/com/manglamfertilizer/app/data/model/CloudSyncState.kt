package com.manglamfertilizer.app.data.model

enum class CloudSyncState {
  CONNECTED_SYNCED, // Internet connected + Firestore sync healthy (Green)
  SYNCING_OR_WEAK,  // Weak network or sync in progress (Yellow/Amber)
  OFFLINE_OR_ERROR  // Offline / sync error (Red)
}
