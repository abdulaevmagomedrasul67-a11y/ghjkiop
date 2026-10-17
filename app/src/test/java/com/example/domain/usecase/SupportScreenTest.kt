package com.example.domain.usecase

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.security.SessionManager
import com.example.data.remote.api.StudToolApiService
import com.example.data.remote.dto.CreateSupportTicketRequest
import com.example.data.remote.dto.CreateSupportTicketResponse
import com.example.data.remote.dto.MySupportTicketsRequest
import com.example.data.remote.dto.MySupportTicketsResponse
import com.example.data.remote.dto.SupportTicketDto
import com.example.data.repository.SupportRepository
import com.example.domain.model.SupportCategory
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupportScreenTest {

    @Test
    fun testSupportCategoriesContainsAll5RequiredCategories() {
        val categories = SupportCategory.entries
        assertEquals(5, categories.size)

        assertEquals(SupportCategory.REFUND, SupportCategory.fromCode("refund"))
        assertEquals(SupportCategory.PASSWORD, SupportCategory.fromCode("password"))
        assertEquals(SupportCategory.BUG, SupportCategory.fromCode("bug"))
        assertEquals(SupportCategory.QUESTION, SupportCategory.fromCode("question"))
        assertEquals(SupportCategory.OTHER, SupportCategory.fromCode("other"))
        assertEquals("Возврат коинов", SupportCategory.REFUND.titleRu)
    }

    @Test
    fun testSupportRepositoryCreatesTicketSuccessfully() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val fakeApi = object : StudToolApiService {
            override suspend fun createSupportTicket(request: CreateSupportTicketRequest): CreateSupportTicketResponse {
                return CreateSupportTicketResponse(status = "success", ticketId = 42L)
            }

            override suspend fun getMySupportTickets(request: MySupportTicketsRequest): MySupportTicketsResponse {
                return MySupportTicketsResponse(status = "success", tickets = emptyList())
            }
        }

        val sessionManager = SessionManager(context)
        sessionManager.saveUserId(6L)
        sessionManager.saveUserEmail("student@example.com")

        val repository = SupportRepository(fakeApi, sessionManager)
        val createUseCase = CreateSupportTicketUseCase(repository)

        val result = createUseCase(
            category = SupportCategory.REFUND,
            subject = "Не сгенерировалась курсовая",
            message = "Списались 480 SC",
            workId = 15L,
            contactEmail = "student@example.com"
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
    }

    @Test
    fun testSupportRepositoryGetsMyTicketsSuccessfully() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val mockTickets = listOf(
            SupportTicketDto(
                id = 42L,
                userId = 6L,
                category = "refund",
                subject = "Не сгенерировалась курсовая",
                message = "Списались 480 SC",
                status = "resolved",
                adminReply = "Вернули 480 SC на баланс",
                refundedAmount = 480,
                createdAt = "2026-10-01 12:30:00"
            )
        )

        val fakeApi = object : StudToolApiService {
            override suspend fun createSupportTicket(request: CreateSupportTicketRequest): CreateSupportTicketResponse {
                return CreateSupportTicketResponse(status = "success", ticketId = 42L)
            }

            override suspend fun getMySupportTickets(request: MySupportTicketsRequest): MySupportTicketsResponse {
                return MySupportTicketsResponse(status = "success", tickets = mockTickets)
            }
        }

        val sessionManager = SessionManager(context)
        sessionManager.saveUserId(6L)

        val repository = SupportRepository(fakeApi, sessionManager)
        val getMyTicketsUseCase = GetMySupportTicketsUseCase(repository)
        val result = getMyTicketsUseCase()

        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertEquals(1, list?.size)
        assertEquals("resolved", list?.first()?.status)
        assertEquals(480, list?.first()?.refundedAmount)
    }
}
