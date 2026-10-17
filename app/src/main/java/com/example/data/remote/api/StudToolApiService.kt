package com.example.data.remote.api

import com.example.data.remote.dto.BaseApiResponse
import com.example.data.remote.dto.CheckDeviceRequest
import com.example.data.remote.dto.CheckDeviceResponse
import com.example.data.remote.dto.CheckPromocodeRequest
import com.example.data.remote.dto.CheckPromocodeResponse
import com.example.data.remote.dto.CreateDepositRequest
import com.example.data.remote.dto.CreateDepositResponse
import com.example.data.remote.dto.CreatePasswordResetRequest
import com.example.data.remote.dto.CreatePasswordResetResponse
import com.example.data.remote.dto.CreateSupportTicketRequest
import com.example.data.remote.dto.CreateSupportTicketResponse
import com.example.data.remote.dto.CreateWorkRequest
import com.example.data.remote.dto.CreateWorkResponse
import com.example.data.remote.dto.GetUserRequest
import com.example.data.remote.dto.GetUserResponse
import com.example.data.remote.dto.MyDepositsRequest
import com.example.data.remote.dto.MyDepositsResponse
import com.example.data.remote.dto.MySupportTicketsRequest
import com.example.data.remote.dto.MySupportTicketsResponse
import com.example.data.remote.dto.PaymentInfoResponse
import com.example.data.remote.dto.RegisterPushTokenRequest
import com.example.data.remote.dto.RegisterRequest
import com.example.data.remote.dto.RegisterResponse
import com.example.data.remote.dto.UpdateProfileRequest
import com.example.data.remote.dto.WorkPricesResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface StudToolApiService {

    @POST("api/support_create.php")
    suspend fun createSupportTicket(
        @Body request: CreateSupportTicketRequest
    ): CreateSupportTicketResponse

    @POST("api/support_my.php")
    suspend fun getMySupportTickets(
        @Body request: MySupportTicketsRequest
    ): MySupportTicketsResponse

    @GET("api/payment_info.php")
    suspend fun getPaymentInfo(): PaymentInfoResponse = PaymentInfoResponse()

    @GET("api/work_prices.php")
    suspend fun getWorkPrices(): WorkPricesResponse = WorkPricesResponse()

    @POST("api/check_promocode.php")
    suspend fun checkPromocode(
        @Body request: CheckPromocodeRequest
    ): CheckPromocodeResponse = CheckPromocodeResponse(valid = false)

    @POST("api/create_deposit.php")
    suspend fun createDeposit(
        @Body request: CreateDepositRequest
    ): CreateDepositResponse = CreateDepositResponse(
        success = true,
        requestId = System.currentTimeMillis() % 100000L,
        eskoinsAmount = (request.amountRub * 1.2).toInt(),
        finalAmountRub = request.amountRub
    )

    @POST("api/my_deposits.php")
    suspend fun getMyDeposits(
        @Body request: MyDepositsRequest
    ): MyDepositsResponse = MyDepositsResponse()

    @POST("api/check_device.php")
    suspend fun checkDevice(
        @Body request: CheckDeviceRequest
    ): CheckDeviceResponse = CheckDeviceResponse(registered = false)

    @POST("api/register.php")
    suspend fun registerUser(
        @Body request: RegisterRequest
    ): RegisterResponse = RegisterResponse(success = true, userId = 1L, eskoins = 200)

    @POST("api/get_user.php")
    suspend fun getUser(
        @Body request: GetUserRequest
    ): GetUserResponse = GetUserResponse(userId = request.userId, eskoins = 1500)

    @POST("api/create_work.php")
    suspend fun createWork(
        @Body request: CreateWorkRequest
    ): CreateWorkResponse = CreateWorkResponse(success = true, workId = System.currentTimeMillis())

    @POST("api/update_profile.php")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest
    ): BaseApiResponse = BaseApiResponse(status = "success")

    @POST("api/password_reset.php")
    suspend fun createPasswordReset(
        @Body request: CreatePasswordResetRequest
    ): CreatePasswordResetResponse = CreatePasswordResetResponse(success = true, message = "Заявка принята")

    @POST("api/push_token.php")
    suspend fun registerPushToken(
        @Body request: RegisterPushTokenRequest
    ): BaseApiResponse = BaseApiResponse(status = "success")
}
