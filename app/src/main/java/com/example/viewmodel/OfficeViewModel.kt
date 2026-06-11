package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.utils.PayslipCalculator
import com.example.utils.PdfService
import com.example.utils.ReportService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class OfficeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = OfficeRepository(
        db.employeeDao(),
        db.attendanceDao(),
        db.payslipDao()
    )

    private val prefs = application.getSharedPreferences("office_prefs", Context.MODE_PRIVATE)

    // --- Authentication State ---
    private val _isSetupComplete = MutableStateFlow(false)
    val isSetupComplete: StateFlow<Boolean> = _isSetupComplete.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _portalEmployeeId = MutableStateFlow<Int?>(null)
    val portalEmployeeId: StateFlow<Int?> = _portalEmployeeId.asStateFlow()

    private val _isPortalUnlocked = MutableStateFlow(false)
    val isPortalUnlocked: StateFlow<Boolean> = _isPortalUnlocked.asStateFlow()

    private val _adminName = MutableStateFlow("Admin")
    val adminName: StateFlow<String> = _adminName.asStateFlow()

    private val _companyName = MutableStateFlow("OfficePro")
    val companyName: StateFlow<String> = _companyName.asStateFlow()

    private val _workingHoursStart = MutableStateFlow("09:00")
    val workingHoursStart: StateFlow<String> = _workingHoursStart.asStateFlow()

    private val _workingHoursEnd = MutableStateFlow("18:00")
    val workingHoursEnd: StateFlow<String> = _workingHoursEnd.asStateFlow()

    private val _workingDaysPerWeek = MutableStateFlow(5)
    val workingDaysPerWeek: StateFlow<Int> = _workingDaysPerWeek.asStateFlow()

    private val _overtimeMultiplier = MutableStateFlow(1.5)
    val overtimeMultiplier: StateFlow<Double> = _overtimeMultiplier.asStateFlow()

    private val _holidays = MutableStateFlow<List<String>>(emptyList())
    val holidays: StateFlow<List<String>> = _holidays.asStateFlow()

    // Login security
    private var failedAttempts = 0
    private val _lockoutTimeRemaining = MutableStateFlow(0)
    val lockoutTimeRemaining: StateFlow<Int> = _lockoutTimeRemaining.asStateFlow()

    private var lockoutTimer: Timer? = null

    // --- Observable Room Data Streams ---
    val employees: StateFlow<List<Employee>> = repository.allEmployees
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val attendance: StateFlow<List<Attendance>> = repository.allAttendance
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val payslips: StateFlow<List<Payslip>> = repository.allPayslips
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Dashboard Specific Stats State ---
    private val _dashboardStatsLoaded = MutableStateFlow(false)
    val dashboardStatsLoaded: StateFlow<Boolean> = _dashboardStatsLoaded.asStateFlow()

    // Selected month-year for reports & analytics
    val currentSelectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH) + 1)
    val currentSelectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))

    // --- Dynamic API Syncing State ---
    private val dynamicContentManager = DynamicContentManager(application)

    private val _announcements = MutableStateFlow<List<DynamicAnnouncement>>(emptyList())
    val announcements: StateFlow<List<DynamicAnnouncement>> = _announcements.asStateFlow()

    private val _syncUrl = MutableStateFlow(dynamicContentManager.getSyncUrl())
    val syncUrl: StateFlow<String> = _syncUrl.asStateFlow()

    private val _lastSyncTime = MutableStateFlow(dynamicContentManager.getLastSyncTime())
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    private val _syncResultState = MutableStateFlow<String?>(null)
    val syncResultState: StateFlow<String?> = _syncResultState.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    init {
        loadSettings()
        checkSetupStatus()
        loadCachedAnnouncements()
    }

    private fun loadCachedAnnouncements() {
        _announcements.value = dynamicContentManager.getCachedAnnouncements()
    }

    fun updateSyncUrl(url: String) {
        dynamicContentManager.setSyncUrl(url)
        _syncUrl.value = url
    }

    fun syncFromRemote() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncResultState.value = "Connecting to API Sync Server..."
            try {
                val result = dynamicContentManager.triggerSynchronization()
                if (result.success) {
                    _announcements.value = result.announcements.ifEmpty { dynamicContentManager.getFallbackAnnouncements() }
                    _lastSyncTime.value = dynamicContentManager.getLastSyncTime()
                    
                    // Force settings reload
                    loadSettings()
                    
                    // Insert remote database roster if any
                    if (result.remoteEmployees.isNotEmpty()) {
                        for (emp in result.remoteEmployees) {
                            val currentList = repository.allEmployees.first()
                            val exists = currentList.any { 
                                (it.email.lowercase() == emp.email.lowercase() && emp.email.isNotEmpty()) || 
                                it.name.lowercase() == emp.name.lowercase() 
                            }
                            if (!exists) {
                                repository.insertEmployee(emp)
                            }
                        }
                    }
                    
                    _syncResultState.value = "Success! ${result.message}"
                } else {
                    _syncResultState.value = "Failed: ${result.message}"
                }
            } catch (e: Exception) {
                _syncResultState.value = "Sync Exception: ${e.localizedMessage}"
                Log.e("OfficeViewModel", "Sync exception", e)
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun applyMockServerPayload() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncResultState.value = "Simulating online database sync..."
            kotlinx.coroutines.delay(1000)
            try {
                val mockJsonString = dynamicContentManager.getMockServerJSONString()
                val json = org.json.JSONObject(mockJsonString)
                
                // Parse Announcements
                val annList = mutableListOf<DynamicAnnouncement>()
                if (json.has("announcements")) {
                    val arr = json.getJSONArray("announcements")
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        annList.add(
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
                    
                    prefs.edit().putString("cached_announcements", arr.toString()).apply()
                    _announcements.value = annList
                }

                // Parse Settings
                if (json.has("company_settings")) {
                    val settings = json.getJSONObject("company_settings")
                    val compName = settings.optString("company_name", "OfficePro Enterprise")
                    val hrStart = settings.optString("working_hours_start", "08:30")
                    val hrEnd = settings.optString("working_hours_end", "17:30")
                    val holidaysList = settings.optString("holidays_list", "")

                    val editor = prefs.edit()
                    editor.putString("company_name", compName)
                    editor.putString("working_hours_start", hrStart)
                    editor.putString("working_hours_end", hrEnd)
                    editor.putString("holidays_list", holidaysList)
                    if (settings.has("working_days_per_week")) {
                        editor.putInt("working_days_per_week", settings.getInt("working_days_per_week"))
                    }
                    if (settings.has("overtime_multiplier")) {
                        editor.putFloat("overtime_multiplier", settings.getDouble("overtime_multiplier").toFloat())
                    }
                    editor.apply()
                }

                // Sync Employees
                if (json.has("remote_employees")) {
                    val rosterAr = json.getJSONArray("remote_employees")
                    for (i in 0 until rosterAr.length()) {
                        val obj = rosterAr.getJSONObject(i)
                        val emp = Employee(
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
                        
                        val currentList = repository.allEmployees.first()
                        if (!currentList.any { it.name.lowercase() == emp.name.lowercase() }) {
                            repository.insertEmployee(emp)
                        }
                    }
                }

                // Update sync time
                val nowString = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                prefs.edit().putString("last_sync_time", nowString).apply()
                _lastSyncTime.value = nowString

                // Reload local settings
                loadSettings()

                _syncResultState.value = "Mock Sync Loaded: ${annList.size} Bulletins, company configurations updated!"
            } catch (e: Exception) {
                _syncResultState.value = "Mock Error: ${e.localizedMessage}"
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun clearSyncStatus() {
        _syncResultState.value = null
    }

    fun generateCurrentStateJSON(onReady: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val json = org.json.JSONObject()

                // 1. Announcements
                val annArr = org.json.JSONArray()
                _announcements.value.forEach { ann ->
                    val annObj = org.json.JSONObject()
                    annObj.put("id", ann.id)
                    annObj.put("title", ann.title)
                    annObj.put("content", ann.content)
                    annObj.put("date", ann.date)
                    annObj.put("category", ann.category)
                    annObj.put("priority", ann.priority)
                    annArr.put(annObj)
                }
                json.put("announcements", annArr)

                // 2. Company Settings
                val settingsObj = org.json.JSONObject()
                settingsObj.put("company_name", _companyName.value)
                settingsObj.put("working_hours_start", _workingHoursStart.value)
                settingsObj.put("working_hours_end", _workingHoursEnd.value)
                settingsObj.put("working_days_per_week", _workingDaysPerWeek.value)
                settingsObj.put("overtime_multiplier", _overtimeMultiplier.value)
                
                val holidaysCsv = _holidays.value.joinToString(",")
                settingsObj.put("holidays_list", holidaysCsv)
                json.put("company_settings", settingsObj)

                // 3. Employees (Roster)
                val empArray = org.json.JSONArray()
                val localEmps = repository.allEmployees.first()
                localEmps.forEach { emp ->
                    val empObj = org.json.JSONObject()
                    empObj.put("id", emp.id)
                    empObj.put("name", emp.name)
                    empObj.put("designation", emp.designation)
                    empObj.put("department", emp.department)
                    empObj.put("email", emp.email)
                    empObj.put("phone", emp.phone)
                    empObj.put("salary_per_day", emp.salaryPerDay)
                    empObj.put("join_date", emp.joinDate)
                    empObj.put("is_active", emp.isActive)
                    empObj.put("password", emp.password)
                    empArray.put(empObj)
                }
                json.put("remote_employees", empArray)

                val prettyJSON = json.toString(2)
                onReady(prettyJSON)
            } catch (e: Exception) {
                onReady("Error generating configuration JSON: ${e.localizedMessage}")
            }
        }
    }

    // --- Settings & Auth Logic ---
    private fun loadSettings() {
        _adminName.value = prefs.getString("admin_name", "Admin") ?: "Admin"
        _companyName.value = prefs.getString("company_name", "OfficePro") ?: "OfficePro"
        _workingHoursStart.value = prefs.getString("working_hours_start", "09:00") ?: "09:00"
        _workingHoursEnd.value = prefs.getString("working_hours_end", "18:00") ?: "18:00"
        _workingDaysPerWeek.value = prefs.getInt("working_days_per_week", 5)
        _overtimeMultiplier.value = prefs.getFloat("overtime_multiplier", 1.5f).toDouble()
        
        val holidayCsv = prefs.getString("holidays_list", "") ?: ""
        if (holidayCsv.isNotEmpty()) {
            _holidays.value = holidayCsv.split(",").filter { it.isNotBlank() }
        } else {
            _holidays.value = emptyList()
        }

        val savedPortalId = prefs.getInt("portal_employee_id", -1)
        if (savedPortalId != -1) {
            _portalEmployeeId.value = savedPortalId
        }
    }

    private fun checkSetupStatus() {
        val hasPin = prefs.contains("admin_pin")
        _isSetupComplete.value = hasPin
    }

    fun completeFirstTimeSetup(company: String, admin: String, pin: String) {
        prefs.edit()
            .putString("company_name", company)
            .putString("admin_name", admin)
            .putString("admin_pin", pin) // Simple local PIN storage
            .apply()

        _companyName.value = company
        _adminName.value = admin
        _isSetupComplete.value = true
        _isAuthenticated.value = true
    }

    fun login(pin: String): Boolean {
        if (_lockoutTimeRemaining.value > 0) return false

        val savedPin = prefs.getString("admin_pin", "") ?: ""
        if (pin == savedPin) {
            failedAttempts = 0
            _isAuthenticated.value = true
            return true
        } else {
            failedAttempts++
            if (failedAttempts >= 3) {
                startLockout()
            }
            return false
        }
    }

    fun logout() {
        _isAuthenticated.value = false
    }

    fun setPortalEmployee(employeeId: Int?) {
        _portalEmployeeId.value = employeeId
        _isPortalUnlocked.value = false
        if (employeeId != null) {
            prefs.edit().putInt("portal_employee_id", employeeId).apply()
        } else {
            prefs.edit().remove("portal_employee_id").apply()
        }
    }

    fun unlockPortal(password: String, employee: Employee): Boolean {
        if (employee.password == password) {
            _isPortalUnlocked.value = true
            return true
        }
        return false
    }

    fun lockPortal() {
        _isPortalUnlocked.value = false
    }

    fun changePin(oldPin: String, newPin: String): Boolean {
        val savedPin = prefs.getString("admin_pin", "") ?: ""
        if (oldPin == savedPin) {
            prefs.edit().putString("admin_pin", newPin).apply()
            return true
        }
        return false
    }

    fun updateCompanySettings(name: String, hrsStart: String, hrsEnd: String, daysWeek: Int) {
        prefs.edit()
            .putString("company_name", name)
            .putString("working_hours_start", hrsStart)
            .putString("working_hours_end", hrsEnd)
            .putInt("working_days_per_week", daysWeek)
            .apply()
        
        _companyName.value = name
        _workingHoursStart.value = hrsStart
        _workingHoursEnd.value = hrsEnd
        _workingDaysPerWeek.value = daysWeek
    }

    fun addHoliday(dateString: String) { // dateString in yyyy-MM-dd
        val currentList = _holidays.value.toMutableList()
        if (!currentList.contains(dateString)) {
            currentList.add(dateString)
            _holidays.value = currentList
            prefs.edit().putString("holidays_list", currentList.joinToString(",")).apply()
        }
    }

    fun removeHoliday(dateString: String) {
        val currentList = _holidays.value.toMutableList()
        if (currentList.remove(dateString)) {
            _holidays.value = currentList
            prefs.edit().putString("holidays_list", currentList.joinToString(",")).apply()
        }
    }

    private fun startLockout() {
        _lockoutTimeRemaining.value = 30
        lockoutTimer?.cancel()
        lockoutTimer = Timer()
        lockoutTimer?.scheduleAtFixedRate(object : TimerTask() {
            override fun run() {
                viewModelScope.launch {
                    val currentVal = _lockoutTimeRemaining.value
                    if (currentVal > 1) {
                        _lockoutTimeRemaining.value = currentVal - 1
                    } else {
                        _lockoutTimeRemaining.value = 0
                        failedAttempts = 0
                        lockoutTimer?.cancel()
                    }
                }
            }
        }, 1000, 1000)
    }

    // --- Employee Actions ---
    fun addEmployee(employee: Employee, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertEmployee(employee)
            onComplete()
        }
    }

    fun updateEmployee(employee: Employee, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateEmployee(employee)
            onComplete()
        }
    }

    fun deleteEmployee(employee: Employee, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // Hard deletion or soft? Let's just do hard deletion to keep database clean
            repository.deleteEmployee(employee)
            onComplete()
        }
    }

    // --- Attendance Actions ---
    fun markAttendance(
        employeeId: Int,
        dateString: String, // yyyy-MM-dd
        status: String,
        checkIn: String?,
        checkOut: String?,
        notes: String?,
        checkInSelfie: String? = null,
        checkOutSelfie: String? = null,
        checkInDistance: Double? = null,
        checkOutDistance: Double? = null,
        onComplete: () -> Unit = {}
    ) {
        viewModelScope.launch {
            val existing = repository.getAttendanceForEmployeeAndDate(employeeId, dateString)
            if (existing != null) {
                repository.updateAttendance(
                    existing.copy(
                        status = status,
                        checkInTime = checkIn,
                        checkOutTime = checkOut,
                        notes = notes,
                        checkInSelfie = checkInSelfie ?: existing.checkInSelfie,
                        checkOutSelfie = checkOutSelfie ?: existing.checkOutSelfie,
                        checkInDistance = checkInDistance ?: existing.checkInDistance,
                        checkOutDistance = checkOutDistance ?: existing.checkOutDistance
                    )
                )
            } else {
                repository.insertAttendance(
                    Attendance(
                        employeeId = employeeId,
                        date = dateString,
                        checkInTime = checkIn,
                        checkOutTime = checkOut,
                        status = status,
                        notes = notes,
                        checkInSelfie = checkInSelfie,
                        checkOutSelfie = checkOutSelfie,
                        checkInDistance = checkInDistance,
                        checkOutDistance = checkOutDistance
                    )
                )
            }
            onComplete()
        }
    }

    fun bulkMarkPresentToday(employeesList: List<Employee>, dateString: String) {
        viewModelScope.launch {
            for (emp in employeesList) {
                val existing = repository.getAttendanceForEmployeeAndDate(emp.id, dateString)
                if (existing == null) {
                    repository.insertAttendance(
                        Attendance(
                            employeeId = emp.id,
                            date = dateString,
                            status = "Present",
                            checkInTime = "09:00",
                            checkOutTime = "18:00"
                        )
                    )
                }
            }
        }
    }

    // Get attendance list for an employee in a month (matches month parameter string formatted as "yyyy-MM")
    fun getAttendanceForEmployeeInMonth(employeeId: Int, monthYear: String): Flow<List<Attendance>> {
        return repository.getAttendanceForEmployeeInMonth(employeeId, monthYear)
    }

    // --- Payslip Actions ---
    fun generatePayslip(
        employee: Employee,
        month: Int,
        year: Int,
        onComplete: (Payslip?) -> Unit = {}
    ) {
        viewModelScope.launch {
            // Fetch attendance for this month
            val monthStr = String.format(Locale.US, "%02d", month)
            val monthQuery = "$year-$monthStr"
            
            // Get from database list matching month query
            val list = repository.allAttendance.first().filter { 
                it.employeeId == employee.id && it.date.startsWith(monthQuery)
            }

            // Calculate metrics
            val result = PayslipCalculator.calculate(employee, month, year, list)

            // Insert payslip model
            val existing = repository.allPayslips.first().find { 
                it.employeeId == employee.id && it.month == month && it.year == year 
            }

            val payslipToSave = Payslip(
                id = existing?.id ?: 0,
                employeeId = employee.id,
                month = month,
                year = year,
                totalDaysWorked = result.presentDays,
                halfDays = result.halfDays,
                leaves = result.absentDays, // Mapping absences to leaves
                grossSalary = result.grossSalary,
                deductions = result.deductions,
                netSalary = result.netSalary,
                generatedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                isPaid = existing?.isPaid ?: false
            )

            val id = repository.insertPayslip(payslipToSave)
            val savedPayslip = payslipToSave.copy(id = if (existing == null) id.toInt() else existing.id)
            onComplete(savedPayslip)
        }
    }

    fun markPayslipAsPaid(payslip: Payslip) {
        viewModelScope.launch {
            repository.updatePayslip(payslip.copy(isPaid = true))
        }
    }

    fun deletePayslip(payslip: Payslip) {
        viewModelScope.launch {
            repository.deletePayslip(payslip)
        }
    }

    // Export PDF Helper
    fun getPayslipPdfFile(context: Context, employee: Employee, payslip: Payslip): File? {
        return PdfService.generatePayslipPdf(context, employee, payslip, _companyName.value)
    }

    // Export CSV Helper
    fun exportReport(context: Context, month: Int, year: Int, onComplete: (File?) -> Unit) {
        viewModelScope.launch {
            val listEmp = repository.allEmployees.first()
            val listAtt = repository.allAttendance.first()
            val monthStr = String.format(Locale.US, "%02d", month)
            val monthPrefix = "$year-$monthStr"

            val records = listEmp.map { emp ->
                val attForEmp = listAtt.filter { it.employeeId == emp.id && it.date.startsWith(monthPrefix) }
                val calc = PayslipCalculator.calculate(emp, month, year, attForEmp)

                // Overtime work hours calculated based on checkout
                var overtimeMinutes = 0.0
                for (att in attForEmp) {
                    if (att.status == "Present" && att.checkOutTime != null) {
                        val parts = att.checkOutTime.split(":")
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull() ?: 0
                            val min = parts[1].toIntOrNull() ?: 0
                            if (hour >= 18) {
                                overtimeMinutes += (hour - 18) * 60 + min
                            }
                        }
                    }
                }
                val totalHours = (calc.presentDays * 8.0) + (calc.halfDays * 4.0) + (overtimeMinutes / 60.0)

                ReportService.CsvReportRecord(
                    name = emp.name,
                    department = emp.department,
                    presentDays = calc.presentDays,
                    halfDays = calc.halfDays,
                    absentDays = calc.absentDays,
                    leaveDays = calc.leaveDays,
                    totalWorkHours = totalHours,
                    grossSalary = calc.grossSalary,
                    deductions = calc.deductions,
                    netSalary = calc.netSalary
                )
            }

            val file = withContext(Dispatchers.IO) {
                ReportService.exportMonthlyReport(context, month, year, records)
            }
            onComplete(file)
        }
    }

    // --- Demo Data Seeding ---
    fun seedDemoData(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            // 1. Clear database first
            db.clearAllTables()

            // 2. Insert 5 Employees
            val employeesList = listOf(
                Employee(name = "Kunal Sharma", designation = "Senior Android Dev", department = "Engineering", email = "kunal@officepro.com", phone = "+919876543210", salaryPerDay = 1500.0, joinDate = "2026-01-15"),
                Employee(name = "Anjali Nair", designation = "HR Lead", department = "HR", email = "anjali@officepro.com", phone = "+919876543211", salaryPerDay = 1200.0, joinDate = "2026-02-10"),
                Employee(name = "Vikram Aditya", designation = "Sales Executive", department = "Sales", email = "vikram@officepro.com", phone = "+919876543212", salaryPerDay = 900.0, joinDate = "2026-03-01"),
                Employee(name = "Priya Gopal", designation = "Marketing Lead", department = "Marketing", email = "priya@officepro.com", phone = "+919876543213", salaryPerDay = 1300.0, joinDate = "2026-01-20"),
                Employee(name = "Rohan Verma", designation = "Junior Dev", department = "Engineering", email = "rohan@officepro.com", phone = "+919876543214", salaryPerDay = 1000.0, joinDate = "2026-04-01")
            )

            val insertedIds = mutableListOf<Int>()
            for (emp in employeesList) {
                val id = repository.insertEmployee(emp)
                insertedIds.add(id.toInt())
            }

            // 3. Insert 30 days of attendance for each employee
            // Let's seed for April and May 2026
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val random = Random()

            // Date Range: 2026-04-20 to 2026-06-08 (approx 50 days)
            val cal = Calendar.getInstance()
            cal.set(2026, Calendar.APRIL, 20)
            
            val statusOptions = listOf("Present", "Present", "Present", "Present", "Present", "Present", "Present", "Half-Day", "Absent", "Leave")

            while (cal.get(Calendar.YEAR) == 2026 && 
                   (cal.get(Calendar.MONTH) < Calendar.JUNE || 
                    (cal.get(Calendar.MONTH) == Calendar.JUNE && cal.get(Calendar.DAY_OF_MONTH) <= 8))
            ) {
                val dateStr = sdf.format(cal.time)
                val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

                if (dayOfWeek != Calendar.SUNDAY) { // No attendance on Sunday
                    for (empId in insertedIds) {
                        val randPct = random.nextInt(100)
                        val status = when {
                            randPct < 85 -> "Present"
                            randPct < 90 -> "Half-Day"
                            randPct < 95 -> "Absent"
                            else -> "Leave"
                        }

                        val checkIn = if (status == "Present" || status == "Half-Day") {
                            // Check in between 08:30 and 09:15
                            val hour = 8 + random.nextInt(2)
                            val min = if (hour == 8) 30 + random.nextInt(30) else random.nextInt(16)
                            String.format(Locale.US, "%02d:%02d", hour, min)
                        } else null

                        val checkOut = if (status == "Present") {
                            // Check out between 17:30 and 18:45
                            val hour = 17 + random.nextInt(2)
                            val min = if (hour == 17) 30 + random.nextInt(30) else random.nextInt(46)
                            String.format(Locale.US, "%02d:%02d", hour, min)
                        } else if (status == "Half-Day") {
                            "13:00"
                        } else null

                        val notes = if (status == "Leave") "Family personal reason" else if (status == "Absent") "No report" else null

                        repository.insertAttendance(
                            Attendance(
                                employeeId = empId,
                                date = dateStr,
                                checkInTime = checkIn,
                                checkOutTime = checkOut,
                                status = status,
                                notes = notes
                            )
                        )
                    }
                }
                cal.add(Calendar.DAY_OF_YEAR, 1)
            }

            // 4. Generate payslips for April 2026 and May 2026
            val loadedEmps = repository.allEmployees.first()
            val loadedAtt = repository.allAttendance.first()

            val monthsToSeed = listOf(
                Pair(4, 2026), // April
                Pair(5, 2026)  // May
            )

            for (emp in loadedEmps) {
                for ((m, y) in monthsToSeed) {
                    val mStr = String.format(Locale.US, "%02d", m)
                    val attForMonth = loadedAtt.filter { it.employeeId == emp.id && it.date.startsWith("$y-$mStr") }
                    val calcResult = PayslipCalculator.calculate(emp, m, y, attForMonth)

                    repository.insertPayslip(
                        Payslip(
                            employeeId = emp.id,
                            month = m,
                            year = y,
                            totalDaysWorked = calcResult.presentDays,
                            halfDays = calcResult.halfDays,
                            leaves = calcResult.absentDays,
                            grossSalary = calcResult.grossSalary,
                            deductions = calcResult.deductions,
                            netSalary = calcResult.netSalary,
                            generatedDate = "$y-$mStr-28",
                            isPaid = true // Auto-mark previous slips as Paid
                        )
                    )
                }
            }

            onComplete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        lockoutTimer?.cancel()
    }
}
