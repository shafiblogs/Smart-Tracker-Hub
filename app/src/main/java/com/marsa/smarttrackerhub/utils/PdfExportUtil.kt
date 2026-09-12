package com.marsa.smarttrackerhub.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.marsa.smarttrackerhub.ui.screens.logs.DayLogSummary
import com.marsa.smarttrackerhub.ui.screens.logs.EmployeeDayRecord
import com.marsa.smarttrackerhub.ui.screens.logs.EmployeeMonthSummary
import com.marsa.smarttrackerhub.ui.screens.logs.ShopMonthSummary
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility for generating and sharing Shop/Employee Logs as PDF.
 *
 * Uses the platform's native android.graphics.pdf.PdfDocument instead of iText7: iText7 relies
 * on reflection in several places and ships no consumer ProGuard rules, so under R8
 * (isMinifyEnabled=true on release, see app/build.gradle.kts) PDF generation was silently
 * failing in production while working fine in debug — the whole call is wrapped in
 * try/catch { e.printStackTrace() }, so the Share button just did nothing. AccountsTracker hit
 * the same class of R8 issue and already solved it this way (see its
 * ui/screens/logs/LogsPdfExport.kt); this ports that same approach here.
 *
 * Over the 200-line util soft cap: the shared pagination/drawing routine (buildLogsPdf) is one
 * responsibility (render a two-table logs report) that both entry points need identically —
 * splitting it into a second file would just move code around, not simplify it. Same tradeoff
 * accepted in AccountsTracker's equivalent file (267 lines).
 */
object PdfExportUtil {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 20f
    private val HEADER_GREEN = Color.rgb(15, 64, 36)
    private val GRAY_TEXT = Color.rgb(110, 110, 110)
    private const val ROW_HEIGHT = 22f
    private const val HEADER_ROW_HEIGHT = 20f

    /** One rendered table row. [closedLabel] non-null means "Closed"/"No record" (day had no sessions). */
    private data class PdfRow(
        val date: String,
        val inTime: String = "",
        val outTime: String = "",
        val duration: String = "",
        val total: String = "",
        val closedLabel: String? = null
    )

    fun generateAndShareShopLogsPdf(
        context: Context,
        shopName: String,
        monthDisplay: String,
        daySummaries: List<DayLogSummary>,
        monthSummary: ShopMonthSummary?
    ) {
        try {
            val rows = daySummaries.flatMap { day ->
                if (day.sessions.isEmpty()) {
                    listOf(PdfRow(date = dayNumber(day.date), closedLabel = "Closed"))
                } else {
                    day.sessions.mapIndexed { index, session ->
                        PdfRow(
                            date = if (index == 0) dayNumber(day.date) else "",
                            inTime = formatLogTime(session.openTime),
                            outTime = if (session.closeTime != null) formatLogTime(session.closeTime) else "--",
                            duration = session.durationMinutes.toHoursLabel(),
                            total = if (index == day.sessions.lastIndex) day.totalMinutes.toHoursLabel() else ""
                        )
                    }
                }
            }
            val stats = listOf(
                "Days Open" to (monthSummary?.totalDaysOpen?.toString() ?: "0"),
                "Total" to (monthSummary?.totalMinutes?.toHoursLabel() ?: "0m"),
                "Average" to (monthSummary?.avgMinutesPerDay?.toHoursLabel() ?: "0m")
            )
            val file = buildLogsPdf(context, shopName, monthDisplay, stats, rows, "shop_logs_${shopName.replace(" ", "_")}")
            sharePdf(context, file, "Share Shop Logs")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun generateAndShareEmployeeLogsPdf(
        context: Context,
        employeeName: String,
        shopName: String,
        monthDisplay: String,
        dayRecords: List<EmployeeDayRecord>,
        monthSummary: EmployeeMonthSummary?
    ) {
        try {
            val rows = dayRecords.flatMap { day ->
                if (day.sessions.isEmpty()) {
                    listOf(PdfRow(date = dayNumber(day.date), closedLabel = "No record"))
                } else {
                    day.sessions.mapIndexed { index, session ->
                        PdfRow(
                            date = if (index == 0) dayNumber(day.date) else "",
                            inTime = formatLogTime(session.loginTime),
                            outTime = if (session.logoutTime != null) formatLogTime(session.logoutTime) else "--",
                            duration = session.durationMinutes.toHoursLabel(),
                            total = if (index == day.sessions.lastIndex) day.totalMinutes.toHoursLabel() else ""
                        )
                    }
                }
            }
            val stats = listOf(
                "Days Worked" to (monthSummary?.totalDays?.toString() ?: "0"),
                "Total" to (monthSummary?.totalMinutes?.toHoursLabel() ?: "0m"),
                "Average" to (monthSummary?.avgMinutesPerDay?.toHoursLabel() ?: "0m")
            )
            val file = buildLogsPdf(
                context, employeeName, "$shopName · $monthDisplay", stats, rows,
                "employee_logs_${employeeName.replace(" ", "_")}"
            )
            sharePdf(context, file, "Share Employee Logs")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Shared drawing engine for both shop and employee logs PDFs — title/subtitle/generated line,
     * a 3-column stats table, then the 5-column (Date | In | Out | Duration | Total) day table,
     * paginating (repeating just the table header on continuation pages) when rows overflow.
     */
    private fun buildLogsPdf(
        context: Context,
        title: String,
        subtitle: String,
        stats: List<Pair<String, String>>,
        rows: List<PdfRow>,
        filePrefix: String
    ): File {
        val document = PdfDocument()
        val paint = Paint().apply { isAntiAlias = true }
        val leftMargin = MARGIN
        val rightMargin = PAGE_WIDTH - MARGIN
        val contentWidth = rightMargin - leftMargin
        val centerX = PAGE_WIDTH / 2f

        val colWidths = floatArrayOf(50f, 145f, 145f, 105f, 110f)
        val colX = FloatArray(5)
        colX[0] = leftMargin
        for (i in 1 until 5) colX[i] = colX[i - 1] + colWidths[i - 1]
        val headers = listOf("Date", "In", "Out", "Duration", "Total")

        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
        var canvas = page.canvas
        var y = MARGIN

        y += 18f
        paint.apply {
            textAlign = Paint.Align.CENTER
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            color = Color.BLACK
        }
        canvas.drawText(title, centerX, y, paint)

        y += 20f
        paint.apply {
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        canvas.drawText(subtitle, centerX, y, paint)

        y += 16f
        paint.apply { textSize = 9f; color = GRAY_TEXT }
        canvas.drawText("Generated: " + SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(Date()), centerX, y, paint)
        y += 16f

        val statColWidth = contentWidth / 3f
        paint.style = Paint.Style.FILL
        paint.color = HEADER_GREEN
        canvas.drawRect(leftMargin, y, rightMargin, y + HEADER_ROW_HEIGHT + 2f, paint)
        paint.color = Color.WHITE
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        stats.forEachIndexed { i, (label, _) ->
            canvas.drawText(label, leftMargin + statColWidth * i + statColWidth / 2f, y + HEADER_ROW_HEIGHT - 5f, paint)
        }
        y += HEADER_ROW_HEIGHT + 2f
        paint.color = Color.BLACK
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        stats.forEachIndexed { i, (_, value) ->
            canvas.drawText(value, leftMargin + statColWidth * i + statColWidth / 2f, y + HEADER_ROW_HEIGHT - 5f, paint)
        }
        y += HEADER_ROW_HEIGHT + 16f

        fun drawTableHeader() {
            paint.style = Paint.Style.FILL
            paint.color = HEADER_GREEN
            canvas.drawRect(leftMargin, y, rightMargin, y + HEADER_ROW_HEIGHT, paint)
            paint.color = Color.WHITE
            paint.textAlign = Paint.Align.CENTER
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            headers.forEachIndexed { i, h ->
                canvas.drawText(h, colX[i] + colWidths[i] / 2f, y + HEADER_ROW_HEIGHT - 6f, paint)
            }
            y += HEADER_ROW_HEIGHT
        }
        drawTableHeader()

        fun newPage() {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            y = MARGIN
            drawTableHeader()
        }

        val borderPaint = Paint().apply { color = Color.LTGRAY; strokeWidth = 0.5f; style = Paint.Style.STROKE }
        for (row in rows) {
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) newPage()
            val rowTop = y
            val rowBottom = y + ROW_HEIGHT
            val baseline = rowBottom - 7f

            paint.style = Paint.Style.FILL
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            paint.textSize = 9f
            paint.color = Color.BLACK

            if (row.closedLabel != null) {
                paint.textAlign = Paint.Align.LEFT
                canvas.drawText(row.date, colX[0] + 4f, baseline, paint)
                canvas.drawText(row.closedLabel, colX[1] + 4f, baseline, paint)
            } else {
                paint.textAlign = Paint.Align.CENTER
                if (row.date.isNotEmpty()) canvas.drawText(row.date, colX[0] + colWidths[0] / 2f, baseline, paint)
                canvas.drawText(row.inTime, colX[1] + colWidths[1] / 2f, baseline, paint)
                canvas.drawText(row.outTime, colX[2] + colWidths[2] / 2f, baseline, paint)
                canvas.drawText(row.duration, colX[3] + colWidths[3] / 2f, baseline, paint)
                if (row.total.isNotEmpty()) {
                    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    canvas.drawText(row.total, colX[4] + colWidths[4] / 2f, baseline, paint)
                }
            }

            canvas.drawLine(leftMargin, rowTop, rightMargin, rowTop, borderPaint)
            canvas.drawLine(leftMargin, rowBottom, rightMargin, rowBottom, borderPaint)
            canvas.drawLine(leftMargin, rowTop, leftMargin, rowBottom, borderPaint)
            canvas.drawLine(rightMargin, rowTop, rightMargin, rowBottom, borderPaint)
            for (i in 1 until 5) canvas.drawLine(colX[i], rowTop, colX[i], rowBottom, borderPaint)

            y = rowBottom
        }

        document.finishPage(page)

        val pdfDir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(pdfDir, "${filePrefix}_${System.currentTimeMillis()}.pdf")
        document.writeTo(FileOutputStream(file))
        document.close()
        return file
    }

    private fun sharePdf(context: Context, file: File, chooserTitle: String) {
        val contentUri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_STREAM, contentUri)
            type = "application/pdf"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, chooserTitle))
    }

    private fun dayNumber(date: String): String = try {
        date.split("-")[2]
    } catch (e: Exception) {
        "01"
    }

    private fun formatLogTime(timestamp: Long): String {
        if (timestamp == 0L) return ""
        return try { SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp)) }
        catch (e: Exception) { "" }
    }
}

private fun Long.toHoursLabel(): String {
    val h = this / 60
    val m = this % 60
    return if (h > 0) "${h}h ${m}m" else "${m}m"
}
