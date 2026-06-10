package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Query("SELECT * FROM employees ORDER BY name ASC")
    fun getAllEmployees(): Flow<List<Employee>>

    @Query("SELECT * FROM employees WHERE id = :id")
    fun getEmployeeById(id: Int): Flow<Employee?>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getEmployeeByIdSuspend(id: Int): Employee?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmployee(employee: Employee): Long

    @Update
    suspend fun updateEmployee(employee: Employee)

    @Delete
    suspend fun deleteEmployee(employee: Employee)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance ORDER BY date DESC")
    fun getAllAttendance(): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId ORDER BY date DESC")
    fun getAttendanceForEmployee(employeeId: Int): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date = :date")
    fun getAttendanceByDate(date: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId AND date = :date LIMIT 1")
    suspend fun getAttendanceForEmployeeAndDate(employeeId: Int, date: String): Attendance?

    @Query("SELECT * FROM attendance WHERE employeeId = :employeeId AND date LIKE :monthQuery ORDER BY date ASC")
    fun getAttendanceForEmployeeInMonth(employeeId: Int, monthQuery: String): Flow<List<Attendance>>

    @Query("SELECT * FROM attendance WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getAttendanceInDateRange(startDate: String, endDate: String): Flow<List<Attendance>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: Attendance): Long

    @Update
    suspend fun updateAttendance(attendance: Attendance)

    @Delete
    suspend fun deleteAttendance(attendance: Attendance)
}

@Dao
interface PayslipDao {
    @Query("SELECT * FROM payslips ORDER BY year DESC, month DESC")
    fun getAllPayslips(): Flow<List<Payslip>>

    @Query("SELECT * FROM payslips WHERE employeeId = :employeeId ORDER BY year DESC, month DESC")
    fun getPayslipsForEmployee(employeeId: Int): Flow<List<Payslip>>

    @Query("SELECT * FROM payslips WHERE employeeId = :employeeId AND month = :month AND year = :year LIMIT 1")
    suspend fun getPayslipForEmployeeMonthYear(employeeId: Int, month: Int, year: Int): Payslip?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayslip(payslip: Payslip): Long

    @Update
    suspend fun updatePayslip(payslip: Payslip)

    @Delete
    suspend fun deletePayslip(payslip: Payslip)
}
