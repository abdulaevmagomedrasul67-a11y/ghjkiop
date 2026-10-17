package com.example.domain.quantization

import com.example.domain.model.SectionType
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SectionQuantizerTest {

    @Test
    fun testCourseworkQuantization35Pages() {
        val meta = WorkMeta(
            topic = "Анализ финансовой устойчивости коммерческого банка",
            type = WorkType.COURSEWORK,
            targetPages = 35
        )

        val plan = SectionQuantizer.quantize(meta)
        assertEquals(35, plan.totalPages)
        assertEquals(35 * 1800, plan.totalTargetCharacters) // 63 000 знаков

        val sections = plan.sections
        assertTrue("Должно быть введение", sections.any { it.type == SectionType.INTRODUCTION })
        assertTrue("Должны быть главы", sections.any { it.type == SectionType.CHAPTER })
        assertTrue("Должны быть подразделы", sections.any { it.type == SectionType.SUBSECTION })
        assertTrue("Должно быть заключение", sections.any { it.type == SectionType.CONCLUSION })
        assertTrue("Должен быть список источников", sections.any { it.type == SectionType.BIBLIOGRAPHY })

        // Проверяем Введение
        val intro = sections.first { it.type == SectionType.INTRODUCTION }
        assertTrue("Введение должно иметь объем > 3000 знаков", intro.targetCharacters >= 3000)

        // Проверяем равномерность подразделов (не должно быть пустых или гигантских перекосов)
        val subsections = sections.filter { it.type == SectionType.SUBSECTION }
        assertTrue("Должно быть минимум 4 подраздела", subsections.size >= 4)
        for (sub in subsections) {
            assertTrue("Каждый подраздел должен иметь квоту > 2000 знаков", sub.targetCharacters > 2000)
        }
    }

    @Test
    fun testDiplomaQuantization65Pages() {
        val meta = WorkMeta(
            topic = "Проектирование распределенной информационной системы",
            type = WorkType.DIPLOMA_VKR,
            targetPages = 65
        )

        val plan = SectionQuantizer.quantize(meta)
        assertEquals(65, plan.totalPages)
        assertEquals(65 * 1800, plan.totalTargetCharacters) // 117 000 знаков

        val chapters = plan.sections.filter { it.type == SectionType.CHAPTER }
        assertEquals("Диплом должен содержать ровно 3 главы", 3, chapters.size)

        val subsections = plan.sections.filter { it.type == SectionType.SUBSECTION }
        assertEquals("Диплом должен содержать 9 подразделов (по 3 в каждой главе)", 9, subsections.size)
    }

    @Test
    fun testMasterDissertationQuantization80Pages() {
        val meta = WorkMeta(
            topic = "Модели и алгоритмы интеллектуального анализа биомедицинских сигналов",
            type = WorkType.MASTER_DISSERTATION,
            targetPages = 80
        )

        val plan = SectionQuantizer.quantize(meta)
        assertEquals(80, plan.totalPages)
        assertEquals(80 * 1800, plan.totalTargetCharacters) // 144 000 знаков

        val generatable = plan.sections.filter { it.targetCharacters > 0 }
        val sumChars = generatable.sumOf { it.targetCharacters }
        // Сумма знаков разделов должна быть в пределах +/- 5% от общего целевого объема
        val diff = Math.abs(sumChars - plan.totalTargetCharacters)
        assertTrue("Расхождение объема не должно превышать 5% (расхождение: $diff)", diff < plan.totalTargetCharacters * 0.05)
    }
}
