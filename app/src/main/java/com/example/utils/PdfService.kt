package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Employee
import com.example.data.Payslip
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.util.Locale

object PdfService {

    fun generatePayslipPdf(
        context: Context,
        employee: Employee,
        payslip: Payslip,
        companyName: String
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1A237E") // Deep Navy
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subTitlePaint = Paint().apply {
            color = Color.parseColor("#00897B") // Teal
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val boldTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val headerBackgroundPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }
        val paidBadgePaint = Paint().apply {
            color = if (payslip.isPaid) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
            style = Paint.Style.FILL
        }
        val badgeTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        var y = 60f
        val startX = 40f
        val endX = 555f

        // Draw Header
        canvas.drawText(companyName.uppercase(Locale.getDefault()), startX, y, titlePaint)
        y += 24f
        canvas.drawText("SALARY SLIP — ${getMonthName(payslip.month)} ${payslip.year}", startX, y, subTitlePaint)
        y += 15f
        canvas.drawLine(startX, y, endX, y, linePaint)
        y += 25f

        // Employee Info
        canvas.drawText("Employee Details", startX, y, boldTextPaint)
        y += 20f
        canvas.drawText("ID: EMP-${String.format(Locale.getDefault(), "%04d", employee.id)}", startX, y, textPaint)
        canvas.drawText("Name: ${employee.name}", startX + 250f, y, textPaint)
        y += 18f
        canvas.drawText("Designation: ${employee.designation}", startX, y, textPaint)
        canvas.drawText("Department: ${employee.department}", startX + 250f, y, textPaint)
        y += 18f
        canvas.drawText("Email: ${employee.email}", startX, y, textPaint)
        canvas.drawText("Phone: ${employee.phone}", startX + 250f, y, textPaint)
        y += 18f
        canvas.drawText("Join Date: ${employee.joinDate}", startX, y, textPaint)
        canvas.drawText("Daily Salary: ₹${formatCurrency(employee.salaryPerDay)}", startX + 250f, y, textPaint)
        y += 25f
        canvas.drawLine(startX, y, endX, y, linePaint)
        y += 20f

        // Attendance Records
        canvas.drawText("Attendance Records Summary", startX, y, boldTextPaint)
        y += 20f
        canvas.drawText("Days Present: ${payslip.totalDaysWorked}", startX, y, textPaint)
        canvas.drawText("Half Days: ${payslip.halfDays}", startX + 160f, y, textPaint)
        canvas.drawText("Unpaid Absences: ${payslip.leaves}", startX + 320f, y, textPaint)
        y += 25f
        canvas.drawLine(startX, y, endX, y, linePaint)
        y += 25f

        // Table Header
        canvas.drawRect(startX, y, endX, y + 25f, headerBackgroundPaint)
        canvas.drawText("Particulars", startX + 10f, y + 17f, boldTextPaint)
        canvas.drawText("Amount (₹)", endX - 100f, y + 17f, boldTextPaint)
        y += 25f

        // Row 1: Gross Earnings
        y += 22f
        canvas.drawText("Gross Earnings (worked days, half days, overtime)", startX + 10f, y, textPaint)
        canvas.drawText("₹${formatCurrency(payslip.grossSalary)}", endX - 100f, y, textPaint)

        // Row 2: Deductions
        y += 22f
        canvas.drawText("Deductions (unpaid absences)", startX + 10f, y, textPaint)
        canvas.drawText("- ₹${formatCurrency(payslip.deductions)}", endX - 100f, y, textPaint)
        
        y += 25f
        canvas.drawLine(startX, y, endX, y, linePaint)
        y += 25f

        // Net Salary Row
        canvas.drawText("NET PAYOUT:", startX + 10f, y, boldTextPaint)
        val netSalaryText = "₹${formatCurrency(payslip.netSalary)}"
        val largeBoldPaint = Paint().apply {
            color = Color.parseColor("#1A237E")
            textSize = 16f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(netSalaryText, endX - 120f, y, largeBoldPaint)

        y += 40f
        // Stamp Status
        val stampText = if (payslip.isPaid) "PAID" else "UNPAID"
        canvas.drawRect(startX + 10f, y, startX + 110f, y + 25f, paidBadgePaint)
        canvas.drawText(stampText, startX + 40f, y + 17f, badgeTextPaint)

        // Signature
        val rightAlignX = endX - 160f
        canvas.drawLine(rightAlignX, y + 15f, endX - 10f, y + 15f, linePaint)
        canvas.drawText("Authorized Signature", rightAlignX + 15f, y + 30f, textPaint)

        pdfDocument.finishPage(page)

        return try {
            val directory = context.getExternalFilesDir("payslips") ?: context.filesDir
            if (!directory.exists()) {
                directory.mkdirs()
            }
            val file = File(directory, "Payslip_EMP_${employee.id}_${payslip.month}_${payslip.year}.pdf")
            val outputStream = FileOutputStream(file)
            pdfDocument.writeTo(outputStream)
            outputStream.flush()
            outputStream.close()
            pdfDocument.close()
            file
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }

    private fun getMonthName(month: Int): String {
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

    private fun formatCurrency(amount: Double): String {
        return String.format(Locale("en", "IN"), "%,.2f", amount)
    }
}
