package com.manglamfertilizer.app.data.accounting

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.manglamfertilizer.app.data.model.Invoice
import com.manglamfertilizer.app.data.model.PaymentMode
import com.manglamfertilizer.app.data.model.ProductUnit
import com.manglamfertilizer.app.data.util.AppConstants
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Supported Export Formats for Daily Accounts.
 */
enum class ExportFormat(val extension: String, val mimeType: String, val title: String) {
  PDF("pdf", "application/pdf", "PDF Document (.pdf)"),
  XLSX("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "Excel Workbook (.xlsx)"),
  CSV("csv", "text/csv", "CSV Spreadsheet (.csv)")
}

data class ExportResult(
  val file: File,
  val format: ExportFormat,
  val invoiceCount: Int,
  val totalSales: Double,
  val totalCash: Double,
  val totalOnline: Double,
  val totalDue: Double
)

private fun ProductUnit.toDisplayLabel(): String = when (this) {
  ProductUnit.BAG -> "Bags"
  ProductUnit.KG -> "Kg"
  ProductUnit.LITER -> "L"
  ProductUnit.PACKET -> "Pkt"
  ProductUnit.BOTTLE -> "Btl"
  ProductUnit.PIECE -> "Pcs"
  ProductUnit.GRAM -> "g"
}

/**
 * Production-ready Custom Data Exporter for Daily Accounts.
 * 
 * Supports:
 * - PDF (Multi-page Landscape A4 with complete accounting columns and totals)
 * - Excel (.xlsx OpenXML standard with numeric cells and formatted headers)
 * - CSV (UTF-8 with commas/quotes escaping)
 * - Mathematically consistent invoice payments reconciliation (No product-multiplied payment duplication)
 */
object DailyAccountsExporter {

  private val numberFormat = DecimalFormat("#,##,###.##")
  private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
  private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
  private val fullDateTimeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())

  /**
   * Generates export file off main thread and returns the generated File and summary.
   */
  suspend fun exportData(
    context: Context,
    invoices: List<Invoice>,
    periodLabel: String,
    format: ExportFormat
  ): ExportResult = withContext(Dispatchers.IO) {
    // 1. Deduplicate by unique invoice ID to guarantee mathematical consistency
    val deduplicatedInvoices = invoices.distinctBy { it.id }.sortedBy { it.timestamp }

    val totalSales = deduplicatedInvoices.sumOf { it.grandTotal }
    val totalCash = deduplicatedInvoices.sumOf { inv ->
      if (inv.paymentMode == PaymentMode.CASH) inv.amountPaid else 0.0
    }
    val totalOnline = deduplicatedInvoices.sumOf { inv ->
      if (inv.paymentMode != PaymentMode.CASH && inv.paymentMode != PaymentMode.CREDIT) inv.amountPaid else 0.0
    }
    val totalDue = deduplicatedInvoices.sumOf { inv ->
      inv.remainingDue.coerceAtLeast(0.0)
    }

    val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
    val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val sanitizedLabel = periodLabel.replace("[^a-zA-Z0-9]".toRegex(), "_").take(25)
    val fileName = "DailyAccounts_${sanitizedLabel}_$timeStampStr.${format.extension}"
    val targetFile = File(exportsDir, fileName)

    when (format) {
      ExportFormat.CSV -> {
        generateCsv(targetFile, deduplicatedInvoices, periodLabel, totalSales, totalCash, totalOnline, totalDue)
      }
      ExportFormat.XLSX -> {
        generateXlsx(targetFile, deduplicatedInvoices, periodLabel, totalSales, totalCash, totalOnline, totalDue)
      }
      ExportFormat.PDF -> {
        generatePdf(targetFile, deduplicatedInvoices, periodLabel, totalSales, totalCash, totalOnline, totalDue)
      }
    }

    ExportResult(
      file = targetFile,
      format = format,
      invoiceCount = deduplicatedInvoices.size,
      totalSales = totalSales,
      totalCash = totalCash,
      totalOnline = totalOnline,
      totalDue = totalDue
    )
  }

  /**
   * Opens Android system share sheet with appropriate FileProvider permissions.
   */
  fun shareExportFile(
    context: Context,
    exportResult: ExportResult,
    periodLabel: String
  ) {
    try {
      val uri: Uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        exportResult.file
      )
      val shareText = "📊 Daily Accounts Report ($periodLabel)\n" +
          "Total Sales: ₹${numberFormat.format(exportResult.totalSales)} | " +
          "Cash: ₹${numberFormat.format(exportResult.totalCash)} | " +
          "Online: ₹${numberFormat.format(exportResult.totalOnline)} | " +
          "Due: ₹${numberFormat.format(exportResult.totalDue)} (${exportResult.invoiceCount} Bills)\n" +
          "Generated by ${AppConstants.OFFICIAL_SHOP_NAME}"

      val intent = Intent(Intent.ACTION_SEND).apply {
        type = exportResult.format.mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, shareText)
        putExtra(Intent.EXTRA_SUBJECT, "Daily Accounts - $periodLabel")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      }

      val chooser = Intent.createChooser(intent, "Share Daily Accounts ${exportResult.format.name}")
      chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      context.startActivity(chooser)
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  // =========================================================================
  // 1. CSV EXPORT GENERATOR
  // =========================================================================

  private fun generateCsv(
    file: File,
    invoices: List<Invoice>,
    periodLabel: String,
    totalSales: Double,
    totalCash: Double,
    totalOnline: Double,
    totalDue: Double
  ) {
    FileOutputStream(file).use { fos ->
      OutputStreamWriter(fos, StandardCharsets.UTF_8).use { writer ->
        // UTF-8 BOM for Excel compatibility
        writer.write("\uFEFF")

        // Shop Title & Metadata Header
        writer.write("${escapeCsv(AppConstants.OFFICIAL_SHOP_NAME)}\n")
        writer.write("${escapeCsv("Daily Accounts Accounting Ledger - $periodLabel")}\n")
        writer.write("${escapeCsv("Generated on: ${fullDateTimeFormat.format(Date())}")}\n\n")

        // Table Header Columns
        val headers = listOf(
          "S.No.",
          "Date",
          "Farmer Name",
          "Product Name",
          "Quantity",
          "Total Amount",
          "Cash",
          "Online",
          "Due"
        )
        writer.write(headers.joinToString(",") { escapeCsv(it) } + "\n")

        // Data Rows
        invoices.forEachIndexed { index, inv ->
          val dateStr = fullDateTimeFormat.format(Date(inv.timestamp))
          val farmerDisplay = if (inv.customerVillage.isNotBlank()) {
            "${inv.customerName} (${inv.customerVillage})"
          } else {
            inv.customerName
          }

          val productsCombined = if (inv.items.isEmpty()) "-" else {
            inv.items.joinToString("; ") { it.productName }
          }

          val qtyCombined = if (inv.items.isEmpty()) "-" else {
            inv.items.joinToString("; ") { item ->
              val q = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
              "$q ${item.unit.toDisplayLabel()}"
            }
          }

          val cashAmt = if (inv.paymentMode == PaymentMode.CASH) inv.amountPaid else 0.0
          val onlineAmt = if (inv.paymentMode != PaymentMode.CASH && inv.paymentMode != PaymentMode.CREDIT) inv.amountPaid else 0.0
          val dueAmt = inv.remainingDue.coerceAtLeast(0.0)

          val rowValues = listOf(
            (index + 1).toString(),
            dateStr,
            farmerDisplay,
            productsCombined,
            qtyCombined,
            String.format(Locale.US, "%.2f", inv.grandTotal),
            String.format(Locale.US, "%.2f", cashAmt),
            String.format(Locale.US, "%.2f", onlineAmt),
            String.format(Locale.US, "%.2f", dueAmt)
          )
          writer.write(rowValues.joinToString(",") { escapeCsv(it) } + "\n")
        }

        // Total Row at the End
        val totalRow = listOf(
          "TOTAL",
          "${invoices.size} Invoices",
          "",
          "",
          "",
          String.format(Locale.US, "%.2f", totalSales),
          String.format(Locale.US, "%.2f", totalCash),
          String.format(Locale.US, "%.2f", totalOnline),
          String.format(Locale.US, "%.2f", totalDue)
        )
        writer.write(totalRow.joinToString(",") { escapeCsv(it) } + "\n")
      }
    }
  }

  private fun escapeCsv(value: String): String {
    val needsEscaping = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")
    return if (needsEscaping) {
      "\"" + value.replace("\"", "\"\"") + "\""
    } else {
      value
    }
  }

  // =========================================================================
  // 2. EXCEL (.XLSX) OPENXML GENERATOR
  // =========================================================================

  private fun generateXlsx(
    file: File,
    invoices: List<Invoice>,
    periodLabel: String,
    totalSales: Double,
    totalCash: Double,
    totalOnline: Double,
    totalDue: Double
  ) {
    ZipOutputStream(BufferedOutputStream(FileOutputStream(file))).use { zos ->
      // [Content_Types].xml
      zos.putNextEntry(ZipEntry("[Content_Types].xml"))
      zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>""".toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()

      // _rels/.rels
      zos.putNextEntry(ZipEntry("_rels/.rels"))
      zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()

      // xl/_rels/workbook.xml.rels
      zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
      zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>""".toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()

      // xl/styles.xml
      zos.putNextEntry(ZipEntry("xl/styles.xml"))
      zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="3">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="14"/><color rgb="FF047857"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FFD1FAE5"/><bgColor indexed="64"/></patternFill></fill>
  </fills>
  <borders count="2">
    <border><left/><right/><top/><bottom/><diagonal/></border>
    <border>
      <left style="thin"><color rgb="FFD1D5DB"/></left>
      <right style="thin"><color rgb="FFD1D5DB"/></right>
      <top style="thin"><color rgb="FFD1D5DB"/></top>
      <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
    </border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="4">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1"/>
  </cellXfs>
</styleSheet>""".toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()

      // xl/workbook.xml
      zos.putNextEntry(ZipEntry("xl/workbook.xml"))
      zos.write("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Daily Accounts" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>""".toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()

      // xl/worksheets/sheet1.xml
      zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
      val sheetXml = buildString {
        append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        append("""<cols>""")
        append("""<col min="1" max="1" width="8" customWidth="1"/>""")
        append("""<col min="2" max="2" width="22" customWidth="1"/>""")
        append("""<col min="3" max="3" width="26" customWidth="1"/>""")
        append("""<col min="4" max="4" width="30" customWidth="1"/>""")
        append("""<col min="5" max="5" width="16" customWidth="1"/>""")
        append("""<col min="6" max="6" width="16" customWidth="1"/>""")
        append("""<col min="7" max="7" width="14" customWidth="1"/>""")
        append("""<col min="8" max="8" width="14" customWidth="1"/>""")
        append("""<col min="9" max="9" width="14" customWidth="1"/>""")
        append("""</cols>""")
        append("""<sheetData>""")

        // Row 1: Title
        append("""<row r="1">""")
        append("""<c r="A1" t="inlineStr"><is><t>${xmlEscape(AppConstants.OFFICIAL_SHOP_NAME)} - Daily Accounts</t></is></c>""")
        append("""</row>""")

        // Row 2: Period & Date
        append("""<row r="2">""")
        append("""<c r="A2" t="inlineStr"><is><t>Period: ${xmlEscape(periodLabel)} | Generated: ${xmlEscape(fullDateTimeFormat.format(Date()))}</t></is></c>""")
        append("""</row>""")

        // Row 3: Empty
        append("""<row r="3"/>""")

        // Row 4: Header (style index 1)
        val headers = listOf("S.No.", "Date", "Farmer Name", "Product Name", "Quantity", "Total Amount", "Cash", "Online", "Due")
        append("""<row r="4">""")
        headers.forEachIndexed { colIdx, colName ->
          val colLetter = ('A'.code + colIdx).toChar()
          append("""<c r="$colLetter"4 s="1" t="inlineStr"><is><t>${xmlEscape(colName)}</t></is></c>""")
        }
        append("""</row>""")

        // Data Rows starting at row 5 (style index 2 for normal bordered cells)
        var currentRowNum = 5
        invoices.forEachIndexed { index, inv ->
          val dateStr = fullDateTimeFormat.format(Date(inv.timestamp))
          val farmerDisplay = if (inv.customerVillage.isNotBlank()) "${inv.customerName} (${inv.customerVillage})" else inv.customerName
          val productsCombined = if (inv.items.isEmpty()) "-" else inv.items.joinToString(", ") { it.productName }
          val qtyCombined = if (inv.items.isEmpty()) "-" else inv.items.joinToString(", ") { item ->
            val q = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
            "$q ${item.unit.toDisplayLabel()}"
          }
          val cashAmt = if (inv.paymentMode == PaymentMode.CASH) inv.amountPaid else 0.0
          val onlineAmt = if (inv.paymentMode != PaymentMode.CASH && inv.paymentMode != PaymentMode.CREDIT) inv.amountPaid else 0.0
          val dueAmt = inv.remainingDue.coerceAtLeast(0.0)

          append("""<row r="$currentRowNum">""")
          append("""<c r="A$currentRowNum" s="2" t="inlineStr"><is><t>${index + 1}</t></is></c>""")
          append("""<c r="B$currentRowNum" s="2" t="inlineStr"><is><t>${xmlEscape(dateStr)}</t></is></c>""")
          append("""<c r="C$currentRowNum" s="2" t="inlineStr"><is><t>${xmlEscape(farmerDisplay)}</t></is></c>""")
          append("""<c r="D$currentRowNum" s="2" t="inlineStr"><is><t>${xmlEscape(productsCombined)}</t></is></c>""")
          append("""<c r="E$currentRowNum" s="2" t="inlineStr"><is><t>${xmlEscape(qtyCombined)}</t></is></c>""")
          append("""<c r="F$currentRowNum" s="2"><v>${String.format(Locale.US, "%.2f", inv.grandTotal)}</v></c>""")
          append("""<c r="G$currentRowNum" s="2"><v>${String.format(Locale.US, "%.2f", cashAmt)}</v></c>""")
          append("""<c r="H$currentRowNum" s="2"><v>${String.format(Locale.US, "%.2f", onlineAmt)}</v></c>""")
          append("""<c r="I$currentRowNum" s="2"><v>${String.format(Locale.US, "%.2f", dueAmt)}</v></c>""")
          append("""</row>""")
          currentRowNum++
        }

        // Summary Total Row (style index 3)
        append("""<row r="$currentRowNum">""")
        append("""<c r="A$currentRowNum" s="3" t="inlineStr"><is><t>TOTAL</t></is></c>""")
        append("""<c r="B$currentRowNum" s="3" t="inlineStr"><is><t>${invoices.size} Invoices</t></is></c>""")
        append("""<c r="C$currentRowNum" s="3" t="inlineStr"><is><t></t></is></c>""")
        append("""<c r="D$currentRowNum" s="3" t="inlineStr"><is><t></t></is></c>""")
        append("""<c r="E$currentRowNum" s="3" t="inlineStr"><is><t></t></is></c>""")
        append("""<c r="F$currentRowNum" s="3"><v>${String.format(Locale.US, "%.2f", totalSales)}</v></c>""")
        append("""<c r="G$currentRowNum" s="3"><v>${String.format(Locale.US, "%.2f", totalCash)}</v></c>""")
        append("""<c r="H$currentRowNum" s="3"><v>${String.format(Locale.US, "%.2f", totalOnline)}</v></c>""")
        append("""<c r="I$currentRowNum" s="3"><v>${String.format(Locale.US, "%.2f", totalDue)}</v></c>""")
        append("""</row>""")

        append("""</sheetData>""")
        append("""</worksheet>""")
      }
      zos.write(sheetXml.toByteArray(StandardCharsets.UTF_8))
      zos.closeEntry()
    }
  }

  private fun xmlEscape(str: String): String {
    return str.replace("&", "&amp;")
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("\"", "&quot;")
      .replace("'", "&apos;")
  }

  // =========================================================================
  // 3. PDF EXPORT GENERATOR (LANDSCAPE A4 MULTI-PAGE ACCOUNTING SHEET)
  // =========================================================================

  private fun generatePdf(
    file: File,
    invoices: List<Invoice>,
    periodLabel: String,
    totalSales: Double,
    totalCash: Double,
    totalOnline: Double,
    totalDue: Double
  ) {
    val pdfDocument = PdfDocument()
    // Landscape A4 dimensions in points: width = 842, height = 595
    val pageWidth = 842
    val pageHeight = 595

    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val margin = 28f
    val tableWidth = pageWidth - (margin * 2)

    // Column widths for 842pt landscape:
    // S.No. (35), Date (90), Farmer (145), Product (170), Qty (70), Total (76), Cash (70), Online (70), Due (60)
    val colSNo = 35f
    val colDate = 90f
    val colFarmer = 145f
    val colProduct = 170f
    val colQty = 70f
    val colTotal = 76f
    val colCash = 70f
    val colOnline = 70f
    val colDue = 60f

    val rowsPerPage = 16
    val totalPages = if (invoices.isEmpty()) 1 else ((invoices.size - 1) / rowsPerPage) + 1

    for (pageIndex in 0 until totalPages) {
      val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex + 1).create()
      val page = pdfDocument.startPage(pageInfo)
      val canvas: Canvas = page.canvas

      var currentY = margin

      // 1. Header Banner
      paint.color = Color.rgb(6, 78, 59) // Emerald900
      paint.style = Paint.Style.FILL
      canvas.drawRect(0f, 0f, pageWidth.toFloat(), 62f, paint)

      paint.color = Color.rgb(16, 185, 129) // Emerald500 accent stripe
      canvas.drawRect(0f, 62f, pageWidth.toFloat(), 66f, paint)

      // Shop Name & Subtitle
      paint.color = Color.WHITE
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 17f
      canvas.drawText(AppConstants.OFFICIAL_SHOP_NAME, margin, 26f, paint)

      paint.color = Color.rgb(209, 250, 229)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
      paint.textSize = 9.5f
      canvas.drawText("DAILY ACCOUNTS & SALES LEDGER • $periodLabel", margin, 42f, paint)
      canvas.drawText("High-Yield Fertilizers, Certified Seeds, Crop Nutrition & Pesticides", margin, 54f, paint)

      // Right header: Generated info & page number
      paint.color = Color.rgb(245, 158, 11) // Gold
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 10.5f
      val genText = "Generated: ${fullDateTimeFormat.format(Date())}"
      canvas.drawText(genText, pageWidth - margin - paint.measureText(genText), 28f, paint)

      paint.color = Color.WHITE
      paint.textSize = 9.5f
      val pageText = "Page ${pageIndex + 1} of $totalPages"
      canvas.drawText(pageText, pageWidth - margin - paint.measureText(pageText), 45f, paint)

      currentY = 80f

      // 2. Table Column Headers
      val headerHeight = 22f
      paint.color = Color.rgb(240, 248, 245)
      paint.style = Paint.Style.FILL
      canvas.drawRect(margin, currentY, margin + tableWidth, currentY + headerHeight, paint)

      paint.color = Color.rgb(6, 78, 59)
      paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
      paint.textSize = 9.5f

      var colX = margin + 4f
      canvas.drawText("S.No.", colX, currentY + 15f, paint); colX += colSNo
      canvas.drawText("Date", colX, currentY + 15f, paint); colX += colDate
      canvas.drawText("Farmer Name", colX, currentY + 15f, paint); colX += colFarmer
      canvas.drawText("Product Name", colX, currentY + 15f, paint); colX += colProduct
      canvas.drawText("Qty", colX, currentY + 15f, paint); colX += colQty
      canvas.drawText("Total (₹)", colX, currentY + 15f, paint); colX += colTotal
      canvas.drawText("Cash (₹)", colX, currentY + 15f, paint); colX += colCash
      canvas.drawText("Online (₹)", colX, currentY + 15f, paint); colX += colOnline
      canvas.drawText("Due (₹)", colX, currentY + 15f, paint)

      // Header bottom border
      paint.color = Color.rgb(16, 185, 129)
      paint.strokeWidth = 1.2f
      paint.style = Paint.Style.STROKE
      canvas.drawLine(margin, currentY + headerHeight, margin + tableWidth, currentY + headerHeight, paint)
      paint.style = Paint.Style.FILL

      currentY += headerHeight

      // 3. Table Rows for Current Page
      val startIdx = pageIndex * rowsPerPage
      val endIdx = minOf(startIdx + rowsPerPage, invoices.size)
      val rowHeight = 22f

      for (i in startIdx until endIdx) {
        val inv = invoices[i]
        val isEven = (i % 2 == 0)

        if (isEven) {
          paint.color = Color.rgb(250, 250, 250)
          paint.style = Paint.Style.FILL
          canvas.drawRect(margin, currentY, margin + tableWidth, currentY + rowHeight, paint)
        }

        paint.color = Color.rgb(31, 41, 55)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        paint.textSize = 8.5f

        colX = margin + 4f

        // S.No.
        canvas.drawText("${i + 1}", colX, currentY + 14f, paint)
        colX += colSNo

        // Date
        val dStr = dateFormat.format(Date(inv.timestamp))
        val tStr = timeFormat.format(Date(inv.timestamp))
        canvas.drawText("$dStr $tStr", colX, currentY + 14f, paint)
        colX += colDate

        // Farmer Name
        val farmerStr = if (inv.customerVillage.isNotBlank()) "${inv.customerName} (${inv.customerVillage})" else inv.customerName
        val truncatedFarmer = if (farmerStr.length > 24) farmerStr.take(22) + ".." else farmerStr
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(truncatedFarmer, colX, currentY + 14f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        colX += colFarmer

        // Product Name
        val prodStr = if (inv.items.isEmpty()) "-" else inv.items.joinToString(", ") { it.productName }
        val truncatedProd = if (prodStr.length > 30) prodStr.take(28) + ".." else prodStr
        canvas.drawText(truncatedProd, colX, currentY + 14f, paint)
        colX += colProduct

        // Qty
        val qtyStr = if (inv.items.isEmpty()) "-" else inv.items.joinToString(", ") { item ->
          val q = if (item.quantity % 1.0 == 0.0) item.quantity.toInt().toString() else item.quantity.toString()
          "$q ${item.unit.toDisplayLabel()}"
        }
        val truncatedQty = if (qtyStr.length > 14) qtyStr.take(12) + ".." else qtyStr
        canvas.drawText(truncatedQty, colX, currentY + 14f, paint)
        colX += colQty

        // Total
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("₹${inv.grandTotal.toInt()}", colX, currentY + 14f, paint)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        colX += colTotal

        // Cash
        val cashAmt = if (inv.paymentMode == PaymentMode.CASH) inv.amountPaid else 0.0
        if (cashAmt > 0) paint.color = Color.rgb(5, 150, 105) else paint.color = Color.rgb(156, 163, 175)
        canvas.drawText("₹${cashAmt.toInt()}", colX, currentY + 14f, paint)
        colX += colCash

        // Online
        val onlineAmt = if (inv.paymentMode != PaymentMode.CASH && inv.paymentMode != PaymentMode.CREDIT) inv.amountPaid else 0.0
        if (onlineAmt > 0) paint.color = Color.rgb(2, 132, 199) else paint.color = Color.rgb(156, 163, 175)
        canvas.drawText("₹${onlineAmt.toInt()}", colX, currentY + 14f, paint)
        colX += colOnline

        // Due
        val dueAmt = inv.remainingDue.coerceAtLeast(0.0)
        if (dueAmt > 0) paint.color = Color.rgb(217, 119, 6) else paint.color = Color.rgb(156, 163, 175)
        canvas.drawText("₹${dueAmt.toInt()}", colX, currentY + 14f, paint)

        // Row bottom divider
        paint.color = Color.rgb(229, 231, 235)
        paint.strokeWidth = 0.5f
        paint.style = Paint.Style.STROKE
        canvas.drawLine(margin, currentY + rowHeight, margin + tableWidth, currentY + rowHeight, paint)
        paint.style = Paint.Style.FILL

        currentY += rowHeight
      }

      // 4. On the final page, draw the TOTAL summary row and signature footer
      if (pageIndex == totalPages - 1) {
        currentY += 4f
        paint.color = Color.rgb(240, 253, 244) // Light emerald
        paint.style = Paint.Style.FILL
        canvas.drawRoundRect(margin, currentY, margin + tableWidth, currentY + 28f, 4f, 4f, paint)

        paint.color = Color.rgb(16, 185, 129)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 1f
        canvas.drawRoundRect(margin, currentY, margin + tableWidth, currentY + 28f, 4f, 4f, paint)
        paint.style = Paint.Style.FILL

        paint.color = Color.rgb(6, 78, 59)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 10f

        canvas.drawText("TOTAL (${invoices.size} Invoices)", margin + 8f, currentY + 18f, paint)

        val totalX = margin + colSNo + colDate + colFarmer + colProduct + colQty
        canvas.drawText("₹${numberFormat.format(totalSales)}", totalX, currentY + 18f, paint)

        paint.color = Color.rgb(5, 150, 105)
        canvas.drawText("₹${numberFormat.format(totalCash)}", totalX + colTotal, currentY + 18f, paint)

        paint.color = Color.rgb(2, 132, 199)
        canvas.drawText("₹${numberFormat.format(totalOnline)}", totalX + colTotal + colCash, currentY + 18f, paint)

        paint.color = if (totalDue > 0) Color.rgb(217, 119, 6) else Color.rgb(5, 150, 105)
        canvas.drawText("₹${numberFormat.format(totalDue)}", totalX + colTotal + colCash + colOnline, currentY + 18f, paint)

        // Signature area
        val sigY = pageHeight - 35f
        paint.color = Color.rgb(107, 114, 128)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Prepared by: Mangalam Fertilizer Accounting System", margin, sigY, paint)
        val authSig = "Authorized Signatory: _____________________"
        canvas.drawText(authSig, pageWidth - margin - paint.measureText(authSig), sigY, paint)
      }

      pdfDocument.finishPage(page)
    }

    FileOutputStream(file).use { out ->
      pdfDocument.writeTo(out)
    }
    pdfDocument.close()
  }
}
