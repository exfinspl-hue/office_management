package com.example.utils

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

object ReportService {

    data class CsvReportRecord(
        val name: String,
        val department: String,
        val presentDays: Int,
        val halfDays: Int,
        val absentDays: Int,
        val leaveDays: Int,
        val totalWorkHours: Double,
        val grossSalary: Double,
        val deductions: Double,
        val netSalary: Double
    )

    fun exportMonthlyReport(
        context: Context,
        month: Int,
        year: Int,
        records: List<CsvReportRecord>
    ): File? {
        val headers = "Employee Name,Department,Total Present,Half Days,Total Absent,Leaves,Total Work Hours,Gross Salary,Deductions,Net Salary\n"
        val csvContent = StringBuilder(headers)

        for (rec in records) {
            csvContent.append(escapeCsvField(rec.name)).append(",")
            csvContent.append(escapeCsvField(rec.department)).append(",")
            csvContent.append(rec.presentDays).append(",")
            csvContent.append(rec.halfDays).append(",")
            csvContent.append(rec.absentDays).append(",")
            csvContent.append(rec.leaveDays).append(",")
            csvContent.append(String.format(Locale.US, "%.1f", rec.totalWorkHours)).append(",")
            csvContent.append(String.format(Locale.US, "%.2f", rec.grossSalary)).append(",")
            csvContent.append(String.format(Locale.US, "%.2f", rec.deductions)).append(",")
            csvContent.append(String.format(Locale.US, "%.2f", rec.netSalary)).append("\n")
        }

        return try {
            val directory = context.getExternalFilesDir("reports") ?: context.filesDir
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val fileName = "Monthly_Report_${month}_${year}.csv"
            val file = File(directory, fileName)
            val outputStream = FileOutputStream(file)
            outputStream.write(csvContent.toString().toByteArray())
            outputStream.flush()
            outputStream.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsvField(field: String): String {
        return if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
    }
}
