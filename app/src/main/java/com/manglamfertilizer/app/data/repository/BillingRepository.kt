package com.manglamfertilizer.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.manglamfertilizer.app.data.local.dao.CreditRecordDao
import com.manglamfertilizer.app.data.local.dao.CustomerDao
import com.manglamfertilizer.app.data.local.dao.InvoiceDao
import com.manglamfertilizer.app.data.local.dao.ProductDao
import com.manglamfertilizer.app.data.local.entity.CreditRecordEntity
import com.manglamfertilizer.app.data.local.entity.CustomerEntity
import com.manglamfertilizer.app.data.local.entity.InvoiceEntity
import com.manglamfertilizer.app.data.local.entity.ProductEntity
import com.manglamfertilizer.app.data.model.CreditRecord
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.InvoiceItem
import com.manglamfertilizer.app.data.model.InvoiceNumberConfig
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.util.AdminAuthUtils
import com.manglamfertilizer.app.data.util.DeletedRecordsTracker
import com.manglamfertilizer.app.data.util.FirestoreProvider
import com.manglamfertilizer.app.data.util.InvoiceNumberManager
import com.manglamfertilizer.app.data.util.NetworkSyncObserver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Authoritative Cloud Billing Repository.
 * All Invoices and Credit / Due Records are stored and synchronized via Cloud Firestore.
 */
class BillingRepository(
  private val invoiceDao: InvoiceDao,
  private val productDao: ProductDao,
  private val customerDao: CustomerDao,
  private val creditRecordDao: CreditRecordDao,
  private val customerRepository: CustomerRepository,
  private val auditRepository: AuditRepository? = null
) {
  private val tag = "BillingRepository"
  private val firestore: FirebaseFirestore? = FirestoreProvider.get()

  private var activeAuditRepository: AuditRepository? = auditRepository
  private var networkSyncObserver: NetworkSyncObserver? = null
  private var settingsRepository: SettingsRepository? = null
  private var invoicesListenerRegistration: ListenerRegistration? = null
  private var creditListenerRegistration: ListenerRegistration? = null
  private val recentlyDeletedInvoiceIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

  fun attachAuditRepository(repo: AuditRepository) {
    this.activeAuditRepository = repo
  }

  fun attachSettingsRepository(repo: SettingsRepository) {
    this.settingsRepository = repo
  }

  fun attachNetworkObserver(observer: NetworkSyncObserver) {
    this.networkSyncObserver = observer
  }

  val invoices: Flow<List<Invoice>> = invoiceDao.getAllInvoices().map { entities ->
    entities.map { it.toInvoice() }
  }

  val creditRecords: Flow<List<CreditRecord>> = creditRecordDao.getAllCreditRecords().map { entities ->
    entities.map { it.toCreditRecord() }
  }

  fun getCreditRecordsForCustomer(customerId: String): Flow<List<CreditRecord>> {
    return creditRecordDao.getCreditRecordsByCustomer(customerId).map { entities ->
      entities.map { it.toCreditRecord() }
    }
  }

  fun getTodayInvoices(): Flow<List<Invoice>> {
    val cal = Calendar.getInstance()
    cal.set(Calendar.HOUR_OF_DAY, 0)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val startOfDay = cal.timeInMillis

    val endCal = Calendar.getInstance().apply {
      timeInMillis = startOfDay
      add(Calendar.DAY_OF_MONTH, 1)
    }
    val startOfNextDay = endCal.timeInMillis

    return invoiceDao.getInvoicesBetween(startOfDay, startOfNextDay).map { entities ->
      entities.map { it.toInvoice() }
    }
  }

  /**
   * Starts realtime synchronization for Invoices and Credit records from Firestore.
   */
  fun startRealtimeSync(scope: CoroutineScope) {
    val db = firestore ?: return
    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    if (authUser == null) {
      Log.d(tag, "Waiting for user authentication before starting Invoices realtime sync")
      return
    }

    val businessId = FirestoreProvider.BUSINESS_ID

    // 1. Invoices snapshot listener
    if (invoicesListenerRegistration == null) {
      try {
        invoicesListenerRegistration = db.collection("businesses").document(businessId)
          .collection("invoices")
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(tag, "Notice on Firestore Invoices SnapshotListener: ${error.message}")
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
                  val remoteInvoices = snapshot.documents.mapNotNull { doc ->
                    parseInvoiceFromDoc(doc)
                  }.filterNot { it.id in recentlyDeletedInvoiceIds || DeletedRecordsTracker.isDeleted(it.id) }

                  if (remoteInvoices.isNotEmpty()) {
                    val remoteEntities = remoteInvoices.map { InvoiceEntity.fromInvoice(it) }
                    invoiceDao.insertInvoices(remoteEntities)

                    // Remove local invoices deleted remotely or pending deletion
                    val remoteIds = remoteInvoices.map { it.id }.toSet()
                    val localEntities = invoiceDao.getAllInvoices().firstOrNull() ?: emptyList()
                    localEntities.filterNot { it.id in remoteIds }.forEach {
                      invoiceDao.deleteInvoiceById(it.id)
                    }
                    localEntities.filter { it.id in recentlyDeletedInvoiceIds || DeletedRecordsTracker.isDeleted(it.id) }.forEach {
                      invoiceDao.deleteInvoiceById(it.id)
                    }
                  } else if (snapshot.documents.isEmpty()) {
                    val rootSnap = db.collection("invoices").get().await()
                    if (rootSnap != null && !rootSnap.isEmpty) {
                      val legacyInvoices = rootSnap.documents.mapNotNull { parseInvoiceFromDoc(it) }
                        .filterNot { it.id in recentlyDeletedInvoiceIds || DeletedRecordsTracker.isDeleted(it.id) }
                      if (legacyInvoices.isNotEmpty()) {
                        invoiceDao.insertInvoices(legacyInvoices.map { InvoiceEntity.fromInvoice(it) })
                        val batch = db.batch()
                        legacyInvoices.forEach { inv ->
                          val targetDoc = db.collection("businesses").document(businessId).collection("invoices").document(inv.id)
                          batch.set(targetDoc, buildInvoiceFirestoreMap(inv))
                        }
                        batch.commit().await()
                      }
                    } else {
                      val localEntities = invoiceDao.getAllInvoices().firstOrNull() ?: emptyList()
                      localEntities.forEach { invoiceDao.deleteInvoiceById(it.id) }
                    }
                  }
                } catch (e: Exception) {
                  Log.w(tag, "Error syncing invoices snapshot to Room: ${e.message}")
                }
              }
            }
          }
      } catch (e: Exception) {
        Log.w(tag, "Failed to attach invoices listener: ${e.message}")
      }
    }

    // 2. Credit Records snapshot listener
    if (creditListenerRegistration == null) {
      try {
        creditListenerRegistration = db.collection("businesses").document(businessId)
          .collection("credit_records")
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(tag, "Notice on Firestore CreditRecords SnapshotListener: ${error.message}")
              return@addSnapshotListener
            }

            if (snapshot != null) {
              scope.launch(Dispatchers.IO) {
                try {
                  val remoteCredits = snapshot.documents.mapNotNull { doc ->
                    parseCreditRecordFromDoc(doc)
                  }
                  if (remoteCredits.isNotEmpty()) {
                    creditRecordDao.insertCreditRecords(remoteCredits.map { CreditRecordEntity.fromCreditRecord(it) })
                  }
                } catch (e: Exception) {
                  Log.w(tag, "Error syncing credit records to Room: ${e.message}")
                }
              }
            }
          }
      } catch (e: Exception) {
        Log.w(tag, "Failed to attach credit records listener: ${e.message}")
      }
    }
  }

  fun stopRealtimeSync() {
    invoicesListenerRegistration?.remove()
    invoicesListenerRegistration = null
    creditListenerRegistration?.remove()
    creditListenerRegistration = null
  }

  @Suppress("UNCHECKED_CAST")
  private fun parseInvoiceFromDoc(doc: DocumentSnapshot): Invoice? {
    return try {
      val id = doc.getString("id") ?: doc.getString("invoiceId") ?: doc.id
      val invoiceNumber = doc.getString("invoiceNumber") ?: id
      val customerId = doc.getString("customerId")
      val customerName = doc.getString("customerName") ?: doc.getString("farmer") ?: doc.getString("customer") ?: "Walk-in Farmer"
      val customerPhone = doc.getString("customerPhone") ?: doc.getString("phone") ?: doc.getString("mobile") ?: ""
      val customerAadhaar = doc.getString("customerAadhaar") ?: doc.getString("aadhaar") ?: ""
      val customerAddress = doc.getString("customerAddress") ?: doc.getString("address") ?: ""
      val customerVillage = doc.getString("customerVillage") ?: doc.getString("village") ?: ""
      val subTotal = doc.getDouble("subTotal") ?: doc.getDouble("subtotal") ?: 0.0
      val gstRate = doc.getDouble("gstRate") ?: 0.0
      val gstAmount = doc.getDouble("gstAmount") ?: doc.getDouble("GST") ?: 0.0
      val discount = doc.getDouble("discount") ?: 0.0
      val grandTotal = doc.getDouble("grandTotal") ?: doc.getDouble("totalAmount") ?: 0.0
      val amountPaid = doc.getDouble("amountPaid") ?: doc.getDouble("receivedAmount") ?: 0.0
      val remainingDue = doc.getDouble("remainingDue") ?: doc.getDouble("dueAmount") ?: 0.0
      val dueDate = doc.getLong("dueDate")
      val paymentModeStr = doc.getString("paymentMode") ?: doc.getString("paymentMethod") ?: "CASH"
      val paymentMode = try { PaymentMode.valueOf(paymentModeStr) } catch (e: Exception) { PaymentMode.CASH }
      val timestamp = doc.getLong("timestamp") ?: doc.getLong("createdAt") ?: System.currentTimeMillis()
      val createdBy = doc.getString("createdBy") ?: "Admin"

      val itemsRaw = doc.get("items") as? List<*>
      val itemsList = mutableListOf<InvoiceItem>()
      if (itemsRaw != null) {
        for (raw in itemsRaw) {
          if (raw is Map<*, *>) {
            val pId = (raw["productId"] as? String) ?: ""
            val pName = (raw["productName"] as? String) ?: ""
            val batch = (raw["batchNumber"] as? String) ?: ""
            val qty = (raw["quantity"] as? Number)?.toDouble() ?: 0.0
            val unitStr = (raw["unit"] as? String) ?: "BAG"
            val unt = try { ProductUnit.valueOf(unitStr) } catch (e: Exception) { ProductUnit.BAG }
            val unitPrice = (raw["unitPrice"] as? Number)?.toDouble() ?: (raw["price"] as? Number)?.toDouble() ?: 0.0
            val totalPrice = (raw["totalPrice"] as? Number)?.toDouble() ?: (qty * unitPrice)

            if (pId.isNotBlank() || pName.isNotBlank()) {
              itemsList.add(
                InvoiceItem(
                  productId = pId,
                  productName = pName,
                  batchNumber = batch,
                  quantity = qty,
                  unit = unt,
                  unitPrice = unitPrice,
                  totalPrice = totalPrice
                )
              )
            }
          }
        }
      }

      Invoice(
        id = id,
        invoiceNumber = invoiceNumber,
        customerId = customerId,
        customerName = customerName,
        customerPhone = customerPhone,
        customerAadhaar = customerAadhaar,
        customerAddress = customerAddress,
        customerVillage = customerVillage,
        items = itemsList,
        subTotal = subTotal,
        gstRate = gstRate,
        gstAmount = gstAmount,
        discount = discount,
        grandTotal = grandTotal,
        amountPaid = amountPaid,
        remainingDue = remainingDue,
        dueDate = dueDate,
        paymentMode = paymentMode,
        timestamp = timestamp,
        createdBy = createdBy
      )
    } catch (e: Exception) {
      Log.e(tag, "Failed to parse invoice doc ${doc.id}: ${e.message}", e)
      null
    }
  }

  private fun parseCreditRecordFromDoc(doc: DocumentSnapshot): CreditRecord? {
    return try {
      val id = doc.getString("id") ?: doc.getString("creditId") ?: doc.id
      val customerId = doc.getString("customerId") ?: ""
      val customerName = doc.getString("customerName") ?: ""
      val invoiceId = doc.getString("invoiceId") ?: ""
      val invoiceNumber = doc.getString("invoiceNumber") ?: ""
      val amount = doc.getDouble("amount") ?: 0.0
      val dueDate = doc.getLong("dueDate")
      val paidAmount = doc.getDouble("paidAmount") ?: 0.0
      val remainingAmount = doc.getDouble("remainingAmount") ?: (amount - paidAmount).coerceAtLeast(0.0)
      val status = doc.getString("status") ?: "PENDING"
      val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
      val createdBy = doc.getString("createdBy") ?: "Admin"
      val createdByEmail = doc.getString("createdByEmail") ?: ""
      val updatedAt = doc.getLong("updatedAt") ?: createdAt

      CreditRecord(
        id = id,
        customerId = customerId,
        customerName = customerName,
        invoiceId = invoiceId,
        invoiceNumber = invoiceNumber,
        amount = amount,
        dueDate = dueDate,
        paidAmount = paidAmount,
        remainingAmount = remainingAmount,
        status = status,
        createdAt = createdAt,
        createdBy = createdBy,
        createdByEmail = createdByEmail,
        updatedAt = updatedAt
      )
    } catch (e: Exception) {
      Log.e(tag, "Failed to parse credit record doc ${doc.id}: ${e.message}", e)
      null
    }
  }

  private fun buildInvoiceFirestoreMap(invoice: Invoice): Map<String, Any?> {
    val itemsMapList = invoice.items.map { item ->
      mapOf(
        "productId" to item.productId,
        "productName" to item.productName,
        "batchNumber" to item.batchNumber,
        "quantity" to item.quantity,
        "unit" to item.unit.name,
        "unitPrice" to item.unitPrice,
        "totalPrice" to item.totalPrice
      )
    }

    val authorEmail = if (invoice.createdBy.contains("@")) invoice.createdBy else ""

    return mapOf(
      "id" to invoice.id,
      "invoiceId" to invoice.id,
      "invoiceNumber" to invoice.invoiceNumber,
      "customerId" to invoice.customerId,
      "customer" to invoice.customerName,
      "farmer" to invoice.customerName,
      "customerName" to invoice.customerName,
      "customerPhone" to invoice.customerPhone,
      "customerAadhaar" to invoice.customerAadhaar,
      "customerAddress" to invoice.customerAddress,
      "customerVillage" to invoice.customerVillage,
      "items" to itemsMapList,
      "subtotal" to invoice.subTotal,
      "subTotal" to invoice.subTotal,
      "gstRate" to invoice.gstRate,
      "GST" to invoice.gstAmount,
      "gstAmount" to invoice.gstAmount,
      "discount" to invoice.discount,
      "totalAmount" to invoice.grandTotal,
      "grandTotal" to invoice.grandTotal,
      "receivedAmount" to invoice.amountPaid,
      "amountPaid" to invoice.amountPaid,
      "dueAmount" to invoice.remainingDue,
      "remainingDue" to invoice.remainingDue,
      "paymentMethod" to invoice.paymentMode.name,
      "paymentMode" to invoice.paymentMode.name,
      "dueDate" to invoice.dueDate,
      "createdBy" to invoice.createdBy,
      "createdByEmail" to authorEmail,
      "createdAt" to invoice.timestamp,
      "updatedAt" to System.currentTimeMillis(),
      "serverTimestamp" to FieldValue.serverTimestamp()
    )
  }

  private fun buildCreditFirestoreMap(record: CreditRecord): Map<String, Any?> {
    return mapOf(
      "id" to record.id,
      "creditId" to record.id,
      "customerId" to record.customerId,
      "customerName" to record.customerName,
      "invoiceId" to record.invoiceId,
      "invoiceNumber" to record.invoiceNumber,
      "amount" to record.amount,
      "dueDate" to record.dueDate,
      "paidAmount" to record.paidAmount,
      "remainingAmount" to record.remainingAmount,
      "status" to record.status,
      "createdAt" to record.createdAt,
      "createdBy" to record.createdBy,
      "createdByEmail" to record.createdByEmail,
      "updatedAt" to record.updatedAt,
      "serverTimestamp" to FieldValue.serverTimestamp()
    )
  }

  /**
   * Creates a new Invoice and creates a Credit record if due > 0 as one atomic transaction.
   */
  suspend fun createInvoice(
    customerId: String?,
    customerName: String,
    customerPhone: String = "",
    customerAadhaar: String = "",
    customerAddress: String = "",
    customerVillage: String = "",
    items: List<InvoiceItem>,
    gstRate: Double = 0.0,
    discount: Double = 0.0,
    amountPaid: Double = 0.0,
    dueDate: Long? = null,
    paymentMode: PaymentMode = PaymentMode.CASH,
    createdBy: String = "Admin"
  ): Result<Invoice> = withContext(Dispatchers.IO) {
    try {
      if (items.isEmpty()) {
        return@withContext Result.failure(Exception("Cannot create invoice with empty items"))
      }

      val calculatedSubTotal = items.sumOf { it.totalPrice }
      val validDiscount = discount.coerceAtLeast(0.0)
      val taxableAmount = (calculatedSubTotal - validDiscount).coerceAtLeast(0.0)
      val validGstRate = if (gstRate in listOf(0.0, 5.0, 12.0, 18.0, 28.0)) gstRate else 0.0
      val calculatedGstAmount = (taxableAmount * validGstRate) / 100.0
      val calculatedGrandTotal = (taxableAmount + calculatedGstAmount).coerceAtLeast(0.0)
      val validAmountPaid = amountPaid.coerceAtLeast(0.0)
      val calculatedDue = (calculatedGrandTotal - validAmountPaid).coerceAtLeast(0.0)
      val effectiveDueDate = if (calculatedDue > 0) dueDate else null

      val effectiveCustomerName = customerName.trim().ifBlank { "Walk-in Farmer" }
      var finalCustomerId = customerId

      if (finalCustomerId.isNullOrBlank() && effectiveCustomerName != "Walk-in Farmer") {
        val newCustResult = customerRepository.addCustomer(
          name = effectiveCustomerName,
          phoneNumber = customerPhone.trim(),
          aadhaarNumber = customerAadhaar.trim(),
          village = customerVillage.trim(),
          address = customerAddress.trim()
        )
        if (newCustResult.isSuccess) {
          finalCustomerId = newCustResult.getOrNull()?.id
        }
      }

      val invoiceId = "inv_${UUID.randomUUID().toString().take(8)}"
      val authorEmail = if (createdBy.contains("@")) createdBy else ""
      val authorRole = if (AdminAuthUtils.isAdmin(authorEmail)) "ADMIN" else "STAFF"

      var finalInvoice: Invoice? = null
      var finalCreditRecord: CreditRecord? = null

      // Compute exact updated customer values locally for idempotent sync
      var updatedCustomerDue = 0.0
      var updatedCustomerPurchases = 0.0
      if (!finalCustomerId.isNullOrBlank()) {
        val existingCust = customerDao.getCustomerById(finalCustomerId)
        if (existingCust != null) {
          val custModel = existingCust.toCustomer()
          updatedCustomerPurchases = custModel.totalPurchases + calculatedGrandTotal
          updatedCustomerDue = (custModel.totalDue + calculatedDue).coerceAtLeast(0.0)
        }
      }

      // 1. Authoritative Atomic Firestore Transaction (Multi-device concurrency safe)
      val db = firestore
      if (db != null) {
        try {
          networkSyncObserver?.setSyncInProgress(true)
          val businessId = FirestoreProvider.BUSINESS_ID
          finalInvoice = db.runTransaction { transaction ->
            val settingsDocRef = db.collection("businesses").document(businessId)
              .collection("settings").document("invoiceNumber")
            val settingsSnap = transaction.get(settingsDocRef)

            val startingNum = settingsSnap.getLong("startingNumber") ?: InvoiceNumberManager.DEFAULT_STARTING_NUMBER
            val nextNum = settingsSnap.getLong("nextInvoiceNumber") ?: startingNum
            val prefix = settingsSnap.getString("prefix") ?: ""
            val suffix = settingsSnap.getString("suffix") ?: ""
            val enabled = settingsSnap.getBoolean("enabled") ?: true

            val allocatedLong = nextNum
            val invoiceNumber = if (prefix.isNotBlank() || suffix.isNotBlank()) {
              "$prefix$allocatedLong$suffix"
            } else {
              allocatedLong.toString()
            }
            val newNextNum = allocatedLong + 1

            // Build invoice with authoritative allocated sequential number
            val inv = Invoice(
              id = invoiceId,
              invoiceNumber = invoiceNumber,
              customerId = finalCustomerId,
              customerName = effectiveCustomerName,
              customerPhone = customerPhone.trim(),
              customerAadhaar = customerAadhaar.trim(),
              customerAddress = customerAddress.trim(),
              customerVillage = customerVillage.trim(),
              items = items,
              subTotal = calculatedSubTotal,
              gstRate = validGstRate,
              gstAmount = calculatedGstAmount,
              discount = validDiscount,
              grandTotal = calculatedGrandTotal,
              amountPaid = validAmountPaid,
              remainingDue = calculatedDue,
              dueDate = effectiveDueDate,
              paymentMode = paymentMode,
              timestamp = System.currentTimeMillis(),
              createdBy = createdBy
            )

            val invDocRef = db.collection("businesses").document(businessId)
              .collection("invoices").document(invoiceId)
            transaction.set(invDocRef, buildInvoiceFirestoreMap(inv))

            val rootInvDoc = db.collection("invoices").document(invoiceId)
            transaction.set(rootInvDoc, buildInvoiceFirestoreMap(inv))

            // Update settings sequence in transaction atomically
            val settingsUpdateMap = hashMapOf<String, Any>(
              "startingNumber" to startingNum,
              "nextInvoiceNumber" to newNextNum,
              "lastIssuedNumber" to allocatedLong,
              "prefix" to prefix,
              "suffix" to suffix,
              "enabled" to enabled,
              "updatedAt" to System.currentTimeMillis(),
              "updatedBy" to (authorEmail.ifBlank { createdBy })
            )
            transaction.set(settingsDocRef, settingsUpdateMap, SetOptions.merge())

            // Create credit record if dueAmount > 0
            if (calculatedDue > 0.0 && !finalCustomerId.isNullOrBlank()) {
              val creditId = "credit_$invoiceId"
              val cr = CreditRecord(
                id = creditId,
                customerId = finalCustomerId,
                customerName = effectiveCustomerName,
                invoiceId = invoiceId,
                invoiceNumber = invoiceNumber,
                amount = calculatedDue,
                dueDate = effectiveDueDate,
                paidAmount = 0.0,
                remainingAmount = calculatedDue,
                status = "PENDING",
                createdAt = inv.timestamp,
                createdBy = createdBy,
                createdByEmail = authorEmail,
                updatedAt = inv.timestamp
              )
              finalCreditRecord = cr
              val creditDoc = db.collection("businesses").document(businessId)
                .collection("credit_records").document(creditId)
              transaction.set(creditDoc, buildCreditFirestoreMap(cr))
              val rootCreditDoc = db.collection("credit_records").document(creditId)
              transaction.set(rootCreditDoc, buildCreditFirestoreMap(cr))
            }

            // Customer aggregate dues
            if (!finalCustomerId.isNullOrBlank()) {
              val custDoc = db.collection("businesses").document(businessId)
                .collection("customers").document(finalCustomerId)
              val custUpdateMap = mutableMapOf<String, Any>(
                "lastTransactionDate" to inv.timestamp,
                "updatedAt" to System.currentTimeMillis()
              )
              if (updatedCustomerPurchases > 0.0) {
                custUpdateMap["totalPurchases"] = updatedCustomerPurchases
              }
              custUpdateMap["totalDue"] = updatedCustomerDue
              transaction.set(custDoc, custUpdateMap, SetOptions.merge())
            }

            inv
          }.await()
          networkSyncObserver?.setFirebaseSuccess()
        } catch (e: Exception) {
          Log.w(tag, "Firestore transaction failed or offline: ${e.message}")
          networkSyncObserver?.setPendingWrites(true)
        } finally {
          networkSyncObserver?.setSyncInProgress(false)
        }
      }

      // Offline or local fallback if transaction wasn't executed or failed
      if (finalInvoice == null) {
        val existingInvoices = invoiceDao.getAllInvoicesList().map { it.toInvoice() }
        val maxExisting = InvoiceNumberManager.findHighestIssuedInvoiceNumber(existingInvoices) ?: 0L
        val cachedConfig = settingsRepository?.invoiceNumberConfig?.value ?: InvoiceNumberConfig()
        val candidateNum = maxOf(cachedConfig.nextInvoiceNumber, maxExisting + 1, InvoiceNumberManager.DEFAULT_STARTING_NUMBER)
        val invoiceNumber = cachedConfig.formatNumber(candidateNum)
        val newNextNum = candidateNum + 1

        val inv = Invoice(
          id = invoiceId,
          invoiceNumber = invoiceNumber,
          customerId = finalCustomerId,
          customerName = effectiveCustomerName,
          customerPhone = customerPhone.trim(),
          customerAadhaar = customerAadhaar.trim(),
          customerAddress = customerAddress.trim(),
          customerVillage = customerVillage.trim(),
          items = items,
          subTotal = calculatedSubTotal,
          gstRate = validGstRate,
          gstAmount = calculatedGstAmount,
          discount = validDiscount,
          grandTotal = calculatedGrandTotal,
          amountPaid = validAmountPaid,
          remainingDue = calculatedDue,
          dueDate = effectiveDueDate,
          paymentMode = paymentMode,
          timestamp = System.currentTimeMillis(),
          createdBy = createdBy
        )
        finalInvoice = inv

        if (calculatedDue > 0.0 && !finalCustomerId.isNullOrBlank()) {
          finalCreditRecord = CreditRecord(
            id = "credit_$invoiceId",
            customerId = finalCustomerId,
            customerName = effectiveCustomerName,
            invoiceId = invoiceId,
            invoiceNumber = invoiceNumber,
            amount = calculatedDue,
            dueDate = effectiveDueDate,
            paidAmount = 0.0,
            remainingAmount = calculatedDue,
            status = "PENDING",
            createdAt = inv.timestamp,
            createdBy = createdBy,
            createdByEmail = authorEmail,
            updatedAt = inv.timestamp
          )
        }

        // Advance local cache
        settingsRepository?.saveInvoiceNumberConfig(
          cachedConfig.copy(
            nextInvoiceNumber = newNextNum,
            lastIssuedNumber = candidateNum,
            updatedAt = System.currentTimeMillis(),
            updatedBy = createdBy
          ),
          authorEmail
        )
      }

      val invoice = finalInvoice!!
      val creditRecord = finalCreditRecord

      if (!finalCustomerId.isNullOrBlank() && updatedCustomerPurchases > 0.0) {
        val existingCust = customerDao.getCustomerById(finalCustomerId)
        if (existingCust != null) {
          val custModel = existingCust.toCustomer()
          val updatedCust = custModel.copy(
            totalPurchases = updatedCustomerPurchases,
            totalDue = updatedCustomerDue,
            lastTransactionDate = invoice.timestamp
          )
          customerDao.updateCustomer(CustomerEntity.fromCustomer(updatedCust))
        }
      }

      // 2. Insert invoice and credit record locally in Room (Upsert idempotent)
      invoiceDao.insertInvoice(InvoiceEntity.fromInvoice(invoice))
      if (creditRecord != null) {
        creditRecordDao.insertCreditRecord(CreditRecordEntity.fromCreditRecord(creditRecord))
      }

      // 4. Reduce inventory stocks in Room
      items.forEach { item ->
        val productEntity = productDao.getProductById(item.productId)
        if (productEntity != null) {
          val currentProd = productEntity.toProduct()
          val newStock = (currentProd.stockQuantity - item.quantity).coerceAtLeast(0.0)
          val updatedProd = currentProd.copy(stockQuantity = newStock)
          productDao.updateProduct(ProductEntity.fromProduct(updatedProd))
        }
      }

      // 5. Authoritative Audit Logs
      activeAuditRepository?.logInvoiceCreated(
        invoiceId = invoiceId,
        invoiceNumber = invoice.invoiceNumber,
        customerName = effectiveCustomerName,
        amount = calculatedGrandTotal,
        paymentMode = paymentMode.name,
        userEmail = authorEmail.ifBlank { createdBy },
        userRole = authorRole,
        userId = "",
        itemCount = items.size
      )

      if (creditRecord != null) {
        activeAuditRepository?.logCreditRecordCreated(
          creditId = creditRecord.id,
          invoiceNumber = invoice.invoiceNumber,
          customerName = effectiveCustomerName,
          amount = calculatedDue,
          dueDate = effectiveDueDate,
          userEmail = authorEmail.ifBlank { createdBy },
          userRole = authorRole,
          userId = ""
        )
      }

      Result.success(invoice)
    } catch (e: Exception) {
      Log.e(tag, "createInvoice error: ${e.message}", e)
      Result.failure(e)
    }
  }

  /**
   * Records a customer due payment in a controlled atomic transaction.
   * Updates credit record and customer aggregate without allowing negative dues.
   */
  suspend fun recordDuePayment(
    customerId: String,
    creditId: String? = null,
    paymentAmount: Double,
    userEmail: String = "",
    userName: String = "Admin"
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      if (paymentAmount <= 0.0) {
        return@withContext Result.failure(Exception("Payment amount must be positive"))
      }

      val customerEntity = customerDao.getCustomerById(customerId)
        ?: return@withContext Result.failure(Exception("Customer not found"))
      val currentCustomer = customerEntity.toCustomer()
      val previousDue = currentCustomer.totalDue
      val newRemainingDue = (previousDue - paymentAmount).coerceAtLeast(0.0)

      val targetCreditEntity = if (!creditId.isNullOrBlank()) {
        creditRecordDao.getCreditRecordById(creditId)
      } else {
        creditRecordDao.getCreditRecordsByCustomer(customerId).firstOrNull()?.firstOrNull { it.remainingAmount > 0 }
      }

      var updatedCreditRecord: CreditRecord? = null
      if (targetCreditEntity != null) {
        val cr = targetCreditEntity.toCreditRecord()
        val newPaid = (cr.paidAmount + paymentAmount).coerceAtMost(cr.amount)
        val rem = (cr.amount - newPaid).coerceAtLeast(0.0)
        val st = if (rem <= 0.0) "PAID" else "PARTIAL"
        updatedCreditRecord = cr.copy(
          paidAmount = newPaid,
          remainingAmount = rem,
          status = st,
          updatedAt = System.currentTimeMillis()
        )
      }

      // 1. Authoritative Cloud Firestore update FIRST
      try {
        networkSyncObserver?.setSyncInProgress(true)
        val db = firestore
        if (db != null) {
          val businessId = FirestoreProvider.BUSINESS_ID
          val batch = db.batch()

          // Customer doc update
          val custDoc = db.collection("businesses").document(businessId).collection("customers").document(customerId)
          batch.update(
            custDoc,
            "totalDue", newRemainingDue,
            "lastTransactionDate", System.currentTimeMillis(),
            "updatedAt", System.currentTimeMillis()
          )

          // Credit record update
          if (updatedCreditRecord != null) {
            val creditDoc = db.collection("businesses").document(businessId).collection("credit_records").document(updatedCreditRecord.id)
            val cMap = buildCreditFirestoreMap(updatedCreditRecord)
            batch.set(creditDoc, cMap, SetOptions.merge())
          }

          batch.commit().await()
          networkSyncObserver?.setFirebaseSuccess()
        }
      } catch (e: Exception) {
        Log.w(tag, "Firestore due payment notice: ${e.message}")
        networkSyncObserver?.setPendingWrites(true)
      } finally {
        networkSyncObserver?.setSyncInProgress(false)
      }

      // 2. Update local Room caches
      val updatedCustomer = currentCustomer.copy(
        totalDue = newRemainingDue,
        lastTransactionDate = System.currentTimeMillis()
      )
      customerDao.updateCustomer(CustomerEntity.fromCustomer(updatedCustomer))

      if (updatedCreditRecord != null) {
        creditRecordDao.updateCreditRecord(CreditRecordEntity.fromCreditRecord(updatedCreditRecord))
      }

      // 3. Log Audit
      val authorRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logCreditPaymentRecorded(
        creditId = updatedCreditRecord?.id ?: "payment_${System.currentTimeMillis()}",
        customerName = currentCustomer.name,
        paymentAmount = paymentAmount,
        previousDue = previousDue,
        newRemainingDue = newRemainingDue,
        userEmail = userEmail.ifBlank { userName },
        userRole = authorRole,
        userId = ""
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(tag, "recordDuePayment error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteInvoice(
    invoiceId: String,
    deletedBy: String = "Admin",
    deletedByEmail: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val effectiveEmail = AdminAuthUtils.resolveAdminEmail(deletedByEmail, deletedBy)
      if (!AdminAuthUtils.isAdmin(effectiveEmail)) {
        Log.e(tag, "Delete invoice rejected: '$effectiveEmail' is not an authorized administrator.")
        return@withContext Result.failure(
          SecurityException("Unauthorized: Only an authorized Admin can delete invoices. Verified email: $effectiveEmail")
        )
      }

      val existing = invoiceDao.getInvoiceById(invoiceId)?.toInvoice()
      val db = firestore
      val businessId = FirestoreProvider.BUSINESS_ID

      // Idempotency: If already deleted locally and in Firestore, succeed cleanly without duplicate side effects
      if (existing == null) {
        if (db != null) {
          try {
            val cloudDoc = db.collection("businesses").document(businessId).collection("invoices").document(invoiceId).get().await()
            if (!cloudDoc.exists()) {
              recentlyDeletedInvoiceIds.add(invoiceId)
              DeletedRecordsTracker.markDeleted(invoiceId)
              return@withContext Result.success(Unit)
            }
          } catch (e: Exception) {
            // Document does not exist or inaccessible; treat as idempotent success
            recentlyDeletedInvoiceIds.add(invoiceId)
            DeletedRecordsTracker.markDeleted(invoiceId)
            return@withContext Result.success(Unit)
          }
        } else {
          recentlyDeletedInvoiceIds.add(invoiceId)
          DeletedRecordsTracker.markDeleted(invoiceId)
          return@withContext Result.success(Unit)
        }
      }

      val targetInvoice = existing ?: return@withContext Result.failure(Exception("Invoice $invoiceId not found"))

      // 1. AUTHORITATIVE FIRESTORE ATOMIC TRANSACTION / BATCH FIRST
      if (db != null) {
        networkSyncObserver?.setSyncInProgress(true)
        try {
          val batch = db.batch()

          // A. Delete authoritative invoice document
          val invDoc = db.collection("businesses").document(businessId).collection("invoices").document(invoiceId)
          batch.delete(invDoc)

          // B. Delete credit record in business collection if linked
          val creditId = "credit_$invoiceId"
          val creditDoc = db.collection("businesses").document(businessId).collection("credit_records").document(creditId)
          batch.delete(creditDoc)

          val crByInv = creditRecordDao.getCreditRecordByInvoiceId(invoiceId)
          if (crByInv != null && crByInv.id != creditId) {
            val customCrDoc = db.collection("businesses").document(businessId).collection("credit_records").document(crByInv.id)
            batch.delete(customCrDoc)
          }

          // C. Revert / restore product inventory stock in Firestore
          targetInvoice.items.forEach { item ->
            val prodDoc = db.collection("businesses").document(businessId).collection("products").document(item.productId)
            batch.update(prodDoc, "stockQuantity", FieldValue.increment(item.quantity.toDouble()))
          }

          // D. Revert customer dues and total purchases in Firestore
          if (!targetInvoice.customerId.isNullOrBlank()) {
            val custId = targetInvoice.customerId
            val custDoc = db.collection("businesses").document(businessId).collection("customers").document(custId)
            val custEntity = customerDao.getCustomerById(custId)
            if (custEntity != null) {
              val newPurchases = (custEntity.totalPurchases - targetInvoice.grandTotal).coerceAtLeast(0.0)
              val newDue = (custEntity.totalDue - targetInvoice.remainingDue).coerceAtLeast(0.0)
              batch.set(
                custDoc,
                mapOf(
                  "totalPurchases" to newPurchases,
                  "totalDue" to newDue,
                  "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
              )
            }
          }

          // COMMIT ATOMIC BATCH TO FIRESTORE FIRST
          batch.commit().await()
          networkSyncObserver?.setFirebaseSuccess()
          recentlyDeletedInvoiceIds.add(invoiceId)
          DeletedRecordsTracker.markDeleted(invoiceId)

          // Best effort cleanup of legacy root paths
          try {
            db.collection("invoices").document(invoiceId).delete().await()
            db.collection("credit_records").document(creditId).delete().await()
          } catch (_: Exception) {}

        } catch (e: Exception) {
          // FIRESTORE DELETION FAILED!
          // DO NOT TOUCH ROOM! DO NOT REVERSE STOCK IN ROOM! DO NOT UPDATE BALANCES IN ROOM!
          FirestoreProvider.logFirebaseError(tag, "DELETE", "invoices", invoiceId, e)
          Log.e(tag, "Firestore deleteInvoice failed for $invoiceId: ${e.message}", e)
          networkSyncObserver?.setSyncError(true)
          return@withContext Result.failure(
            Exception("Unable to delete invoice. Please check your permissions or connection.")
          )
        } finally {
          networkSyncObserver?.setSyncInProgress(false)
        }
      } else {
        recentlyDeletedInvoiceIds.add(invoiceId)
        DeletedRecordsTracker.markDeleted(invoiceId)
      }

      // 2. FIRESTORE DELETION CONFIRMED! NOW APPLY TO LOCAL ROOM DATABASE

      // Restore product stock in Room
      targetInvoice.items.forEach { item ->
        val productEntity = productDao.getProductById(item.productId)
        if (productEntity != null) {
          val currentProd = productEntity.toProduct()
          val newStock = currentProd.stockQuantity + item.quantity
          productDao.updateProduct(ProductEntity.fromProduct(currentProd.copy(stockQuantity = newStock)))
        }
      }

      // Delete credit record from Room
      val creditId = "credit_$invoiceId"
      creditRecordDao.deleteCreditRecordById(creditId)
      val crByInv = creditRecordDao.getCreditRecordByInvoiceId(invoiceId)
      if (crByInv != null) {
        creditRecordDao.deleteCreditRecordById(crByInv.id)
      }

      // Revert customer dues & purchases in Room
      if (!targetInvoice.customerId.isNullOrBlank()) {
        val custId = targetInvoice.customerId
        val custEntity = customerDao.getCustomerById(custId)
        if (custEntity != null) {
          val cust = custEntity.toCustomer()
          val newPurchases = (cust.totalPurchases - targetInvoice.grandTotal).coerceAtLeast(0.0)
          val newDue = (cust.totalDue - targetInvoice.remainingDue).coerceAtLeast(0.0)
          customerDao.updateCustomer(CustomerEntity.fromCustomer(cust.copy(
            totalPurchases = newPurchases,
            totalDue = newDue,
            lastTransactionDate = System.currentTimeMillis()
          )))
        }
      }

      // Delete invoice from Room
      invoiceDao.deleteInvoiceById(invoiceId)

      // 3. Log authoritative audit
      activeAuditRepository?.logInvoiceDeleted(
        invoiceId = invoiceId,
        invoiceNumber = targetInvoice.invoiceNumber,
        customerName = targetInvoice.customerName,
        amount = targetInvoice.grandTotal,
        userEmail = effectiveEmail,
        userRole = "ADMIN",
        userId = ""
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(tag, "deleteInvoice fatal error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun cancelInvoice(
    invoiceId: String,
    reason: String = "",
    cancelledBy: String = "Admin",
    cancelledByEmail: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val existing = invoiceDao.getInvoiceById(invoiceId)?.toInvoice()
      val userEmail = cancelledByEmail.ifBlank { if (cancelledBy.contains("@")) cancelledBy else "" }
      val role = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"

      activeAuditRepository?.logInvoiceCancelled(
        invoiceId = invoiceId,
        invoiceNumber = existing?.invoiceNumber ?: invoiceId,
        customerName = existing?.customerName ?: "Customer",
        amount = existing?.grandTotal ?: 0.0,
        reason = reason,
        userEmail = userEmail.ifBlank { cancelledBy },
        userRole = role,
        userId = ""
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun syncWithFirestore() = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext
      val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
      if (authUser == null) {
        Log.d(tag, "Skipping invoices sync: User not authenticated")
        return@withContext
      }

      val businessId = FirestoreProvider.BUSINESS_ID
      var snapshot = db.collection("businesses").document(businessId).collection("invoices").get().await()
      if (snapshot == null || snapshot.isEmpty) {
        val rootSnap = db.collection("invoices").get().await()
        if (rootSnap != null && !rootSnap.isEmpty) {
          snapshot = rootSnap
        }
      }

      if (snapshot != null && !snapshot.isEmpty) {
        val remoteInvoices = snapshot.documents.mapNotNull { doc ->
          parseInvoiceFromDoc(doc)
        }
        if (remoteInvoices.isNotEmpty()) {
          invoiceDao.insertInvoices(remoteInvoices.map { InvoiceEntity.fromInvoice(it) })
        }
      }

      // Sync credit records
      val creditSnap = db.collection("businesses").document(businessId).collection("credit_records").get().await()
      if (creditSnap != null && !creditSnap.isEmpty) {
        val remoteCredits = creditSnap.documents.mapNotNull { parseCreditRecordFromDoc(it) }
        if (remoteCredits.isNotEmpty()) {
          creditRecordDao.insertCreditRecords(remoteCredits.map { CreditRecordEntity.fromCreditRecord(it) })
        }
      }
    } catch (e: Exception) {
      Log.w(tag, "Notice on invoices syncWithFirestore: ${e.message}")
    }
  }
}
