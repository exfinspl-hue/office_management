package com.example.data

import kotlinx.coroutines.flow.Flow

class OfficeRepository(
    private val employeeDao: EmployeeDao,
    private val attendanceDao: AttendanceDao,
    private val payslipDao: PayslipDao
) {
    // --- Employee Operations ---
    val allEmployees: Flow<List<Employee>> = employeeDao.getAllEmployees()

    fun getEmployeeById(id: Int): Flow<Employee?> = employeeDao.getEmployeeById(id)

    suspend fun getEmployeeByIdSuspend(id: Int): Employee? = employeeDao.getEmployeeByIdSuspend(id)

    suspend fun insertEmployee(employee: Employee): Long = employeeDao.insertEmployee(employee)

    suspend fun updateEmployee(employee: Employee) = employeeDao.updateEmployee(employee)

    suspend fun deleteEmployee(employee: Employee) = employeeDao.deleteEmployee(employee)


    // --- Attendance Operations ---
    val allAttendance: Flow<List<Attendance>> = attendanceDao.getAllAttendance()

    fun getAttendanceForEmployee(employeeId: Int): Flow<List<Attendance>> = 
        attendanceDao.getAttendanceForEmployee(employeeId)

    fun getAttendanceByDate(date: String): Flow<List<Attendance>> = 
        attendanceDao.getAttendanceByDate(date)

    suspend fun getAttendanceForEmployeeAndDate(employeeId: Int, date: String): Attendance? = 
        attendanceDao.getAttendanceForEmployeeAndDate(employeeId, date)

    fun getAttendanceForEmployeeInMonth(employeeId: Int, monthYear: String): Flow<List<Attendance>> {
        // monthYear is expected in "yyyy-MM" format, so the SQL query should match "yyyy-MM-%"
        return attendanceDao.getAttendanceForEmployeeInMonth(employeeId, "$monthYear-%")
    }

    fun getAttendanceInDateRange(startDate: String, endDate: String): Flow<List<Attendance>> = 
        attendanceDao.getAttendanceInDateRange(startDate, endDate)

    suspend fun insertAttendance(attendance: Attendance): Long = attendanceDao.insertAttendance(attendance)

    suspend fun updateAttendance(attendance: Attendance) = attendanceDao.updateAttendance(attendance)

    suspend fun deleteAttendance(attendance: Attendance) = attendanceDao.deleteAttendance(attendance)


    // --- Payslip Operations ---
    val allPayslips: Flow<List<Payslip>> = payslipDao.getAllPayslips()

    fun getPayslipsForEmployee(employeeId: Int): Flow<List<Payslip>> = 
        payslipDao.getPayslipsForEmployee(employeeId)

    suspend fun getPayslipForEmployeeMonthYear(employeeId: Int, month: Int, year: Int): Payslip? = 
        payslipDao.getPayslipForEmployeeMonthYear(employeeId, month, year)

    suspend fun insertPayslip(payslip: Payslip): Long = payslipDao.insertPayslip(payslip)

    suspend fun updatePayslip(payslip: Payslip) = payslipDao.updatePayslip(payslip)

    suspend fun deletePayslip(payslip: Payslip) = payslipDao.deletePayslip(payslip)
}
