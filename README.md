# 📱 Office Management System (Android)

An offline-first, highly secure, and visually stunning native Android application designed to manage employee records, automate daily attendance logging, calculate complex monthly payroll structures, and generate business reports. Built with robust modern architectures, optimized native rendering, and compliant Material Design 3 guidelines.

---

## 🎨 Visual Identity & Brand Consistency

The application features a premium corporate styling theme centered on trust, modernism, and clarity:
*   **Navy Primary (`#1A237E`)**: Establishes a professional, high-contrast visual anchor.
*   **Teal Accent (`#00897B`)**: Drives primary action indications, interactive tabs, and touch callbacks.
*   **M3 Material Principles**: Utilizing consistent 12dp card boundaries, elegant negative space, and smooth layout fade transitions (via `Crossfade` and `animateContentSize`).
*   **Custom Ring & Bar Charts**: Implemented utilizing native hardware-accelerated `Canvas` drawing to visually profile today's attendance roster and monthly departmental payouts.

---

## 🛠️ Technology Stack & Architecture

*   **Runtime Language**: Kotlin 2.0+
*   **UI Framework**: Jetpack Compose using Material Design 3 (M3)
*   **State Management**: MVVM Architecture backed by standard Kotlin `StateFlow` and dynamic structured ViewModel events.
*   **Data Persistence**: Room SQLite Database (using modern asynchronous Kotlin `Flow` querying & lightweight KSP processors).
*   **Utilities & Services**:
    *   **Indian Rupee (₹) Presentation**: Fully supports localized double-grouping standards ("₹XX,XX,XXX.XX").
    *   **Automated Payroll Service**: Integrates active holiday, calendar period days, daily wage rates, and half-day factors.
    *   **Native File Provider**: Generates and securely shares corporate PDF payslips and exports monthly department CSV logs.

---

## 📂 Core Package Structure

```
com.example.
├── MainActivity.kt               # Central Activity binding the App Theme and ViewModel
├── data/
│   ├── Models.kt                  # Room database schemas (Employee, Attendance, Payslip)
│   ├── Daos.kt                    # Asynchronous CRUD DAO queries returning Kotlin Flow
│   ├── AppDatabase.kt             # Thread-safe database instance singleton
│   └── OfficeRepository.kt        # Abstract business boundary mapping ViewModel inputs
├── utils/
│   ├── PayslipCalculator.kt       # Wage details, holiday factor & deduction utility
│   ├── PdfService.kt              # PDF document renderer with clean corporate layout
│   └── ReportService.kt           # CSV generator for monthly database dumps
├── viewmodel/
│   └── OfficeViewModel.kt         # Secure PIN validation, timeout logic, data caches
└── ui/
    ├── theme/
    │   ├── Color.kt               # Navy primary and Teal accent palette definitions
    │   ├── Theme.kt               # Central M3 Light/Dark Application Theme
    │   └── Type.kt                # Structured typography layout configurations
    └── screens/
        └── OfficeScreens.kt       # Central modular composables (Splash, Setup, Login, Dashboard, etc.)
```

---

## 🚀 Key Functional Modules

1.  **Splash & Custom Setup Wizard**: Guides the manager through initial company setup (Company name, system PIN configuration) on the first launch.
2.  **Corporate PIN Authentication**: Protects company files with a 4-6 digit numeric login screen featuring a 30-second security timeout warning after 3 incorrect attempts.
3.  **Dynamic Admin Dashboard**: Displays real-time operational statistics, visual canvas ring charts mapping active attendance rates, and horizontal payout bar charts across departments.
4.  **Employee Directory**: Full CRUD capabilities supporting department allocation, wage settings, joining dates, email notifications, and active statuses.
5.  **Multi-State Attendance Logger**: Quick-action, responsive dialozgs to log daily check-ins, custom work hours, out-of-office notes, and holiday alignments. Includes custom month calendar grid displays.
6.  **Salary & Payslip Generation**: Automates monthly salary calculation. Includes responsive ledger views, instant corporate PDF creation, and CSV payroll exports.

---

## 🔨 Compilation and Verification

To verify, test, or package the application bundle locally, execute these tasks from the root workspace directory.

### Build and Package (Debug APK)
```bash
gradle assembleDebug
```

### Run Local Unit & Robolectric Tests
```bash
gradle :app:testDebugUnitTest
```
