package com.marsa.smarttrackerhub.ui.screens.investers

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
import com.itextpdf.layout.element.Cell
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.TextAlignment
import com.itextpdf.layout.properties.UnitValue
import com.marsa.smarttrackerhub.data.dao.SettlementEntryWithName
import com.marsa.smarttrackerhub.data.entity.YearEndSettlement
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Generates a properly formatted A4 PDF settlement report (one section + per-investor
 * table per settlement) and shares it via the Android share sheet. Reuses the iText7
 * dependency already in the project (see [com.marsa.smarttrackerhub.utils.PdfExportUtil]).
 *
 * Currency is rendered as "AED" (Helvetica can't draw the dirham glyph), so amounts
 * display cleanly in any PDF viewer.
 */
object SettlementPdfExporter {

    private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
    private val fileStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ENGLISH)
    private val headerBg = DeviceRgb(15, 64, 36)        // dark green
    private val zebraBg = DeviceRgb(245, 245, 243)      // warm light grey
    private val muted = DeviceRgb(110, 110, 110)

    private fun money(v: Double) = "AED " + String.format(Locale.ENGLISH, "%,.2f", v)

    fun generateAndShareSettlementPdf(
        context: Context,
        shopName: String,
        settlements: List<YearEndSettlement>,
        entriesBySettlement: Map<Int, List<SettlementEntryWithName>>
    ) {
        try {
            val fileName = "settlement_report_${shopName.replace(" ", "_")}_${fileStamp.format(Date())}.pdf"
            val cacheDir = File(context.cacheDir, "pdfs").also { it.mkdirs() }
            val file = File(cacheDir, fileName)

            val document = Document(PdfDocument(PdfWriter(file)), PageSize.A4)
            document.setMargins(28f, 28f, 28f, 28f)

            val font = PdfFontFactory.createFont(StandardFonts.HELVETICA)
            val bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD)

            // ── Title block ──
            document.add(
                Paragraph("Investor Settlement Report")
                    .setFont(bold).setFontSize(18f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2f)
            )
            document.add(
                Paragraph(shopName)
                    .setFont(bold).setFontSize(12f)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(2f)
            )
            document.add(
                Paragraph("Generated: ${dateFormat.format(Date())}")
                    .setFont(font).setFontSize(9f).setFontColor(muted)
                    .setTextAlignment(TextAlignment.CENTER).setMarginBottom(18f)
            )

            if (settlements.isEmpty()) {
                document.add(Paragraph("No settlements recorded.").setFont(font).setFontSize(11f))
                document.close()
                share(context, file)
                return
            }

            // ── One section per settlement ──
            settlements.forEachIndexed { index, settlement ->
                val periodStart = if (settlement.periodStartDate == 0L) "Beginning"
                else dateFormat.format(Date(settlement.periodStartDate))
                val periodEnd = dateFormat.format(Date(settlement.settlementDate))

                document.add(
                    Paragraph("Settlement ${index + 1}  ·  $periodEnd")
                        .setFont(bold).setFontSize(13f).setMarginTop(8f).setMarginBottom(2f)
                )
                document.add(
                    Paragraph("Period: $periodStart  →  $periodEnd      Total Invested: ${money(settlement.totalInvested)}")
                        .setFont(font).setFontSize(10f).setFontColor(muted).setMarginBottom(if (settlement.note.isBlank()) 8f else 2f)
                )
                if (settlement.note.isNotBlank()) {
                    document.add(
                        Paragraph("Note: ${settlement.note}")
                            .setFont(font).setFontSize(9f).setItalic().setFontColor(muted).setMarginBottom(8f)
                    )
                }

                val entries = entriesBySettlement[settlement.id] ?: emptyList()
                val table = Table(UnitValue.createPercentArray(floatArrayOf(26f, 20f, 20f, 16f, 18f)))
                    .setWidth(UnitValue.createPercentValue(100f))
                    .setMarginBottom(16f)

                headerCell(table, "Investor", bold, TextAlignment.LEFT)
                headerCell(table, "Fair Share", bold, TextAlignment.RIGHT)
                headerCell(table, "Actual Paid", bold, TextAlignment.RIGHT)
                headerCell(table, "Balance", bold, TextAlignment.RIGHT)
                headerCell(table, "Status", bold, TextAlignment.CENTER)

                if (entries.isEmpty()) {
                    val c = Cell(1, 5).add(Paragraph("No investor entries").setFont(font).setFontSize(9f).setFontColor(muted))
                        .setTextAlignment(TextAlignment.CENTER).setPadding(8f)
                    table.addCell(c)
                } else {
                    entries.forEachIndexed { rowIdx, e ->
                        val zebra = rowIdx % 2 == 1
                        val balance = (if (e.balanceAmount >= 0) "+" else "-") +
                            "AED " + String.format(Locale.ENGLISH, "%,.2f", kotlin.math.abs(e.balanceAmount))
                        val status = when {
                            e.balanceAmount == 0.0 -> "Settled"
                            e.settlementPaidAmount > 0.0 -> {
                                val d = e.settlementPaidDate?.let { " ${dateFormat.format(Date(it))}" } ?: ""
                                "Paid$d"
                            }
                            else -> "Outstanding"
                        }
                        bodyCell(table, e.investorName, font, TextAlignment.LEFT, zebra)
                        bodyCell(table, money(e.fairShareAmount), font, TextAlignment.RIGHT, zebra)
                        bodyCell(table, money(e.actualPaidAmount), font, TextAlignment.RIGHT, zebra)
                        bodyCell(table, balance, font, TextAlignment.RIGHT, zebra)
                        bodyCell(table, status, font, TextAlignment.CENTER, zebra)
                    }
                }
                document.add(table)
            }

            document.add(
                Paragraph("Generated by TrackerHub")
                    .setFont(font).setFontSize(8f).setFontColor(muted)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(8f)
            )

            document.close()
            share(context, file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun headerCell(table: Table, text: String, font: PdfFont, align: TextAlignment) {
        table.addHeaderCell(
            Cell().add(Paragraph(text).setFont(font).setFontSize(10f).setFontColor(DeviceRgb(255, 255, 255)))
                .setBackgroundColor(headerBg)
                .setTextAlignment(align)
                .setPadding(6f)
        )
    }

    private fun bodyCell(table: Table, text: String, font: PdfFont, align: TextAlignment, zebra: Boolean) {
        val cell = Cell().add(Paragraph(text).setFont(font).setFontSize(9f))
            .setTextAlignment(align)
            .setPadding(6f)
        if (zebra) cell.setBackgroundColor(zebraBg)
        table.addCell(cell)
    }

    private fun share(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Settlement Report")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share Settlement Report"))
    }
}
