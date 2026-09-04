package com.manglamfertilizer.app.data.repository

import com.manglamfertilizer.app.data.model.AlertType
import com.manglamfertilizer.app.data.model.Customer
import com.manglamfertilizer.app.data.model.DashboardMetrics
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.Product
import com.manglamfertilizer.app.data.model.StockAlert
import com.manglamfertilizer.app.data.util.AlertEngine
import java.util.Calendar
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DashboardRepository(
  private val inventoryRepository: InventoryRepository,
  private val billingRepository: BillingRepository,
  private val customerRepository: CustomerRepository
) {

  val dashboardMetrics: Flow<DashboardMetrics> = combine(
    inventoryRepository.products,
    billingRepository.invoices,
    customerRepository.customers
  ) { products, invoices, customers ->
    calculateMetrics(products, invoices, customers)
  }

  val stockAlerts: Flow<List<StockAlert>> = inventoryRepository.products.combine(
    billingRepository.invoices
  ) { products, _ ->
    AlertEngine.generateAlerts(products)
  }

  private fun calculateMetrics(
    products: List<Product>,
    invoices: List<Invoice>,
    customers: List<Customer>
  ): DashboardMetrics {
    val cal = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, 0)
      set(Calendar.MINUTE, 0)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
    }
    val startOfDay = cal.timeInMillis
    val now = System.currentTimeMillis()

    val todayInvoices = invoices.filter { it.timestamp >= startOfDay }
    val todaySales = todayInvoices.sumOf { it.grandTotal }
    val totalDues = customers.sumOf { it.totalDue }

    var lowStockCount = 0
    var nearExpiryCount = 0
    var expiredCount = 0
    var totalStockValue = 0.0

    products.forEach { prod ->
      if (prod.stockQuantity <= prod.minStockAlert) {
        lowStockCount++
      }
      val expiryEval = AlertEngine.evaluateExpiry(prod.expiryDate, now)
      if (expiryEval.isExpired) {
        expiredCount++
      } else if (expiryEval.isNearExpiry) {
        nearExpiryCount++
      }
      totalStockValue += (prod.stockQuantity * prod.purchasePrice)
    }

    return DashboardMetrics(
      todaySales = todaySales,
      todayInvoicesCount = todayInvoices.size,
      totalCustomerDues = totalDues,
      lowStockCount = lowStockCount,
      nearExpiryCount = nearExpiryCount,
      expiredCount = expiredCount,
      totalStockValue = totalStockValue
    )
  }
}

