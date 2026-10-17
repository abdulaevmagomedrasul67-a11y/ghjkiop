package com.example.data.docx

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.domain.model.SectionType
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkSection
import com.example.domain.model.WorkType
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.zip.ZipFile

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocxGenerationTest {

    @Test
    fun `generateDocx creates valid OpenXML docx archive with GOST margins and content`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val generator = DocxGenerator(context)

        val meta = WorkMeta(
            topic = "Разработка интеллектуальной системы анализа данных",
            type = WorkType.COURSEWORK,
            targetPages = 35,
            university = "МГТУ им. Н.Э. Баумана",
            authorName = "Иванов И.И.",
            supervisorName = "проф. Смирнов А.В."
        )

        val sections = listOf(
            WorkSection(
                id = "sec_intro",
                title = "ВВЕДЕНИЕ",
                type = SectionType.INTRODUCTION,
                targetPages = 3.0,
                targetCharacters = 5400,
                minCharacters = 4860,
                maxCharacters = 5940,
                content = "Актуальность темы обусловлена стремительным развитием алгоритмов машинного обучения.\n\nЦелью исследования является повышение точности прогнозирования.",
                isGenerated = true
            ),
            WorkSection(
                id = "sec_ch_1",
                title = "ГЛАВА 1. Теоретические основы",
                type = SectionType.CHAPTER,
                chapterNumber = 1,
                targetPages = 0.0,
                targetCharacters = 0,
                minCharacters = 0,
                maxCharacters = 0,
                content = "",
                isGenerated = true
            ),
            WorkSection(
                id = "sec_ch_1_sub_1",
                title = "1.1. Анализ существующих методов",
                type = SectionType.SUBSECTION,
                chapterNumber = 1,
                subsectionNumber = 1,
                targetPages = 5.0,
                targetCharacters = 9000,
                minCharacters = 8100,
                maxCharacters = 9900,
                content = "В данном подразделе рассматриваются современные архитектуры нейронных сетей [1, с. 45].",
                isGenerated = true
            ),
            WorkSection(
                id = "sec_conclusion",
                title = "ЗАКЛЮЧЕНИЕ",
                type = SectionType.CONCLUSION,
                targetPages = 2.0,
                targetCharacters = 3600,
                minCharacters = 3240,
                maxCharacters = 3960,
                content = "Полученные результаты подтверждают эффективность предложенного подхода.",
                isGenerated = true
            ),
            WorkSection(
                id = "sec_biblio",
                title = "СПИСОК ИСПОЛЬЗОВАННЫХ ИСТОЧНИКОВ",
                type = SectionType.BIBLIOGRAPHY,
                targetPages = 1.5,
                targetCharacters = 2700,
                minCharacters = 2295,
                maxCharacters = 3105,
                content = "1. Иванов И.И. Системы искусственного интеллекта. — М.: Наука, 2024. — 320 с.",
                isGenerated = true
            )
        )

        val document = WorkDocument(
            meta = meta,
            sections = sections
        )

        val result = generator.generateDocx(document)
        assertTrue("Генерация DOCX должна завершиться успешно", result.isSuccess)

        val filePath = result.getOrNull()
        assertNotNull(filePath)
        val file = File(filePath!!)
        assertTrue("Файл DOCX должен существовать на диске", file.exists())
        assertTrue("Размер файла DOCX должен быть больше 1 КБ", file.length() > 1000)

        ZipFile(file).use { zip ->
            val docEntry = zip.getEntry("word/document.xml")
            val stylesEntry = zip.getEntry("word/styles.xml")
            assertNotNull("В архиве должен быть word/document.xml", docEntry)
            assertNotNull("В архиве должен быть word/styles.xml", stylesEntry)

            val docXml = zip.getInputStream(docEntry).bufferedReader().readText()
            assertTrue("Должен содержать название университета", docXml.contains("МГТУ им. Н.Э. Баумана"))
            assertTrue("Должен содержать тему работы", docXml.contains("Разработка интеллектуальной системы анализа данных"))
            assertTrue("Должен содержать оглавление", docXml.contains("СОДЕРЖАНИЕ"))
            assertTrue("Должен содержать поля по ГОСТ (left=1701 dxa = 30мм)", docXml.contains("w:left=\"1701\""))
        }
    }
}
