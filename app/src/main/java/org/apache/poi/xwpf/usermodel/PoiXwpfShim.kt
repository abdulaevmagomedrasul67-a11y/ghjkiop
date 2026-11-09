package org.apache.poi.xwpf.usermodel

import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTP
import java.io.Closeable
import java.io.InputStream
import java.util.zip.ZipInputStream

enum class ParagraphAlignment {
    LEFT, CENTER, RIGHT, BOTH
}

class XWPFRun {
    var fontFamily: String? = "Times New Roman"
    var fontSize: Int = 14
    var isBold: Boolean = false
    private val textBuilder = StringBuilder()

    fun setText(value: String?) {
        if (value != null) {
            textBuilder.append(value)
        }
    }

    fun addBreak() {
        textBuilder.append("\n")
    }

    fun addTab() {
        textBuilder.append("\t")
    }

    fun getText(): String = textBuilder.toString()
}

class XWPFParagraph {
    var alignment: ParagraphAlignment = ParagraphAlignment.LEFT
    var spacingAfter: Int = 0
    var spacingBefore: Int = 0
    var indentationLeft: Int = 0
    @JvmField var isPageBreak: Boolean = false
    val ctp: CTP = CTP()
    private val runs = mutableListOf<XWPFRun>()

    fun createRun(): XWPFRun {
        val run = XWPFRun()
        runs.add(run)
        return run
    }

    fun setPageBreak(pageBreak: Boolean) {
        this.isPageBreak = pageBreak
    }

    val text: String
        get() {
            val runText = runs.joinToString("") { it.getText() }
            val hyperlinkText = ctp.hyperlinks.flatMap { it.runs }.mapNotNull { it.t?.stringValue }.joinToString("")
            return runText + hyperlinkText
        }
}

class XWPFDocument() : Closeable {
    val paragraphs: MutableList<XWPFParagraph> = mutableListOf()

    constructor(inputStream: InputStream) : this() {
        try {
            ZipInputStream(inputStream).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (entry.name == "word/document.xml") {
                        val xml = zis.readBytes().toString(Charsets.UTF_8)
                        val pRegex = Regex("<w:p[\\s>].*?</w:p>", RegexOption.DOT_MATCHES_ALL)
                        val tRegex = Regex("<w:t(?:\\s[^>]*)?>(.*?)</w:t>", RegexOption.DOT_MATCHES_ALL)
                        pRegex.findAll(xml).forEach { pMatch ->
                            val pXml = pMatch.value
                            val p = createParagraph()
                            val r = p.createRun()
                            val texts = tRegex.findAll(pXml).map {
                                it.groupValues[1]
                                    .replace("&quot;", "\"")
                                    .replace("&apos;", "'")
                                    .replace("&lt;", "<")
                                    .replace("&gt;", ">")
                                    .replace("&amp;", "&")
                            }.joinToString("")
                            r.setText(texts)
                        }
                        break
                    }
                    entry = zis.nextEntry
                }
            }
        } catch (_: Exception) {
        }
    }

    fun createParagraph(): XWPFParagraph {
        val p = XWPFParagraph()
        paragraphs.add(p)
        return p
    }

    override fun close() {
        // No-op
    }
}
