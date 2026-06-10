package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(tableName = "employees")
data class Employee(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val designation: String,
    val department: String, // e.g., HR, Engineering, Sales, Marketing, Finance, Operations
    val email: String,
    val phone: String,
    val salaryPerDay: Double,
    val joinDate: String, // yyyy-MM-dd
    val isActive: Boolean = true,
    val password: String = "1234"
)

@Entity(
    tableName = "attendance",
    indices = [Index(value = ["employeeId", "date"], unique = true)]
)
data class Attendance(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val date: String, // yyyy-MM-dd
    val checkInTime: String? = null, // HH:mm
    val checkOutTime: String? = null, // HH:mm
    val status: String, // "Present", "Absent", "Half-Day", "Leave"
    val notes: String? = null,
    val checkInSelfie: String? = null,
    val checkOutSelfie: String? = null,
    val checkInDistance: Double? = null,
    val checkOutDistance: Double? = null
)

@Entity(tableName = "payslips")
data class Payslip(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val employeeId: Int,
    val month: Int, // 1 to 12
    val year: Int,
    val totalDaysWorked: Int,
    val halfDays: Int,
    val leaves: Int,
    val grossSalary: Double,
    val deductions: Double,
    val netSalary: Double,
    val generatedDate: String, // yyyy-MM-dd
    val isPaid: Boolean = false
)
