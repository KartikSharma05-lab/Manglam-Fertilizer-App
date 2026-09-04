package com.manglamfertilizer.app.ui.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

interface AppStrings {
  // Navigation
  val navHome: String
  val navBilling: String
  val navInventory: String
  val navCustomers: String
  val navReports: String
  val navSettings: String

  // Common Actions
  val cancel: String
  val save: String
  val delete: String
  val confirm: String
  val edit: String
  val done: String
  val search: String
  val viewAll: String
  val close: String
  val back: String
  val filter: String
  val reset: String
  val clear: String

  // Home Screen & Dashboard
  val welcome: String
  val goodMorning: String
  val goodAfternoon: String
  val goodEvening: String
  val dealerSubtitle: String
  val todayHighlight: String
  val businessSummary: String
  val todaySales: String
  val todayInvoices: String
  val todayCollections: String
  val billsCount: String
  val pendingDues: String
  val customerDues: String
  val stockValue: String
  val lowStock: String
  val nearExpiry: String
  val lowStockAlert: String
  val quickActions: String
  val newBill: String
  val addProduct: String
  val addFarmer: String
  val recentInvoices: String
  val noBillsFound: String
  val stockAlerts: String
  val allInStock: String
  val itemsLowStock: String
  val krishiAdvisor: String
  val krishiAdvisorDesc: String
  val voiceAssistant: String
  val voiceAssistantDesc: String
  val searchHintHome: String
  val dailyAccounts: String
  val dailyAccountsTitle: String

  // Billing
  val billingTitle: String
  val createInvoice: String
  val invoiceHistory: String
  val selectFarmer: String
  val farmerName: String
  val farmerMobile: String
  val farmerVillage: String
  val paymentMethod: String
  val cash: String
  val onlineUPI: String
  val creditUdhar: String
  val scanBarcode: String
  val addItem: String
  val itemName: String
  val qty: String
  val rate: String
  val discount: String
  val total: String
  val subtotal: String
  val taxGst: String
  val grandTotal: String
  val paidAmount: String
  val balanceDue: String
  val notesRemarks: String
  val saveAndPrint: String
  val saveInvoice: String
  val invoiceSuccessTitle: String
  val invoiceSuccessSubtitle: String
  val printThermal: String
  val shareWhatsApp: String
  val noInvoicesFound: String
  val billNumber: String
  val date: String

  // Inventory
  val inventoryTitle: String
  val searchInventoryHint: String
  val allCategories: String
  val catFertilizer: String
  val catPesticide: String
  val catSeeds: String
  val catEquipment: String
  val catOther: String
  val addNewProduct: String
  val editProduct: String
  val productName: String
  val companyBrand: String
  val category: String
  val batchNumber: String
  val mfgDate: String
  val expiryDate: String
  val mrp: String
  val salePrice: String
  val purchaseCost: String
  val stockQuantity: String
  val unit: String
  val minStockAlertLevel: String
  val barcode: String
  val rackLocation: String
  val hsnCode: String
  val chemicalComposition: String
  val inStock: String
  val outOfStock: String
  val deleteProductConfirm: String

  // Customers
  val customersTitle: String
  val searchCustomerHint: String
  val allCustomers: String
  val pendingUdharList: String
  val addNewFarmer: String
  val editFarmer: String
  val recordPayment: String
  val amount: String
  val savePayment: String
  val totalUdhar: String
  val noPendingDues: String
  val call: String
  val whatsApp: String
  val paymentHistory: String
  val deleteCustomerConfirm: String

  // Reports
  val reportsTitle: String
  val periodToday: String
  val periodThisWeek: String
  val periodThisMonth: String
  val periodAllTime: String
  val totalRevenue: String
  val totalPurchases: String
  val grossProfit: String
  val cashCollected: String
  val creditExtended: String
  val stockValuation: String
  val topSellingProducts: String
  val exportPdf: String
  val exportExcel: String

  // Settings
  val settingsTitle: String
  val accountProfile: String
  val displayNameGreeting: String
  val changeGreetingName: String
  val hardwareBilling: String
  val bluetoothThermalPrinter: String
  val printerConfigDesc: String
  val prefLocalization: String
  val themeMode: String
  val themeLight: String
  val themeDark: String
  val themeSystem: String
  val languageTitle: String
  val langEnglish: String
  val langHindi: String
  val cloudSecurity: String
  val biometricUnlock: String
  val biometricUnlockDescEnabled: String
  val biometricUnlockDescDisabled: String
  val backupCloud: String
  val backupCloudDesc: String
  val appVersion: String
  val checkForUpdates: String
  val checkingForUpdates: String
  val updateAvailable: String
  val updateAvailableTitle: String
  val appUpToDate: String
  val downloadUpdate: String
  val downloadingUpdate: String
  val updateReadyToInstall: String
  val installUpdate: String
  val installingUpdate: String
  val forcedUpdateTitle: String
  val forcedUpdateDesc: String
  val whatsNew: String
  val updateFailed: String
  val verificationFailed: String
  val releaseTypeOptional: String
  val releaseTypeRecommended: String
  val releaseTypeForced: String
  val signOut: String
  val signOutConfirmTitle: String
  val signOutConfirmMsg: String

  // Auth & Prompts
  val unlockAppTitle: String
  val welcomeBack: String
  val signIn: String
  val signInWithAnother: String
  val whatShouldWeCallYou: String
  val chooseGreetingNameDesc: String
  val useGoogleName: String
  val customName: String
  val enterYourName: String
  val getStarted: String
  val shopName: String
}

object EnglishStrings : AppStrings {
  override val shopName = "MANGALAM FERTILIZER"
  override val navHome = "Home"
  override val navBilling = "Bills"
  override val navInventory = "Inventory"
  override val navCustomers = "Customers"
  override val navReports = "Reports"
  override val navSettings = "Settings"

  override val cancel = "Cancel"
  override val save = "Save"
  override val delete = "Delete"
  override val confirm = "Confirm"
  override val edit = "Edit"
  override val done = "Done"
  override val search = "Search"
  override val viewAll = "View All"
  override val close = "Close"
  override val back = "Back"
  override val filter = "Filter"
  override val reset = "Reset"
  override val clear = "Clear"

  override val welcome = "Welcome"
  override val goodMorning = "Good Morning"
  override val goodAfternoon = "Good Afternoon"
  override val goodEvening = "Good Evening"
  override val dealerSubtitle = "Fertilizers, Pesticides, Seeds & Agricultural Equipment"
  override val todayHighlight = "Today's Highlight"
  override val businessSummary = "Business Dashboard"
  override val todaySales = "Today's Sales"
  override val todayInvoices = "Today's Invoices"
  override val todayCollections = "Today's Collection"
  override val billsCount = "Bills Created"
  override val pendingDues = "Pending Dues"
  override val customerDues = "Customer Dues"
  override val stockValue = "Stock Value"
  override val lowStock = "Low Stock"
  override val nearExpiry = "Near Expiry"
  override val lowStockAlert = "Low Stock Alert"
  override val quickActions = "Quick Actions"
  override val newBill = "New Bill"
  override val addProduct = "Add Product"
  override val addFarmer = "Add Farmer"
  override val recentInvoices = "Recent Invoices"
  override val noBillsFound = "No bills created today"
  override val stockAlerts = "Stock & Expiry Alerts"
  override val allInStock = "All products in healthy stock"
  override val itemsLowStock = "products need reordering"
  override val krishiAdvisor = "AI Advisor"
  override val krishiAdvisorDesc = "Dosage, crop diseases & fertilizer suggestions"
  override val voiceAssistant = "Voice AI"
  override val voiceAssistantDesc = "Create bills & query stock hands-free in Hindi/English"
  override val searchHintHome = "Search products, farmers, bills or batches..."
  override val dailyAccounts = "Daily Accounts"
  override val dailyAccountsTitle = "Daily Accounts & Day Ledger"

  override val billingTitle = "Billing & GST Invoices"
  override val createInvoice = "Create New Invoice"
  override val invoiceHistory = "Invoice History"
  override val selectFarmer = "Select or Add Farmer"
  override val farmerName = "Farmer / Customer Name"
  override val farmerMobile = "Mobile Number"
  override val farmerVillage = "Village / Town"
  override val paymentMethod = "Payment Mode"
  override val cash = "Cash"
  override val onlineUPI = "UPI / Online"
  override val creditUdhar = "Credit (Udhar)"
  override val scanBarcode = "Scan Barcode"
  override val addItem = "Add Item"
  override val itemName = "Item Name"
  override val qty = "Qty"
  override val rate = "Rate (₹)"
  override val discount = "Discount"
  override val total = "Total"
  override val subtotal = "Subtotal"
  override val taxGst = "GST (CGST+SGST)"
  override val grandTotal = "Grand Total"
  override val paidAmount = "Amount Paid"
  override val balanceDue = "Remaining Due"
  override val notesRemarks = "Remarks / Notes"
  override val saveAndPrint = "Save & Print Bill"
  override val saveInvoice = "Save Invoice"
  override val invoiceSuccessTitle = "Invoice Saved Successfully!"
  override val invoiceSuccessSubtitle = "Receipt generated and stock deducted"
  override val printThermal = "Print Thermal Receipt"
  override val shareWhatsApp = "Share on WhatsApp"
  override val noInvoicesFound = "No invoices found"
  override val billNumber = "Bill No."
  override val date = "Date"

  override val inventoryTitle = "Inventory Management"
  override val searchInventoryHint = "Search by name, company, batch or formula..."
  override val allCategories = "All"
  override val catFertilizer = "Fertilizer"
  override val catPesticide = "Pesticide"
  override val catSeeds = "Seeds"
  override val catEquipment = "Equipment"
  override val catOther = "Other"
  override val addNewProduct = "Add New Product"
  override val editProduct = "Edit Product"
  override val productName = "Product Name"
  override val companyBrand = "Company / Brand"
  override val category = "Category"
  override val batchNumber = "Batch No."
  override val mfgDate = "Mfg Date"
  override val expiryDate = "Expiry Date"
  override val mrp = "MRP (₹)"
  override val salePrice = "Sale Price (₹)"
  override val purchaseCost = "Purchase Cost (₹)"
  override val stockQuantity = "Stock Quantity"
  override val unit = "Unit (Bags, L, Kg, Pcs)"
  override val minStockAlertLevel = "Min Stock Alert Level"
  override val barcode = "Barcode / QR"
  override val rackLocation = "Rack / Shelf Location"
  override val hsnCode = "HSN Code"
  override val chemicalComposition = "Chemical Composition / Formula"
  override val inStock = "In Stock"
  override val outOfStock = "Out of Stock"
  override val deleteProductConfirm = "Are you sure you want to delete this product?"

  override val customersTitle = "Farmers & Ledger"
  override val searchCustomerHint = "Search farmer by name, village or mobile..."
  override val allCustomers = "All Farmers"
  override val pendingUdharList = "Pending Udhar"
  override val addNewFarmer = "Add New Farmer"
  override val editFarmer = "Edit Farmer"
  override val recordPayment = "Record Payment / Jama"
  override val amount = "Amount (₹)"
  override val savePayment = "Save Payment Entry"
  override val totalUdhar = "Total Udhar Outstanding"
  override val noPendingDues = "No pending dues from farmers"
  override val call = "Call"
  override val whatsApp = "WhatsApp"
  override val paymentHistory = "Payment History"
  override val deleteCustomerConfirm = "Are you sure you want to remove this customer record?"

  override val reportsTitle = "Business Reports & GST"
  override val periodToday = "Today"
  override val periodThisWeek = "This Week"
  override val periodThisMonth = "This Month"
  override val periodAllTime = "All Time"
  override val totalRevenue = "Total Sales"
  override val totalPurchases = "Total Purchases"
  override val grossProfit = "Gross Profit"
  override val cashCollected = "Cash Collected"
  override val creditExtended = "Credit (Udhar) Given"
  override val stockValuation = "Total Stock Valuation"
  override val topSellingProducts = "Top Selling Products"
  override val exportPdf = "Export PDF Report"
  override val exportExcel = "Export Excel (CSV)"

  override val settingsTitle = "Settings & Configuration"
  override val accountProfile = "Account & Profile"
  override val displayNameGreeting = "Display Name & Greeting"
  override val changeGreetingName = "Change Greeting Name"
  override val hardwareBilling = "Hardware & Billing"
  override val bluetoothThermalPrinter = "Bluetooth Thermal Printer"
  override val printerConfigDesc = "Configure 58mm / 80mm ESC/POS printer"
  override val prefLocalization = "Preferences & Localization"
  override val themeMode = "Theme Mode"
  override val themeLight = "Light Theme"
  override val themeDark = "Dark Theme"
  override val themeSystem = "System Default"
  override val languageTitle = "Language / भाषा"
  override val langEnglish = "English"
  override val langHindi = "हिन्दी (Hindi)"
  override val cloudSecurity = "Data Backup & Security"
  override val biometricUnlock = "Biometric & Device Unlock"
  override val biometricUnlockDescEnabled = "Fingerprint / PIN required on opening"
  override val biometricUnlockDescDisabled = "Direct access without prompt"
  override val backupCloud = "Cloud Sync Status"
  override val backupCloudDesc = "Real-time sync active with Firebase Cloud"
  override val appVersion = "Application Version"
  override val checkForUpdates = "Check for Updates"
  override val checkingForUpdates = "Checking for updates..."
  override val updateAvailable = "Update Available"
  override val updateAvailableTitle = "New Version Available"
  override val appUpToDate = "Application is up to date"
  override val downloadUpdate = "Download & Install"
  override val downloadingUpdate = "Downloading Update..."
  override val updateReadyToInstall = "Update Ready to Install"
  override val installUpdate = "Install Now"
  override val installingUpdate = "Installing Update..."
  override val forcedUpdateTitle = "Mandatory Update Required"
  override val forcedUpdateDesc = "A critical update is required to continue using MANGALAM FERTILIZER safely."
  override val whatsNew = "What's New in this Version"
  override val updateFailed = "Update check failed"
  override val verificationFailed = "Update verification failed"
  override val releaseTypeOptional = "Optional"
  override val releaseTypeRecommended = "Recommended"
  override val releaseTypeForced = "Critical / Required"
  override val signOut = "Sign Out"
  override val signOutConfirmTitle = "Sign Out"
  override val signOutConfirmMsg = "Are you sure you want to sign out from MANGALAM FERTILIZER? You will need to log in with your email and password next time."

  override val unlockAppTitle = "Unlock MANGALAM FERTILIZER"
  override val welcomeBack = "Welcome back"
  override val signIn = "Sign In"
  override val signInWithAnother = "Sign In with Another Account"
  override val whatShouldWeCallYou = "What should we call you?"
  override val chooseGreetingNameDesc = "Choose the name you would like to see in greetings and on your home screen."
  override val useGoogleName = "Use name from Gmail account"
  override val customName = "Custom Name"
  override val enterYourName = "Enter your display name"
  override val getStarted = "Get Started"
}

object HindiStrings : AppStrings {
  override val shopName = "MANGALAM FERTILIZER"
  override val navHome = "होम"
  override val navBilling = "बिलिंग"
  override val navInventory = "स्टॉक"
  override val navCustomers = "किसान"
  override val navReports = "रिपोर्ट"
  override val navSettings = "सेटिंग्स"

  override val cancel = "रद्द करें"
  override val save = "सुरक्षित करें"
  override val delete = "हटाएं"
  override val confirm = "पुष्टि करें"
  override val edit = "संपादित करें"
  override val done = "संपन्न"
  override val search = "खोजें"
  override val viewAll = "सभी देखें"
  override val close = "बंद करें"
  override val back = "वापस"
  override val filter = "फ़िल्टर"
  override val reset = "रीसेट"
  override val clear = "हटाएं"

  override val welcome = "स्वागत है"
  override val goodMorning = "शुभ प्रभात"
  override val goodAfternoon = "शुभ दोपहर"
  override val goodEvening = "शुभ संध्या"
  override val dealerSubtitle = "उर्वरक, कीटनाशक, बीज एवं कृषि उपकरण विक्रेता"
  override val todayHighlight = "आज की खास जानकारी"
  override val businessSummary = "व्यापार डैशबोर्ड"
  override val todaySales = "आज की बिक्री"
  override val todayInvoices = "आज के बिल"
  override val todayCollections = "आज की वसूली"
  override val billsCount = "कुल बिल"
  override val pendingDues = "बकाया उधारी"
  override val customerDues = "कुल उधारी"
  override val stockValue = "स्टॉक मूल्य"
  override val lowStock = "कम स्टॉक"
  override val nearExpiry = "समाप्ति निकट"
  override val lowStockAlert = "कम स्टॉक चेतावनी"
  override val quickActions = "त्वरित कार्य"
  override val newBill = "नया बिल"
  override val addProduct = "नया उत्पाद"
  override val addFarmer = "नया किसान"
  override val recentInvoices = "हाल के बिल"
  override val noBillsFound = "आज कोई बिल नहीं बना है"
  override val stockAlerts = "स्टॉक एवं एक्सपायरी चेतावनी"
  override val allInStock = "सभी उत्पाद पर्याप्त स्टॉक में हैं"
  override val itemsLowStock = "उत्पादों का स्टॉक कम है"
  override val krishiAdvisor = "कृषि सलाहकार AI"
  override val krishiAdvisorDesc = "कीट रोग, खाद की सही मात्रा व फसल सलाह"
  override val voiceAssistant = "वॉयस असिस्टेंट"
  override val voiceAssistantDesc = "बोलकर बिल बनाएं व स्टॉक जांचें"
  override val searchHintHome = "उत्पाद, किसान, बिल या बैच खोजें..."
  override val dailyAccounts = "दैनिक हिसाब"
  override val dailyAccountsTitle = "दैनिक हिसाब-किताब एवं बहीखाता"

  override val billingTitle = "बिलिंग एवं जीएसटी इनवॉइस"
  override val createInvoice = "नया बिल बनाएं"
  override val invoiceHistory = "बिल का इतिहास"
  override val selectFarmer = "किसान चुनें या जोड़ें"
  override val farmerName = "किसान / ग्राहक का नाम"
  override val farmerMobile = "मोबाइल नंबर"
  override val farmerVillage = "गांव / शहर"
  override val paymentMethod = "भुगतान का प्रकार"
  override val cash = "नकद (Cash)"
  override val onlineUPI = "यूपीआई / ऑनलाइन"
  override val creditUdhar = "उधार (खाता)"
  override val scanBarcode = "बारकोड स्कैन करें"
  override val addItem = "सामग्री जोड़ें"
  override val itemName = "सामग्री का नाम"
  override val qty = "मात्रा"
  override val rate = "दर (₹)"
  override val discount = "छूट"
  override val total = "कुल"
  override val subtotal = "उप-कुल"
  override val taxGst = "जीएसटी (CGST+SGST)"
  override val grandTotal = "कुल राशि"
  override val paidAmount = "प्राप्त राशि"
  override val balanceDue = "बकाया राशि"
  override val notesRemarks = "टिप्पणी / विवरण"
  override val saveAndPrint = "सुरक्षित करें व प्रिंट करें"
  override val saveInvoice = "बिल सुरक्षित करें"
  override val invoiceSuccessTitle = "बिल सफलतापूर्वक बन गया!"
  override val invoiceSuccessSubtitle = "रसीद तैयार है व स्टॉक घट गया"
  override val printThermal = "थर्मल रसीद प्रिंट करें"
  override val shareWhatsApp = "व्हाट्सएप पर भेजें"
  override val noInvoicesFound = "कोई बिल नहीं मिला"
  override val billNumber = "बिल नं."
  override val date = "तारीख"

  override val inventoryTitle = "स्टॉक एवं इन्वेंटरी"
  override val searchInventoryHint = "नाम, कंपनी, बैच या फॉर्मूले से खोजें..."
  override val allCategories = "सभी"
  override val catFertilizer = "उर्वरक (खाद)"
  override val catPesticide = "कीटनाशक"
  override val catSeeds = "बीज"
  override val catEquipment = "कृषि उपकरण"
  override val catOther = "अन्य"
  override val addNewProduct = "नया उत्पाद जोड़ें"
  override val editProduct = "उत्पाद सुधारें"
  override val productName = "उत्पाद का नाम"
  override val companyBrand = "कंपनी / ब्रांड"
  override val category = "श्रेणी"
  override val batchNumber = "बैच नंबर"
  override val mfgDate = "निर्माण तिथि"
  override val expiryDate = "समाप्ति तिथि (Expiry)"
  override val mrp = "अधिकतम खुदरा मूल्य (MRP ₹)"
  override val salePrice = "बिक्री मूल्य (₹)"
  override val purchaseCost = "खरीद लागत (₹)"
  override val stockQuantity = "स्टॉक मात्रा"
  override val unit = "इकाई (बोरी, लीटर, किलो, नग)"
  override val minStockAlertLevel = "न्यूनतम स्टॉक चेतावनी सीमा"
  override val barcode = "बारकोड / क्यूआर"
  override val rackLocation = "रैक / स्थान"
  override val hsnCode = "एचएसएन कोड"
  override val chemicalComposition = "रासायनिक संरचना / फॉर्मूला"
  override val inStock = "स्टॉक उपलब्ध"
  override val outOfStock = "स्टॉक समाप्त"
  override val deleteProductConfirm = "क्या आप इस उत्पाद को हटाना चाहते हैं?"

  override val customersTitle = "किसान एवं खाता बही"
  override val searchCustomerHint = "नाम, गांव या मोबाइल से किसान खोजें..."
  override val allCustomers = "सभी किसान"
  override val pendingUdharList = "बकाया उधारी सूची"
  override val addNewFarmer = "नया किसान जोड़ें"
  override val editFarmer = "किसान विवरण बदलें"
  override val recordPayment = "जमा / भुगतान दर्ज करें"
  override val amount = "राशि (₹)"
  override val savePayment = "भुगतान सुरक्षित करें"
  override val totalUdhar = "कुल बकाया उधारी"
  override val noPendingDues = "किसी भी किसान पर बकाया नहीं है"
  override val call = "कॉल करें"
  override val whatsApp = "व्हाट्सएप"
  override val paymentHistory = "भुगतान इतिहास"
  override val deleteCustomerConfirm = "क्या आप इस ग्राहक रिकॉर्ड को हटाना चाहते हैं?"

  override val reportsTitle = "व्यापार रिपोर्ट एवं जीएसटी"
  override val periodToday = "आज"
  override val periodThisWeek = "इस सप्ताह"
  override val periodThisMonth = "इस माह"
  override val periodAllTime = "कुल समय"
  override val totalRevenue = "कुल बिक्री"
  override val totalPurchases = "कुल खरीद"
  override val grossProfit = "सकल लाभ"
  override val cashCollected = "नकद वसूली"
  override val creditExtended = "दी गई उधारी"
  override val stockValuation = "कुल स्टॉक का मूल्य"
  override val topSellingProducts = "सर्वाधिक बिकने वाले उत्पाद"
  override val exportPdf = "पीडीएफ रिपोर्ट निकालें"
  override val exportExcel = "एक्सेल (CSV) निकालें"

  override val settingsTitle = "सेटिंग्स एवं व्यवस्था"
  override val accountProfile = "खाता एवं प्रोफाइल"
  override val displayNameGreeting = "अभिवादन नाम (Display Name)"
  override val changeGreetingName = "अभिवादन नाम बदलें"
  override val hardwareBilling = "हार्डवेयर एवं प्रिंटर"
  override val bluetoothThermalPrinter = "ब्लूटूथ थर्मल प्रिंटर"
  override val printerConfigDesc = "58mm / 80mm प्रिंटर कनेक्ट करें"
  override val prefLocalization = "पसंद एवं भाषा (Preferences)"
  override val themeMode = "थीम मोड (Theme)"
  override val themeLight = "लाइट थीम (Light)"
  override val themeDark = "डार्क थीम (Dark)"
  override val themeSystem = "सिस्टम डिफॉल्ट (System)"
  override val languageTitle = "भाषा (Language)"
  override val langEnglish = "English"
  override val langHindi = "हिन्दी (Hindi)"
  override val cloudSecurity = "डेटा बैकअप एवं सुरक्षा"
  override val biometricUnlock = "फिंगरप्रिंट एवं सुरक्षा लॉक"
  override val biometricUnlockDescEnabled = "ऐप खोलने पर फिंगरप्रिंट आवश्यक"
  override val biometricUnlockDescDisabled = "बिना लॉक के सीधा प्रवेश"
  override val backupCloud = "क्लाउड सिंक स्थिति"
  override val backupCloudDesc = "फायरबेस क्लाउड के साथ रीयल-टाइम सिंक सक्रिय"
  override val appVersion = "ऐप संस्करण"
  override val checkForUpdates = "अपडेट चेक करें"
  override val checkingForUpdates = "अपडेट की जांच हो रही है..."
  override val updateAvailable = "नया अपडेट उपलब्ध है"
  override val updateAvailableTitle = "नया संस्करण उपलब्ध"
  override val appUpToDate = "ऐप अप-टू-डेट है"
  override val downloadUpdate = "डाउनलोड करें व इंस्टॉल करें"
  override val downloadingUpdate = "अपडेट डाउनलोड हो रहा है..."
  override val updateReadyToInstall = "अपडेट इंस्टॉल के लिए तैयार है"
  override val installUpdate = "अभी इंस्टॉल करें"
  override val installingUpdate = "अपडेट इंस्टॉल हो रहा है..."
  override val forcedUpdateTitle = "अनिवार्य अपडेट आवश्यक"
  override val forcedUpdateDesc = "MANGALAM FERTILIZER का उपयोग जारी रखने के लिए यह महत्वपूर्ण अपडेट अनिवार्य है।"
  override val whatsNew = "इस संस्करण में नया क्या है"
  override val updateFailed = "अपडेट जांच विफल"
  override val verificationFailed = "अपडेट सुरक्षा जांच विफल"
  override val releaseTypeOptional = "वैकल्पिक"
  override val releaseTypeRecommended = "अनुशंसित"
  override val releaseTypeForced = "अनिवार्य / महत्वपूर्ण"
  override val signOut = "लॉग आउट"
  override val signOutConfirmTitle = "लॉग आउट"
  override val signOutConfirmMsg = "क्या आप MANGALAM FERTILIZER से लॉग आउट करना चाहते हैं? अगली बार आपको अपने ईमेल और पासवर्ड से लॉग इन करना होगा।"

  override val unlockAppTitle = "MANGALAM FERTILIZER अनलॉक करें"
  override val welcomeBack = "पुनः स्वागत है"
  override val signIn = "साइन इन करें"
  override val signInWithAnother = "अन्य खाते से साइन इन करें"
  override val whatShouldWeCallYou = "हम आपको किस नाम से बुलाएं?"
  override val chooseGreetingNameDesc = "वह नाम चुनें जो आप अभिवादन और होम स्क्रीन पर देखना चाहते हैं।"
  override val useGoogleName = "जीमेल खाते वाला नाम उपयोग करें"
  override val customName = "कस्टम नाम (मनपसंद नाम)"
  override val enterYourName = "अपना नाम दर्ज करें"
  override val getStarted = "शुरू करें"
}

val LocalStrings = staticCompositionLocalOf<AppStrings> { EnglishStrings }

val AppStringsCurrent: AppStrings
  @Composable
  @ReadOnlyComposable
  get() = LocalStrings.current
