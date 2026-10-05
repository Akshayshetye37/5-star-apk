package com.example.util

import android.content.Context
import android.net.Uri
import org.w3c.dom.Element
import java.io.BufferedInputStream
import java.io.File
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Lightweight XLSX importer.
 *
 * It maps known hotel fields into the Room database, preserves every original
 * workbook column/value in the booking notes, and extracts embedded worksheet
 * images so ID photographs are not silently lost.
 */
object WorkbookImportManager {

    data class ImportedRow(
        val rowNumber: Int,
        val bookingId: String,
        val customerName: String,
        val idType: String,
        val idNumber: String,
        val contact: String,
        val whatsapp: String,
        val checkInDate: String,
        val checkOutDate: String,
        val stay: Int,
        val advance: Double,
        val paid: Double,
        val pending: Double,
        val roomNumber: String,
        val roomType: String,
        val roomCharges: Double,
        val discount: Double,
        val mineralWater: Double,
        val ghavaneChatney: Double,
        val tea: Double,
        val kandePohe: Double,
        val total: Double,
        val idPhotoPath: String = "",
        val rawColumns: Map<String, String> = emptyMap()
    )

    data class ImportResult(
        val rows: List<ImportedRow>,
        val warnings: List<String>
    )

    fun parse(context: Context, uri: Uri): ImportResult {
        val zipEntries = HashMap<String, ByteArray>()
        context.contentResolver.openInputStream(uri)?.use { input ->
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                var entry = zip.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) zipEntries[entry.name] = zip.readBytes()
                    entry = zip.nextEntry
                }
            }
        } ?: error("Unable to open workbook.")

        val sheet = zipEntries["xl/worksheets/sheet1.xml"]
            ?: error("Workbook has no first worksheet.")
        val sharedStrings = parseSharedStrings(zipEntries["xl/sharedStrings.xml"])
        val imageByRow = extractWorksheetImages(context, zipEntries)

        return parseBookingSheet(sheet, sharedStrings, imageByRow)
    }

    private fun parseSharedStrings(bytes: ByteArray?): List<String> {
        if (bytes == null) return emptyList()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(bytes.inputStream())
        val nodes = doc.getElementsByTagName("si")
        return (0 until nodes.length).map { i ->
            val si = nodes.item(i) as Element
            val texts = si.getElementsByTagName("t")
            buildString {
                for (j in 0 until texts.length) append(texts.item(j).textContent)
            }
        }
    }

    private fun parseBookingSheet(
        bytes: ByteArray,
        shared: List<String>,
        imageByRow: Map<Int, String>
    ): ImportResult {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(bytes.inputStream())
        val rows = doc.getElementsByTagName("row")
        val all = mutableListOf<Pair<Int, Map<String, String>>>()

        for (i in 0 until rows.length) {
            val row = rows.item(i) as Element
            val rowNo = row.getAttribute("r").toIntOrNull() ?: (i + 1)
            val cells = row.getElementsByTagName("c")
            val values = linkedMapOf<String, String>()
            for (j in 0 until cells.length) {
                val c = cells.item(j) as Element
                val ref = c.getAttribute("r")
                val raw = c.getElementsByTagName("v")
                val inline = c.getElementsByTagName("t")
                val value = when {
                    c.getAttribute("t") == "s" && raw.length > 0 ->
                        shared.getOrNull(raw.item(0).textContent.toIntOrNull() ?: -1).orEmpty()
                    c.getAttribute("t") == "inlineStr" && inline.length > 0 ->
                        inline.item(0).textContent
                    raw.length > 0 -> raw.item(0).textContent
                    else -> ""
                }
                values[ref] = value
            }
            all += rowNo to values
        }

        val headerRow = all.firstOrNull { (_, cells) ->
            cells.values.any { it.equals("Customer Name", true) } &&
                cells.values.any { it.contains("Room No", true) }
        } ?: error("Could not find the Booking header row. Expected 'Customer Name' and 'Room No.'.")

        val headerByColumn = headerRow.second.mapNotNull { (cell, value) ->
            if (value.isBlank()) null else columnLetters(cell) to value.trim()
        }.toMap()

        val normalizedHeaderByColumn = headerByColumn.mapValues { normalizeHeader(it.value) }
        val warnings = mutableListOf<String>()
        val result = mutableListOf<ImportedRow>()

        for ((rowNo, cells) in all) {
            if (rowNo <= headerRow.first) continue

            val rawColumns = linkedMapOf<String, String>()
            cells.forEach { (cell, value) ->
                val col = columnLetters(cell)
                val originalHeader = headerByColumn[col]
                if (!originalHeader.isNullOrBlank()) rawColumns[originalHeader] = value
            }

            val mapped = cells.mapKeys { columnLetters(it.key) }
                .mapKeys { normalizedHeaderByColumn[it.key].orEmpty() }

            val name = mapped["customer name"].orEmpty().trim()
            val room = mapped["room no."].orEmpty().trim()
                .ifBlank { mapped["room no"].orEmpty().trim() }
            if (name.isBlank() && room.isBlank()) continue
            if (name.isBlank() || room.isBlank()) {
                warnings += "Row $rowNo skipped: guest name or room number is missing."
                continue
            }

            result += ImportedRow(
                rowNumber = rowNo,
                bookingId = mapped["s. no."].orEmpty().ifBlank { "IMP-$rowNo" },
                customerName = name,
                idType = mapped["id type"].orEmpty().ifBlank { "Aadhaar Card" },
                idNumber = mapped["id no."].orEmpty(),
                contact = mapped["contact no"].orEmpty(),
                whatsapp = mapped["whatsapp"].orEmpty(),
                checkInDate = excelDate(mapped["check in date"].orEmpty()),
                checkOutDate = excelDate(mapped["check out date"].orEmpty()),
                stay = mapped["stay"].orEmpty().toIntOrNull() ?: 1,
                advance = money(mapped["advance"]),
                paid = money(mapped["paid"]),
                pending = money(mapped["pending"]),
                roomNumber = room,
                roomType = mapped["suite type"].orEmpty(),
                roomCharges = money(mapped["room charges"]),
                discount = money(mapped["discount"]),
                mineralWater = money(mapped["mineral water"]),
                ghavaneChatney = money(mapped["ghavane chatney"]),
                tea = money(mapped["tea"]),
                kandePohe = money(mapped["kande pohe"]),
                total = money(mapped["total"]),
                idPhotoPath = imageByRow[rowNo].orEmpty(),
                rawColumns = rawColumns
            )
        }
        return ImportResult(result, warnings)
    }

    /**
     * Finds worksheet drawings and maps their anchor row to the embedded image.
     * This handles the common Excel pattern used by hotel workbooks:
     * sheet1.xml -> sheet1.xml.rels -> drawing*.xml -> drawing*.xml.rels -> media/<file>.
     */
    fun extractWorksheetImages(
        context: Context,
        entries: Map<String, ByteArray>
    ): Map<Int, String> {
        val result = mutableMapOf<Int, String>()
        val sheetRels = entries["xl/worksheets/_rels/sheet1.xml.rels"] ?: return result
        val relDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(sheetRels.inputStream())
        val relationships = relDoc.getElementsByTagName("Relationship")
        val drawingTargets = mutableListOf<String>()

        for (i in 0 until relationships.length) {
            val rel = relationships.item(i) as Element
            val type = rel.getAttribute("Type")
            if (type.endsWith("/drawing")) {
                drawingTargets += resolveZipTarget("xl/worksheets", rel.getAttribute("Target"))
            }
        }

        for (drawingPath in drawingTargets) {
            val drawingBytes = entries[drawingPath] ?: continue
            val drawingRelPath = drawingPath.substringBeforeLast("/") +
                "/_rels/" + drawingPath.substringAfterLast("/") + ".rels"
            val drawingRelsBytes = entries[drawingRelPath] ?: continue
            val drawingRelDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(drawingRelsBytes.inputStream())

            val targetByRid = mutableMapOf<String, String>()
            val drawingRelationships = drawingRelDoc.getElementsByTagName("Relationship")
            for (i in 0 until drawingRelationships.length) {
                val rel = drawingRelationships.item(i) as Element
                targetByRid[rel.getAttribute("Id")] =
                    resolveZipTarget(drawingPath.substringBeforeLast("/"), rel.getAttribute("Target"))
            }

            val drawingDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(drawingBytes.inputStream())
            val anchors = drawingDoc.getElementsByTagNameNS("*", "oneCellAnchor")
            for (i in 0 until anchors.length) {
                val anchor = anchors.item(i) as Element
                val from = anchor.getElementsByTagNameNS("*", "from").item(0) as? Element ?: continue
                val rowNode = from.getElementsByTagNameNS("*", "row").item(0) ?: continue
                val row = rowNode.textContent.trim().toIntOrNull()?.plus(1) ?: continue
                val blip = anchor.getElementsByTagNameNS("*", "blip").item(0) as? Element ?: continue
                val rid = blip.getAttributeNS(
                    "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                    "embed"
                ).ifBlank { blip.getAttribute("r:embed") }
                val mediaPath = targetByRid[rid] ?: continue
                val image = entries[mediaPath] ?: continue
                val ext = mediaPath.substringAfterLast('.', "jpg").lowercase()
                val file = File(context.filesDir, "id_import_${row}_${System.nanoTime()}.$ext")
                file.writeBytes(image)
                result.putIfAbsent(row, file.absolutePath)
            }
        }
        return result
    }

    private fun resolveZipTarget(baseDir: String, target: String): String {
        val stack = baseDir.split('/').filter { it.isNotBlank() }.toMutableList()
        target.replace("\\", "/").split('/').forEach { part ->
            when (part) {
                "", "." -> Unit
                ".." -> if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex)
                else -> stack += part
            }
        }
        return stack.joinToString("/")
    }

    private fun normalizeHeader(value: String): String =
        value.trim().lowercase()
            .replace(Regex("\\s+"), " ")
            .replace("whatsapp ", "whatsapp")
            .replace("contact no ", "contact no")
            .replace("discount ", "discount")
            .replace("pending ", "pending")
            .replace("suite type ", "suite type")
            .replace("room charges ", "room charges")
            .replace("id's photo", "id's photo")

    private fun money(value: String?): Double =
        value?.replace(",", "")?.replace("₹", "")?.trim()?.toDoubleOrNull() ?: 0.0

    private fun excelDate(value: String): String {
        val n = value.toDoubleOrNull() ?: return value.trim()
        if (n < 1) return ""
        val millis = ((n - 25569.0) * 86400000.0).toLong()
        val date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
        return date.format(java.util.Date(millis))
    }

    private fun columnLetters(ref: String): String =
        ref.takeWhile { it.isLetter() }.uppercase()
}
