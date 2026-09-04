package com.manglamfertilizer.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.manglamfertilizer.app.data.local.dao.CustomerDao
import com.manglamfertilizer.app.data.local.entity.CustomerEntity
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.data.util.DeletedRecordsTracker
import com.manglamfertilizer.app.data.util.FirestoreProvider
import com.manglamfertilizer.app.data.util.NetworkSyncObserver
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CustomerRepository(
  private val customerDao: CustomerDao,
  private val auditRepository: AuditRepository? = null
) {
  private val tag = "CustomerRepository"
  private val firestore: FirebaseFirestore? = FirestoreProvider.get()

  private var activeAuditRepository: AuditRepository? = auditRepository
  private var networkSyncObserver: NetworkSyncObserver? = null
  private var customersListenerRegistration: ListenerRegistration? = null
  private val recentlyDeletedCustomerIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

  fun attachAuditRepository(repo: AuditRepository) {
    this.activeAuditRepository = repo
  }

  fun attachNetworkObserver(observer: NetworkSyncObserver) {
    this.networkSyncObserver = observer
  }

  val customers: Flow<List<Customer>> = customerDao.getAllCustomers().map { entities ->
    entities.map { it.toCustomer() }
  }

  fun startRealtimeSync(scope: CoroutineScope) {
    val db = firestore ?: return
    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    if (authUser == null) {
      Log.d(tag, "Waiting for user authentication before starting Customers realtime sync")
      return
    }

    val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID

    if (customersListenerRegistration == null) {
      try {
        customersListenerRegistration = db.collection("businesses").document(businessId)
          .collection("customers")
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(tag, "Notice on Firestore Customers SnapshotListener: ${error.message}")
              if (error.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                stopRealtimeSync()
              } else {
                networkSyncObserver?.setSyncError(true)
              }
              return@addSnapshotListener
            }

            if (snapshot != null) {
              val hasPending = snapshot.metadata.hasPendingWrites()
              networkSyncObserver?.setPendingWrites(hasPending)
              networkSyncObserver?.setFirebaseSuccess()

              scope.launch(Dispatchers.IO) {
                try {
                  val remoteCustomers = snapshot.documents.mapNotNull { doc ->
                    parseCustomerFromDoc(doc)
                  }.filterNot { it.id in recentlyDeletedCustomerIds || DeletedRecordsTracker.isDeleted(it.id) }

                  if (remoteCustomers.isNotEmpty()) {
                    val remoteEntities = remoteCustomers.map { CustomerEntity.fromCustomer(it) }
                    customerDao.insertCustomers(remoteEntities)

                    // Remove local items deleted remotely or pending deletion
                    val remoteIds = remoteCustomers.map { it.id }.toSet()
                    val localEntities = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
                    localEntities.filterNot { it.id in remoteIds }.forEach {
                      customerDao.deleteCustomerById(it.id)
                    }
                    localEntities.filter { it.id in recentlyDeletedCustomerIds || DeletedRecordsTracker.isDeleted(it.id) }.forEach {
                      customerDao.deleteCustomerById(it.id)
                    }
                  } else if (snapshot.documents.isEmpty()) {
                    // Check if root collection has legacy customers before clearing
                    val rootSnap = db.collection("customers").get().await()
                    if (rootSnap != null && !rootSnap.isEmpty) {
                      val legacyCustomers = rootSnap.documents.mapNotNull { parseCustomerFromDoc(it) }
                        .filterNot { it.id in recentlyDeletedCustomerIds || DeletedRecordsTracker.isDeleted(it.id) }
                      if (legacyCustomers.isNotEmpty()) {
                        customerDao.insertCustomers(legacyCustomers.map { CustomerEntity.fromCustomer(it) })
                        val batch = db.batch()
                        legacyCustomers.forEach { c ->
                          val targetDoc = db.collection("businesses").document(businessId).collection("customers").document(c.id)
                          batch.set(targetDoc, buildCustomerFirestoreMap(c))
                        }
                        batch.commit().await()
                      }
                    } else {
                      val localEntities = customerDao.getAllCustomers().firstOrNull() ?: emptyList()
                      localEntities.forEach { customerDao.deleteCustomerById(it.id) }
                    }
                  }
                } catch (e: Exception) {
                  Log.w(tag, "Error syncing customers snapshot to Room: ${e.message}")
                }
              }
            }
          }
      } catch (e: Exception) {
        Log.w(tag, "Failed to attach customers listener: ${e.message}")
      }
    }
  }

  fun stopRealtimeSync() {
    customersListenerRegistration?.remove()
    customersListenerRegistration = null
  }

  private fun parseCustomerFromDoc(doc: DocumentSnapshot): Customer? {
    return try {
      val id = doc.getString("id") ?: doc.id
      val name = doc.getString("name") ?: doc.getString("customerName") ?: ""
      if (name.isBlank()) return null

      val phone = doc.getString("phoneNumber") ?: doc.getString("phone") ?: ""
      val aadhaar = doc.getString("aadhaarNumber") ?: doc.getString("aadhaar") ?: ""
      val village = doc.getString("village") ?: ""
      val address = doc.getString("address") ?: ""
      val totalPurchases = doc.getDouble("totalPurchases") ?: 0.0
      val totalDue = doc.getDouble("totalDue") ?: 0.0
      val lastTransactionDate = doc.getLong("lastTransactionDate")
      val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()

      Customer(
        id = id,
        name = name,
        phoneNumber = phone,
        aadhaarNumber = aadhaar,
        village = village,
        address = address,
        totalPurchases = totalPurchases,
        totalDue = totalDue,
        lastTransactionDate = lastTransactionDate,
        createdAt = createdAt
      )
    } catch (e: Exception) {
      Log.e(tag, "Failed to parse customer doc ${doc.id}: ${e.message}", e)
      null
    }
  }

  private fun buildCustomerFirestoreMap(customer: Customer): Map<String, Any?> {
    return mapOf(
      "id" to customer.id,
      "customerId" to customer.id,
      "name" to customer.name,
      "customerName" to customer.name,
      "phoneNumber" to customer.phoneNumber,
      "phone" to customer.phoneNumber,
      "mobile" to customer.phoneNumber,
      "aadhaarNumber" to customer.aadhaarNumber,
      "aadhaar" to customer.aadhaarNumber,
      "village" to customer.village,
      "address" to customer.address,
      "totalPurchases" to customer.totalPurchases,
      "totalDue" to customer.totalDue,
      "lastTransactionDate" to customer.lastTransactionDate,
      "createdAt" to customer.createdAt,
      "updatedAt" to System.currentTimeMillis(),
      "serverTimestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
    )
  }

  suspend fun addCustomer(
    name: String,
    phoneNumber: String,
    aadhaarNumber: String = "",
    village: String = "",
    address: String = "",
    userEmail: String = "",
    userName: String = "Admin"
  ): Result<Customer> = withContext(Dispatchers.IO) {
    try {
      val customerId = "cust_${UUID.randomUUID().toString().take(8)}"
      val customer = Customer(
        id = customerId,
        name = name.trim(),
        phoneNumber = phoneNumber.trim(),
        aadhaarNumber = aadhaarNumber.trim(),
        village = village.trim(),
        address = address.trim(),
        totalPurchases = 0.0,
        totalDue = 0.0,
        lastTransactionDate = null,
        createdAt = System.currentTimeMillis()
      )

      // 1. Authoritative Firestore write FIRST
      try {
        networkSyncObserver?.setSyncInProgress(true)
        val db = firestore
        if (db != null) {
          val businessId = FirestoreProvider.BUSINESS_ID
          val map = buildCustomerFirestoreMap(customer)
          db.collection("businesses").document(businessId).collection("customers").document(customerId)
            .set(map).await()
          try {
            db.collection("customers").document(customerId).set(map).await()
          } catch (e: Exception) {}
          networkSyncObserver?.setFirebaseSuccess()
        }
      } catch (e: Exception) {
        Log.w(tag, "Firestore customer write notice: ${e.message}")
        networkSyncObserver?.setPendingWrites(true)
      } finally {
        networkSyncObserver?.setSyncInProgress(false)
      }

      // 2. Save locally to Room cache
      customerDao.insertCustomer(CustomerEntity.fromCustomer(customer))

      // 3. Log Audit
      val authorRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logCustomerCreated(
        customerId = customerId,
        customerName = customer.name,
        phone = customer.phoneNumber,
        village = customer.village,
        userEmail = userEmail.ifBlank { userName },
        userRole = authorRole,
        userId = ""
      )

      Result.success(customer)
    } catch (e: Exception) {
      Log.e(tag, "addCustomer error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun updateCustomer(
    customer: Customer,
    userEmail: String = "",
    userName: String = "Admin"
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      // 1. Authoritative Firestore update FIRST
      try {
        networkSyncObserver?.setSyncInProgress(true)
        val db = firestore
        if (db != null) {
          val businessId = FirestoreProvider.BUSINESS_ID
          val map = buildCustomerFirestoreMap(customer)
          db.collection("businesses").document(businessId).collection("customers").document(customer.id)
            .set(map).await()
          try {
            db.collection("customers").document(customer.id).set(map).await()
          } catch (e: Exception) {}
          networkSyncObserver?.setFirebaseSuccess()
        }
      } catch (e: Exception) {
        Log.w(tag, "Firestore customer update notice: ${e.message}")
        networkSyncObserver?.setPendingWrites(true)
      } finally {
        networkSyncObserver?.setSyncInProgress(false)
      }

      // 2. Update local Room cache
      customerDao.updateCustomer(CustomerEntity.fromCustomer(customer))

      val authorRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logCustomerUpdated(
        customerId = customer.id,
        customerName = customer.name,
        changesSummary = "Updated phone=${customer.phoneNumber}, village=${customer.village}",
        userEmail = userEmail.ifBlank { userName },
        userRole = authorRole,
        userId = ""
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(tag, "updateCustomer error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun updateCustomerDue(customerId: String, addedPurchase: Double, addedDue: Double) = withContext(Dispatchers.IO) {
    try {
      val entity = customerDao.getCustomerById(customerId) ?: return@withContext
      val current = entity.toCustomer()
      val updated = current.copy(
        totalPurchases = current.totalPurchases + addedPurchase,
        totalDue = (current.totalDue + addedDue).coerceAtLeast(0.0),
        lastTransactionDate = System.currentTimeMillis()
      )
      customerDao.updateCustomer(CustomerEntity.fromCustomer(updated))
      try {
        val db = firestore
        if (db != null) {
          val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
          val map = buildCustomerFirestoreMap(updated)
          db.collection("businesses").document(businessId).collection("customers").document(customerId)
            .set(map).await()
          try {
            db.collection("customers").document(customerId).set(map)
          } catch (e: Exception) {}
        }
      } catch (e: Exception) {
        Log.w(tag, "Firestore updateCustomerDue queued or failed: ${e.message}")
      }
    } catch (e: Exception) {
      Log.e(tag, "updateCustomerDue error: ${e.message}", e)
    }
  }

  suspend fun recordDuePayment(customerId: String, paymentAmount: Double): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val entity = customerDao.getCustomerById(customerId)
        ?: return@withContext Result.failure(Exception("Customer not found"))
      val current = entity.toCustomer()
      val updated = current.copy(
        totalDue = (current.totalDue - paymentAmount).coerceAtLeast(0.0),
        lastTransactionDate = System.currentTimeMillis()
      )
      customerDao.updateCustomer(CustomerEntity.fromCustomer(updated))
      try {
        val db = firestore
        if (db != null) {
          val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
          val map = buildCustomerFirestoreMap(updated)
          db.collection("businesses").document(businessId).collection("customers").document(customerId)
            .set(map).await()
          try {
            db.collection("customers").document(customerId).set(map)
          } catch (e: Exception) {}
          networkSyncObserver?.setFirebaseSuccess()
        }
      } catch (e: Exception) {
        Log.w(tag, "Firestore recordDuePayment queued: ${e.message}")
        networkSyncObserver?.setPendingWrites(true)
      }
      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteCustomer(
    customerId: String,
    userEmail: String = "",
    userName: String = "Admin"
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val effectiveEmail = AdminAuthUtils.resolveAdminEmail(userEmail, userName)
      if (!AdminAuthUtils.isAdmin(effectiveEmail)) {
        Log.e(tag, "Delete customer rejected: '$effectiveEmail' is not an authorized administrator.")
        return@withContext Result.failure(
          SecurityException("Unauthorized: Only an authorized Admin can delete farmer/customer records. Verified email: $effectiveEmail")
        )
      }

      val existing = customerDao.getCustomerById(customerId)?.toCustomer()
      val db = firestore
      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID

      // Idempotency: If already deleted locally and in Firestore, succeed cleanly
      if (existing == null) {
        if (db != null) {
          try {
            val cloudDoc = db.collection("businesses").document(businessId).collection("customers").document(customerId).get().await()
            if (!cloudDoc.exists()) {
              recentlyDeletedCustomerIds.add(customerId)
              DeletedRecordsTracker.markDeleted(customerId)
              return@withContext Result.success(Unit)
            }
          } catch (e: Exception) {
            recentlyDeletedCustomerIds.add(customerId)
            DeletedRecordsTracker.markDeleted(customerId)
            return@withContext Result.success(Unit)
          }
        } else {
          recentlyDeletedCustomerIds.add(customerId)
          DeletedRecordsTracker.markDeleted(customerId)
          return@withContext Result.success(Unit)
        }
      }

      val targetCustomer = existing ?: return@withContext Result.failure(Exception("Farmer/Customer not found"))

      // Safety rule: Never delete a customer who has outstanding dues
      if (targetCustomer.totalDue > 0.0) {
        return@withContext Result.failure(
          IllegalStateException("Cannot delete farmer with pending dues of ₹${targetCustomer.totalDue.toInt()}. Please settle all pending dues before deletion.")
        )
      }

      // 1. AUTHORITATIVE FIRESTORE DELETE FIRST
      if (db != null) {
        networkSyncObserver?.setSyncInProgress(true)
        try {
          db.collection("businesses").document(businessId).collection("customers").document(customerId).delete().await()

          // Best-effort cleanup of aliases / fallbacks
          try { db.collection("customers").document(customerId).delete().await() } catch (_: Exception) {}
          try { db.collection("businesses").document(businessId).collection("farmers").document(customerId).delete().await() } catch (_: Exception) {}
          try { db.collection("farmers").document(customerId).delete().await() } catch (_: Exception) {}

          networkSyncObserver?.setFirebaseSuccess()
          recentlyDeletedCustomerIds.add(customerId)
          DeletedRecordsTracker.markDeleted(customerId)
        } catch (e: Exception) {
          // FIRESTORE DELETION FAILED! DO NOT TOUCH ROOM!
          FirestoreProvider.logFirebaseError(tag, "DELETE", "customers", customerId, e)
          Log.e(tag, "Firestore deleteCustomer failed for $customerId: ${e.message}", e)
          networkSyncObserver?.setSyncError(true)
          return@withContext Result.failure(
            Exception("Unable to delete farmer/customer. Please check your permissions or connection.")
          )
        } finally {
          networkSyncObserver?.setSyncInProgress(false)
        }
      } else {
        recentlyDeletedCustomerIds.add(customerId)
        DeletedRecordsTracker.markDeleted(customerId)
      }

      // 2. FIRESTORE DELETION CONFIRMED! NOW REMOVE FROM ROOM DATABASE
      customerDao.deleteCustomerById(customerId)

      // 3. Log Authoritative Audit
      activeAuditRepository?.logCustomerDeleted(
        customerId = customerId,
        customerName = targetCustomer.name,
        userEmail = effectiveEmail,
        userRole = "ADMIN",
        userId = "",
        lastOutstanding = targetCustomer.totalDue
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(tag, "deleteCustomer fatal error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun syncWithFirestore() = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext
      val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
      if (authUser == null) {
        Log.d(tag, "Skipping customers sync: User not authenticated")
        return@withContext
      }

      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
      var snapshot = db.collection("businesses").document(businessId).collection("customers").get().await()
      if (snapshot == null || snapshot.isEmpty) {
        val rootSnap = db.collection("customers").get().await()
        if (rootSnap != null && !rootSnap.isEmpty) {
          snapshot = rootSnap
        }
      }

      if (snapshot != null && !snapshot.isEmpty) {
        val remoteCustomers = snapshot.documents.mapNotNull { doc ->
          parseCustomerFromDoc(doc)
        }
        if (remoteCustomers.isNotEmpty()) {
          customerDao.insertCustomers(remoteCustomers.map { CustomerEntity.fromCustomer(it) })
        }
      }
    } catch (e: Exception) {
      Log.w(tag, "Notice on customers syncWithFirestore: ${e.message}")
    }
  }
}
