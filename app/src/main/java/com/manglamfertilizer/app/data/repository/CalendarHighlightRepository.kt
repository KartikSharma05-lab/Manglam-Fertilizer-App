package com.manglamfertilizer.app.data.repository

import com.manglamfertilizer.app.data.model.DailyHighlight
import java.util.Calendar

class CalendarHighlightRepository {

  fun getTodayHighlight(): DailyHighlight {
    val cal = Calendar.getInstance()
    val month = cal.get(Calendar.MONTH) + 1 // 1-12
    val day = cal.get(Calendar.DAY_OF_MONTH)
    val year = cal.get(Calendar.YEAR)

    // Check known Indian national days, festivals, and major agrarian events
    return when {
      // Fixed National Days
      month == 1 && day == 26 -> DailyHighlight("Republic Day", "Honoring the Constitution of India.", true)
      month == 8 && day == 15 -> DailyHighlight("Independence Day", "Celebrating India's National Freedom.", true)
      month == 10 && day == 2 -> DailyHighlight("Gandhi Jayanti", "Remembering Mahatma Gandhi's ideals of truth and non-violence.", true)
      month == 12 && day == 23 -> DailyHighlight("Kisan Diwas (National Farmers Day)", "Honoring India's annadatas and agricultural community.", true)
      month == 4 && day == 14 -> DailyHighlight("Dr. B.R. Ambedkar Jayanti", "Commemorating the Architect of the Constitution.", true)
      month == 10 && day == 31 -> DailyHighlight("Rashtriya Ekta Diwas", "National Unity Day commemorating Sardar Patel.", true)
      month == 12 && day == 5 -> DailyHighlight("World Soil Day", "Advocating healthy soil management and sustainable agriculture.", true)

      // Variable Calendar Celebrations for 2026
      year == 2026 && month == 1 && day == 14 -> DailyHighlight("Makar Sankranti", "Harvest festival marking the sun's transition into Capricorn.", true)
      year == 2026 && month == 3 && day == 4 -> DailyHighlight("Holi", "Festival of colors and arrival of spring season.", true)
      year == 2026 && month == 3 && day == 20 -> DailyHighlight("Eid-ul-Fitr", "Festival of thanksgiving marking the end of Ramadan.", true)
      year == 2026 && month == 8 && day == 24 -> DailyHighlight("Janmashtami", "Festival celebrating the birth of Lord Krishna.", true)
      year == 2026 && month == 8 && day == 28 -> DailyHighlight("Raksha Bandhan", "Festival celebrating the sacred bond of protection.", true)
      year == 2026 && month == 10 && day == 20 -> DailyHighlight("Dussehra (Vijayadashami)", "Celebrating the triumph of righteousness over evil.", true)
      year == 2026 && month == 11 && day == 8 -> DailyHighlight("Diwali", "Festival of lights symbolizing hope and prosperity.", true)

      // Fallback for regular business days
      else -> DailyHighlight(
        title = "No special event today",
        description = "Regular business and agricultural trading day.",
        isSpecial = false
      )
    }
  }
}
