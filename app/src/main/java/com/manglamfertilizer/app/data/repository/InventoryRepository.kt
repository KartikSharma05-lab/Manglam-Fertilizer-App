package com.manglamfertilizer.app.data.repository

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.manglamfertilizer.app.data.local.dao.CategoryDao
import com.manglamfertilizer.app.data.local.dao.InventoryHistoryDao
import com.manglamfertilizer.app.data.local.dao.ProductDao
import com.manglamfertilizer.app.data.local.entity.CategoryEntity
import com.manglamfertilizer.app.data.local.entity.InventoryHistoryEntity
import com.manglamfertilizer.app.data.local.entity.ProductEntity
import com.manglamfertilizer.app.data.model.CategoryItem
import com.manglamfertilizer.app.data.model.InventoryHistoryItem
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit
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

enum class DuplicateHandlingPolicy {
  UPDATE_EXISTING,
  SKIP_EXISTING,
  KEEP_BOTH
}

class InventoryRepository(
  private val productDao: ProductDao,
  private val categoryDao: CategoryDao,
  private val inventoryHistoryDao: InventoryHistoryDao,
  private val auditRepository: AuditRepository? = null
) {
  private val tag = "InventoryRepository"
  private var activeAuditRepository: AuditRepository? = auditRepository

  fun attachAuditRepository(repo: AuditRepository) {
    this.activeAuditRepository = repo
  }

  private val firestore: FirebaseFirestore? = com.manglamfertilizer.app.data.util.FirestoreProvider.get()

  private var networkSyncObserver: NetworkSyncObserver? = null

  private var productsListenerRegistration: ListenerRegistration? = null
  private var categoriesListenerRegistration: ListenerRegistration? = null
  private var historyListenerRegistration: ListenerRegistration? = null
  private val recentlyDeletedProductIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())
  private val recentlyDeletedCategoryIds = java.util.Collections.synchronizedSet(mutableSetOf<String>())

  val products: Flow<List<Product>> = productDao.getAllProducts().map { entities ->
    entities.map { it.toProduct() }
  }

  val categories: Flow<List<CategoryItem>> = categoryDao.getAllCategories().map { entities ->
    entities.map { it.toCategoryItem() }
  }

  val history: Flow<List<InventoryHistoryItem>> = inventoryHistoryDao.getAllHistory().map { entities ->
    entities.map { it.toHistoryItem() }
  }

  fun attachNetworkObserver(observer: NetworkSyncObserver, scope: CoroutineScope) {
    this.networkSyncObserver = observer
    startRealtimeSync(scope)
  }

  /**
   * Starts Firestore Real-time Snapshot Listeners for products, categories, and history.
   * Only attaches when an authenticated session exists.
   */
  fun startRealtimeSync(scope: CoroutineScope) {
    val db = firestore ?: return
    val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
    if (authUser == null) {
      Log.d(tag, "Waiting for user authentication before starting Firestore realtime sync")
      return
    }

    val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID

    // 1. Real-time Products Listener
    if (productsListenerRegistration == null) {
      try {
        productsListenerRegistration = db.collection("businesses").document(businessId)
          .collection("products")
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(tag, "Notice on Firestore Products SnapshotListener: ${error.message}")
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
                  val remoteProducts = snapshot.documents.mapNotNull { doc ->
                    parseProductFromDoc(doc)
                  }.filterNot { it.id in recentlyDeletedProductIds || DeletedRecordsTracker.isDeleted(it.id) }

                  // Update / Insert into local Room database
                  if (remoteProducts.isNotEmpty()) {
                    val remoteEntities = remoteProducts.map { ProductEntity.fromProduct(it) }
                    productDao.insertProducts(remoteEntities)

                    // Remove local products that were deleted remotely in Firestore or pending deletion
                    val remoteIds = remoteProducts.map { it.id }.toSet()
                    val localEntities = productDao.getAllProducts().firstOrNull() ?: emptyList()
                    localEntities.filterNot { it.id in remoteIds }.forEach {
                      productDao.deleteProductById(it.id)
                    }
                    localEntities.filter { it.id in recentlyDeletedProductIds || DeletedRecordsTracker.isDeleted(it.id) }.forEach {
                      productDao.deleteProductById(it.id)
                    }
                  } else if (snapshot.documents.isEmpty()) {
                    // Check if root /products has legacy items to migrate before clearing
                    val rootSnap = db.collection("products").get().await()
                    if (rootSnap != null && !rootSnap.isEmpty) {
                      val legacyProducts = rootSnap.documents.mapNotNull { parseProductFromDoc(it) }
                        .filterNot { it.id in recentlyDeletedProductIds || DeletedRecordsTracker.isDeleted(it.id) }
                      if (legacyProducts.isNotEmpty()) {
                        productDao.insertProducts(legacyProducts.map { ProductEntity.fromProduct(it) })
                        // Migrate to business path
                        val batch = db.batch()
                        legacyProducts.forEach { p ->
                          val targetDoc = db.collection("businesses").document(businessId).collection("products").document(p.id)
                          batch.set(targetDoc, buildProductFirestoreMap(p))
                        }
                        batch.commit().await()
                      }
                    } else {
                      productDao.clearAll()
                    }
                  }
                } catch (e: Exception) {
                  Log.w(tag, "Error syncing products snapshot to Room: ${e.message}")
                }
              }
            }
          }
      } catch (e: Exception) {
        Log.w(tag, "Failed to attach products listener: ${e.message}")
      }
    }

    // 2. Real-time Categories Listener
    if (categoriesListenerRegistration == null) {
      try {
        categoriesListenerRegistration = db.collection("businesses").document(businessId)
          .collection("categories")
          .addSnapshotListener { snapshot, error ->
            if (error != null) {
              Log.w(tag, "Notice on Firestore Categories SnapshotListener: ${error.message}")
              return@addSnapshotListener
            }

            if (snapshot != null) {
              scope.launch(Dispatchers.IO) {
                try {
                  val remoteCategories = snapshot.documents.mapNotNull { doc ->
                    try {
                      val id = doc.id
                      val name = doc.getString("name") ?: ""
                      val order = doc.getLong("order")?.toInt()
                        ?: doc.getLong("displayOrder")?.toInt() ?: 0
                      val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                      if (name.isNotBlank()) {
                        CategoryItem(id = id, name = name, order = order, createdAt = createdAt)
                      } else null
                    } catch (e: Exception) {
                      null
                    }
                  }.filterNot { it.id in recentlyDeletedCategoryIds || DeletedRecordsTracker.isDeleted(it.id) }

                  if (remoteCategories.isNotEmpty()) {
                    categoryDao.insertCategories(remoteCategories.map { CategoryEntity.fromCategoryItem(it) })
                    val remoteCatIds = remoteCategories.map { it.id }.toSet()
                    val localCats = categoryDao.getAllCategories().firstOrNull() ?: emptyList()
                    localCats.filterNot { it.id in remoteCatIds }.forEach {
                      categoryDao.deleteCategoryById(it.id)
                    }
                    localCats.filter { it.id in recentlyDeletedCategoryIds || DeletedRecordsTracker.isDeleted(it.id) }.forEach {
                      categoryDao.deleteCategoryById(it.id)
                    }
                  } else if (snapshot.documents.isEmpty()) {
                    val rootSnap = db.collection("categories").get().await()
                    if (rootSnap != null && !rootSnap.isEmpty) {
                      val legacyCats = rootSnap.documents.mapNotNull { doc ->
                        val id = doc.id
                        val name = doc.getString("name") ?: ""
                        val order = doc.getLong("order")?.toInt() ?: 0
                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        if (name.isNotBlank()) CategoryItem(id, name, order, createdAt) else null
                      }.filterNot { it.id in recentlyDeletedCategoryIds || DeletedRecordsTracker.isDeleted(it.id) }
                      if (legacyCats.isNotEmpty()) {
                        categoryDao.insertCategories(legacyCats.map { CategoryEntity.fromCategoryItem(it) })
                        val batch = db.batch()
                        legacyCats.forEach { c ->
                          val targetDoc = db.collection("businesses").document(businessId).collection("categories").document(c.id)
                          batch.set(targetDoc, c)
                        }
                        batch.commit().await()
                      }
                    }
                  }
                } catch (e: Exception) {
                  Log.e(tag, "Error syncing categories to Room: ${e.message}", e)
                }
              }
            }
          }
      } catch (e: Exception) {
        Log.e(tag, "Failed to attach categories listener: ${e.message}", e)
      }
    }

    // 3. Real-time Inventory History Listener (Limited to recent 150 items)
    if (historyListenerRegistration == null) {
      try {
        historyListenerRegistration = db.collection("businesses").document(businessId)
          .collection("inventory_history")
          .limit(150)
          .addSnapshotListener { snapshot, error ->
            if (error != null) return@addSnapshotListener
            if (snapshot != null) {
              scope.launch(Dispatchers.IO) {
                try {
                  val remoteHistories = snapshot.documents.mapNotNull { doc ->
                    try {
                      InventoryHistoryItem(
                        id = doc.id,
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        userName = doc.getString("userName") ?: "Admin",
                        userEmail = doc.getString("userEmail") ?: "",
                        userId = doc.getString("userId") ?: "",
                        userRole = doc.getString("userRole") ?: "STAFF",
                        productId = doc.getString("productId") ?: "",
                        actionType = doc.getString("actionType") ?: "Operation",
                        productName = doc.getString("productName") ?: "",
                        details = doc.getString("details") ?: "",
                        previousValue = doc.getString("previousValue"),
                        newValue = doc.getString("newValue")
                      )
                    } catch (e: Exception) {
                      null
                    }
                  }
                  if (remoteHistories.isNotEmpty()) {
                    inventoryHistoryDao.insertHistories(remoteHistories.map { InventoryHistoryEntity.fromHistoryItem(it) })
                  }
                } catch (e: Exception) {}
              }
            }
          }
      } catch (e: Exception) {}
    }
  }

  fun stopRealtimeSync() {
    productsListenerRegistration?.remove()
    productsListenerRegistration = null
    categoriesListenerRegistration?.remove()
    categoriesListenerRegistration = null
    historyListenerRegistration?.remove()
    historyListenerRegistration = null
  }

  private fun parseProductFromDoc(doc: DocumentSnapshot): Product? {
    return try {
      val id = doc.getString("productId") ?: doc.getString("id") ?: doc.id
      val name = doc.getString("productName") ?: doc.getString("name") ?: ""
      if (name.isBlank()) return null

      val catStr = doc.getString("category") ?: ""
      val company = doc.getString("companyBrand") ?: doc.getString("company") ?: ""
      val unitStr = doc.getString("unit") ?: "BAG"
      val unit = try { ProductUnit.valueOf(unitStr) } catch (e: Exception) { ProductUnit.BAG }
      val batchNumber = doc.getString("batchNumber") ?: ""
      val purchasePrice = doc.getDouble("buyPrice") ?: doc.getDouble("purchasePrice") ?: 0.0
      val sellingPrice = doc.getDouble("price") ?: doc.getDouble("sellingPrice") ?: 0.0
      val mrp = doc.getDouble("mrp") ?: 0.0
      val stockQuantity = doc.getDouble("quantity") ?: doc.getDouble("stockQuantity") ?: 0.0
      val minStockAlert = doc.getDouble("minimumStock") ?: doc.getDouble("minStockAlert") ?: 10.0
      val expiryDate = doc.getLong("expiryDate")
      val rackLocation = doc.getString("rackLocation") ?: ""
      val hsnCode = doc.getString("hsnCode") ?: ""
      val chemicalComposition = doc.getString("chemicalComposition") ?: ""
      val barcode = doc.getString("barcode") ?: ""
      val packaging = doc.getString("packaging") ?: ""
      val crop = doc.getString("crop") ?: ""
      val usesInstructions = doc.getString("usesInstructions") ?: ""
      val customFields = doc.getString("customFields") ?: ""
      val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
      val updatedAt = doc.getLong("updatedAt") ?: createdAt
      val createdBy = doc.getString("createdBy") ?: ""
      val updatedBy = doc.getString("updatedBy") ?: ""

      Product(
        id = id,
        name = name,
        category = catStr,
        company = company,
        unit = unit,
        batchNumber = batchNumber,
        purchasePrice = purchasePrice,
        sellingPrice = sellingPrice,
        mrp = mrp,
        stockQuantity = stockQuantity,
        minStockAlert = minStockAlert,
        expiryDate = expiryDate,
        rackLocation = rackLocation,
        hsnCode = hsnCode,
        chemicalComposition = chemicalComposition,
        barcode = barcode,
        packaging = packaging,
        crop = crop,
        usesInstructions = usesInstructions,
        customFields = customFields,
        createdAt = createdAt,
        updatedAt = updatedAt,
        createdBy = createdBy,
        updatedBy = updatedBy
      )
    } catch (e: Exception) {
      Log.e(tag, "Failed to parse product doc ${doc.id}: ${e.message}", e)
      null
    }
  }

  private fun buildProductFirestoreMap(product: Product): Map<String, Any?> {
    return mapOf(
      "id" to product.id,
      "productId" to product.id,
      "name" to product.name,
      "productName" to product.name,
      "chemicalComposition" to product.chemicalComposition,
      "company" to product.company,
      "companyBrand" to product.company,
      "category" to product.category,
      "packaging" to product.packaging,
      "unit" to product.unit.name,
      "batchNumber" to product.batchNumber,
      "purchasePrice" to product.purchasePrice,
      "buyPrice" to product.purchasePrice,
      "sellingPrice" to product.sellingPrice,
      "price" to product.sellingPrice,
      "mrp" to product.mrp,
      "stockQuantity" to product.stockQuantity,
      "quantity" to product.stockQuantity,
      "minStockAlert" to product.minStockAlert,
      "minimumStock" to product.minStockAlert,
      "expiryDate" to product.expiryDate,
      "rackLocation" to product.rackLocation,
      "hsnCode" to product.hsnCode,
      "barcode" to product.barcode,
      "crop" to product.crop,
      "usesInstructions" to product.usesInstructions,
      "customFields" to product.customFields,
      "createdAt" to product.createdAt,
      "updatedAt" to product.updatedAt,
      "createdBy" to product.createdBy,
      "updatedBy" to product.updatedBy
    )
  }

  suspend fun addProduct(
    name: String,
    category: String,
    company: String,
    unit: ProductUnit,
    batchNumber: String,
    purchasePrice: Double,
    sellingPrice: Double,
    mrp: Double,
    stockQuantity: Double,
    minStockAlert: Double,
    expiryDate: Long?,
    rackLocation: String = "",
    hsnCode: String = "",
    chemicalComposition: String = "",
    barcode: String = "",
    packaging: String = "",
    crop: String = "",
    usesInstructions: String = "",
    customFields: String = "",
    userName: String = "Admin",
    userEmail: String = ""
  ): Result<Product> = withContext(Dispatchers.IO) {
    try {
      val trimmedName = name.trim()
      if (trimmedName.isBlank()) {
        return@withContext Result.failure(IllegalArgumentException("Product Name cannot be empty"))
      }
      if (sellingPrice < 0) {
        return@withContext Result.failure(IllegalArgumentException("Price must be a valid non-negative number"))
      }
      if (stockQuantity < 0) {
        return@withContext Result.failure(IllegalArgumentException("Quantity must be a valid non-negative number"))
      }

      val productId = "prod_${UUID.randomUUID().toString().replace("-", "").take(10)}"
      val now = System.currentTimeMillis()
      val author = userEmail.ifBlank { userName }

      val product = Product(
        id = productId,
        name = trimmedName,
        category = category.trim(),
        company = company.trim(),
        unit = unit,
        batchNumber = batchNumber.trim(),
        purchasePrice = purchasePrice,
        sellingPrice = sellingPrice,
        mrp = mrp,
        stockQuantity = stockQuantity,
        minStockAlert = minStockAlert,
        expiryDate = expiryDate,
        rackLocation = rackLocation.trim(),
        hsnCode = hsnCode.trim(),
        chemicalComposition = chemicalComposition.trim(),
        barcode = barcode.trim(),
        packaging = packaging.trim(),
        crop = crop.trim(),
        usesInstructions = usesInstructions.trim(),
        customFields = customFields.trim(),
        createdAt = now,
        updatedAt = now,
        createdBy = author,
        updatedBy = author
      )

      // 1. Cache in Room Database immediately so user data is never lost
      productDao.insertProduct(ProductEntity.fromProduct(product))

      // 2. Authoritative Firestore Write
      try {
        networkSyncObserver?.setSyncInProgress(true)
        val db = firestore
        if (db != null) {
          val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
          val firestoreMap = buildProductFirestoreMap(product)
          db.collection("businesses").document(businessId).collection("products").document(productId).set(firestoreMap).await()
          // Mirror write to root products for backward compatibility
          try {
            db.collection("products").document(productId).set(firestoreMap)
          } catch (e: Exception) {}
          networkSyncObserver?.setFirebaseSuccess()
        }
      } catch (e: Exception) {
        Log.w(tag, "Firestore product save queued or failed: ${e.message}")
        networkSyncObserver?.setPendingWrites(true)
      } finally {
        networkSyncObserver?.setSyncInProgress(false)
      }

      // 3. Log History
      logHistory(
        actionType = "Product Added",
        productName = product.name,
        details = "Added product ${product.name} (Stock: $stockQuantity ${unit.name}, Price: ₹$sellingPrice, Category: ${if (category.isBlank()) "Uncategorized" else category})",
        userName = userName,
        userEmail = userEmail,
        newValue = "$stockQuantity ${unit.name} @ ₹$sellingPrice"
      )

      // 4. Log Firebase Activity Audit (append-only)
      val userRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logProductCreated(
        product = product,
        userEmail = userEmail,
        userRole = userRole,
        userId = ""
      )

      Result.success(product)
    } catch (e: Exception) {
      Log.e(tag, "addProduct error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun updateProduct(
    product: Product,
    previousProduct: Product? = null,
    userName: String = "Admin",
    userEmail: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val now = System.currentTimeMillis()
      val author = userEmail.ifBlank { userName }
      val updated = product.copy(
        updatedAt = now,
        updatedBy = author
      )

      // 1. Cache in Room Database immediately
      productDao.updateProduct(ProductEntity.fromProduct(updated))

      // 2. Authoritative Firestore Write
      try {
        networkSyncObserver?.setSyncInProgress(true)
        val db = firestore
        if (db != null) {
          val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
          val firestoreMap = buildProductFirestoreMap(updated)
          db.collection("businesses").document(businessId).collection("products").document(updated.id).set(firestoreMap).await()
          try {
            db.collection("products").document(updated.id).set(firestoreMap)
          } catch (e: Exception) {}
          networkSyncObserver?.setFirebaseSuccess()
        }
      } catch (e: Exception) {
        Log.w(tag, "Firestore product update queued or failed: ${e.message}")
        networkSyncObserver?.setPendingWrites(true)
      } finally {
        networkSyncObserver?.setSyncInProgress(false)
      }

      // 3. Determine changes for history
      val action = when {
        previousProduct != null && previousProduct.stockQuantity != updated.stockQuantity -> {
          if (updated.stockQuantity > previousProduct.stockQuantity) "Stock Increased" else "Stock Decreased"
        }
        previousProduct != null && (previousProduct.sellingPrice != updated.sellingPrice || previousProduct.purchasePrice != updated.purchasePrice) -> {
          "Price Changed"
        }
        previousProduct != null && previousProduct.category != updated.category -> {
          "Category Changed"
        }
        else -> "Product Edited"
      }

      val prevVal = previousProduct?.let { "Stock: ${it.stockQuantity} ${it.unit.name}, ₹${it.sellingPrice}, Cat: ${it.category}" }
      val newVal = "Stock: ${updated.stockQuantity} ${updated.unit.name}, ₹${updated.sellingPrice}, Cat: ${updated.category}"

      logHistory(
        actionType = action,
        productName = updated.name,
        details = "Updated ${updated.name} ($action)",
        userName = userName,
        userEmail = userEmail,
        previousValue = prevVal,
        newValue = newVal
      )

      val userRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logProductUpdated(
        productId = updated.id,
        productName = updated.name,
        changesSummary = action,
        changedFields = mapOf(
          "stockQuantity" to updated.stockQuantity,
          "sellingPrice" to updated.sellingPrice,
          "category" to updated.category,
          "unit" to updated.unit.name
        ),
        userEmail = userEmail,
        userRole = userRole,
        userId = "",
        previousSnapshot = previousProduct?.let {
          mapOf(
            "stockQuantity" to it.stockQuantity,
            "sellingPrice" to it.sellingPrice,
            "category" to it.category,
            "unit" to it.unit.name
          )
        }
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(tag, "updateProduct error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun deleteProduct(
    productId: String,
    productName: String = "",
    userName: String = "Admin",
    userEmail: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val effectiveEmail = AdminAuthUtils.resolveAdminEmail(userEmail, userName)
      if (!AdminAuthUtils.isAdmin(effectiveEmail)) {
        Log.e(tag, "Delete product rejected: '$effectiveEmail' is not an authorized administrator.")
        return@withContext Result.failure(
          SecurityException("Unauthorized: Only an authorized Admin can delete inventory products. Verified email: $effectiveEmail")
        )
      }

      val existingProd = productDao.getProductById(productId)?.toProduct()
      val db = firestore
      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID

      // Idempotency: If already deleted locally and in Firestore, succeed cleanly
      if (existingProd == null) {
        if (db != null) {
          try {
            val cloudDoc = db.collection("businesses").document(businessId).collection("products").document(productId).get().await()
            if (!cloudDoc.exists()) {
              recentlyDeletedProductIds.add(productId)
              DeletedRecordsTracker.markDeleted(productId)
              return@withContext Result.success(Unit)
            }
          } catch (e: Exception) {
            recentlyDeletedProductIds.add(productId)
            DeletedRecordsTracker.markDeleted(productId)
            return@withContext Result.success(Unit)
          }
        } else {
          recentlyDeletedProductIds.add(productId)
          DeletedRecordsTracker.markDeleted(productId)
          return@withContext Result.success(Unit)
        }
      }

      // 1. Authoritative Firestore Delete FIRST
      if (db != null) {
        networkSyncObserver?.setSyncInProgress(true)
        try {
          db.collection("businesses").document(businessId).collection("products").document(productId).delete().await()
          try { db.collection("products").document(productId).delete().await() } catch (_: Exception) {}

          networkSyncObserver?.setFirebaseSuccess()
          recentlyDeletedProductIds.add(productId)
          DeletedRecordsTracker.markDeleted(productId)
        } catch (e: Exception) {
          // FIRESTORE DELETION FAILED! DO NOT TOUCH ROOM!
          FirestoreProvider.logFirebaseError(tag, "DELETE", "products", productId, e)
          Log.e(tag, "Firestore deleteProduct failed for $productId: ${e.message}", e)
          networkSyncObserver?.setSyncError(true)
          return@withContext Result.failure(
            Exception("Unable to delete product. Please check your permissions or connection.")
          )
        } finally {
          networkSyncObserver?.setSyncInProgress(false)
        }
      } else {
        recentlyDeletedProductIds.add(productId)
        DeletedRecordsTracker.markDeleted(productId)
      }

      // 2. FIRESTORE CONFIRMED! NOW REMOVE FROM ROOM DATABASE
      productDao.deleteProductById(productId)

      val targetName = productName.ifBlank { existingProd?.name ?: "Product $productId" }

      // 3. Log History
      logHistory(
        actionType = "Product Deleted",
        productName = targetName,
        details = "Deleted product $targetName (ID: $productId)",
        userName = userName,
        userEmail = effectiveEmail
      )

      // 4. Log Firebase Activity Audit (append-only)
      activeAuditRepository?.logProductDeleted(
        productId = productId,
        productName = targetName,
        userEmail = effectiveEmail,
        userRole = "ADMIN",
        userId = "",
        deletedSnapshot = existingProd
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(tag, "deleteProduct fatal error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun importProductsBatch(
    productsToImport: List<Product>,
    policy: DuplicateHandlingPolicy,
    fileType: String,
    totalRows: Int,
    userName: String = "Admin",
    userEmail: String = ""
  ): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
    try {
      val existingEntities = productDao.getAllProducts().firstOrNull() ?: emptyList()
      val existingProducts = existingEntities.map { it.toProduct() }

      val existingCats = categoryDao.getAllCategories().firstOrNull() ?: emptyList()
      val existingCatNames = existingCats.map { it.name.trim().lowercase() }.toMutableSet()
      val newCategoriesToInsert = mutableListOf<CategoryEntity>()
      var currentCatOrder = existingCats.size

      var importedCount = 0
      var skippedCount = 0
      val productsToUpsert = mutableListOf<Product>()
      val now = System.currentTimeMillis()
      val author = userEmail.ifBlank { userName }

      for (incoming in productsToImport) {
        // Auto-register category if present and not in categories list
        val catName = incoming.category.trim()
        if (catName.isNotBlank() && !existingCatNames.contains(catName.lowercase())) {
          existingCatNames.add(catName.lowercase())
          val newCatId = "cat_${UUID.randomUUID().toString().take(8)}"
          val newCatEntity = CategoryEntity(
            id = newCatId,
            name = catName,
            displayOrder = currentCatOrder++,
            createdAt = now
          )
          newCategoriesToInsert.add(newCatEntity)
          try {
            firestore?.collection("categories")?.document(newCatId)?.set(newCatEntity.toCategoryItem())
          } catch (e: Exception) {}
        }

        // Match existing product by Barcode OR (Name + Company)
        val existingMatch = existingProducts.firstOrNull { existing ->
          (incoming.barcode.isNotBlank() && incoming.barcode == existing.barcode) ||
              (incoming.name.equals(existing.name, ignoreCase = true) && incoming.company.equals(existing.company, ignoreCase = true))
        }

        if (existingMatch != null) {
          when (policy) {
            DuplicateHandlingPolicy.UPDATE_EXISTING -> {
              val updated = existingMatch.copy(
                name = incoming.name,
                category = if (incoming.category.isNotBlank()) incoming.category else existingMatch.category,
                company = incoming.company,
                unit = incoming.unit,
                batchNumber = if (incoming.batchNumber.isNotBlank()) incoming.batchNumber else existingMatch.batchNumber,
                purchasePrice = if (incoming.purchasePrice > 0) incoming.purchasePrice else existingMatch.purchasePrice,
                sellingPrice = if (incoming.sellingPrice > 0) incoming.sellingPrice else existingMatch.sellingPrice,
                mrp = if (incoming.mrp > 0) incoming.mrp else existingMatch.mrp,
                stockQuantity = incoming.stockQuantity,
                minStockAlert = if (incoming.minStockAlert > 0) incoming.minStockAlert else existingMatch.minStockAlert,
                expiryDate = incoming.expiryDate ?: existingMatch.expiryDate,
                rackLocation = if (incoming.rackLocation.isNotBlank()) incoming.rackLocation else existingMatch.rackLocation,
                hsnCode = if (incoming.hsnCode.isNotBlank()) incoming.hsnCode else existingMatch.hsnCode,
                chemicalComposition = if (incoming.chemicalComposition.isNotBlank()) incoming.chemicalComposition else existingMatch.chemicalComposition,
                barcode = if (incoming.barcode.isNotBlank()) incoming.barcode else existingMatch.barcode,
                packaging = if (incoming.packaging.isNotBlank()) incoming.packaging else existingMatch.packaging,
                crop = if (incoming.crop.isNotBlank()) incoming.crop else existingMatch.crop,
                usesInstructions = if (incoming.usesInstructions.isNotBlank()) incoming.usesInstructions else existingMatch.usesInstructions,
                updatedAt = now,
                updatedBy = author
              )
              productsToUpsert.add(updated)
              importedCount++
            }
            DuplicateHandlingPolicy.SKIP_EXISTING -> {
              skippedCount++
            }
            DuplicateHandlingPolicy.KEEP_BOTH -> {
              val newId = "prod_${UUID.randomUUID().toString().replace("-", "").take(10)}"
              val newProd = incoming.copy(id = newId, createdAt = now, updatedAt = now, createdBy = author, updatedBy = author)
              productsToUpsert.add(newProd)
              importedCount++
            }
          }
        } else {
          val newId = if (incoming.id.isNotBlank()) incoming.id else "prod_${UUID.randomUUID().toString().replace("-", "").take(10)}"
          val newProd = incoming.copy(id = newId, createdAt = now, updatedAt = now, createdBy = author, updatedBy = author)
          productsToUpsert.add(newProd)
          importedCount++
        }
      }

      if (newCategoriesToInsert.isNotEmpty()) {
        categoryDao.insertCategories(newCategoriesToInsert)
      }

      if (productsToUpsert.isNotEmpty()) {
        // Save to Firestore using write batches
        val db = firestore
        if (db != null) {
          try {
            val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
            val chunks = productsToUpsert.chunked(200)
            for (chunk in chunks) {
              val batch = db.batch()
              for (p in chunk) {
                val firestoreMap = buildProductFirestoreMap(p)
                val businessDocRef = db.collection("businesses").document(businessId).collection("products").document(p.id)
                batch.set(businessDocRef, firestoreMap)
                val rootDocRef = db.collection("products").document(p.id)
                batch.set(rootDocRef, firestoreMap)
              }
              batch.commit().await()
            }
            networkSyncObserver?.setFirebaseSuccess()
          } catch (e: Exception) {
            Log.e(tag, "Batch write to Firestore partial failure: ${e.message}", e)
          }
        }

        productDao.insertProducts(productsToUpsert.map { ProductEntity.fromProduct(it) })
      }

      // Record Import History Entry
      logHistory(
        actionType = "Import",
        productName = "Bulk Import ($fileType)",
        details = "Imported $importedCount products, skipped $skippedCount out of $totalRows rows (File: $fileType, Policy: ${policy.name})",
        userName = userName,
        userEmail = userEmail,
        newValue = "$importedCount products imported"
      )

      val userRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logProductImported(
        totalImported = importedCount,
        skippedCount = skippedCount,
        fileName = fileType,
        userEmail = userEmail,
        userRole = userRole,
        userId = ""
      )

      Result.success(Pair(importedCount, skippedCount))
    } catch (e: Exception) {
      Log.e(tag, "importProductsBatch error: ${e.message}", e)
      Result.failure(e)
    }
  }

  // Categories management with cloud persistence
  suspend fun addCategory(
    name: String,
    userName: String = "Admin",
    userEmail: String = ""
  ): Result<CategoryItem> = withContext(Dispatchers.IO) {
    try {
      val trimmed = name.trim()
      if (trimmed.isBlank()) {
        return@withContext Result.failure(IllegalArgumentException("Category name cannot be empty"))
      }
      val categoryId = "cat_${UUID.randomUUID().toString().take(8)}"
      val currentList = categoryDao.getAllCategories().firstOrNull() ?: emptyList()
      val order = currentList.size
      val category = CategoryItem(
        id = categoryId,
        name = trimmed,
        order = order,
        createdAt = System.currentTimeMillis()
      )

      val db = firestore
      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
      if (db != null) {
        try {
          db.collection("businesses").document(businessId).collection("categories").document(categoryId).set(category).await()
          try {
            db.collection("categories").document(categoryId).set(category)
          } catch (e: Exception) {}
        } catch (e: Exception) {
          Log.e(tag, "Firestore addCategory error: ${e.message}", e)
        }
      }

      categoryDao.insertCategory(CategoryEntity.fromCategoryItem(category))

      logHistory(
        actionType = "Category Added",
        productName = trimmed,
        details = "Created category '$trimmed'",
        userName = userName,
        userEmail = userEmail
      )

      val userRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logCategoryCreated(
        categoryId = categoryId,
        categoryName = trimmed,
        userEmail = userEmail,
        userRole = userRole,
        userId = ""
      )

      Result.success(category)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun updateCategory(
    category: CategoryItem,
    oldName: String = "",
    userName: String = "Admin",
    userEmail: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val db = firestore
      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
      if (db != null) {
        try {
          db.collection("businesses").document(businessId).collection("categories").document(category.id).set(category).await()
          try {
            db.collection("categories").document(category.id).set(category)
          } catch (e: Exception) {}
        } catch (e: Exception) {}
      }

      categoryDao.updateCategory(CategoryEntity.fromCategoryItem(category))

      // If category was renamed, update existing products with old category name
      if (oldName.isNotBlank() && oldName != category.name) {
        val currentProducts = productDao.getAllProducts().firstOrNull() ?: emptyList()
        currentProducts.forEach { entity ->
          if (entity.category.equals(oldName, ignoreCase = true)) {
            val updated = entity.copy(category = category.name)
            productDao.updateProduct(updated)
            try {
              val prodModel = updated.toProduct()
              val firestoreMap = buildProductFirestoreMap(prodModel)
              db?.collection("businesses")?.document(businessId)?.collection("products")?.document(updated.id)?.set(firestoreMap)
              db?.collection("products")?.document(updated.id)?.set(firestoreMap)
            } catch (e: Exception) {}
          }
        }
      }

      logHistory(
        actionType = "Category Renamed",
        productName = category.name,
        details = "Renamed category '$oldName' to '${category.name}'",
        userName = userName,
        userEmail = userEmail,
        previousValue = oldName,
        newValue = category.name
      )

      val userRole = if (AdminAuthUtils.isAdmin(userEmail)) "ADMIN" else "STAFF"
      activeAuditRepository?.logCategoryUpdated(
        categoryId = category.id,
        oldName = oldName,
        newName = category.name,
        userEmail = userEmail,
        userRole = userRole,
        userId = ""
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun deleteCategory(
    categoryId: String,
    categoryName: String,
    userName: String = "Admin",
    userEmail: String = ""
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val effectiveEmail = AdminAuthUtils.resolveAdminEmail(userEmail, userName)
      if (!AdminAuthUtils.isAdmin(effectiveEmail)) {
        Log.e(tag, "Delete category rejected: '$effectiveEmail' is not an authorized administrator.")
        return@withContext Result.failure(
          SecurityException("Unauthorized: Only an authorized Admin can delete categories. Verified email: $effectiveEmail")
        )
      }

      val db = firestore
      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID

      // 1. Authoritative Firestore Delete FIRST
      if (db != null) {
        networkSyncObserver?.setSyncInProgress(true)
        try {
          db.collection("businesses").document(businessId).collection("categories").document(categoryId).delete().await()
          try { db.collection("categories").document(categoryId).delete().await() } catch (_: Exception) {}

          networkSyncObserver?.setFirebaseSuccess()
          recentlyDeletedCategoryIds.add(categoryId)
          DeletedRecordsTracker.markDeleted(categoryId)
        } catch (e: Exception) {
          FirestoreProvider.logFirebaseError(tag, "DELETE", "categories", categoryId, e)
          Log.e(tag, "Firestore deleteCategory failed for $categoryId: ${e.message}", e)
          networkSyncObserver?.setSyncError(true)
          return@withContext Result.failure(
            Exception("Unable to delete category. Please check your permissions or connection.")
          )
        } finally {
          networkSyncObserver?.setSyncInProgress(false)
        }
      } else {
        recentlyDeletedCategoryIds.add(categoryId)
        DeletedRecordsTracker.markDeleted(categoryId)
      }

      // 2. FIRESTORE CONFIRMED! NOW REMOVE FROM ROOM
      categoryDao.deleteCategoryById(categoryId)

      // Reassign products of this category to "" (Uncategorized) so no products are silently lost
      val currentProducts = productDao.getAllProducts().firstOrNull() ?: emptyList()
      currentProducts.forEach { entity ->
        if (entity.category.equals(categoryName, ignoreCase = true)) {
          val updated = entity.copy(category = "")
          productDao.updateProduct(updated)
          try {
            val prodModel = updated.toProduct()
            val firestoreMap = buildProductFirestoreMap(prodModel)
            db?.collection("businesses")?.document(businessId)?.collection("products")?.document(updated.id)?.set(firestoreMap)
            db?.collection("products")?.document(updated.id)?.set(firestoreMap)
          } catch (e: Exception) {}
        }
      }

      logHistory(
        actionType = "Category Deleted",
        productName = categoryName,
        details = "Deleted category '$categoryName' (associated products moved to Uncategorized)",
        userName = userName,
        userEmail = effectiveEmail
      )

      activeAuditRepository?.logCategoryDeleted(
        categoryId = categoryId,
        categoryName = categoryName,
        userEmail = effectiveEmail,
        userRole = "ADMIN",
        userId = ""
      )

      Result.success(Unit)
    } catch (e: Exception) {
      Log.e(tag, "deleteCategory fatal error: ${e.message}", e)
      Result.failure(e)
    }
  }

  suspend fun reorderCategories(
    items: List<CategoryItem>
  ): Result<Unit> = withContext(Dispatchers.IO) {
    try {
      val entities = items.mapIndexed { idx, it ->
        CategoryEntity.fromCategoryItem(it.copy(order = idx))
      }
      categoryDao.insertCategories(entities)

      val db = firestore
      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
      if (db != null) {
        try {
          val batch = db.batch()
          entities.forEach {
            val catMap = mapOf(
              "id" to it.id,
              "name" to it.name,
              "order" to it.displayOrder,
              "displayOrder" to it.displayOrder,
              "createdAt" to it.createdAt
            )
            val docRef = db.collection("businesses").document(businessId).collection("categories").document(it.id)
            batch.set(docRef, catMap)
            val rootDocRef = db.collection("categories").document(it.id)
            batch.set(rootDocRef, catMap)
          }
          batch.commit().await()
        } catch (e: Exception) {}
      }

      Result.success(Unit)
    } catch (e: Exception) {
      Result.failure(e)
    }
  }

  suspend fun logHistory(
    actionType: String,
    productName: String,
    details: String,
    userName: String,
    userEmail: String,
    previousValue: String? = null,
    newValue: String? = null
  ) = withContext(Dispatchers.IO) {
    try {
      val item = InventoryHistoryItem(
        id = "hist_${UUID.randomUUID().toString().take(8)}",
        timestamp = System.currentTimeMillis(),
        userName = userName.ifBlank { "Admin" },
        userEmail = userEmail,
        actionType = actionType,
        productName = productName,
        details = details,
        previousValue = previousValue,
        newValue = newValue
      )
      inventoryHistoryDao.insertHistory(InventoryHistoryEntity.fromHistoryItem(item))
      try {
        val historyMap = mapOf(
          "id" to item.id,
          "timestamp" to item.timestamp,
          "userName" to item.userName,
          "userEmail" to item.userEmail,
          "userId" to item.userId,
          "userRole" to item.userRole,
          "productId" to item.productId,
          "actionType" to item.actionType,
          "productName" to item.productName,
          "details" to item.details,
          "previousValue" to item.previousValue,
          "newValue" to item.newValue
        )
        val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID
        firestore?.collection("businesses")?.document(businessId)?.collection("inventory_history")?.document(item.id)?.set(historyMap)?.await()
        try {
          firestore?.collection("inventory_history")?.document(item.id)?.set(historyMap)
        } catch (e: Exception) {}
      } catch (e: Exception) {}
    } catch (e: Exception) {}
  }

  suspend fun syncWithFirestore() = withContext(Dispatchers.IO) {
    try {
      val db = firestore ?: return@withContext
      val authUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
      if (authUser == null) {
        Log.d(tag, "Skipping Firestore sync: User not authenticated")
        return@withContext
      }

      val businessId = com.manglamfertilizer.app.data.util.FirestoreProvider.BUSINESS_ID

      // 1. Fetch products snapshot from business path first
      var snapshot = db.collection("businesses").document(businessId).collection("products").get().await()
      if (snapshot == null || snapshot.isEmpty) {
        val rootSnapshot = db.collection("products").get().await()
        if (rootSnapshot != null && !rootSnapshot.isEmpty) {
          snapshot = rootSnapshot
        }
      }

      if (snapshot != null && !snapshot.isEmpty) {
        val remoteProducts = snapshot.documents.mapNotNull { doc ->
          parseProductFromDoc(doc)
        }
        if (remoteProducts.isNotEmpty()) {
          productDao.insertProducts(remoteProducts.map { ProductEntity.fromProduct(it) })
        }
      }

      // 2. Fetch categories snapshot
      var catSnapshot = db.collection("businesses").document(businessId).collection("categories").get().await()
      if (catSnapshot == null || catSnapshot.isEmpty) {
        val rootCatSnap = db.collection("categories").get().await()
        if (rootCatSnap != null && !rootCatSnap.isEmpty) {
          catSnapshot = rootCatSnap
        }
      }

      if (catSnapshot != null && !catSnapshot.isEmpty) {
        val remoteCategories = catSnapshot.documents.mapNotNull { doc ->
          try {
            val id = doc.id
            val name = doc.getString("name") ?: ""
            val order = doc.getLong("order")?.toInt()
              ?: doc.getLong("displayOrder")?.toInt() ?: 0
            val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
            if (name.isNotBlank()) CategoryItem(id, name, order, createdAt) else null
          } catch (e: Exception) {
            null
          }
        }
        if (remoteCategories.isNotEmpty()) {
          categoryDao.insertCategories(remoteCategories.map { CategoryEntity.fromCategoryItem(it) })
        }
      }
    } catch (e: Exception) {
      Log.w(tag, "Notice on syncWithFirestore: ${e.message}")
    }
  }
}
