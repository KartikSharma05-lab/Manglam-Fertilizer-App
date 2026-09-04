package com.manglamfertilizer.app.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.manglamfertilizer.app.data.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

  private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "IN")).apply {
    maximumFractionDigits = 2
  }

  fun generateInvoicePdf(context: Context, invoice: Invoice): File {
    val pdfDocument = PdfDocument()
    val pageWidth = 595 // A4 standard width in points
    val pageHeight = 842 // A4 standard height in points

    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val dateFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = dateFormat.format(Date(invoice.timestamp))

    // 1. Header Banner
    paint.color = Color.rgb(6, 78, 59) // Emerald900
    canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, paint)

    paint.color = Color.rgb(16, 185, 129) // Emerald500 accent stripe
    canvas.drawRect(0f, 95f, pageWidth.toFloat(), 100f, paint)

    // Store Name & Subtitle
    paint.color = Color.WHITE
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 20f
    canvas.drawText("MANGALAM FERTILIZER", 30f, 38f, paint)

    paint.color = Color.rgb(209, 250, 229) // Emerald100
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 10.5f
    canvas.drawText("High-Yield Fertilizers, Certified Seeds, Crop Nutrition & Pesticides", 30f, 56f, paint)
    canvas.drawText("Shop #12, Krishi Mandi Road, Main Yard • GSTIN: 08AABCM1234F1Z5 • Mob: +91 98765 43210", 30f, 72f, paint)

    // Right Header Tag: TAX INVOICE
    paint.color = Color.rgb(245, 158, 11) // GoldAmber
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 14f
    val titleText = "TAX INVOICE"
    val titleWidth = paint.measureText(titleText)
    canvas.drawText(titleText, pageWidth - 30f - titleWidth, 40f, paint)

    paint.color = Color.WHITE
    paint.textSize = 10f
    val invNum = invoice.invoiceNumber
    val invNumWidth = paint.measureText(invNum)
    canvas.drawText(invNum, pageWidth - 30f - invNumWidth, 58f, paint)

    // 2. Metadata Section: Customer & Invoice Details
    var currentY = 125f

    // Box outline for metadata
    paint.color = Color.rgb(240, 248, 245)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(25f, 110f, pageWidth - 25f, 195f, 6f, 6f, paint)

    paint.color = Color.rgb(209, 213, 219)
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 1f
    canvas.drawRoundRect(25f, 110f, pageWidth - 25f, 195f, 6f, 6f, paint)
    paint.style = Paint.Style.FILL

    // Left Column: Customer Information
    paint.color = Color.rgb(6, 78, 59)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 11f
    canvas.drawText("CUSTOMER / FARMER DETAILS:", 35f, currentY, paint)

    paint.color = Color.rgb(31, 41, 55)
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Name: ${invoice.customerName}", 35f, currentY + 16f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    val phoneStr = if (invoice.customerPhone.isNotBlank()) invoice.customerPhone else "N/A"
    canvas.drawText("Mobile: $phoneStr", 35f, currentY + 31f, paint)

    val villageStr = if (invoice.customerVillage.isNotBlank()) invoice.customerVillage else "Local Area"
    val aadhaarStr = if (invoice.customerAadhaar.isNotBlank()) " | Aadhaar: ${invoice.customerAadhaar}" else ""
    canvas.drawText("Village: $villageStr$aadhaarStr", 35f, currentY + 46f, paint)

    if (invoice.customerAddress.isNotBlank()) {
      val addrShort = if (invoice.customerAddress.length > 40) invoice.customerAddress.take(38) + "..." else invoice.customerAddress
      canvas.drawText("Address: $addrShort", 35f, currentY + 60f, paint)
    }

    // Right Column: Invoice Information
    val rightColX = 340f
    paint.color = Color.rgb(6, 78, 59)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("INVOICE METADATA:", rightColX, currentY, paint)

    paint.color = Color.rgb(31, 41, 55)
    paint.textSize = 10.5f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Invoice Date: $formattedDate", rightColX, currentY + 16f, paint)
    canvas.drawText("Payment Mode: ${invoice.paymentMode.name}", rightColX, currentY + 31f, paint)
    canvas.drawText("Billed By: ${invoice.createdBy.ifBlank { "Admin" }}", rightColX, currentY + 46f, paint)

    val paymentStatus = if (invoice.remainingDue <= 0) "PAID (FULL)" else "PARTIAL / CREDIT"
    paint.color = if (invoice.remainingDue <= 0) Color.rgb(5, 150, 105) else Color.rgb(217, 119, 6)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    canvas.drawText("Status: $paymentStatus", rightColX, currentY + 60f, paint)

    // 3. Items Table Header
    currentY = 215f
    paint.color = Color.rgb(6, 78, 59)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(25f, currentY, pageWidth - 25f, currentY + 24f, 4f, 4f, paint)

    paint.color = Color.WHITE
    paint.textSize = 10f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)

    canvas.drawText("S.N.", 35f, currentY + 16f, paint)
    canvas.drawText("Item Description & Chemical Details", 65f, currentY + 16f, paint)
    canvas.drawText("Qty & Unit", 320f, currentY + 16f, paint)
    canvas.drawText("Rate (₹)", 410f, currentY + 16f, paint)
    canvas.drawText("Total (₹)", 495f, currentY + 16f, paint)

    currentY += 28f

    // 4. Items Table Rows
    paint.textSize = 9.5f
    invoice.items.forEachIndexed { index, item ->
      if (index % 2 == 0) {
        paint.color = Color.rgb(249, 250, 251)
        paint.style = Paint.Style.FILL
        canvas.drawRect(25f, currentY - 4f, pageWidth - 25f, currentY + 18f, paint)
      }

      paint.color = Color.rgb(55, 65, 81)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

      // Serial No
      canvas.drawText("${index + 1}", 35f, currentY + 11f, paint)

      // Item Name
      val itemName = if (item.productName.length > 36) item.productName.take(34) + ".." else item.productName
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText(itemName, 65f, currentY + 11f, paint)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

      // Qty
      val qtyStr = "${item.quantity.toInt()} ${item.unit}"
      canvas.drawText(qtyStr, 320f, currentY + 11f, paint)

      // Unit Price
      val rateStr = currencyFormatter.format(item.unitPrice).replace("₹", "")
      canvas.drawText(rateStr, 410f, currentY + 11f, paint)

      // Line Total
      val totalStr = currencyFormatter.format(item.totalPrice).replace("₹", "")
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText(totalStr, 495f, currentY + 11f, paint)

      currentY += 22f
    }

    // Divider after items
    paint.color = Color.rgb(209, 213, 219)
    paint.strokeWidth = 1f
    canvas.drawLine(25f, currentY, pageWidth - 25f, currentY, paint)
    currentY += 15f

    // 5. Summary & Financial Calculations Box
    val summaryBoxTop = currentY
    val summaryBoxLeft = 280f
    val summaryBoxRight = pageWidth - 25f
    val summaryBoxBottom = summaryBoxTop + 145f

    paint.color = Color.rgb(249, 250, 251)
    paint.style = Paint.Style.FILL
    canvas.drawRoundRect(summaryBoxLeft, summaryBoxTop, summaryBoxRight, summaryBoxBottom, 6f, 6f, paint)

    paint.color = Color.rgb(209, 213, 219)
    paint.style = Paint.Style.STROKE
    canvas.drawRoundRect(summaryBoxLeft, summaryBoxTop, summaryBoxRight, summaryBoxBottom, 6f, 6f, paint)
    paint.style = Paint.Style.FILL

    var calcY = summaryBoxTop + 20f
    paint.textSize = 10f

    // Subtotal
    paint.color = Color.rgb(75, 85, 99)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Subtotal:", summaryBoxLeft + 15f, calcY, paint)
    val subTotalText = currencyFormatter.format(invoice.subTotal)
    canvas.drawText(subTotalText, summaryBoxRight - 15f - paint.measureText(subTotalText), calcY, paint)

    // GST
    if (invoice.gstRate > 0) {
      calcY += 18f
      canvas.drawText("GST (${invoice.gstRate.toInt()}%):", summaryBoxLeft + 15f, calcY, paint)
      val gstText = "+ " + currencyFormatter.format(invoice.gstAmount)
      canvas.drawText(gstText, summaryBoxRight - 15f - paint.measureText(gstText), calcY, paint)
    }

    // Discount
    if (invoice.discount > 0) {
      calcY += 18f
      paint.color = Color.rgb(5, 150, 105)
      canvas.drawText("Discount Applied:", summaryBoxLeft + 15f, calcY, paint)
      val discText = "- " + currencyFormatter.format(invoice.discount)
      canvas.drawText(discText, summaryBoxRight - 15f - paint.measureText(discText), calcY, paint)
    }

    // Grand Total Divider
    calcY += 14f
    paint.color = Color.rgb(209, 213, 219)
    canvas.drawLine(summaryBoxLeft + 10f, calcY, summaryBoxRight - 10f, calcY, paint)

    calcY += 18f
    paint.color = Color.rgb(6, 78, 59)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 12f
    canvas.drawText("Grand Total:", summaryBoxLeft + 15f, calcY, paint)
    val grandTotalText = currencyFormatter.format(invoice.grandTotal)
    canvas.drawText(grandTotalText, summaryBoxRight - 15f - paint.measureText(grandTotalText), calcY, paint)

    // Amount Received
    calcY += 20f
    paint.color = Color.rgb(55, 65, 81)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 10f
    canvas.drawText("Received (${invoice.paymentMode.name}):", summaryBoxLeft + 15f, calcY, paint)
    val receivedText = currencyFormatter.format(invoice.amountPaid)
    canvas.drawText(receivedText, summaryBoxRight - 15f - paint.measureText(receivedText), calcY, paint)

    // Balance Due
    calcY += 18f
    if (invoice.remainingDue > 0) {
      paint.color = Color.rgb(217, 119, 6)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Balance Due (Udhar):", summaryBoxLeft + 15f, calcY, paint)
      val dueText = currencyFormatter.format(invoice.remainingDue)
      canvas.drawText(dueText, summaryBoxRight - 15f - paint.measureText(dueText), calcY, paint)

      if (invoice.dueDate != null) {
        calcY += 16f
        paint.textSize = 9f
        paint.color = Color.rgb(180, 83, 9)
        val dueDtFormatted = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(invoice.dueDate))
        canvas.drawText("Due Repayment Date: $dueDtFormatted", summaryBoxLeft + 15f, calcY, paint)
      }
    } else {
      paint.color = Color.rgb(5, 150, 105)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      canvas.drawText("Balance Due:", summaryBoxLeft + 15f, calcY, paint)
      val dueText = "₹0 (Fully Cleared)"
      canvas.drawText(dueText, summaryBoxRight - 15f - paint.measureText(dueText), calcY, paint)
    }

    // Left Side Notes & Declaration
    val notesLeft = 25f
    val notesTop = summaryBoxTop
    paint.color = Color.rgb(75, 85, 99)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 9.5f
    canvas.drawText("Terms & Conditions:", notesLeft, notesTop + 14f, paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 8.5f
    canvas.drawText("1. Goods once sold will not be returned without original invoice.", notesLeft, notesTop + 28f, paint)
    canvas.drawText("2. Store agro-chemicals safely away from children and livestock.", notesLeft, notesTop + 42f, paint)
    canvas.drawText("3. Use registered spray equipment as per manufacturer instructions.", notesLeft, notesTop + 56f, paint)
    canvas.drawText("4. All legal disputes are subject to local district jurisdiction.", notesLeft, notesTop + 70f, paint)

    // 6. Signatures Footer
    val footerY = pageHeight - 65f

    paint.color = Color.rgb(209, 213, 219)
    paint.strokeWidth = 1f
    canvas.drawLine(35f, footerY - 15f, 180f, footerY - 15f, paint)
    canvas.drawLine(pageWidth - 190f, footerY - 15f, pageWidth - 35f, footerY - 15f, paint)

    paint.color = Color.rgb(107, 114, 128)
    paint.textSize = 9f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    canvas.drawText("Customer / Farmer Signature", 40f, footerY, paint)
    canvas.drawText("For MANGALAM FERTILIZER", pageWidth - 185f, footerY, paint)
    canvas.drawText("(Authorized Signatory)", pageWidth - 165f, footerY + 12f, paint)

    // Finish Page
    pdfDocument.finishPage(page)

    // Write to cache directory file
    val invoicesDir = File(context.cacheDir, "invoices").apply { mkdirs() }
    val file = File(invoicesDir, "${invoice.invoiceNumber}.pdf")

    FileOutputStream(file).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()

    return file
  }

  fun viewInvoicePdf(context: Context, file: File) {
    try {
      val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(uri, "application/pdf")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }
      val chooser = Intent.createChooser(intent, "Open Invoice PDF")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "No PDF viewer found on device: ${e.message}", Toast.LENGTH_LONG).show()
    }
  }

  fun shareInvoicePdf(context: Context, file: File, invoice: Invoice) {
    try {
      val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
      )
      val shareText = "Tax Invoice ${invoice.invoiceNumber} from ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME} for ${invoice.customerName}. Grand Total: ₹${invoice.grandTotal.toInt()}."
      val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Invoice ${invoice.invoiceNumber} - ${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME}")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      val chooser = Intent.createChooser(intent, "Share Invoice via")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      Toast.makeText(context, "Failed to share invoice: ${e.message}", Toast.LENGTH_SHORT).show()
    }
  }

  fun saveInvoicePdfToDownloads(context: Context, file: File, invoice: Invoice): Boolean {
    return try {
      val fileName = "${invoice.invoiceNumber}.pdf"
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val values = ContentValues().apply {
          put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
          put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
          put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MangalamInvoices")
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        if (uri != null) {
          context.contentResolver.openOutputStream(uri)?.use { out ->
            file.inputStream().use { input ->
              input.copyTo(out)
            }
          }
          Toast.makeText(context, "Saved to Downloads/MangalamInvoices/$fileName", Toast.LENGTH_LONG).show()
          true
        } else {
          false
        }
      } else {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val destFile = File(downloadsDir, fileName)
        file.copyTo(destFile, overwrite = true)
        Toast.makeText(context, "Saved to ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
        true
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Failed to save to downloads: ${e.message}", Toast.LENGTH_SHORT).show()
      false
    }
  }
}
