package com.example.data.docx

import android.content.Context
import android.util.Log
import com.example.domain.model.SectionType
import com.example.domain.model.WorkDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Генератор файлов формата Microsoft Word (.docx) в строгом соответствии с ГОСТ 7.32-2017 и ГОСТ Р 7.0.11-2011:
 * - Шрифт: Times New Roman, 14 pt (основной текст), 1.5 интервал
 * - Абзацный отступ (красная строка): 1.25 см (709 dxa)
 * - Поля: левое 30 мм, правое 10 мм, верхнее 20 мм, нижнее 20 мм
 * - Выравнивание основного текста по ширине (both)
 * - Нумерация страниц в нижнем колонтитуле по центру со 2-3 страницы
 */
class DocxGenerator(private val context: Context) {

    suspend fun generateDocx(document: WorkDocument): Result<String> = withContext(Dispatchers.IO) {
        try {
            val fileName = "StudTool_${document.meta.type.name}_${System.currentTimeMillis()}.docx"
            val outputDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "documents")
            if (!outputDir.exists()) outputDir.mkdirs()
            val outputFile = File(outputDir, fileName)

            Log.d("DOCX", "Compiling document into ${outputFile.absolutePath}")

            FileOutputStream(outputFile).use { fos ->
                ZipOutputStream(fos).use { zos ->
                    // 1. [Content_Types].xml
                    writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml())

                    // 2. _rels/.rels
                    writeZipEntry(zos, "_rels/.rels", buildRootRelsXml())

                    // 3. word/_rels/document.xml.rels
                    writeZipEntry(zos, "word/_rels/document.xml.rels", buildDocumentRelsXml())

                    // 4. word/styles.xml
                    writeZipEntry(zos, "word/styles.xml", buildStylesXml())

                    // 5. word/footer1.xml (нумерация страниц)
                    writeZipEntry(zos, "word/footer1.xml", buildFooterXml())

                    // 6. word/document.xml (содержимое работы по ГОСТу)
                    writeZipEntry(zos, "word/document.xml", buildDocumentXml(document))
                }
            }

            Log.d("DOCX", "Successfully created: ${outputFile.length()} bytes")
            Result.success(outputFile.absolutePath)
        } catch (e: Exception) {
            Log.e("DOCX", "Error generating docx file", e)
            Result.failure(e)
        }
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        zos.putNextEntry(ZipEntry(entryName))
        val bytes = content.toByteArray(Charsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    private fun buildContentTypesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
    <Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>
    <Override PartName="/word/footer1.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"/>
</Types>"""

    private fun buildRootRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

    private fun buildDocumentRelsXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rIdStyles" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
    <Relationship Id="rIdFooter1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/footer" Target="footer1.xml"/>
</Relationships>"""

    private fun buildStylesXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:docDefaults>
        <w:rPrDefault>
            <w:rPr>
                <w:rFonts w:ascii="Times New Roman" w:hAnsi="Times New Roman" w:cs="Times New Roman"/>
                <w:sz w:val="28"/> <!-- 14pt (28 half-points) -->
                <w:szCs w:val="28"/>
                <w:lang w:val="ru-RU"/>
            </w:rPr>
        </w:rPrDefault>
        <w:pPrDefault>
            <w:pPr>
                <w:spacing w:line="360" w:lineRule="auto" w:after="0" w:before="0"/> <!-- 1.5 line spacing -->
                <w:ind w:firstLine="709"/> <!-- 1.25 cm indent -->
                <w:jc w:val="both"/> <!-- Justified -->
            </w:pPr>
        </w:pPrDefault>
    </w:docDefaults>
</w:styles>"""

    private fun buildFooterXml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:ftr xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
    <w:p>
        <w:pPr>
            <w:pStyle w:val="Footer"/>
            <w:jc w:val="center"/>
            <w:ind w:firstLine="0"/>
        </w:pPr>
        <w:fldSimple w:instr="PAGE"/>
    </w:p>
</w:ftr>"""

    private fun buildDocumentXml(document: WorkDocument): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
<w:body>
""")

        val meta = document.meta

        // --- 1. ТИТУЛЬНЫЙ ЛИСТ ПО ГОСТ ---
        sb.append("""
<w:p><w:pPr><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="24"/><w:b/></w:rPr><w:t>${meta.university.escapeXml()}</w:t></w:r></w:p>
<w:p><w:pPr><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="22"/></w:rPr><w:t>${meta.faculty.escapeXml()}</w:t></w:r></w:p>
<w:p><w:pPr><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="22"/></w:rPr><w:t>${meta.department.escapeXml()}</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:before="1200"/><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="32"/><w:b/></w:rPr><w:t>${meta.type.titleRu.uppercase().escapeXml()}</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:before="240"/><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="28"/></w:rPr><w:t>по дисциплине «${meta.discipline.escapeXml()}»</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:before="400"/><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="28"/><w:b/></w:rPr><w:t>на тему: «${meta.topic.escapeXml()}»</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:before="1800"/><w:jc w:val="right"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="24"/></w:rPr><w:t>Выполнил: ${meta.authorName.escapeXml()}</w:t></w:r></w:p>
<w:p><w:pPr><w:jc w:val="right"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="24"/></w:rPr><w:t>Проверил: ${meta.supervisorName.escapeXml()}</w:t></w:r></w:p>
<w:p><w:pPr><w:spacing w:before="1400"/><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="24"/></w:rPr><w:t>${meta.city}, ${meta.year}</w:t></w:r></w:p>
<w:p><w:r><w:br w:type="page"/></w:r></w:p>
""")

        // --- 2. ОГЛАВЛЕНИЕ (СОДЕРЖАНИЕ) ---
        sb.append("""
<w:p><w:pPr><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="28"/><w:b/></w:rPr><w:t>СОДЕРЖАНИЕ</w:t></w:r></w:p>
""")
        for (sec in document.sections) {
            val isMainHeading = sec.type == SectionType.INTRODUCTION ||
                    sec.type == SectionType.CHAPTER ||
                    sec.type == SectionType.CONCLUSION ||
                    sec.type == SectionType.BIBLIOGRAPHY
            val boldXml = if (isMainHeading) "<w:b/>" else ""
            val indentVal = if (sec.type == SectionType.SUBSECTION) "360" else "0"

            sb.append("""
<w:p><w:pPr><w:spacing w:line="280" w:lineRule="auto"/><w:ind w:firstLine="$indentVal"/><w:jc w:val="left"/></w:pPr><w:r><w:rPr><w:sz w:val="26"/>$boldXml</w:rPr><w:t>${sec.title.escapeXml()}</w:t></w:r></w:p>
""")
        }
        sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")

        // --- 3. РАЗДЕЛЫ РАБОТЫ ---
        for (section in document.sections) {
            if (section.type == SectionType.CHAPTER) {
                // Разрыв страницы перед каждой новой главой по ГОСТ
                sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                sb.append("""
<w:p><w:pPr><w:spacing w:before="400" w:after="300"/><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="30"/><w:b/></w:rPr><w:t>${section.title.escapeXml()}</w:t></w:r></w:p>
""")
                continue
            }

            if (section.type == SectionType.INTRODUCTION ||
                section.type == SectionType.CONCLUSION ||
                section.type == SectionType.BIBLIOGRAPHY
            ) {
                sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>")
                sb.append("""
<w:p><w:pPr><w:spacing w:before="400" w:after="300"/><w:jc w:val="center"/><w:ind w:firstLine="0"/></w:pPr><w:r><w:rPr><w:sz w:val="30"/><w:b/></w:rPr><w:t>${section.title.escapeXml()}</w:t></w:r></w:p>
""")
            } else if (section.type == SectionType.SUBSECTION) {
                sb.append("""
<w:p><w:pPr><w:spacing w:before="360" w:after="240"/><w:jc w:val="left"/><w:ind w:firstLine="709"/></w:pPr><w:r><w:rPr><w:sz w:val="28"/><w:b/></w:rPr><w:t>${section.title.escapeXml()}</w:t></w:r></w:p>
""")
            }

            // Абзацы текста
            val paragraphs = section.content.split("\n\n", "\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }

            for (p in paragraphs) {
                // Если первая строка дублирует заголовок, пропускаем
                if (p.equals(section.title, ignoreCase = true)) continue

                sb.append("""
<w:p><w:pPr><w:spacing w:line="360" w:lineRule="auto"/><w:ind w:firstLine="709"/><w:jc w:val="both"/></w:pPr><w:r><w:rPr><w:sz w:val="28"/></w:rPr><w:t xml:space="preserve">${p.escapeXml()}</w:t></w:r></w:p>
""")
            }
        }

        // --- 4. ПАРАМЕТРЫ СТРАНИЦЫ (ГОСТ: Поля 30/10/20/20 мм, футер с нумерацией) ---
        sb.append("""
<w:sectPr>
    <w:footerReference w:type="default" r:id="rIdFooter1" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"/>
    <w:titlePg/> <!-- Титульный лист без номера страницы -->
    <w:pgSz w:w="11906" w:h="16838"/> <!-- A4: 210 x 297 mm -->
    <w:pgMar w:top="1134" w:right="567" w:bottom="1134" w:left="1701" w:header="720" w:footer="720" w:gutter="0"/> <!-- 20, 10, 20, 30 mm -->
</w:sectPr>
</w:body>
</w:document>
""")
        return sb.toString()
    }

    private fun String.escapeXml(): String {
        return this.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
