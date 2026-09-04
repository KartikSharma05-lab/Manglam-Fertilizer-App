package com.manglamfertilizer.app.data.model

data class Product(
  val id: String,
  val name: String,
  val category: String = "",
  val company: String,
  val unit: ProductUnit,
  val batchNumber: String,
  val purchasePrice: Double,
  val sellingPrice: Double,
  val mrp: Double,
  val stockQuantity: Double,
  val minStockAlert: Double = 10.0,
  val expiryDate: Long? = null,
  val rackLocation: String = "",
  val hsnCode: String = "",
  val chemicalComposition: String = "",
  val barcode: String = "",
  val packaging: String = "",
  val crop: String = "",
  val usesInstructions: String = "",
  val customFields: String = "",
  val createdAt: Long = System.currentTimeMillis(),
  val updatedAt: Long = System.currentTimeMillis(),
  val createdBy: String = "",
  val updatedBy: String = ""
)

enum class ProductUnit {
  BAG,
  KG,
  LITER,
  PACKET,
  BOTTLE,
  PIECE,
  GRAM
}

