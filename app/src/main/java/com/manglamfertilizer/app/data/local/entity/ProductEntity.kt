package com.manglamfertilizer.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.ProductUnit

@Entity(tableName = "products")
data class ProductEntity(
  @PrimaryKey val id: String,
  val name: String,
  val category: String,
  val company: String,
  val unit: String,
  val batchNumber: String,
  val purchasePrice: Double,
  val sellingPrice: Double,
  val mrp: Double,
  val stockQuantity: Double,
  val minStockAlert: Double,
  val expiryDate: Long?,
  val rackLocation: String,
  val hsnCode: String,
  val chemicalComposition: String = "",
  val barcode: String = "",
  val packaging: String = "",
  val crop: String = "",
  val usesInstructions: String = "",
  val customFields: String = "",
  val createdAt: Long,
  val updatedAt: Long = createdAt,
  val createdBy: String = "",
  val updatedBy: String = ""
) {
  fun toProduct(): Product {
    val unt = try {
      ProductUnit.valueOf(unit)
    } catch (e: Exception) {
      ProductUnit.BAG
    }
    return Product(
      id = id,
      name = name,
      category = category.trim(),
      company = company,
      unit = unt,
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
  }

  companion object {
    fun fromProduct(p: Product): ProductEntity {
      return ProductEntity(
        id = p.id,
        name = p.name,
        category = p.category.trim(),
        company = p.company,
        unit = p.unit.name,
        batchNumber = p.batchNumber,
        purchasePrice = p.purchasePrice,
        sellingPrice = p.sellingPrice,
        mrp = p.mrp,
        stockQuantity = p.stockQuantity,
        minStockAlert = p.minStockAlert,
        expiryDate = p.expiryDate,
        rackLocation = p.rackLocation,
        hsnCode = p.hsnCode,
        chemicalComposition = p.chemicalComposition,
        barcode = p.barcode,
        packaging = p.packaging,
        crop = p.crop,
        usesInstructions = p.usesInstructions,
        customFields = p.customFields,
        createdAt = p.createdAt,
        updatedAt = p.updatedAt,
        createdBy = p.createdBy,
        updatedBy = p.updatedBy
      )
    }
  }
}
