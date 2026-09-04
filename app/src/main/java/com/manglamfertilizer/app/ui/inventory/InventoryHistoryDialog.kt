package com.manglamfertilizer.app.ui.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FactCheck
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.manglamfertilizer.app.data.model.AuditCleanupRun
import com.manglamfertilizer.app.data.model.AuditLogItem
import com.manglamfertilizer.app.data.model.AuditRetentionConstants
import com.manglamfertilizer.app.data.model.InventoryHistoryItem
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.SoftRed
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AuditTab {
  CLOUD_AUDIT_LOGS,
  LOCAL_INVENTORY_HISTORY,
  RETENTION_POLICY
}

enum class DateFilter(val label: String) {
  ALL("All Time"),
  TODAY("Today"),
  LAST_7_DAYS("7 Days"),
  LAST_30_DAYS("30 Days")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InventoryHistoryDialog(
  history: List<InventoryHistoryItem> = emptyList(),
  auditLogs: List<AuditLogItem> = emptyList(),
  auditCleanupRuns: List<AuditCleanupRun> = emptyList(),
  isAuditLoading: Boolean = false,
  isRetentionSimulating: Boolean = false,
  latestRetentionSimulation: AuditCleanupRun? = null,
  onRefreshAudit: () -> Unit = {},
  onRunRetentionSimulation: (String) -> Unit = {},
  onDismiss: () -> Unit
) {
  var selectedTab by remember { mutableStateOf(AuditTab.CLOUD_AUDIT_LOGS) }
  var searchQuery by remember { mutableStateOf("") }
  var selectedEntityFilter by remember { mutableStateOf("ALL") }
  var selectedRoleFilter by remember { mutableStateOf("ALL") }
  var selectedDateFilter by remember { mutableStateOf(DateFilter.ALL) }
  var showFilters by remember { mutableStateOf(false) }

  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
  val timeFormat = remember { SimpleDateFormat("hh:mm:ss a", Locale.getDefault()) }

  // Filter Cloud Audit Logs
  val now = System.currentTimeMillis()
  val filteredAuditLogs = remember(
    auditLogs,
    searchQuery,
    selectedEntityFilter,
    selectedRoleFilter,
    selectedDateFilter
  ) {
    val q = searchQuery.trim().lowercase()
    auditLogs.filter { event ->
      // Entity type filter
      val matchesEntity = when (selectedEntityFilter) {
        "ALL" -> true
        "PRODUCT" -> event.entityType.equals("PRODUCT", ignoreCase = true)
        "CATEGORY" -> event.entityType.equals("CATEGORY", ignoreCase = true)
        "INVOICE" -> event.entityType.equals("INVOICE", ignoreCase = true)
        "CUSTOMER" -> event.entityType.equals("CUSTOMER", ignoreCase = true)
        "AUTH" -> event.entityType.equals("USER", ignoreCase = true) || event.action.contains("LOGIN", ignoreCase = true)
        "SETTINGS" -> event.entityType.equals("SETTINGS", ignoreCase = true)
        "UPDATE" -> event.entityType.equals("APP_UPDATE", ignoreCase = true) || event.action.startsWith("UPDATE_")
        else -> true
      }

      // Role filter
      val matchesRole = when (selectedRoleFilter) {
        "ALL" -> true
        "ADMIN" -> event.userRole.equals("ADMIN", ignoreCase = true)
        "STAFF" -> event.userRole.equals("STAFF", ignoreCase = true)
        else -> true
      }

      // Date range filter
      val eventTime = event.timestamp
      val matchesDate = when (selectedDateFilter) {
        DateFilter.ALL -> true
        DateFilter.TODAY -> (now - eventTime) < (24L * 60 * 60 * 1000)
        DateFilter.LAST_7_DAYS -> (now - eventTime) < (7L * 24 * 60 * 60 * 1000)
        DateFilter.LAST_30_DAYS -> (now - eventTime) < (30L * 24 * 60 * 60 * 1000)
      }

      // Text search
      val matchesSearch = if (q.isBlank()) true else {
        event.action.lowercase().contains(q) ||
            event.userEmail.lowercase().contains(q) ||
            event.description.lowercase().contains(q) ||
            event.entityType.lowercase().contains(q) ||
            event.entityId.lowercase().contains(q) ||
            event.deviceInstallationId.lowercase().contains(q) ||
            event.metadata.values.any { it.lowercase().contains(q) }
      }

      matchesEntity && matchesRole && matchesDate && matchesSearch
    }
  }

  // Filter Local History
  val filteredHistory = remember(history, searchQuery) {
    if (searchQuery.isBlank()) history
    else {
      val q = searchQuery.trim().lowercase()
      history.filter {
        it.productName.lowercase().contains(q) ||
            it.actionType.lowercase().contains(q) ||
            it.userName.lowercase().contains(q) ||
            it.details.lowercase().contains(q)
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .statusBarsPadding()
        .navigationBarsPadding()
        .imePadding()
        .padding(horizontal = 10.dp, vertical = 8.dp),
      contentAlignment = Alignment.Center
    ) {
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = DarkSurface,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
          .fillMaxWidth()
          .fillMaxHeight(0.92f)
          .testTag("inventory_history_dialog")
      ) {
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(16.dp)
      ) {
        // Header
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Surface(
              shape = CircleShape,
              color = Emerald900.copy(alpha = 0.5f),
              modifier = Modifier.size(36.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Icon(
                  imageVector = Icons.Default.Security,
                  contentDescription = null,
                  tint = Emerald400,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            Column {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                  text = "Firebase Activity Audit",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                  ),
                  color = TextPrimaryDark
                )
                Surface(
                  shape = RoundedCornerShape(4.dp),
                  color = Color(0xFF1E3A8A).copy(alpha = 0.5f),
                  border = BorderStroke(0.5.dp, Color(0xFF60A5FA).copy(alpha = 0.4f))
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp),
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.CloudDone,
                      contentDescription = null,
                      tint = Color(0xFF93C5FD),
                      modifier = Modifier.size(10.dp)
                    )
                    Text(
                      text = "FIRESTORE",
                      style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                      ),
                      color = Color(0xFF93C5FD)
                    )
                  }
                }
              }
              Text(
                text = "${auditLogs.size} authoritative cloud events • Immutably recorded",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                color = TextSecondaryDark
              )
            }
          }

          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            IconButton(
              onClick = onRefreshAudit,
              modifier = Modifier.size(32.dp).testTag("refresh_audit_logs_btn")
            ) {
              if (isAuditLoading) {
                CircularProgressIndicator(
                  modifier = Modifier.size(16.dp),
                  color = Emerald400,
                  strokeWidth = 2.dp
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Refresh",
                  tint = Emerald400,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
            IconButton(
              onClick = onDismiss,
              modifier = Modifier.size(32.dp).testTag("close_audit_dialog_btn")
            ) {
              Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondaryDark)
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Selector
        TabRow(
          selectedTabIndex = selectedTab.ordinal,
          containerColor = DarkBg,
          contentColor = Emerald400,
          indicator = { tabPositions ->
            TabRowDefaults.SecondaryIndicator(
              Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
              color = Emerald400
            )
          },
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
        ) {
          Tab(
            selected = selectedTab == AuditTab.CLOUD_AUDIT_LOGS,
            onClick = { selectedTab = AuditTab.CLOUD_AUDIT_LOGS },
            text = {
              Text(
                text = "Cloud Logs (${filteredAuditLogs.size})",
                fontWeight = if (selectedTab == AuditTab.CLOUD_AUDIT_LOGS) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                color = if (selectedTab == AuditTab.CLOUD_AUDIT_LOGS) Emerald400 else TextMutedDark
              )
            }
          )
          Tab(
            selected = selectedTab == AuditTab.LOCAL_INVENTORY_HISTORY,
            onClick = { selectedTab = AuditTab.LOCAL_INVENTORY_HISTORY },
            text = {
              Text(
                text = "Stock Logs (${filteredHistory.size})",
                fontWeight = if (selectedTab == AuditTab.LOCAL_INVENTORY_HISTORY) FontWeight.Bold else FontWeight.Normal,
                fontSize = 12.sp,
                color = if (selectedTab == AuditTab.LOCAL_INVENTORY_HISTORY) Emerald400 else TextMutedDark
              )
            }
          )
          Tab(
            selected = selectedTab == AuditTab.RETENTION_POLICY,
            onClick = { selectedTab = AuditTab.RETENTION_POLICY },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(
                  imageVector = Icons.Default.Shield,
                  contentDescription = null,
                  modifier = Modifier.size(13.dp),
                  tint = if (selectedTab == AuditTab.RETENTION_POLICY) GoldAmber else TextMutedDark
                )
                Text(
                  text = "Retention Policy",
                  fontWeight = if (selectedTab == AuditTab.RETENTION_POLICY) FontWeight.Bold else FontWeight.Normal,
                  fontSize = 12.sp,
                  color = if (selectedTab == AuditTab.RETENTION_POLICY) GoldAmber else TextMutedDark
                )
              }
            }
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Search & Filter Bar (Hidden on Retention Policy Tab)
        if (selectedTab != AuditTab.RETENTION_POLICY) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedTextField(
              value = searchQuery,
              onValueChange = { searchQuery = it },
              placeholder = {
                Text(
                  if (selectedTab == AuditTab.CLOUD_AUDIT_LOGS) "Search user, action, entity, device..." else "Search product, user, or action...",
                  color = TextMutedDark,
                  fontSize = 12.sp
                )
              },
              leadingIcon = {
                Icon(
                  Icons.Default.Search,
                  contentDescription = null,
                  tint = TextSecondaryDark,
                  modifier = Modifier.size(16.dp)
                )
              },
              trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                  IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextMutedDark, modifier = Modifier.size(14.dp))
                  }
                }
              },
              singleLine = true,
              modifier = Modifier
                .weight(1f)
                .testTag("audit_search_input"),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Emerald400,
                unfocusedBorderColor = DarkBorder,
                focusedTextColor = TextPrimaryDark,
                unfocusedTextColor = TextPrimaryDark,
                focusedContainerColor = DarkBg,
                unfocusedContainerColor = DarkBg
              )
            )

            if (selectedTab == AuditTab.CLOUD_AUDIT_LOGS) {
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (showFilters) Emerald900.copy(alpha = 0.6f) else DarkCard,
                border = BorderStroke(1.dp, if (showFilters) Emerald400 else DarkBorder),
                modifier = Modifier
                  .clickable { showFilters = !showFilters }
                  .testTag("toggle_audit_filters_btn")
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (showFilters) Emerald400 else TextSecondaryDark,
                    modifier = Modifier.size(18.dp)
                  )
                  Text(
                    text = "Filters",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (showFilters) Emerald400 else TextSecondaryDark
                  )
                }
              }
            }
          }

          // Expandable Filters for Cloud Audit Logs
          AnimatedVisibility(visible = showFilters && selectedTab == AuditTab.CLOUD_AUDIT_LOGS) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .background(DarkBg.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                .padding(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              // Filter by Entity Type
              Text("ENTITY TYPE:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = TextMutedDark)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf("ALL", "PRODUCT", "CATEGORY", "INVOICE", "CUSTOMER", "AUTH", "SETTINGS", "UPDATE").forEach { entity ->
                  val isSelected = selectedEntityFilter == entity
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) Emerald500 else DarkCard,
                    border = BorderStroke(0.5.dp, if (isSelected) Emerald400 else DarkBorder),
                    modifier = Modifier.clickable { selectedEntityFilter = entity }
                  ) {
                    Text(
                      text = entity,
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) DarkBg else TextSecondaryDark,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
              }

              // Filter by Role
              Text("USER ROLE:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = TextMutedDark)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                listOf("ALL", "ADMIN", "STAFF").forEach { role ->
                  val isSelected = selectedRoleFilter == role
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) (if (role == "ADMIN") SoftRed else Emerald500) else DarkCard,
                    border = BorderStroke(0.5.dp, DarkBorder),
                    modifier = Modifier.clickable { selectedRoleFilter = role }
                  ) {
                    Text(
                      text = role,
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) Color.White else TextSecondaryDark,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
              }

              // Filter by Date Range
              Text("TIME PERIOD:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = TextMutedDark)
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                DateFilter.values().forEach { filter ->
                  val isSelected = selectedDateFilter == filter
                  Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isSelected) GoldAmber else DarkCard,
                    border = BorderStroke(0.5.dp, DarkBorder),
                    modifier = Modifier.clickable { selectedDateFilter = filter }
                  ) {
                    Text(
                      text = filter.label,
                      fontSize = 10.5.sp,
                      fontWeight = FontWeight.Bold,
                      color = if (isSelected) DarkBg else TextSecondaryDark,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                  }
                }
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Content based on Selected Tab
        when (selectedTab) {
          AuditTab.CLOUD_AUDIT_LOGS -> {
            if (filteredAuditLogs.isEmpty()) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(48.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = if (auditLogs.isEmpty()) "No Firebase audit logs recorded yet." else "No audit logs matching current filter.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedDark
                  )
                  if (auditLogs.isEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "Any system login, product edit, billing, or setting change will appear here automatically.",
                      style = MaterialTheme.typography.bodySmall,
                      color = TextMutedDark.copy(alpha = 0.7f),
                      fontSize = 11.sp
                    )
                  }
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth()
                  .testTag("audit_logs_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(filteredAuditLogs, key = { it.logId }) { event ->
                  AuditLogCard(
                    event = event,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat
                  )
                }
              }
            }
          }

          AuditTab.LOCAL_INVENTORY_HISTORY -> {
            if (filteredHistory.isEmpty()) {
              Box(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = TextMutedDark,
                    modifier = Modifier.size(48.dp)
                  )
                  Spacer(modifier = Modifier.height(8.dp))
                  Text(
                    text = if (history.isEmpty()) "No local stock operations recorded yet." else "No history matching search.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextMutedDark
                  )
                }
              }
            } else {
              LazyColumn(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                items(filteredHistory, key = { it.id }) { item ->
                  LocalHistoryItemCard(
                    item = item,
                    dateFormat = dateFormat,
                    timeFormat = timeFormat
                  )
                }
              }
            }
          }

          AuditTab.RETENTION_POLICY -> {
            AuditRetentionPolicyView(
              auditLogs = auditLogs,
              cleanupRuns = auditCleanupRuns,
              isSimulating = isRetentionSimulating,
              latestSimulation = latestRetentionSimulation,
              onRunSimulation = onRunRetentionSimulation,
              modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Close Button
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Emerald500, contentColor = DarkBg),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().testTag("close_audit_dialog_bottom_btn")
        ) {
          Text("Close Audit Viewer", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuditLogCard(
  event: AuditLogItem,
  dateFormat: SimpleDateFormat,
  timeFormat: SimpleDateFormat
) {
  var isExpanded by remember { mutableStateOf(false) }

  // Derive visual style based on action
  val (actionColor, actionIcon) = getActionVisuals(event.action)
  val isAdmin = event.userRole.equals("ADMIN", ignoreCase = true)
  val eventMillis = event.timestamp

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, if (isExpanded) actionColor.copy(alpha = 0.5f) else DarkBorder),
    modifier = Modifier
      .fillMaxWidth()
      .clickable { isExpanded = !isExpanded }
      .testTag("audit_card_${event.logId}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      // Top Row: Action Tag + Role Badge + Timestamp
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = actionColor.copy(alpha = 0.15f),
            border = BorderStroke(1.dp, actionColor.copy(alpha = 0.4f))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.5.dp),
              horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
              Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = actionColor,
                modifier = Modifier.size(11.dp)
              )
              Text(
                text = event.action,
                style = MaterialTheme.typography.labelSmall.copy(
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                ),
                color = actionColor
              )
            }
          }

          // Role Badge
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = if (isAdmin) SoftRed.copy(alpha = 0.15f) else Emerald900.copy(alpha = 0.4f),
            border = BorderStroke(0.5.dp, if (isAdmin) SoftRed.copy(alpha = 0.4f) else Emerald400.copy(alpha = 0.3f))
          ) {
            Text(
              text = if (isAdmin) "ADMIN" else "STAFF",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              ),
              color = if (isAdmin) SoftRed else Emerald400,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.5.dp)
            )
          }
        }

        // Timestamp
        Text(
          text = if (eventMillis > 0) {
            "${dateFormat.format(Date(eventMillis))} • ${timeFormat.format(Date(eventMillis))}"
          } else "Just now (Pending)",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
          color = TextMutedDark
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      // Description
      Text(
        text = event.description,
        style = MaterialTheme.typography.titleSmall.copy(
          fontSize = 13.5.sp,
          fontWeight = FontWeight.SemiBold
        ),
        color = TextPrimaryDark,
        maxLines = if (isExpanded) 10 else 2,
        overflow = TextOverflow.Ellipsis
      )

      Spacer(modifier = Modifier.height(6.dp))

      // User & Entity Row
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = TextMutedDark,
            modifier = Modifier.size(12.dp)
          )
          Text(
            text = event.userEmail.ifBlank { "System User" },
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = TextSecondaryDark
          )
        }

        if (event.entityType.isNotBlank()) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = DarkBg,
            border = BorderStroke(0.5.dp, DarkBorder)
          ) {
            Text(
              text = "${event.entityType}${if (event.entityId.isNotBlank()) " #${event.entityId.take(8)}" else ""}",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace
              ),
              color = TextMutedDark,
              modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
            )
          }
        }
      }

      // Expandable Details (Installation ID, Metadata, Document ID)
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .background(DarkBg, RoundedCornerShape(8.dp))
            .padding(8.dp),
          verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            text = "AUDIT SECURITY TELEMETRY",
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold
            ),
            color = TextMutedDark
          )

          // Document Log ID
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Log ID:", fontSize = 10.sp, color = TextMutedDark)
            Text(event.logId, fontSize = 10.sp, color = TextSecondaryDark, fontFamily = FontFamily.Monospace)
          }

          // User ID
          if (event.userId.isNotBlank()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("User UID:", fontSize = 10.sp, color = TextMutedDark)
              Text(event.userId, fontSize = 10.sp, color = TextSecondaryDark, fontFamily = FontFamily.Monospace)
            }
          }

          // Installation ID
          if (event.deviceInstallationId.isNotBlank()) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Device Install ID:", fontSize = 10.sp, color = TextMutedDark)
              Text(event.deviceInstallationId, fontSize = 10.sp, color = TextSecondaryDark, fontFamily = FontFamily.Monospace)
            }
          }

          // Metadata Key-Values
          if (event.metadata.isNotEmpty()) {
            HorizontalDivider(color = DarkBorder, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
            Text(
              text = "METADATA PAYLOAD (${event.metadata.size} fields):",
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
              ),
              color = Emerald400
            )

            for ((k, v) in event.metadata) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "$k:",
                  fontSize = 10.sp,
                  color = TextMutedDark,
                  fontWeight = FontWeight.SemiBold
                )
                Text(
                  text = v,
                  fontSize = 10.sp,
                  color = TextPrimaryDark,
                  fontFamily = FontFamily.Monospace
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun LocalHistoryItemCard(
  item: InventoryHistoryItem,
  dateFormat: SimpleDateFormat,
  timeFormat: SimpleDateFormat
) {
  val actionColor = when (item.actionType) {
    "Product Added" -> Emerald400
    "Stock Increased" -> Emerald400
    "Stock Decreased" -> GoldAmber
    "Price Changed" -> GoldAmber
    "Product Deleted" -> SoftRed
    "Category Deleted" -> SoftRed
    "Exported Inventory" -> Color(0xFF60A5FA)
    else -> Emerald400
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = DarkCard,
    border = BorderStroke(1.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth().testTag("history_item_${item.id}")
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(
          shape = RoundedCornerShape(6.dp),
          color = actionColor.copy(alpha = 0.15f),
          border = BorderStroke(1.dp, actionColor.copy(alpha = 0.4f))
        ) {
          Text(
            text = item.actionType.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
              fontSize = 10.5.sp,
              fontWeight = FontWeight.Bold
            ),
            color = actionColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }

        Text(
          text = "${dateFormat.format(Date(item.timestamp))} • ${timeFormat.format(Date(item.timestamp))}",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
          color = TextMutedDark
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = item.productName,
        style = MaterialTheme.typography.titleSmall.copy(
          fontSize = 14.sp,
          fontWeight = FontWeight.Bold
        ),
        color = TextPrimaryDark
      )

      if (item.details.isNotBlank()) {
        Text(
          text = item.details,
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
          color = TextSecondaryDark,
          modifier = Modifier.padding(top = 2.dp)
        )
      }

      if (item.previousValue != null || item.newValue != null) {
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (item.previousValue != null) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = DarkBg,
              border = BorderStroke(1.dp, DarkBorder)
            ) {
              Text(
                text = "Old: ${item.previousValue}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMutedDark,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          if (item.newValue != null) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Emerald900.copy(alpha = 0.5f),
              border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.3f))
            ) {
              Text(
                text = "New: ${item.newValue}",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = Emerald400,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(6.dp))
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Person,
          contentDescription = null,
          tint = TextMutedDark,
          modifier = Modifier.size(12.dp)
        )
        Text(
          text = "By ${item.userName}${if (item.userEmail.isNotBlank()) " (${item.userEmail})" else ""}",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
          color = TextMutedDark
        )
      }
    }
  }
}

private fun getActionVisuals(action: String): Pair<Color, ImageVector> {
  return when {
    action == "USER_LOGIN" -> Pair(Emerald400, Icons.Default.Lock)
    action.contains("DELETE") -> Pair(SoftRed, Icons.Default.Close)
    action.contains("CANCEL") -> Pair(SoftRed, Icons.Default.Close)
    action.contains("FAILED") -> Pair(SoftRed, Icons.Default.Info)
    action.contains("CREATE") -> Pair(Emerald400, Icons.Default.CheckCircle)
    action.contains("ADD") -> Pair(Emerald400, Icons.Default.CheckCircle)
    action.contains("UPDATE") || action.contains("EDIT") -> Pair(GoldAmber, Icons.Default.Settings)
    action.contains("CHANGE") -> Pair(GoldAmber, Icons.Default.Settings)
    action.startsWith("INVOICE") -> Pair(Color(0xFF60A5FA), Icons.Default.ReceiptLong)
    action.startsWith("CUSTOMER") -> Pair(Color(0xFFA78BFA), Icons.Default.People)
    action.startsWith("PRODUCT") -> Pair(Emerald400, Icons.Default.Inventory2)
    action.startsWith("CATEGORY") -> Pair(GoldAmber, Icons.Default.Category)
    action.startsWith("UPDATE_") -> Pair(Color(0xFF38BDF8), Icons.Default.SystemUpdate)
    else -> Pair(Emerald400, Icons.Default.History)
  }
}

@Composable
private fun AuditRetentionPolicyView(
  auditLogs: List<AuditLogItem>,
  cleanupRuns: List<AuditCleanupRun>,
  isSimulating: Boolean,
  latestSimulation: AuditCleanupRun?,
  onRunSimulation: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
  val shortDateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

  Column(
    modifier = modifier
      .verticalScroll(rememberScrollState())
      .padding(vertical = 4.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Mandatory Policy & Server Function Configuration Card
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = DarkCard,
      border = BorderStroke(1.dp, Emerald400.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Surface(
            shape = CircleShape,
            color = Emerald900.copy(alpha = 0.6f),
            modifier = Modifier.size(36.dp)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = Emerald400,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Column {
            Text(
              text = "Authoritative Server-Side Retention Policy",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
              color = TextPrimaryDark
            )
            Text(
              text = "${com.manglamfertilizer.app.data.util.AppConstants.OFFICIAL_SHOP_NAME} • Cloud Functions Automated Retention Engine",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              color = Emerald400
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = DarkBorder)
        Spacer(modifier = Modifier.height(12.dp))

        // Policy Attributes Grid
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          RetentionAttributeRow(
            label = "Tripwire / First Cleanup Date",
            value = "30 January 2028",
            highlightColor = GoldAmber,
            subtext = "NO audit records deleted during current development"
          )
          RetentionAttributeRow(
            label = "Execution Schedule",
            value = "Once yearly on Jan 30 at 02:00 UTC",
            highlightColor = Color(0xFF60A5FA),
            subtext = "Automated Cloud Scheduler (cron: 0 2 30 1 *)"
          )
          RetentionAttributeRow(
            label = "Retention Window",
            value = "365 Days (1 Year)",
            highlightColor = Emerald400,
            subtext = "Deletes ONLY records older than (Run Date - 365 Days)"
          )
          RetentionAttributeRow(
            label = "Scope Restrictions",
            value = "auditLogs Collection ONLY",
            highlightColor = Emerald400,
            subtext = "Never touches products, customers, invoices, inventory, auth or settings"
          )
          RetentionAttributeRow(
            label = "Batch Processing Limit",
            value = "400 documents / batch",
            highlightColor = TextPrimaryDark,
            subtext = "Controlled deletion batches preventing transaction timeout"
          )
          RetentionAttributeRow(
            label = "Execution Log Output",
            value = "auditCleanupRuns Collection",
            highlightColor = Color(0xFFA78BFA),
            subtext = "Dedicated summary prevents infinite audit loop"
          )
        }
      }
    }

    // 2. Development / Test Mode Simulation Harness
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = DarkCard,
      border = BorderStroke(1.dp, GoldAmber.copy(alpha = 0.4f)),
      modifier = Modifier.fillMaxWidth().testTag("retention_simulator_card")
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = GoldAmber,
            modifier = Modifier.size(20.dp)
          )
          Column {
            Text(
              text = "Development & Test Mode Simulation",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 14.sp),
              color = GoldAmber
            )
            Text(
              text = "Dry-run verification of tripwires & cutoffs (0 production records deleted)",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
              color = TextSecondaryDark
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = "Select a simulated run date to test backend retention rules:",
          style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
          color = TextMutedDark
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Three Simulation Action Buttons
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          SimulationButton(
            title = "29 Jan 2028",
            subtitle = "Pre-Launch (Blocked)",
            color = SoftRed,
            enabled = !isSimulating,
            onClick = { onRunSimulation("JAN_29_2028") },
            modifier = Modifier.weight(1f).testTag("sim_jan_29_btn")
          )
          SimulationButton(
            title = "30 Jan 2028",
            subtitle = "First Active Run",
            color = Emerald400,
            enabled = !isSimulating,
            onClick = { onRunSimulation("JAN_30_2028") },
            modifier = Modifier.weight(1f).testTag("sim_jan_30_btn")
          )
          SimulationButton(
            title = "31 Jan 2028",
            subtitle = "Post-Launch Run",
            color = Color(0xFF60A5FA),
            enabled = !isSimulating,
            onClick = { onRunSimulation("JAN_31_2028") },
            modifier = Modifier.weight(1f).testTag("sim_jan_31_btn")
          )
        }

        // Live Simulation Running Indicator
        if (isSimulating) {
          Spacer(modifier = Modifier.height(12.dp))
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              color = GoldAmber,
              strokeWidth = 2.dp
            )
            Text(
              text = "Evaluating Firestore audit collection against retention cutoff in dry-run mode...",
              style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
              color = GoldAmber
            )
          }
        }

        // Simulation Result Display
        if (latestSimulation != null) {
          Spacer(modifier = Modifier.height(12.dp))
          SimulationResultCard(result = latestSimulation, dateFormat = dateFormat)
        }
      }
    }

    // 3. Past Cleanup & Simulation Run History
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = DarkCard,
      border = BorderStroke(1.dp, DarkBorder),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(
              imageVector = Icons.Default.FactCheck,
              contentDescription = null,
              tint = Emerald400,
              modifier = Modifier.size(18.dp)
            )
            Text(
              text = "Server Cleanup Run History",
              style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.5.sp),
              color = TextPrimaryDark
            )
          }
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = DarkBg,
            border = BorderStroke(1.dp, DarkBorder)
          ) {
            Text(
              text = "${cleanupRuns.size} recorded runs",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
              color = TextMutedDark,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (cleanupRuns.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(
                imageVector = Icons.Default.History,
                contentDescription = null,
                tint = TextMutedDark,
                modifier = Modifier.size(32.dp)
              )
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "No scheduled cleanup runs executed yet.",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                color = TextMutedDark
              )
              Text(
                text = "Scheduled to start automatically on 30 January 2028.",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                color = TextMutedDark.copy(alpha = 0.7f)
              )
            }
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            cleanupRuns.take(5).forEach { run ->
              CleanupRunItem(run = run, dateFormat = dateFormat)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun RetentionAttributeRow(
  label: String,
  value: String,
  highlightColor: Color,
  subtext: String
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.Top
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = label,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold),
        color = TextSecondaryDark
      )
      Text(
        text = subtext,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
        color = TextMutedDark
      )
    }
    Surface(
      shape = RoundedCornerShape(6.dp),
      color = highlightColor.copy(alpha = 0.12f),
      border = BorderStroke(1.dp, highlightColor.copy(alpha = 0.35f))
    ) {
      Text(
        text = value,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.Bold),
        color = highlightColor,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
      )
    }
  }
}

@Composable
private fun SimulationButton(
  title: String,
  subtitle: String,
  color: Color,
  enabled: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = color.copy(alpha = 0.12f),
    border = BorderStroke(1.dp, color.copy(alpha = 0.45f)),
    modifier = modifier.clickable(enabled = enabled, onClick = onClick)
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = null,
          tint = color,
          modifier = Modifier.size(12.dp)
        )
        Text(
          text = title,
          style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.5.sp),
          color = color
        )
      }
      Text(
        text = subtitle,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
        color = TextMutedDark,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )
    }
  }
}

@Composable
private fun SimulationResultCard(
  result: AuditCleanupRun,
  dateFormat: SimpleDateFormat
) {
  val isTripwireBlocked = result.status == "SKIPPED_BEFORE_FIRST_RUN_DATE"
  val isSuccess = result.status == "SIMULATION_SUCCESS" || result.status == "SUCCESS"
  val statusColor = if (isTripwireBlocked) GoldAmber else if (isSuccess) Emerald400 else SoftRed

  Surface(
    shape = RoundedCornerShape(10.dp),
    color = DarkBg,
    border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f)),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Icon(
            imageVector = if (isTripwireBlocked) Icons.Default.Warning else Icons.Default.CheckCircle,
            contentDescription = null,
            tint = statusColor,
            modifier = Modifier.size(16.dp)
          )
          Text(
            text = "Simulation Result",
            style = MaterialTheme.typography.titleSmall.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold),
            color = TextPrimaryDark
          )
        }
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = statusColor.copy(alpha = 0.2f),
          border = BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
        ) {
          Text(
            text = result.status,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
            color = statusColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
          )
        }
      }

      Text(
        text = result.message,
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
        color = TextSecondaryDark
      )

      HorizontalDivider(color = DarkBorder.copy(alpha = 0.5f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Column {
          Text("Simulated Date:", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = TextMutedDark)
          Text(
            text = dateFormat.format(Date(result.timestamp)),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = TextPrimaryDark
          )
        }
        Column(horizontalAlignment = Alignment.End) {
          Text("Cutoff Date (365d):", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = TextMutedDark)
          Text(
            text = dateFormat.format(Date(result.cutoffTimestamp)),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = TextPrimaryDark
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text("Evaluated Records: ${result.recordsEvaluated}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryDark)
        Text("Expired / Eligible: ${result.recordsDeleted}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = statusColor)
        Text("Batches: ${result.batchesProcessed}", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = TextSecondaryDark)
      }
    }
  }
}

@Composable
private fun CleanupRunItem(
  run: AuditCleanupRun,
  dateFormat: SimpleDateFormat
) {
  val isDryRun = run.dryRun
  val statusColor = if (run.status.contains("SKIPPED")) GoldAmber else if (run.status.contains("SUCCESS")) Emerald400 else SoftRed

  Surface(
    shape = RoundedCornerShape(8.dp),
    color = DarkBg,
    border = BorderStroke(0.5.dp, DarkBorder),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = statusColor.copy(alpha = 0.15f)
          ) {
            Text(
              text = if (isDryRun) "SIMULATION" else "SCHEDULED",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
              color = statusColor,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
          Text(
            text = dateFormat.format(Date(run.timestamp)),
            style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.5.sp, fontWeight = FontWeight.SemiBold),
            color = TextPrimaryDark
          )
        }
        Text(
          text = "${run.recordsDeleted} deleted of ${run.recordsEvaluated} evaluated • ${run.triggeredBy}",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
          color = TextMutedDark
        )
      }

      Text(
        text = run.status,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp, fontWeight = FontWeight.Bold),
        color = statusColor
      )
    }
  }
}
