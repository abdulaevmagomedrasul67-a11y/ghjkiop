package com.example.domain.usecase

import com.example.data.local.security.SessionManager
import com.example.data.remote.api.StudToolApiService
import com.example.data.remote.dto.*
import com.example.data.repository.SupportRepository
import com.example.domain.model.SupportCategories
import com.example.domain.model.WorkDocument
import com.example.domain.model.WorkType
import com.example.domain.repository.WorkRepository
import com.example.presentation.support.SupportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SupportScreenTest {

    private val testDispatcher = StandardTestDispatcher()
    private val context = RuntimeEnvironment.getApplication()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testSupportCategoriesContainsAll5RequiredCategories() {
        val categories = SupportCategories.ALL
        assertEquals(5, categories.size)

        val values = categories.map { it.value }
        assertTrue(values.contains("refund"))
        assertTrue(values.contains("password"))
        assertTrue(values.contains("bug"))
        assertTrue(values.contains("question"))
        assertTrue(values.contains("other"))

        val refundCat = SupportCategories.findByValue("refund")
        assertNotNull(refundCat)
        assertEquals("Подписка и оплата", refundCat?.label)
    }

    @Test
    fun testSupportRepositoryCreatesTicketSuccessfully() = runTest {
        val fakeApi = object : FakeApiService() {
            override suspend fun createSupportTicket(request: CreateSupportTicketRequest): CreateSupportTicketResponse {
                return CreateSupportTicketResponse(status = "success", ticketId = 42L)
            }
        }

        val sessionManager = SessionManager(context)
        val repository = SupportRepository(fakeApi, sessionManager)
        val result = repository.createTicket(
            category = "refund",
            subject = "Вопрос по подписке",
            message = "Оплатил подписку на месяц",
            contact = "student@example.com",
            workId = 15L
        )

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())
    }

    @Test
    fun testSupportRepositoryGetsMyTicketsSuccessfully() = runTest {
        val mockTickets = listOf(
            SupportTicketDto(
                id = 42L,
                category = "refund",
                subject = "Вопрос по подписке",
                message = "Оплатил подписку",
                workId = 15,
                status = "resolved",
                adminReply = "Подписка активирована",
                refundEskoins = 0,
                createdAt = "2026-10-01 12:30:00",
                processedAt = "2026-10-01 12:45:00"
            )
        )

        val fakeApi = object : FakeApiService() {
            override suspend fun getMySupportTickets(request: MySupportTicketsRequest): MySupportTicketsResponse {
                return MySupportTicketsResponse(status = "success", tickets = mockTickets)
            }
        }

        val sessionManager = SessionManager(context)
        val repository = SupportRepository(fakeApi, sessionManager)
        val result = repository.getMyTickets()

        assertTrue(result.isSuccess)
        val list = result.getOrNull()
        assertNotNull(list)
        assertEquals(1, list?.size)
        assertEquals("resolved", list?.first()?.status)
    }

    @Test
    fun testSupportViewModelValidations() = runTest {
        val fakeApi = FakeApiService()
        val sessionManager = SessionManager(context)

        val fakeWorkRepo = object : WorkRepository {
            override fun getAllWorks(): Flow<List<WorkDocument>> = flowOf(emptyList())
            override fun getWorksByType(type: WorkType): Flow<List<WorkDocument>> = flowOf(emptyList())
            override suspend fun getWorkById(id: Long): WorkDocument? = null
            override suspend fun saveWork(document: WorkDocument): Long = 1L
            override suspend fun updateDocxPath(workId: Long, path: String) {}
            override suspend fun deleteWork(id: Long) {}
        }

        val repository = SupportRepository(fakeApi, sessionManager)
        val createUseCase = CreateSupportTicketUseCase(repository)
        val getUseCase = GetMySupportTicketsUseCase(repository)

        val viewModel = SupportViewModel(
            createTicketUseCase = createUseCase,
            getMyTicketsUseCase = getUseCase,
            sessionManager = sessionManager,
            workRepository = fakeWorkRepo
        )

        viewModel.submit()
        assertEquals("Выберите категорию обращения", viewModel.uiState.value.error)

        viewModel.selectCategory(SupportCategories.ALL.first())
        viewModel.submit()
        assertEquals("Введите заголовок обращения", viewModel.uiState.value.error)

        viewModel.setSubject("Проблема")
        viewModel.submit()
        assertEquals("Опишите проблему подробно", viewModel.uiState.value.error)
    }

    open class FakeApiService : StudToolApiService {
        override suspend fun getSubscriptionStatus(request: GetSubscriptionStatusRequest) =
            SubscriptionStatusResponse(active = true, hasHadSubscription = false)
        override suspend fun createSubscriptionOrder(request: CreateSubscriptionOrderRequest) =
            CreateSubscriptionOrderResponse(orderId = 1L)
        override suspend fun getMySubscriptions(request: MySubscriptionsRequest) =
            MySubscriptionsResponse(orders = emptyList())
        override suspend fun checkPromocode(request: CheckPromocodeRequest) = CheckPromocodeResponse(valid = false)
        override suspend fun createDeposit(request: CreateDepositRequest) = CreateDepositResponse(success = true)
        override suspend fun getMyDeposits(request: MyDepositsRequest) = MyDepositsResponse()
        override suspend fun createPasswordReset(request: CreatePasswordResetRequest) = CreatePasswordResetResponse(success = true)
        override suspend fun getPaymentInfo() = PaymentInfoResponse()
        override suspend fun getWorkPrices() = WorkPricesResponse()
        override suspend fun changePassword(request: ChangePasswordRequest) = ChangePasswordResponse(success = true)
        override suspend fun registerPushToken(request: RegisterPushTokenRequest) = RegisterPushTokenResponse(success = true)
        override suspend fun unregisterPushToken(request: UnregisterPushTokenRequest) = UnregisterPushTokenResponse(success = true)
        override suspend fun createSupportTicket(request: CreateSupportTicketRequest) = CreateSupportTicketResponse(status = "ok", ticketId = 1L)
        override suspend fun getMySupportTickets(request: MySupportTicketsRequest) = MySupportTicketsResponse(status = "ok", tickets = emptyList())
    }
}
