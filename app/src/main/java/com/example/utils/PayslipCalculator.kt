package com.example.utils

import com.example.data.Attendance
import com.example.data.Employee
import java.util.Calendar

object PayslipCalculator {
    data class CalculationResult(
        val totalDaysInMonth: Int,
        val presentDays: Int,
        val halfDays: Int,
        val absentDays: Int,
        val leaveDays: Int,
        val grossSalary: Double,
        val deductions: Double,
        val netSalary: Double,
        val totalOvertimeHours: Double
    )

    fun calculate(
        employee: Employee,
        month: Int, // 1 to 12
        year: Int,
        attendanceList: List<Attendance>
    ): CalculationResult {
        val calendar = Calendar.getInstance()
        calendar.set(year, month - 1, 1)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        var presentCount = 0
        var halfDayCount = 0
        var absentCount = 0
        var leaveCount = 0
        var overtimeHours = 0.0

        for (att in attendanceList) {
            when (att.status) {
                "Present" -> {
                    presentCount++
                    att.checkOutTime?.let { outTime ->
                        val parts = outTime.split(":")
                        if (parts.size == 2) {
                            val hour = parts[0].toIntOrNull() ?: 0
                            val min = parts[1].toIntOrNull() ?: 0
                            // Overtime starts if check-out is past 18:00 (6:00 PM)
                            if (hour >= 18) {
                                val minutesOver = (hour - 18) * 60 + min
                                overtimeHours += minutesOver / 60.0
                            }
                        }
                    }
                }
                "Half-Day" -> halfDayCount++
                "Absent" -> absentCount++
                "Leave" -> leaveCount++
            }
        }

        // Present Day earnings: presentCount * daily rate
        // Half-Day earnings: halfDayCount * daily rate * 0.5
        val presentEarning = presentCount * employee.salaryPerDay
        val halfDayEarning = halfDayCount * employee.salaryPerDay * 0.5
        
        // Simple overtime pay calculation (Assuming an 8-hour workday, rate is daily_rate / 8)
        val hourlyRate = employee.salaryPerDay / 8.0
        val overtimeEarning = overtimeHours * hourlyRate

        val grossSalary = presentEarning + halfDayEarning + overtimeEarning
        
        // Deductions calculation: absent days times daily salary
        val deductions = absentCount * employee.salaryPerDay

        val netSalary = maxOf(0.0, grossSalary - deductions)

        return CalculationResult(
            totalDaysInMonth = maxDays,
            presentDays = presentCount,
            halfDays = halfDayCount,
            absentDays = absentCount,
            leaveDays = leaveCount,
            grossSalary = grossSalary,
            deductions = deductions,
            netSalary = netSalary,
            totalOvertimeHours = overtimeHours
        )
    }
}
