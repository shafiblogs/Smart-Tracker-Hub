package com.marsa.smarttrackerhub.utils

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.itextpdf.io.font.constants.StandardFonts
import com.itextpdf.kernel.colors.DeviceRgb
import com.itextpdf.kernel.font.PdfFont
import com.itextpdf.kernel.font.PdfFontFactory
import com.itextpdf.kernel.geom.PageSize
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.borders.Border
import com.itextpdf.layout.borders.SolidBorder
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.HorizontalAlignment
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.marsa.smarttrackerhub.ui.screens.logs.DayLogSummary
import com.marsa.smarttrackerhub.ui.screens.logs.EmployeeDayRecord
import com.marsa.smarttrackerhub.ui.screens.logs.EmployeeMonthSummary
import com.marsa.smarttrackerhub.ui.screens.logs.ShopMonthSummary
import java.io.File

/**
 * Utility for generating and sharing Shop Logs as PDF
 */
object PdfExportUtil {

    /**
     * Generates a PDF from shop logs data and shares it
     *
     * @param context Application context
     * @param shopName Name of the shop
     * @param monthDisplay Display label for the month (e.g., "June 2026")
     * @param daySummaries List of daily summaries with session data
     * @param monthSummary Monthly summary with total statistics
     */
    fun generateAndShareShopLogsPdf(
        context: Context,
        shopName: String,
        monthDisplay: String,
        daySummaries: List<DayLogSummary>,
        monthSummary: ShopMonthSummary?
    ) {
        try {
            val fileName = "shop_logs_${shopName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val cachePath = File(context.cacheDir, "pdfs")
            cachePath.mkdirs()

            val file = File(cachePath, fileName)
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(20f, 20f, 20f, 20f)

            // Get font (using built-in Helvetica which supports basic text)
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

            // Header with shop name and month
            val headerParagraph = Paragraph(shopName)
                .setFont(boldFont)
                .setFontSize(18f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4f)
            document.add(headerParagraph)

            val monthParagraph = Paragraph(monthDisplay)
                .setFont(font)
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2f)
            document.add(monthParagraph)
            document.add(generatedParagraph(font))

            // Stats section
            if (monthSummary != null) {
                val statsTable = Table(UnitValue.createPercentArray(3))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(16f)

                // Stats headers
                addTableCell(statsTable, "Days Open", font, 10f, true)
                addTableCell(statsTable, "Total", font, 10f, true)
                addTableCell(statsTable, "Average", font, 10f, true)

                // Stats values
                addTableCell(statsTable, "${monthSummary.totalDaysOpen}", font, 11f, false)
                addTableCell(statsTable, monthSummary.totalMinutes.toHoursLabel(), font, 11f, false)
                addTableCell(statsTable, monthSummary.avgMinutesPerDay.toHoursLabel(), font, 11f, false)

                document.add(statsTable)
            }

            // Shop logs table
            val logsTable = Table(UnitValue.createPercentArray(5))
                .setWidth(UnitValue.createPercentValue(100f))

            // Table headers
            addTableCell(logsTable, "Date", font, 10f, true)
            addTableCell(logsTable, "Open", font, 10f, true)
            addTableCell(logsTable, "Close", font, 10f, true)
            addTableCell(logsTable, "Duration", font, 10f, true)
            addTableCell(logsTable, "Total", font, 10f, true)

            // Table data
            for (day in daySummaries) {
                val dateNumber = try {
                    day.date.split("-")[2]
                } catch (e: Exception) {
                    "01"
                }

                if (day.sessions.isNotEmpty()) {
                    for ((index, session) in day.sessions.withIndex()) {
                        val isFirstSession = index == 0
                        val isLastSession = index == day.sessions.size - 1

                        // Date column (only on first session of the day)
                        if (isFirstSession) {
                            addTableCell(logsTable, dateNumber, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = true)
                        } else {
                            addTableCell(logsTable, "", font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = false)
                        }

                        // In time
                        val inTime = formatLogTime(session.openTime)
                        addTableCell(logsTable, inTime, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = isFirstSession)

                        // Out time
                        val outTime = if (session.closeTime != null) {
                            formatLogTime(session.closeTime)
                        } else {
                            "--"
                        }
                        addTableCell(logsTable, outTime, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = isFirstSession)

                        // Duration
                        val duration = session.durationMinutes.toHoursLabel()
                        addTableCell(logsTable, duration, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = isFirstSession)

                        // Total (only on last session of the day)
                        if (isLastSession) {
                            addTableCell(logsTable, day.totalMinutes.toHoursLabel(), font, 9f, false, hasBottomBorder = true, isFirstSessionOfDay = isFirstSession)
                        } else {
                            addTableCell(logsTable, "", font, 9f, false, hasBottomBorder = false, isFirstSessionOfDay = isFirstSession)
                        }
                    }
                }
            }

            document.add(logsTable)
            document.close()

            // Share the PDF
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Shop Logs"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addTableCell(
        table: Table,
        text: String,
        font: PdfFont,
        fontSize: Float,
        isHeader: Boolean,
        hasBottomBorder: Boolean = true,
        isFirstSessionOfDay: Boolean = true
    ) {
        val para = Paragraph(text).setFont(font).setFontSize(fontSize)
        if (isHeader) para.setFontColor(DeviceRgb(255, 255, 255))   // white text on the header band
        val cell = Cell()
            .add(para)
            .setTextAlignment(TextAlignment.CENTER)
            .setPadding(8f)

        // Set borders
        val topBorder = if (isFirstSessionOfDay || isHeader) SolidBorder(0.5f) else Border.NO_BORDER
        val bottomBorder = if (hasBottomBorder) SolidBorder(0.5f) else Border.NO_BORDER
        val leftBorder = SolidBorder(0.5f)
        val rightBorder = SolidBorder(0.5f)

        cell.setBorderTop(topBorder)
        cell.setBorderBottom(bottomBorder)
        cell.setBorderLeft(leftBorder)
        cell.setBorderRight(rightBorder)

        if (isHeader) {
            cell.setBold()
            cell.setBackgroundColor(DeviceRgb(15, 64, 36))          // dark-green header band
        }

        table.addCell(cell)
    }

    /**
     * Generates a PDF from employee logs data and shares it
     *
     * @param context Application context
     * @param employeeName Name of the employee
     * @param shopName Name of the associated shop
     * @param monthDisplay Display label for the month (e.g., "June 2026")
     * @param dayRecords List of daily records with session data
     * @param monthSummary Monthly summary with total statistics
     */
    fun generateAndShareEmployeeLogsPdf(
        context: Context,
        employeeName: String,
        shopName: String,
        monthDisplay: String,
        dayRecords: List<EmployeeDayRecord>,
        monthSummary: EmployeeMonthSummary?
    ) {
        try {
            val fileName = "employee_logs_${employeeName.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val cachePath = File(context.cacheDir, "pdfs")
            cachePath.mkdirs()

            val file = File(cachePath, fileName)
            val pdfWriter = PdfWriter(file)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument, PageSize.A4)
            document.setMargins(20f, 20f, 20f, 20f)

            // Get font (using built-in Helvetica which supports basic text)
            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            val boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

            // Header with employee name
            val headerParagraph = Paragraph(employeeName)
                .setFont(boldFont)
                .setFontSize(18f)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4f)
            document.add(headerParagraph)

            // Shop and month info
            val infoParagraph = Paragraph("$shopName · $monthDisplay")
                .setFont(font)
                .setFontSize(12f)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(2f)
            document.add(infoParagraph)
            document.add(generatedParagraph(font))

            // Stats section
            if (monthSummary != null) {
                val statsTable = Table(UnitValue.createPercentArray(3))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(16f)

                // Stats headers
                addTableCell(statsTable, "Days Worked", font, 10f, true)
                addTableCell(statsTable, "Total", font, 10f, true)
                addTableCell(statsTable, "Average", font, 10f, true)

                // Stats values
                addTableCell(statsTable, "${monthSummary.totalDays}", font, 11f, false)
                addTableCell(statsTable, monthSummary.totalMinutes.toHoursLabel(), font, 11f, false)
                addTableCell(statsTable, monthSummary.avgMinutesPerDay.toHoursLabel(), font, 11f, false)

                document.add(statsTable)
            }

            // Employee logs table
            val logsTable = Table(UnitValue.createPercentArray(5))
                .setWidth(UnitValue.createPercentValue(100f))

            // Table headers
            addTableCell(logsTable, "Date", font, 10f, true)
            addTableCell(logsTable, "In", font, 10f, true)
            addTableCell(logsTable, "Out", font, 10f, true)
            addTableCell(logsTable, "Duration", font, 10f, true)
            addTableCell(logsTable, "Total", font, 10f, true)

            // Table data
            for (day in dayRecords) {
                val dateNumber = try {
                    day.date.split("-")[2]
                } catch (e: Exception) {
                    "01"
                }

                if (day.sessions.isNotEmpty()) {
                    for ((index, session) in day.sessions.withIndex()) {
                        val isFirstSession = index == 0
                        val isLastSession = index == day.sessions.size - 1

                        // Date column (only on first session of the day)
                        if (isFirstSession) {
                            addTableCell(logsTable, dateNumber, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = true)
                        } else {
                            addTableCell(logsTable, "", font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = false)
                        }

                        // In time
                        val inTime = formatLogTime(session.loginTime)
                        addTableCell(logsTable, inTime, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = isFirstSession)

                        // Out time
                        val outTime = if (session.logoutTime != null) {
                            formatLogTime(session.logoutTime)
                        } else {
                            "--"
                        }
                        addTableCell(logsTable, outTime, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = isFirstSession)

                        // Duration
                        val duration = session.durationMinutes.toHoursLabel()
                        addTableCell(logsTable, duration, font, 9f, false, hasBottomBorder = isLastSession, isFirstSessionOfDay = isFirstSession)

                        // Total (only on last session of the day)
                        if (isLastSession) {
                            addTableCell(logsTable, day.totalMinutes.toHoursLabel(), font, 9f, false, hasBottomBorder = true, isFirstSessionOfDay = isFirstSession)
                        } else {
                            addTableCell(logsTable, "", font, 9f, false, hasBottomBorder = false, isFirstSessionOfDay = isFirstSession)
                        }
                    }
                }
            }

            document.add(logsTable)
            document.close()

            // Share the PDF
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, contentUri)
                type = "application/pdf"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(shareIntent, "Share Employee Logs"))

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /** Standard "Generated: <date>" subtitle used on both reports. */
    private fun generatedParagraph(font: PdfFont): Paragraph =
        Paragraph(
            "Generated: " + java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.ENGLISH)
                .format(java.util.Date())
        )
            .setFont(font)
            .setFontSize(9f)
            .setFontColor(DeviceRgb(110, 110, 110))
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(16f)

    private fun formatLogTime(timestamp: Long): String {
        val dateFormat = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
        return dateFormat.format(java.util.Date(timestamp))
    }
}

private fun Long.toHoursLabel(): String {
    val h = this / 60
    val m = this % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
