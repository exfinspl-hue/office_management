package com.example.ui.screens

import kotlin.math.absoluteValue
import androidx.compose.animation.core.*
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.FileProvider
import com.example.data.Attendance
import com.example.data.Employee
import com.example.data.Payslip
import com.example.ui.theme.*
import com.example.viewmodel.OfficeViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

// --- Navigation States ---
sealed class AppScreen {
    object Splash : AppScreen()
    object Setup : AppScreen()
    object ZoneSelection : AppScreen()
    object Login : AppScreen()
    object Main : AppScreen()
    object EmployeePortal : AppScreen()
}

sealed class SubScreen {
    object DashboardHome : SubScreen()
    object EmployeesList : SubScreen()
    data class EmployeeDetail(val employeeId: Int) : SubScreen()
    data class EmployeeAddEdit(val employeeId: Int? = null) : SubScreen()
    object AttendanceHome : SubScreen()
    data class AttendanceHistory(val employeeId: Int? = null) : SubScreen()
    object PayslipsHome : SubScreen()
    data class PayslipDetail(val employeeId: Int, val month: Int, val year: Int) : SubScreen()
    object SettingsHome : SubScreen()
}

@Composable
fun OfficeRootView(viewModel: OfficeViewModel) {
    val isSetup by viewModel.isSetupComplete.collectAsState()
    val isAuth by viewModel.isAuthenticated.collectAsState()

    var currentAppScreen by remember { mutableStateOf<AppScreen>(AppScreen.Splash) }

    LaunchedEffect(isSetup, isAuth) {
        if (currentAppScreen == AppScreen.Splash) {
            // Splash screen delay
            kotlinx.coroutines.delay(1800)
            if (!isSetup) {
                currentAppScreen = AppScreen.Setup
            } else {
                currentAppScreen = AppScreen.ZoneSelection
            }
        } else {
            if (!isSetup) {
                currentAppScreen = AppScreen.Setup
            } else if (isAuth) {
                currentAppScreen = AppScreen.Main
            } else {
                // If not authenticated and we are in Main admin area, send back to ZoneSelection screen
                if (currentAppScreen == AppScreen.Main) {
                    currentAppScreen = AppScreen.ZoneSelection
                }
            }
        }
    }

    Crossfade(targetState = currentAppScreen, label = "ScreenTransition") { screen ->
        when (screen) {
            AppScreen.Splash -> SplashScreen()
            AppScreen.Setup -> SetupScreen(viewModel)
            AppScreen.ZoneSelection -> ZoneSelectionScreen(
                viewModel = viewModel,
                onSelectAdmin = {
                    currentAppScreen = AppScreen.Login
                },
                onSelectEmployee = {
                    currentAppScreen = AppScreen.EmployeePortal
                }
            )
            AppScreen.Login -> LoginScreen(
                viewModel = viewModel,
                onBack = {
                    currentAppScreen = AppScreen.ZoneSelection
                }
            )
            AppScreen.Main -> MainScaffold(viewModel)
            AppScreen.EmployeePortal -> EmployeePortalScreen(
                viewModel = viewModel,
                onBack = {
                    currentAppScreen = AppScreen.ZoneSelection
                }
            )
        }
    }
}

// --- Splash Screen ---
@Composable
fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyPrimary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.animateContentSize()
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CorporateFare,
                    contentDescription = "Office Logo",
                    tint = NavyPrimary,
                    modifier = Modifier.size(56.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "OfficePro",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "SYSTEM ENGINE",
                color = TealAccent,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                letterSpacing = 4.sp
            )
        }
    }
}

// --- First Time Setup Screen ---
@Composable
fun SetupScreen(viewModel: OfficeViewModel) {
    var company by remember { mutableStateOf("OfficePro") }
    var adminName by remember { mutableStateOf("Admin") }
    var pin by remember { mutableStateOf("1234") }
    var confirmPin by remember { mutableStateOf("1234") }
    var errorMsg by remember { mutableStateOf("") }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBlueBg)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Filled.Business,
                    contentDescription = "Setup Icon",
                    tint = NavyPrimary,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Configure Office",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = "First-time Administrative Setup",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = company,
                    onValueChange = { company = it },
                    label = { Text("Company Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_company_name")
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = adminName,
                    onValueChange = { adminName = it },
                    label = { Text("Admin Manager Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("setup_admin_name")
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 6) pin = it },
                    label = { Text("Security PIN (4-6 digits)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("setup_pin")
                )
                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { if (it.length <= 6) confirmPin = it },
                    label = { Text("Confirm Security PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().testTag("setup_confirm_pin")
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(text = errorMsg, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (company.isBlank() || adminName.isBlank() || pin.isBlank()) {
                            errorMsg = "Please fill in all details."
                        } else if (pin.length < 4) {
                            errorMsg = "PIN must be 4 to 6 digits."
                        } else if (pin != confirmPin) {
                            errorMsg = "PIN codes do not match."
                        } else {
                            viewModel.completeFirstTimeSetup(company, adminName, pin)
                            Toast.makeText(context, "Welcome to $company!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_setup"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Initialize System", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Login Screen ---
@Composable
fun LoginScreen(viewModel: OfficeViewModel, onBack: () -> Unit) {
    val companyName by viewModel.companyName.collectAsState()
    val adminName by viewModel.adminName.collectAsState()
    val lockoutRemaining by viewModel.lockoutTimeRemaining.collectAsState()

    var pinEntered by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyPrimary)
            .statusBarsPadding()
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .testTag("login_back_button")
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White
            )
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = "Lock",
                tint = TealAccent,
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = companyName.uppercase(Locale.getDefault()),
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Welcome back, $adminName",
                color = Color.White.copy(alpha = 0.7f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(30.dp))

            // Pin Status Indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                for (i in 0 until 6) {
                    val isFilled = i < pinEntered.length
                    Box(
                        modifier = Modifier
                            .padding(8.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) TealAccent else Color.White.copy(alpha = 0.3f)
                            )
                            .border(1.dp, Color.White.copy(alpha = 0.5f), CircleShape)
                    )
                }
            }

            if (lockoutRemaining > 0) {
                Text(
                    text = "Too many failed attempts. Locked out for $lockoutRemaining seconds.",
                    color = Color.Red,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else if (pinError) {
                Text(
                    text = "Incorrect PIN. Please try again.",
                    color = Color.Red,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(8.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(24.dp))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Numeric Keypad Grid (Requirement)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val keys = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("Clear", "0", "Back")
                    )

                    for (row in keys) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            for (key in row) {
                                Button(
                                    onClick = {
                                        if (lockoutRemaining > 0) return@Button
                                        when (key) {
                                            "Clear" -> pinEntered = ""
                                            "Back" -> if (pinEntered.isNotEmpty()) {
                                                pinEntered = pinEntered.dropLast(1)
                                            }
                                            else -> {
                                                if (pinEntered.length < 6) {
                                                    pinEntered += key
                                                    if (pinEntered.length >= 4) {
                                                        // Automatically evaluate if 4 or more digits
                                                        val authenticated = viewModel.login(pinEntered)
                                                        if (authenticated) {
                                                            pinError = false
                                                            Toast.makeText(context, "Log in successful!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            // Check if password failed fully or matching is expected
                                                            if (pinEntered.length >= 4) {
                                                                pinError = true
                                                                pinEntered = ""
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(60.dp)
                                        .testTag("keypad_$key"),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (key == "Clear" || key == "Back") Color.White.copy(alpha = 0.1f) else Color.White.copy(alpha = 0.2f),
                                        contentColor = Color.White
                                    ),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    if (key == "Back") {
                                        Icon(imageVector = Icons.Filled.Backspace, contentDescription = "Backspace")
                                    } else {
                                        Text(text = key, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            TextButton(
                onClick = {
                    Toast.makeText(context, "Hint: Key configured in setup.", Toast.LENGTH_LONG).show()
                }
            ) {
                Text("FORGOT PIN?", color = TealAccent, fontWeight = FontWeight.Bold)
            }
        }
    }
}
}

// --- Main App Scaffold with custom State Stack Navigation ---
@Composable
fun MainScaffold(viewModel: OfficeViewModel) {
    var activeTab by remember { mutableStateOf(0) } // 0: Home, 1: Attendance, 2: Employees, 3: Payslips, 4: Settings
    var navStack by remember { mutableStateOf<List<SubScreen>>(listOf(SubScreen.DashboardHome)) }

    val context = LocalContext.current

    // Handle android core back handler
    BackHandler(enabled = navStack.size > 1) {
        navStack = navStack.dropLast(1)
    }

    // Map tab changes to resetting base navigation stack
    val onTabChanged: (Int) -> Unit = { index ->
        activeTab = index
        val baseScreen = when (index) {
            0 -> SubScreen.DashboardHome
            1 -> SubScreen.AttendanceHome
            2 -> SubScreen.EmployeesList
            3 -> SubScreen.PayslipsHome
            4 -> SubScreen.SettingsHome
            else -> SubScreen.DashboardHome
        }
        navStack = listOf(baseScreen)
    }

    Scaffold(
        containerColor = LightBlueBg,
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFF0F2FA),
                modifier = Modifier
                    .navigationBarsPadding()
                    .border(width = 1.dp, color = Color(0xFFE1E2EC)),
                tonalElevation = 0.dp
            ) {
                val items = listOf(
                    Triple("Overview", Icons.Filled.Dashboard, 0),
                    Triple("Attendance", Icons.Filled.CalendarMonth, 1),
                    Triple("Team", Icons.Filled.Group, 2),
                    Triple("Payroll", Icons.Filled.ReceiptLong, 3),
                    Triple("Settings", Icons.Filled.Settings, 4)
                )

                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = activeTab == index,
                        onClick = { onTabChanged(index) },
                        icon = { Icon(imageVector = icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = NavyPrimary,
                            selectedTextColor = NavyPrimary,
                            unselectedIconColor = Color(0xFF44474E),
                            unselectedTextColor = Color(0xFF44474E),
                            indicatorColor = Color(0xFFD1E1FF)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(targetState = navStack.last(), label = "SubScreenTransition") { currentScreen ->
                when (currentScreen) {
                    is SubScreen.DashboardHome -> DashboardHomeScreen(viewModel, onNavigate = { navStack = navStack + it })
                    is SubScreen.EmployeesList -> EmployeesListScreen(viewModel, onNavigate = { navStack = navStack + it })
                    is SubScreen.EmployeeDetail -> EmployeeDetailScreen(viewModel, currentScreen.employeeId, onNavigate = { navStack = navStack + it }, onBack = { navStack = navStack.dropLast(1) })
                    is SubScreen.EmployeeAddEdit -> EmployeeFormScreen(viewModel, currentScreen.employeeId, onBack = { navStack = navStack.dropLast(1) })
                    is SubScreen.AttendanceHome -> AttendanceHomeScreen(viewModel, onNavigate = { navStack = navStack + it })
                    is SubScreen.AttendanceHistory -> AttendanceHistoryScreen(viewModel, currentScreen.employeeId, onBack = { navStack = navStack.dropLast(1) })
                    is SubScreen.PayslipsHome -> PayslipsHomeScreen(viewModel, onNavigate = { navStack = navStack + it })
                    is SubScreen.PayslipDetail -> PayslipDetailsSubScreen(viewModel, currentScreen.employeeId, currentScreen.month, currentScreen.year, onBack = { navStack = navStack.dropLast(1) })
                    is SubScreen.SettingsHome -> SettingsHomeScreen(viewModel, onNavigate = { navStack = navStack + it })
                }
            }
        }
    }
}

// --- Dashboard Screen (Tab 0) ---
@Composable
fun DashboardHomeScreen(viewModel: OfficeViewModel, onNavigate: (SubScreen) -> Unit) {
    val employeesList by viewModel.employees.collectAsState()
    val attendanceList by viewModel.attendance.collectAsState()
    val payslipsList by viewModel.payslips.collectAsState()
    val companyName by viewModel.companyName.collectAsState()
    val adminName by viewModel.adminName.collectAsState()

    val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    // Calculations for today's active rates
    val totalEmps = employeesList.filter { it.isActive }.size
    val todayAttendance = attendanceList.filter { it.date == todayDateString }
    
    val presentToday = todayAttendance.filter { it.status == "Present" }.size
    val absentToday = todayAttendance.filter { it.status == "Absent" }.size
    val halfDayToday = todayAttendance.filter { it.status == "Half-Day" }.size
    val leaveToday = todayAttendance.filter { it.status == "Leave" }.size
    val unMarkedToday = maxOf(0, totalEmps - todayAttendance.size)

    val pendingPayslipsCount = payslipsList.filter { !it.isPaid }.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Welcome Header
        val formattedDate = remember { SimpleDateFormat("EEEE, MMM d", Locale.getDefault()).format(Date()) }
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = companyName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1C1E)
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF44474E).copy(alpha = 0.7f)
                )
            }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary)
                    .border(4.dp, Color(0xFFE1E2EC), CircleShape)
                    .clickable { onNavigate(SubScreen.SettingsHome) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(adminName),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Collect Dynamic Announcements State
        val announcements by viewModel.announcements.collectAsState()
        val isSyncing by viewModel.isSyncing.collectAsState()

        if (announcements.isNotEmpty()) {
            Text(
                text = "REMOTE BULLETIN BOARD",
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                color = Color(0xFF44474E),
                modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFCFDF2)),
                border = BorderStroke(1.dp, Color(0xFFE2E4CD)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.Campaign,
                                contentDescription = "Campaign",
                                tint = OrangeWarning, 
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Dynamic Active Updates",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1B1C17)
                            )
                        }
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = TealAccent)
                        } else {
                            Text(
                                text = "Sync Now",
                                fontSize = 12.sp,
                                color = TealAccent,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable { viewModel.syncFromRemote() }
                                    .padding(4.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    announcements.forEachIndexed { i, ann ->
                        if (i > 0) {
                            HorizontalDivider(
                                color = Color(0xFFE2E4CD).copy(alpha = 0.5f),
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val priorityColor = if (ann.priority.lowercase() == "high") RedError else NavyPrimary
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(priorityColor.copy(alpha = 0.12f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = ann.priority.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = priorityColor
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFFEFEFEF))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = ann.category,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                                Text(
                                    text = ann.date,
                                    fontSize = 11.sp,
                                    color = Color.Gray,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ann.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF1B1C17)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = ann.content,
                                fontSize = 12.sp,
                                color = Color(0xFF44483D),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Main Bento Stats Card: Today's Attendance Ratios (Section 2)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFD1E1FF)),
            border = BorderStroke(1.dp, Color(0xFFBDD2F2)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = "ATTENDANCE TODAY",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.sp,
                    color = Color(0xFF001D36),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            Text(
                                text = "$presentToday",
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF001D36)
                            )
                            Text(
                                text = " / $totalEmps",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF001D36).copy(alpha = 0.6f),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        val attendRate = if (totalEmps > 0) ((presentToday + halfDayToday) * 100 / totalEmps) else 0
                        Text(
                            text = if (totalEmps > 0) "+$presentToday active present today ($attendRate% rate)" else "No active employees registered",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF004A77)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        // Compact colored indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(GreenSuccess).align(Alignment.CenterVertically))
                            Text("Pres: $presentToday", fontSize = 10.sp, color = Color(0xFF001D36), fontWeight = FontWeight.Bold)
                            
                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(OrangeWarning).align(Alignment.CenterVertically))
                            Text("Half: $halfDayToday", fontSize = 10.sp, color = Color(0xFF001D36), fontWeight = FontWeight.Bold)

                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(RedError).align(Alignment.CenterVertically))
                            Text("Abs: $absentToday", fontSize = 10.sp, color = Color(0xFF001D36), fontWeight = FontWeight.Bold)
                        }
                    }

                    // Ring Chart drawing standard canvas
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(100.dp)) {
                            val strokeWidthVal = 12.dp.toPx()
                            val totalAngleSegments = totalEmps.toFloat()
                            if (totalAngleSegments == 0f) {
                                drawArc(
                                    color = Color.White.copy(alpha = 0.5f),
                                    startAngle = 0f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidthVal)
                                )
                            } else {
                                val presentSweep = (presentToday.toFloat() / totalAngleSegments) * 360f
                                val halfSweep = (halfDayToday.toFloat() / totalAngleSegments) * 360f
                                val absentSweep = (absentToday.toFloat() / totalAngleSegments) * 360f
                                val leaveSweep = (leaveToday.toFloat() / totalAngleSegments) * 360f
                                val unmarkedSweep = (unMarkedToday.toFloat() / totalAngleSegments) * 360f

                                var currentAngle = 270f // start from top
                                
                                if (presentSweep > 0) {
                                    drawArc(color = GreenSuccess, startAngle = currentAngle, sweepAngle = presentSweep, useCenter = false, style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round))
                                    currentAngle += presentSweep
                                }
                                if (halfSweep > 0) {
                                    drawArc(color = OrangeWarning, startAngle = currentAngle, sweepAngle = halfSweep, useCenter = false, style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round))
                                    currentAngle += halfSweep
                                }
                                if (absentSweep > 0) {
                                    drawArc(color = RedError, startAngle = currentAngle, sweepAngle = absentSweep, useCenter = false, style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round))
                                    currentAngle += absentSweep
                                }
                                if (leaveSweep > 0) {
                                    drawArc(color = BlueInfo, startAngle = currentAngle, sweepAngle = leaveSweep, useCenter = false, style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round))
                                    currentAngle += leaveSweep
                                }
                                if (unmarkedSweep > 0) {
                                    drawArc(color = Color.White.copy(alpha = 0.6f), startAngle = currentAngle, sweepAngle = unmarkedSweep, useCenter = false, style = Stroke(width = strokeWidthVal, cap = StrokeCap.Round))
                                }
                            }
                        }
                        val attendPercent = if (totalEmps > 0) ((presentToday + halfDayToday) * 100 / totalEmps) else 0
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$attendPercent%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF001D36)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Grid Cards (Section 1) - Bento Style Cells
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStatsCard(
                title = "Total Team",
                value = "$totalEmps",
                label = "Active Employees",
                icon = Icons.Filled.People,
                color = TealAccent,
                modifier = Modifier.weight(1f),
                containerColor = Color(0xFFE0F2F1),
                textColor = Color.Black,
                labelColor = Color.Black,
                iconBgColor = Color.White.copy(alpha = 0.6f)
            )
            val attendPercent = if (totalEmps > 0) ((presentToday + halfDayToday) * 100 / totalEmps) else 0
            DashboardStatsCard(
                title = "Checked-In",
                value = "$presentToday",
                label = "$attendPercent% Work Rate",
                icon = Icons.Filled.CheckCircle,
                color = NavyPrimary,
                modifier = Modifier.weight(1f),
                containerColor = Color(0xFFE8EAF6),
                textColor = Color.Black,
                labelColor = Color.Black,
                iconBgColor = Color.White.copy(alpha = 0.6f)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            DashboardStatsCard(
                title = "Absence Today",
                value = "$absentToday",
                label = "Daily Absence",
                icon = Icons.Filled.Cancel,
                color = RedError,
                modifier = Modifier.weight(1f),
                containerColor = Color(0xFFFBE9E7),
                textColor = Color.Black,
                labelColor = Color.Black,
                iconBgColor = Color.White.copy(alpha = 0.6f)
            )
            DashboardStatsCard(
                title = "Pending Pay",
                value = "$pendingPayslipsCount",
                label = "Ledgers Awaiting",
                icon = Icons.Filled.ReceiptLong,
                color = OrangeWarning,
                modifier = Modifier.weight(1f),
                containerColor = Color(0xFFFFF3E0),
                textColor = Color.Black,
                labelColor = Color.Black,
                iconBgColor = Color.White.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Quick Actions Section (Section 6)
        Text(
            text = "QUICK ACTIONS DESK",
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Black,
            letterSpacing = 1.sp
        )
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = { onNavigate(SubScreen.EmployeesList) },
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Manage Team", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { onNavigate(SubScreen.AttendanceHome) },
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Today's Roll", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Recent Activity Feed List (Section 5)
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENT MARKING ACTIVITY LOG",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Black,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "View All",
                        color = TealAccent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        modifier = Modifier.clickable { onNavigate(SubScreen.AttendanceHome) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))

                if (todayAttendance.isEmpty()) {
                    Text(
                        text = "No updates recorded today yet.",
                        color = Color.Black,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    )
                } else {
                    todayAttendance.take(4).forEachIndexed { index, att ->
                        val empName = employeesList.find { it.id == att.employeeId }?.name ?: "Unknown"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF0F2FA)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = getInitials(empName),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = NavyPrimary
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = empName, fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
                                if (att.checkInTime != null) {
                                    Text(text = "In: ${att.checkInTime} | Out: ${att.checkOutTime ?: "--:--"}", fontSize = 11.sp, color = Color.Black)
                                } else {
                                    Text(text = "No work timestamps recorded", fontSize = 11.sp, color = Color.Black)
                                }
                            }
                            StatusChip(status = att.status)
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun DashboardStatsCard(
    title: String,
    value: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.White,
    textColor: Color = Color.Black,
    labelColor: Color = Color.Black,
    iconBgColor: Color = Color.Transparent
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.dp, Color(0xFFE1E2EC)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(iconBgColor)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = labelColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                color = labelColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun ChartLegendTile(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 3.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
    }
}

// --- Team Management Screens (Tab 2) ---
@Composable
fun EmployeesListScreen(viewModel: OfficeViewModel, onNavigate: (SubScreen) -> Unit) {
    val employeesList by viewModel.employees.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Active", "Inactive"

    val filteredList = employeesList.filter { emp ->
        val matchesQuery = emp.name.contains(searchQuery, ignoreCase = true) ||
                emp.department.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilter) {
            "Active" -> emp.isActive
            "Inactive" -> !emp.isActive
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Our Team", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(text = "${employeesList.size} registered team members", style = MaterialTheme.typography.bodyMedium, color = Color.Black)
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = { onNavigate(SubScreen.EmployeeAddEdit()) },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.testTag("register_employee_button")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Register Employee",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Register",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Search Bar (Requirement)
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name or department...") },
                leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = "Search") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("employee_search_bar"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NavyPrimary)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filters Status Chips (Requirement)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Active", "Inactive").forEach { filter ->
                    FilterChip(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        label = { Text(filter) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = NavyPrimary,
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (employeesList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.PeopleOutline,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No registered team members found.",
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Register your first employee to start managing attendance and payroll.",
                            color = Color.Black,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        Button(
                            onClick = { onNavigate(SubScreen.EmployeeAddEdit()) },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Register Employee", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            } else if (filteredList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(imageVector = Icons.Filled.PeopleOutline, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "No matching employees found.", color = Color.Black, fontSize = 14.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredList) { emp ->
                        EmployeeItemCard(emp, onClick = { onNavigate(SubScreen.EmployeeDetail(emp.id)) })
                    }
                }
            }
        }
    }
}

@Composable
fun EmployeeItemCard(employee: Employee, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Initials Avatar (Requirement)
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(NavyPrimary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(employee.name),
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = employee.name, fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 16.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = employee.designation, color = Color.Black, fontSize = 12.sp, maxLines = 1)
                Spacer(modifier = Modifier.height(4.dp))
                
                // Department Badge with color mapping
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(getDeptColor(employee.department).copy(alpha = 0.15f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = employee.department.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = getDeptColor(employee.department)
                    )
                }
            }

            // Status indicator badge
            val statusColor = if (employee.isActive) GreenSuccess else RedError
            val statusText = if (employee.isActive) "ACTIVE" else "INACTIVE"
            Column(horizontalAlignment = Alignment.End) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(text = statusText, color = statusColor, fontWeight = FontWeight.Black, fontSize = 9.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "₹${formatIndianCurrency(employee.salaryPerDay)}/d", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Add / Edit Employee Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeFormScreen(viewModel: OfficeViewModel, employeeId: Int?, onBack: () -> Unit) {
    val employeesList by viewModel.employees.collectAsState()
    val existingEmployee = remember(employeesList, employeeId) { employeesList.find { it.id == employeeId } }

    var name by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("") }
    var selectedDept by remember { mutableStateOf("Engineering") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var dailyRateStr by remember { mutableStateOf("") }
    var joinDate by remember { mutableStateOf("") }
    var isActive by remember { mutableStateOf(true) }
    var password by remember { mutableStateOf("") }

    val departments = listOf("Engineering", "HR", "Sales", "Marketing", "Finance", "Operations")
    var isDeptDropdownExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    LaunchedEffect(existingEmployee) {
        existingEmployee?.let {
            name = it.name
            designation = it.designation
            selectedDept = it.department
            email = it.email
            phone = it.phone
            dailyRateStr = it.salaryPerDay.toInt().toString()
            joinDate = it.joinDate
            isActive = it.isActive
            password = it.password
        } ?: run {
            if (joinDate.isEmpty()) {
                joinDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            }
            password = ""
        }
    }

    Scaffold(
        topBar = {
            OptInTopAppBar(
                title = { Text(if (employeeId == null) "Register employee" else "Modify Member Info") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { paddingVal ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVal)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Full Name*") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("form_employee_name")
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = designation,
                    onValueChange = { designation = it },
                    label = { Text("Designation*") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("form_employee_designation")
                )

                // Department select dropdown
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = selectedDept,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Department") },
                        trailingIcon = {
                            IconButton(onClick = { isDeptDropdownExpanded = true }) {
                                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { isDeptDropdownExpanded = true }
                    )
                    DropdownMenu(
                        expanded = isDeptDropdownExpanded,
                        onDismissRequest = { isDeptDropdownExpanded = false }
                    ) {
                        departments.forEach { dept ->
                            DropdownMenuItem(
                                text = { Text(dept) },
                                onClick = {
                                    selectedDept = dept
                                    isDeptDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email Address") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.weight(1.1f).testTag("form_employee_email")
                )

                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.weight(0.9f).testTag("form_employee_phone")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = dailyRateStr,
                    onValueChange = { dailyRateStr = it },
                    label = { Text("Daily Rates (₹)*") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f).testTag("form_employee_salary")
                )

                OutlinedTextField(
                    value = joinDate,
                    onValueChange = { joinDate = it },
                    label = { Text("Join Date*") },
                    placeholder = { Text("yyyy-MM-dd") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("form_employee_joindate")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = password,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            password = input
                        }
                    },
                    label = { Text("PIN (4 Digits)*") },
                    placeholder = { Text("e.g. 7482") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.1f).testTag("form_employee_password")
                )

                Column(
                    modifier = Modifier.weight(0.9f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "Active Status", fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 12.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = {
                    val rate = dailyRateStr.toDoubleOrNull()
                    if (name.isBlank() || designation.isBlank() || rate == null || rate <= 0 || joinDate.isBlank() || password.length != 4) {
                        Toast.makeText(context, "Please configure all mandatory inputs properly. Password must be exactly 4 digits.", Toast.LENGTH_SHORT).show()
                    } else {
                        val toSave = Employee(
                            id = employeeId ?: 0,
                            name = name,
                            designation = designation,
                            department = selectedDept,
                            email = email,
                            phone = phone,
                            salaryPerDay = rate,
                            joinDate = joinDate,
                            isActive = isActive,
                            password = password
                        )
                        if (employeeId == null) {
                            viewModel.addEmployee(toSave) {
                                Toast.makeText(context, "$name added successfully!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        } else {
                            viewModel.updateEmployee(toSave) {
                                Toast.makeText(context, "$name metadata updated!", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("submit_employee_form"),
                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(text = if (employeeId == null) "Register employee" else "Apply Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

// --- Employee Detail View (With History tab and local invoice lists) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeDetailScreen(
    viewModel: OfficeViewModel,
    employeeId: Int,
    onNavigate: (SubScreen) -> Unit,
    onBack: () -> Unit
) {
    val employeesList by viewModel.employees.collectAsState()
    val attendanceList by viewModel.attendance.collectAsState()
    val payslipsList by viewModel.payslips.collectAsState()

    val employee = remember(employeesList) { employeesList.find { it.id == employeeId } }

    var activeTabDetail by remember { mutableStateOf(0) } // 0: Contacts / Info, 1: History, 2: Payslips Ledger

    val context = LocalContext.current

    if (employee == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Employee metadata not found. It may have been removed.")
        }
        return
    }

    // Filter attendance and payslips for this employee
    val empAttendance = remember(attendanceList) { attendanceList.filter { it.employeeId == employeeId }.sortedByDescending { it.date } }
    val empPayslips = remember(payslipsList) { payslipsList.filter { it.employeeId == employeeId }.sortedByDescending { "${it.year}-${it.month}" } }

    Scaffold(
        topBar = {
            OptInTopAppBar(
                title = { Text(employee.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { onNavigate(SubScreen.EmployeeAddEdit(employeeId)) }) {
                        Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit profiles", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            viewModel.deleteEmployee(employee) {
                                Toast.makeText(context, "Employee removed from system.", Toast.LENGTH_SHORT).show()
                                onBack()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete employees", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { paddingVal ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingVal)
        ) {
            // Header Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(NavyPrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = getInitials(employee.name), fontSize = 24.sp, fontWeight = FontWeight.Black, color = NavyPrimary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(text = employee.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                        Text(text = employee.designation, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(getDeptColor(employee.department).copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = employee.department.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = getDeptColor(employee.department))
                        }
                    }
                }
            }

            // Tab Rows
            TabRow(
                selectedTabIndex = activeTabDetail,
                containerColor = Color.Transparent,
                contentColor = NavyPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[activeTabDetail]),
                        color = TealAccent
                    )
                }
            ) {
                Tab(selected = activeTabDetail == 0, onClick = { activeTabDetail = 0 }) {
                    Text("Info Card", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTabDetail == 1, onClick = { activeTabDetail = 1 }) {
                    Text("Attendance", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
                Tab(selected = activeTabDetail == 2, onClick = { activeTabDetail = 2 }) {
                    Text("Payslips", modifier = Modifier.padding(12.dp), fontWeight = FontWeight.Bold)
                }
            }

            // Tab Content
            when (activeTabDetail) {
                0 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DetailInfoRow("Email", employee.email.ifBlank { "Not provided" }, Icons.Filled.Email)
                        DetailInfoRow("Phone", employee.phone.ifBlank { "Not provided" }, Icons.Filled.Phone)
                        DetailInfoRow("Joined", employee.joinDate, Icons.Filled.CalendarToday)
                        DetailInfoRow("Base Salary / Day", "₹${formatIndianCurrency(employee.salaryPerDay)}", Icons.Filled.AttachMoney)

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                onNavigate(SubScreen.AttendanceHistory(employeeId))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.CalendarMonth, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Inspect Monthly History Grid")
                        }
                    }
                }
                1 -> {
                    if (empAttendance.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No logged attendance updates.")
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(empAttendance) { att ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = formatDateFriendly(att.date), fontWeight = FontWeight.Bold, color = NavyPrimary)
                                            if (att.checkInTime != null) {
                                                Text(text = "Timestamps: ${att.checkInTime} to ${att.checkOutTime ?: "--:--"}", fontSize = 11.sp, color = Color.Gray)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                                    modifier = Modifier.padding(top = 4.dp)
                                                ) {
                                                    if (att.checkInSelfie != null) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            modifier = Modifier
                                                                .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.PhotoCamera,
                                                                contentDescription = null,
                                                                tint = Color(0xFF2E7D32),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                "Selfie In",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                        }
                                                    }
                                                    if (att.checkOutSelfie != null) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            modifier = Modifier
                                                                .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.PhotoCamera,
                                                                contentDescription = null,
                                                                tint = Color(0xFF2E7D32),
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                "Selfie Out",
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = Color(0xFF2E7D32)
                                                            )
                                                        }
                                                    }
                                                    val trackingDist = att.checkOutDistance ?: att.checkInDistance
                                                    if (trackingDist != null) {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                            modifier = Modifier
                                                                .background(Color(0xFFE8EAF6), RoundedCornerShape(4.dp))
                                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Filled.LocationOn,
                                                                contentDescription = null,
                                                                tint = NavyPrimary,
                                                                modifier = Modifier.size(10.dp)
                                                            )
                                                            Text(
                                                                "GPS: %.1fm".format(trackingDist),
                                                                fontSize = 9.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = NavyPrimary
                                                            )
                                                        }
                                                    }
                                                }
                                            } else {
                                                Text(text = "No recorded standard timings", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            if (!att.notes.isNullOrBlank()) {
                                                Text(text = "Note: ${att.notes}", fontSize = 11.sp, color = Color.DarkGray)
                                            }
                                        }
                                        StatusChip(status = att.status)
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Button(
                            onClick = {
                                // Direct Quick Calc and generate for current month/year
                                val c = Calendar.getInstance()
                                viewModel.generatePayslip(employee, c.get(Calendar.MONTH) + 1, c.get(Calendar.YEAR)) {
                                    Toast.makeText(context, "Payroll generated for current month!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                        ) {
                            Text("Run Payroll calculations - Current Month")
                        }

                        if (empPayslips.isEmpty()) {
                            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No generated payslips found.", color = Color.Gray)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(empPayslips) { payslip ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { onNavigate(SubScreen.PayslipDetail(employeeId, payslip.month, payslip.year)) },
                                        colors = CardDefaults.cardColors(containerColor = Color.White)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                val monthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Calendar.getInstance().apply { set(Calendar.MONTH, payslip.month - 1) }.time)
                                                Text(text = "$monthName ${payslip.year}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                                Text(text = "Days present: ${payslip.totalDaysWorked} | Deductions: ₹${payslip.deductions.toInt()}", fontSize = 11.sp, color = Color.Gray)
                                            }
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = "₹${formatIndianCurrency(payslip.netSalary)}", color = NavyPrimary, fontWeight = FontWeight.Black, modifier = Modifier.padding(end = 8.dp))
                                                if (payslip.isPaid) {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(GreenSuccess.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text("PAID", color = GreenSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                } else {
                                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(RedError.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                        Text("UNPAID", color = RedError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailInfoRow(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = NavyPrimary)
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(text = label, fontSize = 11.sp, color = Color.Gray)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = NavyPrimary)
        }
    }
}

// --- Attendance Module Screens (Tab 1) ---
@Composable
fun AttendanceHomeScreen(viewModel: OfficeViewModel, onNavigate: (SubScreen) -> Unit) {
    val context = LocalContext.current
    val employeesList by viewModel.employees.collectAsState()
    val attendanceList by viewModel.attendance.collectAsState()

    val todayDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    val totalEmps = employeesList.filter { it.isActive }.size
    // Filter attendance for today
    val todayAttendance = attendanceList.filter { it.date == todayDateString }

    val presentToday = todayAttendance.filter { it.status == "Present" }.size
    val absentToday = todayAttendance.filter { it.status == "Absent" }.size
    val halfDayToday = todayAttendance.filter { it.status == "Half-Day" }.size
    val leaveToday = todayAttendance.filter { it.status == "Leave" }.size
    val unMarkedToday = maxOf(0, totalEmps - todayAttendance.size)

    var currentMarkingEmployee by remember { mutableStateOf<Employee?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Today Title
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val dayNameStr = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date())
                Text(text = "Daily attendance", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text(text = dayNameStr, style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            }
            IconButton(
                onClick = { onNavigate(SubScreen.AttendanceHistory()) },
                colors = IconButtonDefaults.iconButtonColors(containerColor = NavyPrimary.copy(alpha = 0.1f))
            ) {
                Icon(imageVector = Icons.Filled.History, contentDescription = "History Logs", tint = NavyPrimary)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Summary Counters (Requirement)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "PRESENT", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "$presentToday", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = GreenSuccess)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "HALF DAY", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "$halfDayToday", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = OrangeWarning)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "ABSENT", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "$absentToday", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = RedError)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "LEAVES", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "$leaveToday", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = BlueInfo)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "UNMARKED", fontSize = 10.sp, color = Color.Gray)
                    Text(text = "$unMarkedToday", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Actions Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Daily Attendance Registry", fontWeight = FontWeight.Bold, color = NavyPrimary)
            TextButton(
                onClick = {
                    viewModel.bulkMarkPresentToday(employeesList.filter { it.isActive }, todayDateString)
                    Toast.makeText(context, "Marked unmarked roster present", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = TealAccent)
            ) {
                Text("MARK ALL PRESENT", fontWeight = FontWeight.Black)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Worker List
        val activeEmployees = employeesList.filter { it.isActive }
        if (activeEmployees.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No active employees listed.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(activeEmployees) { emp ->
                    val record = todayAttendance.find { it.employeeId == emp.id }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentMarkingEmployee = emp },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = emp.name, fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text(text = emp.designation, fontSize = 11.sp, color = Color.Gray)
                                if (record?.checkInTime != null) {
                                    Text(text = "Entered: ${record.checkInTime} | Left: ${record.checkOutTime ?: "--:--"}", fontSize = 11.sp, color = Color.Gray)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        if (record.checkInSelfie != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier
                                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PhotoCamera,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    "Selfie In",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                        if (record.checkOutSelfie != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier
                                                    .background(Color(0xFFE8F5E9), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.PhotoCamera,
                                                    contentDescription = null,
                                                    tint = Color(0xFF2E7D32),
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    "Selfie Out",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF2E7D32)
                                                )
                                            }
                                        }
                                        val trackingDist = record.checkOutDistance ?: record.checkInDistance
                                        if (trackingDist != null) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                                modifier = Modifier
                                                    .background(Color(0xFFE8EAF6), RoundedCornerShape(4.dp))
                                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.LocationOn,
                                                    contentDescription = null,
                                                    tint = NavyPrimary,
                                                    modifier = Modifier.size(10.dp)
                                                )
                                                Text(
                                                    "GPS: %.1fm".format(trackingDist),
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = NavyPrimary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (record != null) {
                                StatusChip(status = record.status)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.LightGray.copy(alpha = 0.3f))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(text = "UNMARKED", color = Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Mark Dialog (Requirement)
    currentMarkingEmployee?.let { emp ->
        val record = todayAttendance.find { it.employeeId == emp.id }
        QuickMarkAttendanceDialog(
            employeeName = emp.name,
            employeeId = emp.id,
            todayRecord = record,
            onDismiss = { currentMarkingEmployee = null },
            onSave = { status, inTime, outTime, notes, checkInSelfie, checkOutSelfie, checkInDist, checkOutDist ->
                viewModel.markAttendance(
                    employeeId = emp.id,
                    dateString = todayDateString,
                    status = status,
                    checkIn = inTime,
                    checkOut = outTime,
                    notes = notes,
                    checkInSelfie = checkInSelfie,
                    checkOutSelfie = checkOutSelfie,
                    checkInDistance = checkInDist,
                    checkOutDistance = checkOutDist
                )
                currentMarkingEmployee = null
                Toast.makeText(context, "Attendance registered for ${emp.name}", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun SelfieAvatar(
    employeeId: Int,
    name: String,
    modifier: Modifier = Modifier,
    isCheckOut: Boolean = false
) {
    val seed = (employeeId * 31 + (if (isCheckOut) 53 else 17)).absoluteValue
    val bgColors = listOf(
        Color(0xFFE0F2F1), Color(0xFFE8EAF6), Color(0xFFFFF3E0), Color(0xFFFBE9E7),
        Color(0xFFE0F7FA), Color(0xFFF3E5F5), Color(0xFFE8F5E9), Color(0xFFFFFDE7)
    )
    val faceBg = bgColors[seed % bgColors.size]
    
    val hairColors = listOf(
        Color(0xFF2C1A04), Color(0xFF4E3629), Color(0xFF8D5B4C), Color(0xFF1C1C1C),
        Color(0xFFD7A15C), Color(0xFF705030)
    )
    val hairColor = hairColors[(seed + 3) % hairColors.size]
    
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(faceBg)
            .border(1.5.dp, NavyPrimary, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            
            val eyeRadius = width * 0.08f
            val eyeY = height * 0.45f
            drawCircle(color = Color.White, radius = eyeRadius, center = Offset(width * 0.33f, eyeY))
            drawCircle(color = Color.White, radius = eyeRadius, center = Offset(width * 0.67f, eyeY))
            
            val pupilRadius = eyeRadius * 0.5f
            drawCircle(color = Color(0xFF333333), radius = pupilRadius, center = Offset(width * 0.33f, eyeY))
            drawCircle(color = Color(0xFF333333), radius = pupilRadius, center = Offset(width * 0.67f, eyeY))
            
            drawCircle(color = Color(0xFFF58A5F).copy(alpha = 0.8f), radius = width * 0.04f, center = Offset(width * 0.5f, height * 0.56f))
            
            val smilePath = androidx.compose.ui.graphics.Path().apply {
                arcTo(
                    rect = androidx.compose.ui.geometry.Rect(
                        left = width * 0.37f,
                        top = height * 0.58f,
                        right = width * 0.63f,
                        bottom = height * 0.76f
                    ),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 180f,
                    forceMoveTo = true
                )
            }
            drawPath(
                path = smilePath,
                color = Color(0xFFE53935),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = width * 0.04f, cap = StrokeCap.Round)
            )
            
            drawArc(
                color = hairColor,
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                size = androidx.compose.ui.geometry.Size(width, height * 0.65f),
                topLeft = Offset(0f, -height * 0.08f)
            )
            
            drawCircle(
                color = GreenSuccess,
                radius = width * 0.14f,
                center = Offset(width * 0.82f, height * 0.82f)
            )
        }
        
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .fillMaxSize(0.24f)
                .align(Alignment.BottomEnd)
                .padding(bottom = 1.dp, end = 1.dp)
        )
    }
}

@Composable
fun SelfieCameraPreviewMock(
    employeeName: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "LaserPulse")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0.15f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserLine"
    )

    Box(
        modifier = modifier
            .background(Color(0xFF121212))
            .border(2.dp, Color.Gray, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            val cornerLen = 24.dp.toPx()
            val strokeW = 4.dp.toPx()

            drawLine(Color.White, Offset(0f, 0f), Offset(cornerLen, 0f), strokeWidth = strokeW)
            drawLine(Color.White, Offset(0f, 0f), Offset(0f, cornerLen), strokeWidth = strokeW)
            
            drawLine(Color.White, Offset(width, 0f), Offset(width - cornerLen, 0f), strokeWidth = strokeW)
            drawLine(Color.White, Offset(width, 0f), Offset(width, cornerLen), strokeWidth = strokeW)
            
            drawLine(Color.White, Offset(0f, height), Offset(cornerLen, height), strokeWidth = strokeW)
            drawLine(Color.White, Offset(0f, height), Offset(0f, height - cornerLen), strokeWidth = strokeW)
            
            drawLine(Color.White, Offset(width, height), Offset(width - cornerLen, height), strokeWidth = strokeW)
            drawLine(Color.White, Offset(width, height), Offset(width, height - cornerLen), strokeWidth = strokeW)

            drawOval(
                color = Color.LightGray.copy(alpha = 0.35f),
                topLeft = Offset(width * 0.28f, height * 0.15f),
                size = Size(width * 0.44f, height * 0.6f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(15f, 15f))
                )
            )

            drawLine(
                color = GreenSuccess.copy(alpha = 0.8f),
                start = Offset(0f, height * laserY),
                end = Offset(width, height * laserY),
                strokeWidth = 3.dp.toPx()
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Text(
                "CAMERA SENSOR ACTIVE",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = Color.LightGray
            )
            Text(
                "Position face in circle scan target",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GreenSuccess
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(8.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.Red.copy(alpha = 0.2f))
                .border(0.5.dp, Color.Red, RoundedCornerShape(20.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(Color.Red)
                )
                Text("LIVE GEODESIC BIOMETRICS", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun QuickMarkAttendanceDialog(
    employeeName: String,
    employeeId: Int,
    todayRecord: Attendance?,
    onDismiss: () -> Unit,
    onSave: (
        status: String,
        checkIn: String?,
        checkOut: String?,
        notes: String?,
        checkInSelfie: String?,
        checkOutSelfie: String?,
        checkInDistance: Double?,
        checkOutDistance: Double?
    ) -> Unit
) {
    var activeTabDetailSec by remember { mutableStateOf(0) } // 0: Secure Selfie & GPS, 1: Admin Override Form
    
    var testDistance by remember { mutableStateOf(5.0f) }
    val isNear = testDistance <= 10.0f
    
    val hasCheckedIn = todayRecord?.checkInTime != null
    var isCheckInMode by remember { mutableStateOf(!hasCheckedIn) }
    
    var isSelfieTaken by remember { mutableStateOf(false) }
    
    var status by remember { mutableStateOf(todayRecord?.status ?: "Present") }
    var inTime by remember { mutableStateOf(todayRecord?.checkInTime ?: "09:00") }
    var outTime by remember { mutableStateOf(todayRecord?.checkOutTime ?: "18:00") }
    var notes by remember { mutableStateOf(todayRecord?.notes ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Security Verification Terminal", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = NavyPrimary)
                Text(text = employeeName, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                
                TabRow(
                    selectedTabIndex = activeTabDetailSec,
                    containerColor = Color.Transparent,
                    contentColor = NavyPrimary,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(selected = activeTabDetailSec == 0, onClick = { activeTabDetailSec = 0 }) {
                        Text("Secure Selfie & GPS", modifier = Modifier.padding(6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Tab(selected = activeTabDetailSec == 1, onClick = { activeTabDetailSec = 1 }) {
                        Text("Manual Override", modifier = Modifier.padding(6.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Spacer(modifier = Modifier.height(2.dp))
                
                if (activeTabDetailSec == 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF1F3F9), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { isCheckInMode = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCheckInMode) NavyPrimary else Color.Transparent,
                                contentColor = if (isCheckInMode) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Check-In Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { isCheckInMode = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isCheckInMode) NavyPrimary else Color.Transparent,
                                contentColor = if (!isCheckInMode) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f).height(30.dp),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Check-Out Scan", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isNear) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(
                                    imageVector = if (isNear) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                                    contentDescription = "GPS Status",
                                    tint = if (isNear) GreenSuccess else RedError,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isNear) "GPS Status: Inside Boundary" else "GPS Status: Outside Boundary",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isNear) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                )
                            }
                            Text(
                                text = if (isNear) {
                                    "Location Verified: %.1f meters from office center (Limit: 10m).".format(testDistance)
                                } else {
                                    "Location Blocked: %.1f meters from office. Must be within 10 meters!".format(testDistance)
                                },
                                fontSize = 10.sp,
                                color = if (isNear) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
                    
                    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Simulate GPS Distance:", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                            Text("${testDistance.toInt()} meters", fontSize = 10.sp, color = NavyPrimary, fontWeight = FontWeight.Black)
                        }
                        Slider(
                            value = testDistance,
                            onValueChange = { testDistance = it },
                            valueRange = 1.0f..25.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = NavyPrimary,
                                activeTrackColor = NavyPrimary,
                                inactiveTrackColor = Color.LightGray.copy(alpha = 0.3f)
                            )
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            TextButton(
                                onClick = { testDistance = 3.0f },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Teleport Inside (3m)", fontSize = 9.sp, color = TealAccent)
                            }
                            TextButton(
                                onClick = { testDistance = 18.0f },
                                contentPadding = PaddingValues(0.dp),
                                modifier = Modifier.height(24.dp)
                            ) {
                                Text("Teleport Outside (18m)", fontSize = 9.sp, color = RedError)
                            }
                        }
                    }
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                    ) {
                        if (!isSelfieTaken) {
                            SelfieCameraPreviewMock(
                                employeeName = employeeName,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFE8EAF6), RoundedCornerShape(12.dp))
                                    .border(1.5.dp, GreenSuccess, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    SelfieAvatar(
                                        employeeId = employeeId,
                                        name = employeeName,
                                        modifier = Modifier.size(60.dp),
                                        isCheckOut = !isCheckInMode
                                    )
                                    Text("Biometric Scan Registered ✓", color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    TextButton(onClick = { isSelfieTaken = false }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) {
                                        Text("Retake Photo", fontSize = 10.sp, color = NavyPrimary)
                                    }
                                }
                            }
                        }
                    }
                    
                    if (!isSelfieTaken) {
                        Button(
                            onClick = { isSelfieTaken = true },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth().height(36.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Capture Biometric Selfie", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 11.sp)
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1.0f)
                        ) {
                            Text("Cancel", fontSize = 12.sp)
                        }
                        
                        Button(
                            onClick = {
                                val sdf = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                                val currentTime = sdf.format(java.util.Date())
                                
                                if (isCheckInMode) {
                                    onSave(
                                        "Present",
                                        currentTime,
                                        todayRecord?.checkOutTime,
                                        "Biometric GPS Verified Check-In",
                                        "selfie_${employeeId}_in",
                                        todayRecord?.checkOutSelfie,
                                        testDistance.toDouble(),
                                        todayRecord?.checkOutDistance
                                    )
                                } else {
                                    onSave(
                                        todayRecord?.status ?: "Present",
                                        todayRecord?.checkInTime ?: "09:00",
                                        currentTime,
                                        "Biometric GPS Verified Check-Out",
                                        todayRecord?.checkInSelfie,
                                        "selfie_${employeeId}_out",
                                        todayRecord?.checkInDistance,
                                        testDistance.toDouble()
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                            enabled = isNear && isSelfieTaken,
                            modifier = Modifier.weight(1.2f).testTag("save_secure_attendance")
                        ) {
                            Icon(imageVector = Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = if (isCheckInMode) "Verify Check-In" else "Verify Check-Out",
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 11.sp
                            )
                        }
                    }
                    
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("Present", "Half-Day", "Absent", "Leave").forEach { opt ->
                            Button(
                                onClick = { status = opt },
                                modifier = Modifier.weight(1f).height(38.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (status == opt) NavyPrimary else Color.LightGray.copy(alpha = 0.25f),
                                    contentColor = if (status == opt) Color.White else Color.Black
                                ),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(text = opt, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (status == "Present" || status == "Half-Day") {
                        OutlinedTextField(
                            value = inTime,
                            onValueChange = { inTime = it },
                            label = { Text("Check-In Time") },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_checkin")
                        )
                        OutlinedTextField(
                            value = outTime,
                            onValueChange = { outTime = it },
                            label = { Text("Check-Out Time") },
                            modifier = Modifier.fillMaxWidth().testTag("dialog_checkout")
                        )
                    }

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Optional Notes") },
                        modifier = Modifier.fillMaxWidth().testTag("dialog_notes")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        Button(
                            onClick = {
                                val checkedIn = if (status == "Present" || status == "Half-Day") inTime else null
                                val checkedOut = if (status == "Present" || status == "Half-Day") outTime else null
                                onSave(status, checkedIn, checkedOut, notes.ifBlank { null }, null, null, null, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.weight(1f).testTag("save_attendance_dialog")
                        ) {
                            Text("Save Override", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// --- Attendance Calendar Grid View Screen (Requirement) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceHistoryScreen(viewModel: OfficeViewModel, employeeId: Int?, onBack: () -> Unit) {
    val employeesList by viewModel.employees.collectAsState()
    val attendanceList by viewModel.attendance.collectAsState()
    val holidaysList by viewModel.holidays.collectAsState()

    var selectedEmployee by remember { mutableStateOf<Employee?>(null) }
    var year by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }

    LaunchedEffect(employeesList, employeeId) {
        if (selectedEmployee == null) {
            selectedEmployee = if (employeeId != null) {
                employeesList.find { it.id == employeeId }
            } else {
                employeesList.firstOrNull()
            }
        }
    }

    var showEmpMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            OptInTopAppBar(
                title = { Text("Attendance Grid") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Employee Filter selector
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedEmployee?.name ?: "All Team Records",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Employee") },
                    trailingIcon = {
                        IconButton(onClick = { showEmpMenu = true }) {
                            Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().clickable { showEmpMenu = true }
                )
                DropdownMenu(expanded = showEmpMenu, onDismissRequest = { showEmpMenu = false }) {
                    DropdownMenuItem(text = { Text("All Team Members") }, onClick = { selectedEmployee = null; showEmpMenu = false })
                    employeesList.forEach { emp ->
                        DropdownMenuItem(text = { Text(emp.name) }, onClick = { selectedEmployee = emp; showEmpMenu = false })
                    }
                }
            }

            // Month Year Filter controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        if (month == 1) {
                            month = 12
                            year -= 1
                        } else {
                            month -= 1
                        }
                    }
                ) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                Text(text = "${getMonthName(month)} $year", fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 16.sp)
                OutlinedButton(
                    onClick = {
                        if (month == 12) {
                            month = 1
                            year += 1
                        } else {
                            month += 1
                        }
                    }
                ) {
                    Icon(imageVector = Icons.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                }
            }

            // Custom Calendar Grid Widget (Requirement)
            CustomMonthGridCalendar(
                selectedEmployee = selectedEmployee,
                month = month,
                year = year,
                attendanceList = attendanceList,
                holidaysList = holidaysList
            )
        }
    }
}

@Composable
fun CustomMonthGridCalendar(
    selectedEmployee: Employee?,
    month: Int,
    year: Int,
    attendanceList: List<Attendance>,
    holidaysList: List<String>
) {
    val daysInMonth: Int
    val firstDayOfWeek: Int

    val cal = Calendar.getInstance()
    cal.set(year, month - 1, 1)
    daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // 1 = Sunday, 2 = Monday...

    val weekdays = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Weekday headings
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                weekdays.forEach { wd ->
                    Text(
                        text = wd,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Calendar cells Grid rows
            var dayCounter = 1
            var cellIndex = 1

            while (dayCounter <= daysInMonth) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    for (i in 1..7) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(3.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (cellIndex >= firstDayOfWeek && dayCounter <= daysInMonth) {
                                // Real Day Date Number
                                val dateStr = String.format(Locale.US, "%d-%02d-%02d", year, month, dayCounter)
                                val attendanceRecords = attendanceList.filter { 
                                    it.date == dateStr && (selectedEmployee == null || it.employeeId == selectedEmployee.id)
                                }

                                // Determine color
                                val isHoliday = holidaysList.contains(dateStr)
                                val isWeekend = i == 1 // Sunday weekend

                                val backColor = when {
                                    isHoliday || isWeekend -> Color.LightGray.copy(alpha = 0.5f)
                                    attendanceRecords.any { it.status == "Present" } -> GreenSuccess.copy(alpha = 0.15f)
                                    attendanceRecords.any { it.status == "Half-Day" } -> OrangeWarning.copy(alpha = 0.15f)
                                    attendanceRecords.any { it.status == "Leave" } -> BlueInfo.copy(alpha = 0.15f)
                                    attendanceRecords.any { it.status == "Absent" } -> RedError.copy(alpha = 0.15f)
                                    else -> Color.Transparent
                                }

                                val contentColor = when {
                                    attendanceRecords.any { it.status == "Present" } -> GreenSuccess
                                    attendanceRecords.any { it.status == "Half-Day" } -> OrangeWarning
                                    attendanceRecords.any { it.status == "Leave" } -> BlueInfo
                                    attendanceRecords.any { it.status == "Absent" } -> RedError
                                    isWeekend || isHoliday -> Color.Gray
                                    else -> Color.Black
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(backColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = "$dayCounter", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = contentColor)
                                        if (attendanceRecords.isNotEmpty() && selectedEmployee == null) {
                                            // Show simple aggregate present count for the entire team
                                            val presentInCell = attendanceRecords.filter { it.status == "Present" }.size
                                            Text(text = "p:$presentInCell", fontSize = 8.sp, color = GreenSuccess)
                                        }
                                    }
                                }
                                dayCounter++
                            }
                            cellIndex++
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Legende info
            Text(text = "Date indicators Map", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = NavyPrimary)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IndicatorTag(color = GreenSuccess, label = "Present")
                IndicatorTag(color = OrangeWarning, label = "Half")
                IndicatorTag(color = RedError, label = "Absent")
                IndicatorTag(color = BlueInfo, label = "Leave")
                IndicatorTag(color = Color.Gray, label = "Holiday")
            }
        }
    }
}

@Composable
fun IndicatorTag(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = Color.Gray)
    }
}

// --- Payslip Generation & Ledger Views (Tab 3) ---
@Composable
fun PayslipsHomeScreen(viewModel: OfficeViewModel, onNavigate: (SubScreen) -> Unit) {
    val employeesList by viewModel.employees.collectAsState()
    val payslipsList by viewModel.payslips.collectAsState()

    var selectedEmployeeForSlip by remember { mutableStateOf<Employee?>(null) }
    var selectedMonth by remember { mutableStateOf(Calendar.getInstance().get(Calendar.MONTH) + 1) }
    var selectedYear by remember { mutableStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

    var dropdownEmployeeExp by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val displayedSlips = payslipsList.filter { 
        it.month == selectedMonth && it.year == selectedYear &&
        (selectedEmployeeForSlip == null || it.employeeId == selectedEmployeeForSlip?.id)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(text = "Payroll Ledger Portal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)
        Text(text = "Calculate and download ledger payslips", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

        Spacer(modifier = Modifier.height(16.dp))

        // Trigger / Selector Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(text = "Run Salary Formulas", fontWeight = FontWeight.Bold, color = NavyPrimary)

                // Employee select
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = selectedEmployeeForSlip?.name ?: "Generate for specific member...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = {
                            IconButton(onClick = { dropdownEmployeeExp = true }) {
                                Icon(imageVector = Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().clickable { dropdownEmployeeExp = true }
                    )
                    DropdownMenu(expanded = dropdownEmployeeExp, onDismissRequest = { dropdownEmployeeExp = false }) {
                        employeesList.forEach { emp ->
                            DropdownMenuItem(text = { Text(emp.name) }, onClick = { selectedEmployeeForSlip = emp; dropdownEmployeeExp = false })
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = "$selectedMonth",
                        onValueChange = { selectedMonth = it.toIntOrNull() ?: selectedMonth },
                        label = { Text("Month (1-12)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = "$selectedYear",
                        onValueChange = { selectedYear = it.toIntOrNull() ?: selectedYear },
                        label = { Text("Year") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        val emp = selectedEmployeeForSlip
                        if (emp == null) {
                            Toast.makeText(context, "Please configure target employee first", Toast.LENGTH_SHORT).show()
                        } else {
                            viewModel.generatePayslip(emp, selectedMonth, selectedYear) {
                                Toast.makeText(context, "Payroll calculation completed!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("GENERATE EMPLOYEE PAYSLIP", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // History slip records list
        Text(text = "Generated Ledgers — ${getMonthName(selectedMonth)} $selectedYear", fontWeight = FontWeight.Bold, color = NavyPrimary)
        Spacer(modifier = Modifier.height(8.dp))

        if (displayedSlips.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No matching ledger lists found for filters.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedSlips) { slip ->
                    val emp = employeesList.find { it.id == slip.employeeId }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { emp?.let { onNavigate(SubScreen.PayslipDetail(it.id, slip.month, slip.year)) } },
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = emp?.name ?: "Unknown", fontWeight = FontWeight.Bold, color = NavyPrimary)
                                Text(text = "Attended days: ${slip.totalDaysWorked} | Deducted absents: ₹${slip.deductions.toInt()}", fontSize = 11.sp, color = Color.Gray)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(text = "₹${formatIndianCurrency(slip.netSalary)}", color = NavyPrimary, fontWeight = FontWeight.Black)
                                Spacer(modifier = Modifier.height(4.dp))
                                if (slip.isPaid) {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(GreenSuccess.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("PAID", color = GreenSuccess, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Box(modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(RedError.copy(alpha = 0.12f)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                        Text("UNPAID", color = RedError, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Pixel-Perfect Ledger Payslip Detail View ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PayslipDetailsSubScreen(
    viewModel: OfficeViewModel,
    employeeId: Int,
    month: Int,
    year: Int,
    onBack: () -> Unit
) {
    val employeesList by viewModel.employees.collectAsState()
    val payslipsList by viewModel.payslips.collectAsState()

    val employee = remember(employeesList) { employeesList.find { it.id == employeeId } }
    val payslip = remember(payslipsList) { payslipsList.find { it.employeeId == employeeId && it.month == month && it.year == year } }

    val context = LocalContext.current

    if (employee == null || payslip == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ledger invoice mismatch or deleted.")
        }
        return
    }

    Scaffold(
        topBar = {
            OptInTopAppBar(
                title = { Text("Payslip Ledger Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val f = viewModel.getPayslipPdfFile(context, employee, payslip)
                            if (f != null && f.exists()) {
                                Toast.makeText(context, "Slip downloaded matching: ${f.absolutePath}", Toast.LENGTH_LONG).show()
                                sharePayslipPdf(context, f)
                            } else {
                                Toast.makeText(context, "Core drawing engine fault.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(imageVector = Icons.Filled.Share, contentDescription = "Export share", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Receipt Canvas styling
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "SALARY SLIP", fontWeight = FontWeight.Black, fontSize = 20.sp, color = NavyPrimary)
                    Text(text = "${getMonthName(month)} $year", fontSize = 14.sp, color = TealAccent, fontWeight = FontWeight.Bold)

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "EMPLOYEE CORPORATE PROFILE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Text(text = "Name: ${employee.name}", fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Text(text = "Designation: ${employee.designation}")
                    Text(text = "Department: ${employee.department}")
                    Text(text = "Daily Rate: ₹${formatIndianCurrency(employee.salaryPerDay)}")

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "ATTENDANCE RECAP", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Present Working Days: ${payslip.totalDaysWorked}")
                        Text(text = "Half-Days: ${payslip.halfDays}")
                    }
                    Text(text = "Unpaid Absences: ${payslip.leaves}")

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "PAYROLL CALCULATED SUMMARY", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.Gray)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Gross Calculated Salary:")
                        Text(text = "₹${formatIndianCurrency(payslip.grossSalary)}", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Absence Deductions Applied:")
                        Text(text = "- ₹${formatIndianCurrency(payslip.deductions)}", color = Color.Red, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(NavyPrimary.copy(alpha = 0.08f))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "NET DISBURSED PAY:", fontWeight = FontWeight.Bold, color = NavyPrimary)
                            Text(text = "₹${formatIndianCurrency(payslip.netSalary)}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = NavyPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Ledger Stamps Status (Requirement)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (payslip.isPaid) {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(GreenSuccess).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text("PAID STAMP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            Box(modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(RedError).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                Text("UNPAID LEDGER", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                        Text(text = "Ledger Disbursed Date: ${payslip.generatedDate}", fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }

            if (!payslip.isPaid) {
                Button(
                    onClick = {
                        viewModel.markPayslipAsPaid(payslip)
                        Toast.makeText(context, "Payroll ticket disbursed!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenSuccess),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Icon(imageVector = Icons.Filled.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("MARK TICKET AS DISBURSED / PAID", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// --- Settings, Reports & Utilities screen (Tab 4) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsHomeScreen(viewModel: OfficeViewModel, onNavigate: (SubScreen) -> Unit) {
    val companyName by viewModel.companyName.collectAsState()
    val adminName by viewModel.adminName.collectAsState()
    val workingHrsS by viewModel.workingHoursStart.collectAsState()
    val workingHrsE by viewModel.workingHoursEnd.collectAsState()
    val holidaysList by viewModel.holidays.collectAsState()

    var editableCompanyName by remember { mutableStateOf(companyName) }
    var editableAdminName by remember { mutableStateOf(adminName) }
    var hrsStart by remember { mutableStateOf(workingHrsS) }
    var hrsEnd by remember { mutableStateOf(workingHrsE) }

    var holidayDateInput by remember { mutableStateOf("") }

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(text = "System Console Settings", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = NavyPrimary)

        // General rules Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Company Profile Controls", fontWeight = FontWeight.Bold, color = NavyPrimary)
                
                OutlinedTextField(
                    value = editableCompanyName,
                    onValueChange = { editableCompanyName = it },
                    label = { Text("Company branding name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = editableAdminName,
                    onValueChange = { editableAdminName = it },
                    label = { Text("Admins Username") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = hrsStart,
                        onValueChange = { hrsStart = it },
                        label = { Text("Start (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hrsEnd,
                        onValueChange = { hrsEnd = it },
                        label = { Text("Finish (HH:mm)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Button(
                    onClick = {
                        viewModel.updateCompanySettings(editableCompanyName, hrsStart, hrsEnd, 5)
                        Toast.makeText(context, "System records updated successfully!", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Apply Rules Changes", fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- OTA Dynamic Content & Database API Sync Controls ---
        val syncUrl by viewModel.syncUrl.collectAsState()
        val lastSyncTime by viewModel.lastSyncTime.collectAsState()
        val syncResultState by viewModel.syncResultState.collectAsState()
        val isSyncing by viewModel.isSyncing.collectAsState()

        var inputSyncUrl by remember { mutableStateOf(syncUrl) }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F4F7)),
            border = BorderStroke(1.dp, Color(0xFFD0D5DD))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "Dynamic Sync",
                        tint = NavyPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "OTA API & Remote Database Sync",
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                
                Text(
                    text = "Update app content, business rules, holiday lists, and employee rosters dynamically over-the-air from an online JSON API without reinstalling the APK.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                OutlinedTextField(
                    value = inputSyncUrl,
                    onValueChange = { 
                        inputSyncUrl = it
                        viewModel.updateSyncUrl(it)
                    },
                    label = { Text("API Sync Endpoint URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.syncFromRemote() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Text("Sync API Now", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Button(
                        onClick = { viewModel.applyMockServerPayload() },
                        enabled = !isSyncing,
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Simulate Sync", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                if (syncResultState != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .border(1.dp, Color(0xFFC8E6C9), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = syncResultState ?: "",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearSyncStatus() },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF2E7D32),
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Last Sync Status: $lastSyncTime", fontSize = 10.sp, color = Color.Gray)
                    Text(
                        text = "Reset URL",
                        fontSize = 11.sp,
                        color = NavyPrimary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            inputSyncUrl = "https://raw.githubusercontent.com/aistudio-cdn/office-management/main/dynamic_config.json"
                            viewModel.updateSyncUrl(inputSyncUrl)
                        }
                    )
                }
            }
        }

        // --- Export Config JSON for GitHub ---
        var generatedJson by remember { mutableStateOf("") }
        var showJsonDialog by remember { mutableStateOf(false) }

        val gToken by viewModel.githubToken.collectAsState()
        val gRepo by viewModel.githubRepo.collectAsState()
        val gBranch by viewModel.githubBranch.collectAsState()
        val gPath by viewModel.githubPath.collectAsState()
        val gPushState by viewModel.githubPushState.collectAsState()
        val isPushingG by viewModel.isPushingToGithub.collectAsState()

        var inputGToken by remember { mutableStateOf(gToken) }
        var inputGRepo by remember { mutableStateOf(gRepo) }
        var inputGBranch by remember { mutableStateOf(gBranch) }
        var inputGPath by remember { mutableStateOf(gPath) }
        var showSettingsPanel by remember { mutableStateOf(false) }

        LaunchedEffect(gToken, gRepo, gBranch, gPath) {
            inputGToken = gToken
            inputGRepo = gRepo
            inputGBranch = gBranch
            inputGPath = gPath
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(
                            imageVector = Icons.Filled.CloudUpload,
                            contentDescription = "Export JSON",
                            tint = NavyPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "GitHub Dynamic Auto-Sync",
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    IconButton(onClick = { showSettingsPanel = !showSettingsPanel }) {
                        Icon(
                            imageVector = if (showSettingsPanel) Icons.Filled.ArrowDropUp else Icons.Filled.Settings,
                            contentDescription = "Toggle Settings",
                            tint = NavyPrimary
                        )
                    }
                }

                Text(
                    text = "Sync all local changes (company settings, active employee rosters, work schedules) to other users' apps instantly. Configure your repo variables once under the settings gear, and push updates dynamically with 1-click!",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                if (showSettingsPanel || gToken.isEmpty() || gRepo.isEmpty()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFF8FAFC), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Repository Target Settings",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = NavyPrimary
                        )

                        OutlinedTextField(
                            value = inputGToken,
                            onValueChange = { inputGToken = it },
                            label = { Text("GitHub Token (PAT)", fontSize = 11.sp) },
                            placeholder = { Text("ghp_xxxxxxxxxxxxxxxxxxxxxx", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        OutlinedTextField(
                            value = inputGRepo,
                            onValueChange = { inputGRepo = it },
                            label = { Text("GitHub Repository Path", fontSize = 11.sp) },
                            placeholder = { Text("username/repository-name", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                        )

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = inputGBranch,
                                onValueChange = { inputGBranch = it },
                                label = { Text("Branch", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )

                            OutlinedTextField(
                                value = inputGPath,
                                onValueChange = { inputGPath = it },
                                label = { Text("File Path", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(2f),
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
                            )
                        }

                        Button(
                            onClick = {
                                viewModel.updateGithubSettings(
                                    inputGToken,
                                    inputGRepo,
                                    inputGBranch,
                                    inputGPath
                                )
                                showSettingsPanel = false
                                Toast.makeText(context, "Settings saved!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Target Parameters", fontSize = 12.sp)
                        }
                    }
                }

                // Status banner if pushing to GitHub
                if (gPushState != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (gPushState?.startsWith("Error") == true || gPushState?.startsWith("Failed") == true) Color(0xFFFEF2F2) else Color(0xFFEFF6FF),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (gPushState?.startsWith("Error") == true || gPushState?.startsWith("Failed") == true) Color(0xFFFECACA) else Color(0xFFBFDBFE),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (gPushState?.startsWith("Success") == true) Icons.Filled.CheckCircle else Icons.Filled.Error,
                                contentDescription = null,
                                tint = if (gPushState?.startsWith("Success") == true) Color(0xFF2563EB) else Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = gPushState ?: "",
                                fontSize = 11.sp,
                                color = if (gPushState?.startsWith("Success") == true) Color(0xFF1E40AF) else Color(0xFF991B1B),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { viewModel.clearGithubPushState() },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(imageVector = Icons.Filled.Close, contentDescription = "Clear Status", modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.pushCurrentStateToGitHub { success, message ->
                                if (success) {
                                    Toast.makeText(context, "Successfully synchronized with GitHub!", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(context, "Sync Failed: $message", Toast.LENGTH_LONG).show()
                                }
                            }
                        },
                        enabled = !isPushingG,
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1.3f)
                    ) {
                        if (isPushingG) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(imageVector = Icons.Filled.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isPushingG) "Pushing to GitHub..." else "Push to GitHub",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.generateCurrentStateJSON { json ->
                                generatedJson = json
                                showJsonDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Filled.DataObject, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Get Raw JSON", color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        if (showJsonDialog) {
            AlertDialog(
                onDismissRequest = { showJsonDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Active App JSON Configuration", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showJsonDialog = false }) {
                            Icon(imageVector = Icons.Filled.Close, contentDescription = "Close")
                        }
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "Copy the complete text contents below and paste them into your GitHub repository file.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFF8FAFC))
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                Text(
                                    text = generatedJson,
                                    fontSize = 10.sp,
                                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                val clip = android.content.ClipData.newPlainText("Dynamic Config", generatedJson)
                                clipboardManager.setPrimaryClip(clip)
                                Toast.makeText(context, "Copied JSON configuration code to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Copy Code", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                try {
                                    val sendIntent = Intent().apply {
                                        action = Intent.ACTION_SEND
                                        putExtra(Intent.EXTRA_TEXT, generatedJson)
                                        type = "text/plain"
                                    }
                                    val shareIntent = Intent.createChooser(sendIntent, "Share JSON Configuration")
                                    context.startActivity(shareIntent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Share failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF64748B)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(imageVector = Icons.Filled.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Share Text", fontSize = 12.sp)
                        }
                    }
                }
            )
        }

        // Export metrics Card (Requirement)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Excel CSV Reports Engine", fontWeight = FontWeight.Bold, color = NavyPrimary)
                Text(text = "Compile month attendance sheets and disburse sums", fontSize = 11.sp, color = Color.Gray)

                Button(
                    onClick = {
                        val cal = Calendar.getInstance()
                        viewModel.exportReport(context, cal.get(Calendar.MONTH) + 1, cal.get(Calendar.YEAR)) { file ->
                            if (file != null && file.exists()) {
                                Toast.makeText(context, "Exported successfully: ${file.name}", Toast.LENGTH_LONG).show()
                                shareFile(context, file, "text/csv")
                            } else {
                                Toast.makeText(context, "Compilation fail.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(imageVector = Icons.Filled.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("COMPILE & EXPORT TODAY'S MONTH CSV")
                }
            }
        }

        // Holiday schedule modifier (Requirement)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Holiday calendar Schedule", fontWeight = FontWeight.Bold, color = NavyPrimary)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = holidayDateInput,
                        onValueChange = { holidayDateInput = it },
                        placeholder = { Text("yyyy-MM-dd") },
                        modifier = Modifier.weight(1f)
                    )
                    Button(
                        onClick = {
                            if (holidayDateInput.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                                viewModel.addHoliday(holidayDateInput)
                                holidayDateInput = ""
                                Toast.makeText(context, "Added Holiday date!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Format required: yyyy-MM-dd", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Text("Add")
                    }
                }

                if (holidaysList.isEmpty()) {
                    Text(text = "No non-working holiday overrides defined.", fontSize = 11.sp, color = Color.Gray)
                } else {
                    Text(text = "Holiday exemptions list:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NavyPrimary)
                    Column {
                        holidaysList.forEach { hol ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = formatDateFriendly(hol), fontSize = 13.sp)
                                IconButton(onClick = { viewModel.removeHoliday(hol) }) {
                                    Icon(imageVector = Icons.Filled.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // System seeds reset controls (Requirement)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(text = "Seeder demo & resets Desk", fontWeight = FontWeight.Bold, color = Color.Red)
                Text(text = "Load synthetic databases instantly to test operations graphs.", fontSize = 11.sp, color = Color.Gray)

                Button(
                    onClick = {
                        viewModel.seedDemoData {
                            Toast.makeText(context, "Seed complete! 5 Employees & 30 days history loaded.", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("LOAD SYNTHETIC DEMO DATASET", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        // Resets first setup PIN
                        context.getSharedPreferences("office_prefs", Context.MODE_PRIVATE).edit().clear().apply()
                        Toast.makeText(context, "Internal specs reset complete. Relaunch app.", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                    border = BorderStroke(1.dp, Color.Red),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("FACTORY RESTORES RULES PIN", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Switch Portal / Exit Administrative Console
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F0)),
            border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.2f))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Security Session", fontWeight = FontWeight.Bold, color = RedError)
                Text(text = "Exit Administrative Console and return to the main Portal selection lobby.", fontSize = 11.sp, color = Color.Gray)

                Button(
                    onClick = {
                        viewModel.logout()
                        Toast.makeText(context, "Logged out of Administrative system.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError),
                    modifier = Modifier.fillMaxWidth().testTag("logout_admin_button")
                ) {
                    Icon(imageVector = Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Exit Administrative Portal", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// --- Share sheet utilities ---
fun shareFile(context: Context, file: File, mimeType: String) {
    try {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Export Document"))
    } catch (e: Exception) {
        Toast.makeText(context, "Share fault: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun sharePayslipPdf(context: Context, f: File) {
    shareFile(context, f, "application/pdf")
}

// --- Opt-in Material 3 top bars helper ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OptInTopAppBar(
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors
) {
    TopAppBar(
        title = title,
        navigationIcon = navigationIcon,
        actions = actions,
        colors = colors
    )
}

// --- Help rendering statuses ---
@Composable
fun StatusChip(status: String) {
    val chipColor = when (status) {
        "Present" -> GreenSuccess
        "Half-Day" -> OrangeWarning
        "Leave" -> BlueInfo
        "Absent" -> RedError
        else -> Color.Gray
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(chipColor.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.uppercase(),
            color = chipColor,
            fontWeight = FontWeight.Black,
            fontSize = 9.sp
        )
    }
}

// --- Utility parsing helpers ---
fun getInitials(name: String): String {
    val parts = name.trim().split("\\s+".toRegex())
    return if (parts.size >= 2) {
        "${parts[0].take(1)}${parts[1].take(1)}".uppercase()
    } else if (parts.isNotEmpty()) {
        parts[0].take(2).uppercase()
    } else {
        "OP"
    }
}

fun getDeptColor(dept: String): Color {
    return when (dept) {
        "Engineering" -> NavyPrimary
        "HR" -> TealAccent
        "Sales" -> Color(0xFFEF6C00)
        "Marketing" -> Color(0xFFC2185B)
        "Finance" -> Color(0xFF00796B)
        "Operations" -> Color(0xFF5D4037)
        else -> Color.Gray
    }
}

fun formatIndianCurrency(amount: Double): String {
    return String.format(Locale("en", "IN"), "%,.2f", amount)
}

fun getMonthName(month: Int): String {
    return when (month) {
        1 -> "January"
        2 -> "February"
        3 -> "March"
        4 -> "April"
        5 -> "May"
        6 -> "June"
        7 -> "July"
        8 -> "August"
        9 -> "September"
        10 -> "October"
        11 -> "November"
        12 -> "December"
        else -> ""
    }
}

fun formatDateFriendly(dateStr: String): String {
    return try {
        val originalFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val targetFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val date = originalFormat.parse(dateStr)
        if (date != null) targetFormat.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}

// --- Dynamic Portal Zone Selection Screen ---
@Composable
fun ZoneSelectionScreen(
    viewModel: OfficeViewModel,
    onSelectAdmin: () -> Unit,
    onSelectEmployee: () -> Unit
) {
    val companyName by viewModel.companyName.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NavyPrimary)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White)
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.CorporateFare,
                        contentDescription = "Office",
                        tint = NavyPrimary,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = companyName,
                    color = Color.White,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "SELECT WORK PORTAL LOBBY",
                    color = TealAccent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.W600,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Option 1: Employee Portal (ESS)
            Card(
                onClick = onSelectEmployee,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_employee_portal")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(TealAccent.copy(alpha = 0.2f))
                            .border(1.dp, TealAccent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Badge,
                            contentDescription = "Employee Badge",
                            tint = TealAccent,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Employee Terminal",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Biometric Check-In/Out & personal pay slips",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Go",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            // Option 2: Admin Panel
            Card(
                onClick = onSelectAdmin,
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.12f)),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("select_admin_portal")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.15f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.7f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AdminPanelSettings,
                            contentDescription = "Superuser Shield",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Administrative Console",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Manage shifts, team rosters, and process payroll",
                            color = Color.White.copy(alpha = 0.65f),
                            fontSize = 11.sp
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowRight,
                        contentDescription = "Go",
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
            Text(
                text = "Enterprise Node secure verification.",
                fontSize = 10.sp,
                color = Color.White.copy(alpha = 0.4f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// --- Employee Self-Service Panel Screen ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeePortalScreen(
    viewModel: OfficeViewModel,
    onBack: () -> Unit
) {
    val employeesList by viewModel.employees.collectAsState()
    val attendanceList by viewModel.attendance.collectAsState()
    val payslipsList by viewModel.payslips.collectAsState()
    val companyName by viewModel.companyName.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableStateOf(0) } // 0: Attendance, 1: Payslips

    // Selected Payslip detailed overlay
    var detailedPayslip by remember { mutableStateOf<Payslip?>(null) }

    val context = LocalContext.current
    val todayDateString = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    val portalEmployeeId by viewModel.portalEmployeeId.collectAsState()
    val isPortalUnlocked by viewModel.isPortalUnlocked.collectAsState()

    // Find the logged-in employee (ESS Session)
    val currentEmployee = remember(employeesList, portalEmployeeId) {
        employeesList.find { it.id == portalEmployeeId && it.isActive }
    }

    if (currentEmployee == null) {
        // If employee is not logged into profile, show the profile directory selector list
        Scaffold(
            topBar = {
                OptInTopAppBar(
                    title = { Text("Employee Portal Hub") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyPrimary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = LightBlueBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Identify Profile",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = NavyPrimary
                        )
                        Text(
                            text = "Please touch/click your profile to check-in/out and view your verified payslips.",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search your name...") },
                    leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        cursorColor = NavyPrimary
                    )
                )

                val activeEmployees = remember(employeesList, searchQuery) {
                    employeesList.filter {
                        it.isActive && (it.name.contains(searchQuery, ignoreCase = true) || it.department.contains(searchQuery, ignoreCase = true))
                    }
                }

                if (activeEmployees.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(imageVector = Icons.Filled.PeopleOutline, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
                            Text("No employees registered or name mismatch.", color = Color.Gray, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(activeEmployees) { emp ->
                            Card(
                                onClick = {
                                    viewModel.setPortalEmployee(emp.id)
                                    activeTab = 0
                                },
                                modifier = Modifier.fillMaxWidth().testTag("employee_profile_${emp.id}"),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(getDeptColor(emp.department).copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = getInitials(emp.name),
                                            fontWeight = FontWeight.Bold,
                                            color = getDeptColor(emp.department),
                                            fontSize = 14.sp
                                        )
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(text = emp.name, fontWeight = FontWeight.Bold, color = NavyPrimary, fontSize = 15.sp)
                                        Text(text = "${emp.designation} • ${emp.department}", fontSize = 11.sp, color = Color.Gray)
                                    }

                                    Icon(
                                        imageVector = Icons.Filled.ChevronRight,
                                        contentDescription = null,
                                        tint = Color.LightGray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else if (!isPortalUnlocked) {
        val emp = currentEmployee
        var pinInput by remember { mutableStateOf("") }
        var isPinError by remember { mutableStateOf(false) }

        Scaffold(
            topBar = {
                OptInTopAppBar(
                    title = { Text("Profile Protection") },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setPortalEmployee(null) }) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = NavyPrimary,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = LightBlueBg
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))
                
                // Employee initials badge
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .clip(CircleShape)
                        .background(getDeptColor(emp.department).copy(alpha = 0.15f))
                        .border(2.dp, getDeptColor(emp.department), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getInitials(emp.name),
                        fontWeight = FontWeight.Bold,
                        color = getDeptColor(emp.department),
                        fontSize = 32.sp
                    )
                }

                // Greeting & Info
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Hello, ${emp.name}",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = NavyPrimary,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "${emp.designation} • ${emp.department}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // PIN indicator circles
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until 4) {
                        val isFilled = pinInput.length > i
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isFilled) NavyPrimary else Color.LightGray.copy(alpha = 0.5f))
                                .border(1.5.dp, if (isFilled) NavyPrimary else Color.Gray, CircleShape)
                        )
                    }
                }

                // Optional error text
                if (isPinError) {
                    Text(
                        text = "Incorrect security PIN. Please try again.",
                        color = Color.Red,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // OutlinedTextField for entering PIN
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            pinInput = input
                            isPinError = false
                            if (input.length == 4) {
                                val success = viewModel.unlockPortal(input, emp)
                                if (success) {
                                    Toast.makeText(context, "Access Granted!", Toast.LENGTH_SHORT).show()
                                } else {
                                    isPinError = true
                                    pinInput = "" // Clear PIN on failure
                                }
                            }
                        }
                    },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = androidx.compose.ui.text.TextStyle(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 20.sp, fontWeight = FontWeight.Bold, letterSpacing = 8.sp),
                    singleLine = true,
                    label = { Text("4-Digit Security PIN") },
                    modifier = Modifier
                        .width(220.dp)
                        .testTag("portal_pin_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NavyPrimary,
                        cursorColor = NavyPrimary
                    )
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Switch account option
                TextButton(
                    onClick = { viewModel.setPortalEmployee(null) },
                    modifier = Modifier.testTag("portal_switch_account_btn")
                ) {
                    Text(
                        text = "Not you? Switch Profile",
                        color = NavyPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    } else {
        // ESS Single-Employee Portal Layout
        val emp = currentEmployee
        val personalAttendance = remember(attendanceList, emp.id) {
            attendanceList.filter { it.employeeId == emp.id }.sortedByDescending { it.date }
        }
        val personalPayslips = remember(payslipsList, emp.id) {
            payslipsList.filter { it.employeeId == emp.id }.sortedByDescending { "${it.year}-${String.format("%02d", it.month)}" }
        }

        val todayRecord = remember(attendanceList, emp.id, todayDateString) {
            attendanceList.find { it.employeeId == emp.id && it.date == todayDateString }
        }

        // Detailed overlay subscreen if select to view item
        if (detailedPayslip != null) {
            val dp = detailedPayslip!!
            PayslipDetailsSubScreen(
                viewModel = viewModel,
                employeeId = emp.id,
                month = dp.month,
                year = dp.year,
                onBack = { detailedPayslip = null }
            )
            return
        }

        Scaffold(
            topBar = {
                OptInTopAppBar(
                    title = {
                        Column {
                            Text(text = emp.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            Text(text = "${emp.designation} • ${emp.department}", fontSize = 10.sp, color = Color.White.copy(alpha = 0.8f))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { viewModel.setPortalEmployee(null) }) {
                            Icon(imageVector = Icons.Filled.Logout, contentDescription = "Log out", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = TealAccent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = LightBlueBg,
            bottomBar = {
                NavigationBar(containerColor = Color.White, modifier = Modifier.navigationBarsPadding()) {
                    NavigationBarItem(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        icon = { Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null) },
                        label = { Text("Daily Punch-In", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealAccent, selectedTextColor = TealAccent, indicatorColor = TealAccent.copy(alpha = 0.15f))
                    )
                    NavigationBarItem(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        icon = { Icon(imageVector = Icons.Filled.ReceiptLong, contentDescription = null) },
                        label = { Text("My Salary Slips", fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = TealAccent, selectedTextColor = TealAccent, indicatorColor = TealAccent.copy(alpha = 0.15f))
                    )
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(14.dp)
            ) {
                if (activeTab == 0) {
                    // Daily Punch block
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Biometric Shift Attendance",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyPrimary
                        )

                        // Real-time Punch Status card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val currentHourMinutes = remember { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()) }

                                if (todayRecord == null) {
                                    // Punch In setup
                                    Text("CHECK-IN punch REQUIRED", fontWeight = FontWeight.Black, fontSize = 13.sp, color = OrangeWarning)
                                    Text("Simulated Scan Boundary. Take selfie and teleport to office to clock-in.", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Center)

                                    var testDistancePunch by remember { mutableStateOf(4.5f) }
                                    var isSelfieTakenPunch by remember { mutableStateOf(false) }
                                    val isInsideOffice = testDistancePunch <= 10.0f

                                    // Display Scan distance stats
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isInsideOffice) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isInsideOffice) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                                            contentDescription = null,
                                            tint = if (isInsideOffice) GreenSuccess else RedError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Boundary Check: " + if (isInsideOffice) "Inside HQ Center" else "Outside Office Radius",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isInsideOffice) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                        )
                                    }

                                    Slider(
                                        value = testDistancePunch,
                                        onValueChange = { testDistancePunch = it },
                                        valueRange = 1.0f..25.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = TealAccent,
                                            activeTrackColor = TealAccent
                                        )
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        TextButton(onClick = { testDistancePunch = 2.0f }) {
                                            Text("Move inside office (2m)", fontSize = 10.sp, color = TealAccent)
                                        }
                                        TextButton(onClick = { testDistancePunch = 18.0f }) {
                                            Text("Go outside radius (18m)", fontSize = 10.sp, color = RedError)
                                        }
                                    }

                                    // Camera Sandbox
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                    ) {
                                        if (!isSelfieTakenPunch) {
                                            SelfieCameraPreviewMock(
                                                employeeName = emp.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFE8EAF6), RoundedCornerShape(12.dp))
                                                    .border(1.dp, GreenSuccess, RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    SelfieAvatar(
                                                        employeeId = emp.id,
                                                        name = emp.name,
                                                        modifier = Modifier.size(60.dp),
                                                        isCheckOut = false
                                                    )
                                                    Text("Face Verified ✓", color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    TextButton(onClick = { isSelfieTakenPunch = false }) {
                                                        Text("Retake biometric scan", fontSize = 10.sp, color = NavyPrimary)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!isSelfieTakenPunch) {
                                        Button(
                                            onClick = { isSelfieTakenPunch = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Process Face Scan", fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.markAttendance(
                                                    employeeId = emp.id,
                                                    dateString = todayDateString,
                                                    status = "Present",
                                                    checkIn = currentHourMinutes,
                                                    checkOut = null,
                                                    notes = "Biometric Check-In Verified",
                                                    checkInSelfie = "selfie_${emp.id}_in",
                                                    checkOutSelfie = null,
                                                    checkInDistance = testDistancePunch.toDouble(),
                                                    checkOutDistance = null
                                                )
                                                Toast.makeText(context, "Shift Punch-In Completed!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                                            enabled = isInsideOffice && isSelfieTakenPunch,
                                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("employee_punch_in"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.Fingerprint, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PUNCH SHIFT IN NOW", fontWeight = FontWeight.Black)
                                        }
                                    }

                                } else if (todayRecord.checkOutTime == null) {
                                    // Punch Out setup
                                    Text(" punch-in logs complete: Present", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = GreenSuccess)
                                    Text("Entered dynamic shift at: ${todayRecord.checkInTime}", fontWeight = FontWeight.Black, fontSize = 16.sp, color = NavyPrimary)
                                    Text("Swipe scanning for Clock Out active.", fontSize = 10.sp, color = Color.Gray)

                                    var testDistancePunchOut by remember { mutableStateOf(3.2f) }
                                    var isSelfieTakenPunchOut by remember { mutableStateOf(false) }
                                    val isInsideOfficeOut = testDistancePunchOut <= 10.0f

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                if (isInsideOfficeOut) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isInsideOfficeOut) Icons.Filled.LocationOn else Icons.Filled.LocationOff,
                                            contentDescription = null,
                                            tint = if (isInsideOfficeOut) GreenSuccess else RedError,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            "Boundary Check: " + if (isInsideOfficeOut) "Verified at Site" else "Out of boundaries",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isInsideOfficeOut) Color(0xFF1B5E20) else Color(0xFFB71C1C)
                                        )
                                    }

                                    Slider(
                                        value = testDistancePunchOut,
                                        onValueChange = { testDistancePunchOut = it },
                                        valueRange = 1.0f..25.0f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = TealAccent,
                                            activeTrackColor = TealAccent
                                        )
                                    )

                                    // Camera Sandbox
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(130.dp)
                                    ) {
                                        if (!isSelfieTakenPunchOut) {
                                            SelfieCameraPreviewMock(
                                                employeeName = emp.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(Color(0xFFE8EAF6), RoundedCornerShape(12.dp))
                                                    .border(1.dp, GreenSuccess, RoundedCornerShape(12.dp)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                                    SelfieAvatar(
                                                        employeeId = emp.id,
                                                        name = emp.name,
                                                        modifier = Modifier.size(60.dp),
                                                        isCheckOut = true
                                                    )
                                                    Text("Face Verified ✓", color = GreenSuccess, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                                    TextButton(onClick = { isSelfieTakenPunchOut = false }) {
                                                        Text("Retake biometric scan", fontSize = 10.sp, color = NavyPrimary)
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (!isSelfieTakenPunchOut) {
                                        Button(
                                            onClick = { isSelfieTakenPunchOut = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                            modifier = Modifier.fillMaxWidth().height(36.dp),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Process Face Scan", fontSize = 11.sp)
                                        }
                                    } else {
                                        Button(
                                            onClick = {
                                                viewModel.markAttendance(
                                                    employeeId = emp.id,
                                                    dateString = todayDateString,
                                                    status = todayRecord.status,
                                                    checkIn = todayRecord.checkInTime,
                                                    checkOut = currentHourMinutes,
                                                    notes = "Biometric Clock-Out Completed",
                                                    checkInSelfie = todayRecord.checkInSelfie,
                                                    checkOutSelfie = "selfie_${emp.id}_out",
                                                    checkInDistance = todayRecord.checkInDistance,
                                                    checkOutDistance = testDistancePunchOut.toDouble()
                                                )
                                                Toast.makeText(context, "Shift Punch-Out Completed!", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = RedError),
                                            enabled = isInsideOfficeOut && isSelfieTakenPunchOut,
                                            modifier = Modifier.fillMaxWidth().height(40.dp).testTag("employee_punch_out"),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(imageVector = Icons.Filled.DirectionsWalk, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("PUNCH SHIFT OUT NOW", fontWeight = FontWeight.Black)
                                        }
                                    }

                                } else {
                                    // Punch complete for today
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = GreenSuccess,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Text(
                                        "Shift completed for today",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 15.sp,
                                        color = GreenSuccess
                                    )
                                    Text(
                                        text = " Punch-In Hour: ${todayRecord.checkInTime} • Punch-Out Hour: ${todayRecord.checkOutTime}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NavyPrimary
                                    )
                                    Text(
                                        text = "Biometric check details and GPS verification are logged.",
                                        fontSize = 10.sp,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        // Shift journal logs Below
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "My Attendance Logs History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = NavyPrimary
                        )

                        if (personalAttendance.isEmpty()) {
                            Text("No previously entered records on this ledger.", fontSize = 11.sp, color = Color.Gray)
                        } else {
                            personalAttendance.forEach { att ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(0.5.dp, Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                text = formatDateFriendly(att.date),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = NavyPrimary
                                            )
                                            if (att.checkInTime != null) {
                                                Text(
                                                    text = "Punch: ${att.checkInTime} to ${att.checkOutTime ?: "--:--"}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    if (att.checkInSelfie != null) {
                                                        Icon(imageVector = Icons.Filled.Check, contentDescription = null, tint = GreenSuccess, modifier = Modifier.size(10.dp))
                                                        Text("Selfie", fontSize = 9.sp, color = GreenSuccess, fontWeight = FontWeight.Bold)
                                                    }
                                                    val dist = att.checkOutDistance ?: att.checkInDistance
                                                    if (dist != null) {
                                                        Text("• GPS: %.1fm".format(dist), fontSize = 9.sp, color = NavyPrimary)
                                                    }
                                                }
                                            }
                                        }
                                        StatusChip(status = att.status)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Payslips History display section
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "My Payroll Ledger History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NavyPrimary
                        )

                        if (personalPayslips.isEmpty()) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Icon(imageVector = Icons.Filled.Receipt, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                                    Text("No payslips prepared yet for this financial account.", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        } else {
                            personalPayslips.forEach { ps ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                                ) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "${getMonthName(ps.month)} ${ps.year}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 15.sp,
                                                    color = NavyPrimary
                                                )
                                                Text(
                                                    text = "Gross: ₹${formatIndianCurrency(ps.grossSalary)}",
                                                    fontSize = 11.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(
                                                        if (ps.isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                                    )
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            ) {
                                                Text(
                                                    text = if (ps.isPaid) "PAID" else "PENDING",
                                                    color = if (ps.isPaid) GreenSuccess else OrangeWarning,
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }

                                        Divider(color = Color.LightGray.copy(alpha = 0.4f))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(text = "NET DISBURSED AMOUNT", fontSize = 9.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                                                Text(text = "₹${formatIndianCurrency(ps.netSalary)}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = TealAccent)
                                            }

                                            Button(
                                                onClick = { detailedPayslip = ps },
                                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                                shape = RoundedCornerShape(8.dp),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(32.dp).testTag("employee_view_payslip_${ps.month}_${ps.year}")
                                            ) {
                                                Icon(imageVector = Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("View Invoice", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

