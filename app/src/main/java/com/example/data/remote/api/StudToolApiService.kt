package com.example.data.remote.api

import com.example.data.remote.dto.ChangePasswordRequest
import com.example.data.remote.dto.ChangePasswordResponse
import com.example.data.remote.dto.CheckPromocodeRequest
import com.example.data.remote.dto.CheckPromocodeResponse
import com.example.data.remote.dto.CreateDepositRequest
import com.example.data.remote.dto.CreateDepositResponse
import com.example.data.remote.dto.CreatePasswordResetRequest
import com.example.data.remote.dto.CreatePasswordResetResponse
import com.example.data.remote.dto.CreateSubscriptionOrderRequest
import com.example.data.remote.dto.CreateSubscriptionOrderResponse
import com.example.data.remote.dto.CreateSupportTicketRequest
import com.example.data.remote.dto.CreateSupportTicketResponse
import com.example.data.remote.dto.GetSubscriptionStatusRequest
import com.example.data.remote.dto.MyDepositsRequest
import com.example.data.remote.dto.MyDepositsResponse
import com.example.data.remote.dto.MySubscriptionsRequest
import com.example.data.remote.dto.MySubscriptionsResponse
import com.example.data.remote.dto.MySupportTicketsRequest
import com.example.data.remote.dto.MySupportTicketsResponse
import com.example.data.remote.dto.PaymentInfoResponse
import com.example.data.remote.dto.RegisterPushTokenRequest
import com.example.data.remote.dto.RegisterPushTokenResponse
import com.example.data.remote.dto.SubscriptionStatusResponse
import com.example.data.remote.dto.UnregisterPushTokenRequest
import com.example.data.remote.dto.UnregisterPushTokenResponse
import com.example.data.remote.dto.WorkPricesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StudToolApiService {

    // Подписки (Этап 2.4)
    @POST("admin/api.php?action=get_subscription_status")
    suspend fun getSubscriptionStatus(
        @Body request: GetSubscriptionStatusRequest
    ): SubscriptionStatusResponse

    @POST("admin/api.php?action=create_subscription_order")
    suspend fun createSubscriptionOrder(
        @Body request: CreateSubscriptionOrderRequest
    ): CreateSubscriptionOrderResponse

    @POST("admin/api.php?action=my_subscriptions")
    suspend fun getMySubscriptions(
        @Body request: MySubscriptionsRequest
    ): MySubscriptionsResponse

    // Поддержка
    @POST("admin/api.php?action=create_support_ticket")
    suspend fun createSupportTicket(
        @Body request: CreateSupportTicketRequest
    ): CreateSupportTicketResponse

    @POST("admin/api.php?action=my_support_tickets")
    suspend fun getMySupportTickets(
        @Body request: MySupportTicketsRequest
    ): MySupportTicketsResponse

    // Профиль и безопасность
    @POST("admin/api.php?action=change_password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest
    ): ChangePasswordResponse

    @POST("admin/api.php?action=create_password_reset")
    suspend fun createPasswordReset(
        @Body request: CreatePasswordResetRequest
    ): CreatePasswordResetResponse

    @POST("admin/api.php?action=register_push_token")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequest
    ): RegisterPushTokenResponse

    @POST("admin/api.php?action=unregister_push_token")
    suspend fun unregisterPushToken(
        @Body request: UnregisterPushTokenRequest
    ): UnregisterPushTokenResponse

    // Устаревшие эндпоинты (оставлены для обратной совместимости репозиториев)
    @GET("admin/api.php?action=payment_info")
    suspend fun getPaymentInfo(): PaymentInfoResponse

    @GET("admin/api.php?action=work_prices")
    suspend fun getWorkPrices(): WorkPricesResponse

    @POST("admin/api.php?action=create_deposit")
    suspend fun createDeposit(
        @Body request: CreateDepositRequest
    ): CreateDepositResponse

    @POST("admin/api.php?action=my_deposits")
    suspend fun getMyDeposits(
        @Body request: MyDepositsRequest
    ): MyDepositsResponse

    @POST("admin/api.php?action=check_promocode")
    suspend fun checkPromocode(
        @Body request: CheckPromocodeRequest
    ): CheckPromocodeResponse
}
