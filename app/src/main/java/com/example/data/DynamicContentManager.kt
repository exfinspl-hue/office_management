package com.example.data

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

data class DynamicAnnouncement(
    val id: Int,
    val title: String,
    val content: String,
    val date: String,
    val category: String, // e.g. "General", "Alert", "Holiday", "Event"
    val priority: String  // e.g. "High", "Normal", "Low"
)

data class RemoteSyncResult(
    val success: Boolean,
    val message: String,
    val announcements: List<DynamicAnnouncement> = emptyList(),
    val companyName: String? = null,
    val workingHoursStart: String? = null,
    val workingHoursEnd: String? = null,
    val holidaysList: String? = null,
    val remoteEmployees: List<Employee> = emptyList()
)

class DynamicContentManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("office_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "DynamicContentManager"
        const val DEFAULT_SYNC_URL = "https://raw.githubusercontent.com/aistudio-cdn/office-management/main/dynamic_config.json"
        
        // Cache constants
        private const val PREF_CACHED_ANNOUNCEMENTS = "cached_announcements"
        private const val PREF_SYNC_URL = "dynamic_sync_url"
        private const val PREF_LAST_SYNC_TIME = "last_sync_time"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun getSyncUrl(): String {
        return prefs.getString(PREF_SYNC_URL, DEFAULT_SYNC_URL) ?: DEFAULT_SYNC_URL
    }

    fun setSyncUrl(url: String) {
        prefs.edit().putString(PREF_SYNC_URL, url).apply()
    }

    fun getLastSyncTime(): String {
        return prefs.getString(PREF_LAST_SYNC_TIME, "Never Synced") ?: "Never Synced"
    }

    /**
     * Retrieves stored announcements from the cache
     */
    fun getCachedAnnouncements(): List<DynamicAnnouncement> {
        val serialized = prefs.getString(PREF_CACHED_ANNOUNCEMENTS, null) ?: return getFallbackAnnouncements()
        return try {
            val arr = JSONArray(serialized)
            val list = mutableListOf<DynamicAnnouncement>()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(
                    DynamicAnnouncement(
                        id = obj.optInt("id", i),
                        title = obj.optString("title", ""),
                        content = obj.optString("content", ""),
                        date = obj.optString("date", ""),
                        category = obj.optString("category", "General"),
                        priority = obj.optString("priority", "Normal")
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse cached announcements", e)
            getFallbackAnnouncements()
        }
    }

    /**
     * Executes the API synchronization call to fetch remote configurations & databases
     */
    suspend fun triggerSynchronization(): RemoteSyncResult = withContextAndCatching {
        val url = getSyncUrl()
        Log.d(TAG, "Triggering sync to $url")

        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@use RemoteSyncResult(
                        success = false,
                        message = "HTTP Server Error: ${response.code}"
                    )
                }

                val bodyString = response.body?.string() ?: return@use RemoteSyncResult(
                    success = false,
                    message = "Server returned empty response"
                )

                val json = JSONObject(bodyString)

                // 1. Parse Announcements
                val annList = mutableListOf<DynamicAnnouncement>()
                if (json.has("announcements")) {
                    val arr = json.getJSONArray("announcements")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        annList.add(
                            DynamicAnnouncement(
                                id = obj.optInt("id", i),
                                title = obj.optString("title"),
                                content = obj.optString("content"),
                                date = obj.optString("date"),
                                category = obj.optString("category", "General"),
                                priority = obj.optString("priority", "Normal")
                            )
                        )
                    }
                    // Cache the announcements
                    prefs.edit().putString(PREF_CACHED_ANNOUNCEMENTS, arr.toString()).apply()
                }

                // 2. Parse Company Settings
                var companyName: String? = null
                var hrStart: String? = null
                var hrEnd: String? = null
                var holidaysList: String? = null

                if (json.has("company_settings")) {
                    val settings = json.getJSONObject("company_settings")
                    companyName = settings.optString("company_name").takeIf { it.isNotBlank() }
                    hrStart = settings.optString("working_hours_start").takeIf { it.isNotBlank() }
                    hrEnd = settings.optString("working_hours_end").takeIf { it.isNotBlank() }
                    holidaysList = settings.optString("holidays_list").takeIf { it.isNotBlank() }

                    // Apply settings directly to SharedPreferences dynamically!
                    val editor = prefs.edit()
                    companyName?.let { editor.putString("company_name", it) }
                    hrStart?.let { editor.putString("working_hours_start", it) }
                    hrEnd?.let { editor.putString("working_hours_end", it) }
                    holidaysList?.let { editor.putString("holidays_list", it) }
                    
                    if (settings.has("working_days_per_week")) {
                        editor.putInt("working_days_per_week", settings.getInt("working_days_per_week"))
                    }
                    if (settings.has("overtime_multiplier")) {
                        editor.putFloat("overtime_multiplier", settings.getDouble("overtime_multiplier").toFloat())
                    }
                    editor.apply()
                }

                // 3. Parse Remote Employees (Roster Synchronization)
                val rosters = mutableListOf<Employee>()
                if (json.has("remote_employees")) {
                    val arr = json.getJSONArray("remote_employees")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        rosters.add(
                            Employee(
                                id = obj.optInt("id", 0), // Use server layout ID if exists or generate
                                name = obj.getString("name"),
                                designation = obj.getString("designation"),
                                department = obj.optString("department", "Operations"),
                                email = obj.optString("email", ""),
                                phone = obj.optString("phone", ""),
                                salaryPerDay = obj.optDouble("salary_per_day", 1000.0),
                                joinDate = obj.optString("join_date", "2026-01-01"),
                                isActive = obj.optBoolean("is_active", true),
                                password = obj.optString("password", "1234")
                            )
                        )
                    }
                }

                // Update last sync time
                val nowString = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                prefs.edit().putString(PREF_LAST_SYNC_TIME, nowString).apply()

                RemoteSyncResult(
                    success = true,
                    message = "Successfully synchronized " +
                            "${annList.size} Announcements, " +
                            "${if (companyName != null) "Company profile, " else ""}" +
                            "${rosters.size} Employees.",
                    announcements = annList,
                    companyName = companyName,
                    workingHoursStart = hrStart,
                    workingHoursEnd = hrEnd,
                    holidaysList = holidaysList,
                    remoteEmployees = rosters
                )
            }
        } catch (e: IOException) {
            Log.e(TAG, "Network connection error", e)
            RemoteSyncResult(success = false, message = "Network Error: Could not connect to API server. Please check internet connection.")
        } catch (e: Exception) {
            Log.e(TAG, "Parsing configuration error", e)
            RemoteSyncResult(success = false, message = "Parsing Error: Config JSON was malformed. (${e.localizedMessage})")
        }
    }

    private suspend fun <T> withContextAndCatching(block: suspend () -> T): T {
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            block()
        }
    }

    /**
     * Fallback local system announcements when offline or before initial sync
     */
    fun getFallbackAnnouncements(): List<DynamicAnnouncement> {
        return listOf(
            DynamicAnnouncement(
                id = 101,
                title = "Welcome back to OfficePro Panel",
                content = "No app updates needed dynamically! All announcements, general bulletins, holidays, and settings update automatically over-the-air (OTA) via API syncing.",
                date = "2026-06-10",
                category = "General",
                priority = "Normal"
            ),
            DynamicAnnouncement(
                id = 102,
                title = "OTA Synchronization Active",
                content = "Head into Systems Settings panel to customize the dynamic content server URL and sync custom holiday lists or employee rosters dynamically.",
                date = "2026-06-10",
                category = "System",
                priority = "High"
            )
        )
    }

    /**
     * Returns a JSON mockup of dynamic announcements & settings so the user can see it works even without hosting a file
     */
    fun getMockServerJSONString(): String {
        return """
        {
          "announcements": [
            {
              "id": 1,
              "title": "🎉 Anniversary Dinner Tonight",
              "content": "To celebrate our annual success, join us for a company-sponsored dinner at Marriott Grand Ballroom starting at 7:00 PM. All departments invited!",
              "date": "2026-06-10",
              "category": "Event",
              "priority": "High"
            },
            {
              "id": 2,
              "title": "📅 Corporate Holiday Announcement",
              "content": "Next Friday is declared an official non-working day. Local attendance trackers will be automatically paused.",
              "date": "2026-06-12",
              "category": "Holiday",
              "priority": "High"
            },
            {
              "id": 3,
              "title": "🔒 Weekly PIN Security Audits",
              "content": "Please remind your respective staff members to keep their 4-digit security PIN updated via the portal settings to avoid identity overlapping.",
              "date": "2026-06-14",
              "category": "Alert",
              "priority": "Normal"
            }
          ],
          "company_settings": {
            "company_name": "OfficePro Enterprise",
            "working_hours_start": "08:30",
            "working_hours_end": "17:30",
            "working_days_per_week": 6,
            "overtime_multiplier": 1.75,
            "holidays_list": "2026-01-01,2026-05-01,2026-08-15,2026-12-25"
          },
          "remote_employees": [
            {
              "name": "Sarah Jenkins",
              "designation": "Staff UI/UX Designer",
              "department": "Engineering",
              "email": "sarah.j@officepro.com",
              "phone": "+91 98765 43210",
              "salary_per_day": 1800.0,
              "join_date": "2026-02-15",
              "is_active": true,
              "password": "9081"
            },
            {
              "name": "Arjun Malhotra",
              "designation": "Lead backend Developer",
              "department": "Engineering",
              "email": "arjun.m@officepro.com",
              "phone": "+91 87654 32109",
              "salary_per_day": 2200.0,
              "join_date": "2026-01-10",
              "is_active": true,
              "password": "5544"
            }
          ]
        }
        """.trimIndent()
    }
}
