package com.example

import com.example.domain.model.SupportCategory
import com.example.domain.model.WorkMeta
import com.example.domain.model.WorkType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StudToolBusinessLogicTest {

    @Test
    fun testSupportCategoriesCount() {
        val categories = SupportCategory.entries
        assertEquals(5, categories.size)
        assertNotNull(SupportCategory.fromCode("refund"))
        assertNotNull(SupportCategory.fromCode("password"))
        assertNotNull(SupportCategory.fromCode("bug"))
        assertNotNull(SupportCategory.fromCode("question"))
        assertNotNull(SupportCategory.fromCode("other"))
    }

    @Test
    fun testWorkTypesAndPricing() {
        assertEquals(480, WorkType.COURSEWORK.priceSc)
        assertEquals(1200, WorkType.DIPLOMA_VKR.priceSc)
        assertEquals(1800, WorkType.MASTER_DISSERTATION.priceSc)
        assertEquals(220, WorkType.ESSAY_REFERAT.priceSc)
    }

    @Test
    fun testWorkMetaDefaults() {
        val meta = WorkMeta(
            topic = "Тестовая тема исследования",
            type = WorkType.COURSEWORK,
            targetPages = 35
        )
        assertEquals(35, meta.targetPages)
        assertEquals(WorkType.COURSEWORK, meta.type)
        assertTrue(meta.university.isNotBlank())
    }
}
