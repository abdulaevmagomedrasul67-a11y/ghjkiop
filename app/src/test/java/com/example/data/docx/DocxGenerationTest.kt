package com.example.data.docx

import com.example.domain.model.GostProfile
import com.example.domain.model.Section
import com.example.domain.model.TitlePage
import com.example.domain.model.TocItem
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkType
import kotlinx.coroutines.test.runTest
import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File
import java.io.FileInputStream

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DocxGenerationTest {

    @Test
    fun `generateDocx creates valid openable docx file with sections and title page`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val generator = DocxGenerator(context)

        val sampleDoc = WorkDocument(
            id = 1L,
            meta = WorkMeta(
                type = WorkType.REFERAT,
                topic = "Тестовая тема по ГОСТ",
                subject = "Информатика",
                language = "ru",
                targetPages = 15,
                gostProfile = "ГОСТ 7.32",
                estimatedWords = 2500
            ),
            titlePage = TitlePage(
                university = "Московский государственный университет",
                faculty = "Факультет ВМК",
                department = "Кафедра АСВК",
                workTypeLabel = "РЕФЕРАТ",
                topic = "ТЕСТОВАЯ ТЕМА ПО ГОСТ",
                subject = "Информатика",
                student = "Сидоров С.С.",
                group = "Группа 101",
                teacher = "доц. Петров П.П.",
                city = "Москва",
                year = 2026
            ),
            toc = listOf(
                TocItem(level = 1, title = "Введение", anchor = "h_intro"),
                TocItem(level = 1, title = "1. Анализ технологий", anchor = "h_ch1"),
                TocItem(level = 1, title = "Заключение", anchor = "h_concl"),
                TocItem(level = 1, title = "Список использованных источников", anchor = "h_refs")
            ),
            sections = listOf(
                Section(
                    anchor = "h_intro",
                    level = 1,
                    heading = "Введение",
                    paragraphs = listOf(
                        "Актуальность темы обоснована развитием современных технологий [1].",
                        "Целью работы является систематизация подходов к оформлению [2]."
                    )
                ),
                Section(
                    anchor = "h_ch1",
                    level = 1,
                    heading = "1. Анализ технологий",
                    paragraphs = listOf(
                        "Первый абзац содержательной части документа [1, с. 25]."
                    )
                ),
                Section(
                    anchor = "h_concl",
                    level = 1,
                    heading = "Заключение",
                    paragraphs = listOf(
                        "В ходе исследования поставленные задачи были полностью решены."
                    )
                )
            ),
            references = listOf(
                "1. Иванов И.И. Информационные системы. — М.: Наука, 2024. — 300 с.",
                "2. Петров П.П. Алгоритмы анализа // Вестник РАН. — 2025. — № 2. — С. 10–20."
            ),
            appendix = emptyList()
        )

        val result = generator.generateDocx(sampleDoc)
        assertTrue("Docx generation should succeed", result.isSuccess)

        val file = File(result.getOrThrow())
        assertTrue("Generated file should exist", file.exists())
        assertTrue("Generated file should have non-zero size", file.length() > 500)

        FileInputStream(file).use { fis ->
            val docx = XWPFDocument(fis)
            assertNotNull(docx)
            assertTrue("Document should contain paragraphs", docx.paragraphs.size > 5)
            docx.close()
        }
    }

    @Test
    fun testGenerateDocxWithGpkProfile() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val generator = DocxGenerator(context)
        val gpkProfile = GostProfile(
            id = 6L,
            name = "ГПК (Гуманитарно-педагогический колледж)",
            universityName = "ПРОФЕССИОНАЛЬНОЕ ОБРАЗОВАТЕЛЬНОЕ ЧАСТНОЕ УЧРЕЖДЕНИЕ «ГУМАНИТАРНО-ПЕДАГОГИЧЕСКИЙ КОЛЛЕДЖ»",
            leftMarginMm = 30f,
            rightMarginMm = 15f,
            topMarginMm = 20f,
            bottomMarginMm = 20f
        )

        val sampleDoc = WorkDocument(
            id = 2L,
            meta = WorkMeta(
                type = WorkType.COURSEWORK,
                topic = "Основы моделирования. Модель, моделирование, процесс моделирования",
                subject = "Основы моделирования",
                university = gpkProfile.universityName,
                authorName = "Ильясов М.Х.",
                supervisorName = "Гусейнов М.М.",
                city = "Махачкала",
                year = 2026,
                language = "ru",
                targetPages = 25,
                gostProfile = gpkProfile.name,
                estimatedWords = 5000
            ),
            titlePage = TitlePage(
                university = gpkProfile.universityName,
                faculty = "",
                department = "",
                workTypeLabel = "КУРСОВАЯ РАБОТА",
                topic = "Модель, моделирование, процесс моделирования",
                subject = "Основы моделирования",
                student = "Ильясов М.Х.",
                group = "3 курса ИСиП",
                teacher = "Гусейнов М.М.",
                city = "Махачкала",
                year = 2026
            ),
            toc = listOf(
                TocItem(level = 1, title = "ВВЕДЕНИЕ", anchor = "sec_intro"),
                TocItem(level = 1, title = "ГЛАВА 1. ОСНОВНЫЕ ПОНЯТИЯ", anchor = "sec_1")
            ),
            sections = listOf(
                Section(
                    anchor = "sec_intro",
                    level = 1,
                    heading = "ВВЕДЕНИЕ",
                    paragraphs = listOf("Текст введения для колледжа.")
                ),
                Section(
                    anchor = "sec_1",
                    level = 1,
                    heading = "ГЛАВА 1. ОСНОВНЫЕ ПОНЯТИЯ",
                    paragraphs = listOf("Текст главы 1.")
                )
            ),
            references = listOf("1. Учебное пособие по моделированию. — Махачкала, 2024."),
            appendix = emptyList()
        )

        val result = generator.generateDocx(sampleDoc)
        assertTrue("Docx generation for GPK should succeed", result.isSuccess)
        val file = File(result.getOrThrow())
        assertTrue(file.exists())
        assertTrue(file.length() > 500)

        FileInputStream(file).use { fis ->
            val docx = XWPFDocument(fis)
            val fullText = docx.paragraphs.joinToString("\n") { it.text }
            assertTrue(fullText.contains("ГУМАНИТАРНО-ПЕДАГОГИЧЕСКИЙ КОЛЛЕДЖ"))
            assertTrue(fullText.contains("КУРСОВАЯ РАБОТА"))
            assertTrue(fullText.contains("Ильясов М.Х."))
            assertTrue(fullText.contains("Махачкала"))
            docx.close()
        }
    }
}
