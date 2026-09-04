package com.manglamfertilizer.app.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.ui.theme.DarkBg
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurface
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.Emerald500
import com.manglamfertilizer.app.ui.theme.Emerald900
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.TextMutedDark
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark

/**
 * Global Design Constants & Tokens for Manglam Fertilizer Application
 */
object ManglamDesignTokens {
  // Spacing Scale
  val Space2 = 2.dp
  val Space4 = 4.dp
  val Space6 = 6.dp
  val Space8 = 8.dp
  val Space10 = 10.dp
  val Space12 = 12.dp
  val Space14 = 14.dp
  val Space16 = 16.dp
  val Space20 = 20.dp
  val Space24 = 24.dp
  val Space32 = 32.dp

  // Component Heights
  val SearchBarHeight = 48.dp
  val InputMinHeight = 48.dp
  val ButtonHeight = 44.dp
  val ButtonHeightSmall = 34.dp
  val ButtonHeightLarge = 50.dp
  val HeaderHeight = 56.dp

  // Corner Radii
  val SearchBarRadius = RoundedCornerShape(12.dp)
  val CardRadius = RoundedCornerShape(14.dp)
  val InputRadius = RoundedCornerShape(10.dp)
  val ButtonRadius = RoundedCornerShape(10.dp)
  val DialogRadius = RoundedCornerShape(16.dp)
  val BadgeRadius = RoundedCornerShape(6.dp)
  val ActionButtonRadius = RoundedCornerShape(8.dp)
  val ChipRadius = RoundedCornerShape(20.dp)
  val FabRadius = RoundedCornerShape(16.dp)

  // Icon Sizing
  val IconSmall = 16.dp
  val IconMedium = 20.dp
  val IconLarge = 24.dp

  // Borders & Elevation
  val DefaultBorderWidth = 1.dp
  val FocusedBorderWidth = 1.5.dp
  val HeaderElevation = 4.dp
  val CardElevation = 0.dp

  // Accessibility & Responsive
  val MinTouchTarget = 48.dp
  val MaxContentWidth = 720.dp
  val MaxCardWidth = 640.dp
}

/**
 * Standardized Search Bar for all screens (Home, Inventory, Billing, Customers, Daily Accounts, Alerts, Reports)
 */
@Composable
fun ManglamSearchBar(
  value: String,
  onValueChange: (String) -> Unit,
  placeholder: String,
  modifier: Modifier = Modifier,
  leadingIcon: ImageVector = Icons.Default.Search,
  leadingIconTint: Color = Emerald400,
  trailingContent: (@Composable RowScope.() -> Unit)? = null,
  testTag: String = "search_bar",
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  enabled: Boolean = true
) {
  OutlinedTextField(
    value = value,
    onValueChange = onValueChange,
    placeholder = {
      Text(
        text = placeholder,
        style = TextStyle(
          fontSize = 13.sp,
          color = TextMutedDark,
          fontWeight = FontWeight.Normal
        )
      )
    },
    leadingIcon = {
      Icon(
        imageVector = leadingIcon,
        contentDescription = "Search",
        tint = leadingIconTint,
        modifier = Modifier.size(18.dp)
      )
    },
    trailingIcon = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier.padding(end = 4.dp)
      ) {
        if (value.isNotBlank()) {
          IconButton(
            onClick = { onValueChange("") },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Clear,
              contentDescription = "Clear Search",
              tint = TextMutedDark,
              modifier = Modifier.size(16.dp)
            )
          }
        }
        trailingContent?.invoke(this)
      }
    },
    singleLine = true,
    enabled = enabled,
    shape = ManglamDesignTokens.SearchBarRadius,
    keyboardOptions = keyboardOptions,
    keyboardActions = keyboardActions,
    colors = OutlinedTextFieldDefaults.colors(
      focusedBorderColor = Emerald400,
      unfocusedBorderColor = DarkBorder,
      focusedTextColor = TextPrimaryDark,
      unfocusedTextColor = TextPrimaryDark,
      focusedContainerColor = DarkSurfaceElevated,
      unfocusedContainerColor = DarkCard,
      cursorColor = Emerald400
    ),
    textStyle = TextStyle(
      fontSize = 13.5.sp,
      fontWeight = FontWeight.Normal,
      color = TextPrimaryDark
    ),
    modifier = modifier
      .fillMaxWidth()
      .height(ManglamDesignTokens.SearchBarHeight)
      .testTag(testTag)
  )
}

/**
 * Standardized App Top Bar with automatic status bar padding and consistent branding
 */
@Composable
fun ManglamTopHeader(
  title: String,
  subtitle: String? = null,
  onBack: (() -> Unit)? = null,
  actions: (@Composable RowScope.() -> Unit)? = null,
  modifier: Modifier = Modifier,
  testTag: String = "top_header"
) {
  Surface(
    color = DarkSurface,
    tonalElevation = ManglamDesignTokens.HeaderElevation,
    modifier = modifier
      .fillMaxWidth()
      .testTag(testTag)
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.weight(1f, fill = false)
        ) {
          if (onBack != null) {
            IconButton(
              onClick = onBack,
              modifier = Modifier
                .size(36.dp)
                .padding(end = 4.dp)
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimaryDark,
                modifier = Modifier.size(20.dp)
              )
            }
          }
          Column {
            Text(
              text = title,
              style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
              ),
              color = TextPrimaryDark
            )
            if (!subtitle.isNullOrBlank()) {
              Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondaryDark
              )
            }
          }
        }

        if (actions != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            actions()
          }
        }
      }
    }
  }
}

/**
 * Standardized Base Card with border and rounded corners
 */
@Composable
fun ManglamCard(
  modifier: Modifier = Modifier,
  shape: Shape = ManglamDesignTokens.CardRadius,
  colors: CardColors = CardDefaults.cardColors(containerColor = DarkCard),
  border: BorderStroke? = BorderStroke(ManglamDesignTokens.DefaultBorderWidth, DarkBorder),
  onClick: (() -> Unit)? = null,
  content: @Composable () -> Unit
) {
  if (onClick != null) {
    Card(
      onClick = onClick,
      shape = shape,
      colors = colors,
      border = border,
      modifier = modifier
    ) {
      content()
    }
  } else {
    Card(
      shape = shape,
      colors = colors,
      border = border,
      modifier = modifier
    ) {
      content()
    }
  }
}

/**
 * Standardized Section Card wrapper with consistent margin, padding, and optional header
 */
@Composable
fun ManglamSectionCard(
  modifier: Modifier = Modifier,
  title: String? = null,
  icon: ImageVector? = null,
  iconTint: Color = Emerald400,
  iconContainerColor: Color = Emerald900,
  titleColor: Color = Emerald400,
  headerTrailing: (@Composable RowScope.() -> Unit)? = null,
  contentPadding: PaddingValues = PaddingValues(12.dp),
  shape: Shape = ManglamDesignTokens.CardRadius,
  content: @Composable () -> Unit
) {
  ManglamCard(
    modifier = modifier.fillMaxWidth(),
    shape = shape
  ) {
    Column(modifier = Modifier.padding(contentPadding)) {
      if (title != null) {
        ManglamSectionHeader(
          title = title,
          icon = icon,
          iconTint = iconTint,
          iconContainerColor = iconContainerColor,
          titleColor = titleColor,
          trailingContent = headerTrailing,
          modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
      }
      content()
    }
  }
}

/**
 * Standardized Section Header with rounded icon container and title
 */
@Composable
fun ManglamSectionHeader(
  title: String,
  icon: ImageVector? = null,
  iconTint: Color = Emerald400,
  iconContainerColor: Color = Emerald900,
  titleColor: Color = Emerald400,
  trailingContent: (@Composable RowScope.() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.weight(1f, fill = false)
    ) {
      if (icon != null) {
        Surface(
          shape = CircleShape,
          color = iconContainerColor,
          modifier = Modifier.size(24.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = iconTint,
              modifier = Modifier.size(14.dp)
            )
          }
        }
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = title,
        style = MaterialTheme.typography.labelMedium.copy(
          fontWeight = FontWeight.Bold,
          letterSpacing = 0.5.sp
        ),
        color = titleColor
      )
    }

    if (trailingContent != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        trailingContent()
      }
    }
  }
}

/**
 * Standardized Form Input Field for consistent styling across dialogs, customer forms, product add/edit
 */
@Composable
fun ManglamTextField(
  value: String,
  onValueChange: (String) -> Unit,
  label: String? = null,
  placeholder: String? = null,
  leadingIcon: ImageVector? = null,
  trailingIcon: (@Composable () -> Unit)? = null,
  prefix: (@Composable () -> Unit)? = null,
  suffix: (@Composable () -> Unit)? = null,
  isError: Boolean = false,
  errorMessage: String? = null,
  singleLine: Boolean = true,
  maxLines: Int = if (singleLine) 1 else 4,
  keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
  keyboardActions: KeyboardActions = KeyboardActions.Default,
  visualTransformation: VisualTransformation = VisualTransformation.None,
  enabled: Boolean = true,
  readOnly: Boolean = false,
  shape: Shape = ManglamDesignTokens.InputRadius,
  modifier: Modifier = Modifier,
  testTag: String = "text_input"
) {
  Column(modifier = modifier.fillMaxWidth()) {
    OutlinedTextField(
      value = value,
      onValueChange = onValueChange,
      label = label?.let { { Text(it, fontSize = 12.sp) } },
      placeholder = placeholder?.let { { Text(it, fontSize = 12.5.sp, color = TextMutedDark) } },
      leadingIcon = leadingIcon?.let {
        {
          Icon(
            imageVector = it,
            contentDescription = null,
            tint = if (isError) Color(0xFFEF4444) else Emerald400,
            modifier = Modifier.size(18.dp)
          )
        }
      },
      trailingIcon = trailingIcon,
      prefix = prefix,
      suffix = suffix,
      isError = isError,
      singleLine = singleLine,
      maxLines = maxLines,
      enabled = enabled,
      readOnly = readOnly,
      keyboardOptions = keyboardOptions,
      keyboardActions = keyboardActions,
      visualTransformation = visualTransformation,
      shape = shape,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Emerald400,
        unfocusedBorderColor = DarkBorder,
        errorBorderColor = Color(0xFFEF4444),
        focusedTextColor = TextPrimaryDark,
        unfocusedTextColor = TextPrimaryDark,
        focusedContainerColor = DarkSurfaceElevated,
        unfocusedContainerColor = DarkCard,
        focusedLabelColor = Emerald400,
        unfocusedLabelColor = TextSecondaryDark,
        cursorColor = Emerald400
      ),
      textStyle = TextStyle(
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Normal,
        color = TextPrimaryDark
      ),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = ManglamDesignTokens.InputMinHeight)
        .testTag(testTag)
    )
    if (isError && !errorMessage.isNullOrBlank()) {
      Text(
        text = errorMessage,
        color = Color(0xFFEF4444),
        fontSize = 11.sp,
        modifier = Modifier.padding(start = 6.dp, top = 2.dp)
      )
    }
  }
}

/**
 * Standardized Primary Action Button
 */
@Composable
fun ManglamPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  colors: ButtonColors = ButtonDefaults.buttonColors(
    containerColor = Emerald500,
    contentColor = DarkBg,
    disabledContainerColor = DarkSurfaceElevated,
    disabledContentColor = TextMutedDark
  ),
  shape: Shape = ManglamDesignTokens.ButtonRadius,
  height: Dp = ManglamDesignTokens.ButtonHeight,
  testTag: String = "primary_button"
) {
  Button(
    onClick = onClick,
    enabled = enabled,
    shape = shape,
    colors = colors,
    modifier = modifier
      .height(height)
      .heightIn(min = 44.dp)
      .testTag(testTag)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.3.sp
      )
    }
  }
}

/**
 * Standardized Secondary / Outlined Action Button
 */
@Composable
fun ManglamSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  icon: ImageVector? = null,
  borderColor: Color = DarkBorder,
  textColor: Color = TextPrimaryDark,
  shape: Shape = ManglamDesignTokens.ButtonRadius,
  height: Dp = ManglamDesignTokens.ButtonHeight,
  testTag: String = "secondary_button"
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = shape,
    border = BorderStroke(1.dp, borderColor),
    colors = ButtonDefaults.outlinedButtonColors(
      contentColor = textColor,
      disabledContentColor = TextMutedDark
    ),
    modifier = modifier
      .height(height)
      .heightIn(min = 44.dp)
      .testTag(testTag)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
      }
      Text(
        text = text,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

/**
 * Standardized Floating Action Button with consistent safe area positioning
 */
@Composable
fun ManglamFloatingActionButton(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector = Icons.Default.Add,
  contentDescription: String? = "Add",
  text: String? = null,
  containerColor: Color = Emerald500,
  contentColor: Color = DarkBg,
  shape: Shape = ManglamDesignTokens.FabRadius,
  testTag: String = "floating_action_button"
) {
  FloatingActionButton(
    onClick = onClick,
    shape = shape,
    containerColor = containerColor,
    contentColor = contentColor,
    elevation = FloatingActionButtonDefaults.elevation(
      defaultElevation = 6.dp,
      pressedElevation = 8.dp
    ),
    modifier = modifier
      .testTag(testTag)
  ) {
    if (text != null) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        Icon(
          imageVector = icon,
          contentDescription = contentDescription,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = text,
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold
        )
      }
    } else {
      Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.size(24.dp)
      )
    }
  }
}

/**
 * Standardized Filter / Category Chip
 */
@Composable
fun ManglamFilterChip(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  count: Int? = null,
  leadingIcon: ImageVector? = null,
  testTag: String = "filter_chip"
) {
  val backgroundColor = if (selected) Emerald900 else DarkCard
  val borderColor = if (selected) Emerald400 else DarkBorder
  val textColor = if (selected) Emerald400 else TextSecondaryDark

  Surface(
    shape = ManglamDesignTokens.ChipRadius,
    color = backgroundColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
      .clip(ManglamDesignTokens.ChipRadius)
      .clickable(onClick = onClick)
      .testTag(testTag)
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center,
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      if (leadingIcon != null) {
        Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          tint = textColor,
          modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = text,
        fontSize = 11.5.sp,
        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        color = textColor
      )
      if (count != null && count > 0) {
        Spacer(modifier = Modifier.width(4.dp))
        Surface(
          shape = CircleShape,
          color = if (selected) Emerald400 else DarkBorder,
          modifier = Modifier.size(16.dp)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text(
              text = "$count",
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              color = if (selected) DarkBg else TextPrimaryDark
            )
          }
        }
      }
    }
  }
}

/**
 * Standardized Safe Scaffold container
 */
@Composable
fun ManglamSafeScaffold(
  modifier: Modifier = Modifier,
  topBar: @Composable () -> Unit = {},
  bottomBar: @Composable () -> Unit = {},
  floatingActionButton: @Composable () -> Unit = {},
  content: @Composable (PaddingValues) -> Unit
) {
  Scaffold(
    modifier = modifier
      .fillMaxSize()
      .background(DarkBg),
    containerColor = DarkBg,
    contentColor = TextPrimaryDark,
    topBar = topBar,
    bottomBar = bottomBar,
    floatingActionButton = floatingActionButton,
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
    content = content
  )
}

/**
 * Standardized Dialog Container
 * Ensures consistent dark container color (DarkCard), 16dp rounded corner shape,
 * proper IME padding, title styling, and standard action button order:
 * [ Dismiss / Cancel ] [ Primary Action ]
 */
@Composable
fun ManglamDialog(
  onDismissRequest: () -> Unit,
  title: String,
  modifier: Modifier = Modifier,
  titleIcon: ImageVector? = null,
  confirmButtonText: String = "Confirm",
  onConfirm: () -> Unit,
  confirmButtonEnabled: Boolean = true,
  confirmButtonColor: Color = Emerald500,
  dismissButtonText: String = "Cancel",
  onDismiss: (() -> Unit)? = onDismissRequest,
  showDismissButton: Boolean = true,
  testTag: String = "manglam_dialog",
  content: @Composable () -> Unit
) {
  AlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier
      .widthIn(max = 520.dp)
      .fillMaxWidth(0.92f)
      .imePadding()
      .testTag(testTag),
    shape = ManglamDesignTokens.DialogRadius,
    containerColor = DarkCard,
    title = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (titleIcon != null) {
          Icon(
            imageVector = titleIcon,
            contentDescription = null,
            tint = Emerald400,
            modifier = Modifier.size(22.dp)
          )
        }
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = TextPrimaryDark
        )
      }
    },
    text = {
      Box(modifier = Modifier.fillMaxWidth()) {
        content()
      }
    },
    dismissButton = if (showDismissButton && onDismiss != null) {
      {
        TextButton(
          onClick = onDismiss,
          shape = ManglamDesignTokens.ButtonRadius,
          modifier = Modifier
            .heightIn(min = 40.dp)
            .testTag("${testTag}_dismiss")
        ) {
          Text(
            text = dismissButtonText,
            color = TextSecondaryDark,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
          )
        }
      }
    } else null,
    confirmButton = {
      Button(
        onClick = onConfirm,
        enabled = confirmButtonEnabled,
        shape = ManglamDesignTokens.ButtonRadius,
        colors = ButtonDefaults.buttonColors(
          containerColor = confirmButtonColor,
          contentColor = DarkBg,
          disabledContainerColor = DarkSurfaceElevated,
          disabledContentColor = TextMutedDark
        ),
        modifier = Modifier
          .heightIn(min = 40.dp)
          .testTag("${testTag}_confirm")
      ) {
        Text(
          text = confirmButtonText,
          fontWeight = FontWeight.Bold,
          fontSize = 13.sp
        )
      }
    }
  )
}

/**
 * Standardized Confirmation Alert Dialog
 */
@Composable
fun ManglamConfirmDialog(
  onDismissRequest: () -> Unit,
  title: String,
  message: String,
  onConfirm: () -> Unit,
  confirmButtonText: String = "Confirm",
  confirmButtonColor: Color = Emerald500,
  dismissButtonText: String = "Cancel",
  onDismiss: () -> Unit = onDismissRequest,
  icon: ImageVector? = null,
  testTag: String = "confirm_dialog"
) {
  ManglamDialog(
    onDismissRequest = onDismissRequest,
    title = title,
    titleIcon = icon,
    confirmButtonText = confirmButtonText,
    confirmButtonColor = confirmButtonColor,
    onConfirm = onConfirm,
    dismissButtonText = dismissButtonText,
    onDismiss = onDismiss,
    testTag = testTag
  ) {
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = TextSecondaryDark
    )
  }
}

