package com.manglamfertilizer.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.manglamfertilizer.app.data.model.DailyHighlight
import com.manglamfertilizer.app.data.model.GreetingInfo
import com.manglamfertilizer.app.data.model.User
import com.manglamfertilizer.app.ui.localization.LocalStrings
import com.manglamfertilizer.app.ui.theme.DarkBorder
import com.manglamfertilizer.app.ui.theme.DarkCard
import com.manglamfertilizer.app.ui.theme.DarkSurfaceElevated
import com.manglamfertilizer.app.ui.theme.Emerald400
import com.manglamfertilizer.app.ui.theme.GoldAmber
import com.manglamfertilizer.app.ui.theme.TextPrimaryDark
import com.manglamfertilizer.app.ui.theme.TextSecondaryDark
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun TodayHighlightCard(
  user: User?,
  greetingInfo: GreetingInfo,
  highlight: DailyHighlight,
  modifier: Modifier = Modifier
) {
  val strings = LocalStrings.current
  var currentTime by remember {
    mutableStateOf(SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()))
  }
  var currentDateStr by remember {
    mutableStateOf(SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date()))
  }

  val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
  val dynamicGreeting = when (hour) {
    in 5..11 -> strings.goodMorning
    in 12..16 -> strings.goodAfternoon
    in 17..20 -> strings.goodEvening
    else -> strings.welcome
  }

  // Update live clock dynamically
  LaunchedEffect(Unit) {
    while (true) {
      delay(30000L)
      currentTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date())
      currentDateStr = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale.getDefault()).format(Date())
    }
  }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .testTag("today_highlight_section")
  ) {
    // 1. Dynamic Greeting + Date + Live Time Pill
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        val configuredName = user?.name?.trim()?.takeIf { it.isNotBlank() }
        val greetingText = if (configuredName != null) {
          "$dynamicGreeting, $configuredName"
        } else {
          dynamicGreeting
        }

        Text(
          text = greetingText,
          style = MaterialTheme.typography.titleMedium.copy(
            fontSize = 17.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.2.sp
          ),
          color = TextPrimaryDark,
          maxLines = 1
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = currentDateStr,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 14.5.sp,
            fontWeight = FontWeight.Medium
          ),
          color = TextSecondaryDark
        )
      }

      Spacer(modifier = Modifier.width(8.dp))

      // Compact Time Pill
      Surface(
        shape = RoundedCornerShape(10.dp),
        color = DarkSurfaceElevated,
        border = BorderStroke(1.dp, DarkBorder)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = GoldAmber,
            modifier = Modifier.size(13.dp)
          )
          Spacer(modifier = Modifier.width(5.dp))
          Text(
            text = currentTime,
            style = MaterialTheme.typography.labelMedium.copy(
              fontSize = 12.5.sp,
              fontWeight = FontWeight.SemiBold
            ),
            color = TextPrimaryDark
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 2. Today's Highlight UI Card
    Surface(
      shape = RoundedCornerShape(14.dp),
      color = DarkCard,
      border = BorderStroke(1.dp, if (highlight.isSpecial) GoldAmber else DarkBorder),
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp)
        .testTag("today_highlight_card")
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 8.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = if (highlight.isSpecial) Icons.Default.Star else Icons.Default.CalendarMonth,
              contentDescription = null,
              tint = if (highlight.isSpecial) GoldAmber else Emerald400,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = strings.todayHighlight.uppercase(),
              style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
              ),
              color = if (highlight.isSpecial) GoldAmber else TextSecondaryDark
            )
          }

          if (highlight.isSpecial) {
            Surface(
              shape = RoundedCornerShape(4.dp),
              color = Color(0xFF3B2A06),
              border = BorderStroke(0.5.dp, GoldAmber)
            ) {
              Text(
                text = "SPECIAL",
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAmber,
                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(3.dp))

        Text(
          text = highlight.title,
          style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 13.5.sp,
            fontWeight = FontWeight.SemiBold
          ),
          color = TextPrimaryDark
        )

        Spacer(modifier = Modifier.height(1.dp))

        Text(
          text = highlight.description,
          style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 11.5.sp,
            fontWeight = FontWeight.Normal
          ),
          color = TextSecondaryDark,
          maxLines = 2
        )
      }
    }
  }
}
