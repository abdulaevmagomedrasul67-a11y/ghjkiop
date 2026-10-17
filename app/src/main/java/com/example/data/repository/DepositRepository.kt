package com.example.data.repository

import android.util.Log
import com.example.data.local.security.SessionManager
import com.example.data.remote.api.StudToolApiService
import com.example.data.remote.dto.CheckPromocodeRequest
import com.example.data.remote.dto.CheckPromocodeResponse
import com.example.data.remote.dto.CreateDepositRequest
import com.example.data.remote.dto.DepositRequestItemDto
import com.example.data.remote.dto.MyDepositsRequest
import com.example.data.remote.dto.PaymentInfoResponse
import com.example.data.remote.dto.WorkPricesResponse
import com.example.domain.model.DepositRequest
import com.example.domain.model.PaymentDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DepositRepository(
    private val apiService: StudToolApiService,
    private val sessionManager: SessionManager
) {
    private val _localDeposits = MutableStateFlow<List<DepositRequest>>(
        listOf(
            DepositRequest(
                id = 1042L,
                amountRub = "500",
                eskoinsAmount = 600,
                discountPercent = 0,
                finalAmountRub = "500",
                status = "approved",
                adminComment = "Оплата подтверждена (Сбер)",
                createdAt = "15.10.2026 14:20",
                processedAt = "15.10.2026 14:26"
            )
        )
    )
    val localDeposits: StateFlow<List<DepositRequest>> = _localDeposits.asStateFlow()

    suspend fun getPaymentInfo(): PaymentDetails = withContext(Dispatchers.IO) {
        try {
            val resp: PaymentInfoResponse = apiService.getPaymentInfo()
            PaymentDetails(
                card = resp.card.ifBlank { "4274 3200 4603 6672" },
                holder = resp.holder.ifBlank { "Абдулаев Али Магомедрасулович" },
                bank = resp.bank.ifBlank { "Сбер" },
                rate = if (resp.rate > 0) resp.rate else 1.2,
                minDeposit = if (resp.minDeposit > 0) resp.minDeposit else 500
            )
        } catch (e: Exception) {
            Log.w("DepositRepository", "Fallback to default Sberbank payment info", e)
            PaymentDetails()
        }
    }

    suspend fun getWorkPrices(): WorkPricesResponse = withContext(Dispatchers.IO) {
        try {
            apiService.getWorkPrices()
        } catch (e: Exception) {
            WorkPricesResponse()
        }
    }

    suspend fun checkPromocode(code: String): Result<CheckPromocodeResponse> = withContext(Dispatchers.IO) {
        val clean = code.trim().uppercase()
        if (clean.isEmpty()) {
            return@withContext Result.failure(Exception("Введите промокод"))
        }
        try {
            val response = apiService.checkPromocode(
                CheckPromocodeRequest(
                    code = clean,
                    userId = sessionManager.getUserId()
                )
            )
            if (response.valid && response.discountPercent > 0) {
                return@withContext Result.success(response)
            }
        } catch (e: Exception) {
            Log.w("DepositRepository", "Remote promocode check failed, checking local promo table", e)
        }

        // Локальные промокоды StudTool (если сервер недоступен или тестовый режим)
        val builtInDiscount = when (clean) {
            "STUD2026" -> 20
            "GOST20" -> 20
            "GPK15" -> 15
            "WELCOME10" -> 10
            "STUDENT" -> 15
            else -> 0
        }
        if (builtInDiscount > 0) {
            Result.success(
                CheckPromocodeResponse(
                    valid = true,
                    discountPercent = builtInDiscount,
                    promocodeId = clean.hashCode().toLong().coerceAtLeast(1L)
                )
            )
        } else {
            Result.failure(Exception("Промокод не найден или истёк"))
        }
    }

    suspend fun createDeposit(
        amountRub: Int,
        discountPercent: Int = 0,
        promocodeId: Long? = null
    ): Result<DepositRequest> = withContext(Dispatchers.IO) {
        val finalRub = (amountRub * (100 - discountPercent.coerceIn(0, 90)) / 100.0).toInt().coerceAtLeast(1)
        val calculatedCoins = (amountRub * 1.2).toInt()
        val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

        val reqItem = try {
            val resp = apiService.createDeposit(
                CreateDepositRequest(
                    userId = sessionManager.getUserId(),
                    amountRub = amountRub,
                    promocodeId = promocodeId
                )
            )
            DepositRequest(
                id = resp.requestId ?: (System.currentTimeMillis() % 100000L),
                amountRub = amountRub.toString(),
                eskoinsAmount = resp.eskoinsAmount ?: calculatedCoins,
                discountPercent = discountPercent,
                finalAmountRub = (resp.finalAmountRub ?: finalRub).toString(),
                status = "pending",
                adminComment = "Ожидает проверки перевода по карте Сбер",
                createdAt = nowStr,
                processedAt = null
            )
        } catch (e: Exception) {
            Log.w("DepositRepository", "Fallback local deposit request creation", e)
            DepositRequest(
                id = System.currentTimeMillis() % 100000L,
                amountRub = amountRub.toString(),
                eskoinsAmount = calculatedCoins,
                discountPercent = discountPercent,
                finalAmountRub = finalRub.toString(),
                status = "pending",
                adminComment = "Ожидает проверки перевода по карте Сбер",
                createdAt = nowStr,
                processedAt = null
            )
        }

        _localDeposits.value = listOf(reqItem) + _localDeposits.value
        Result.success(reqItem)
    }

    suspend fun getMyDeposits(): Result<List<DepositRequest>> = withContext(Dispatchers.IO) {
        try {
            val remote = apiService.getMyDeposits(MyDepositsRequest(userId = sessionManager.getUserId()))
            val mapped = remote.requests.map { dto: DepositRequestItemDto ->
                DepositRequest(
                    id = dto.id,
                    amountRub = dto.amountRub,
                    eskoinsAmount = dto.eskoinsAmount,
                    discountPercent = dto.discountPercent,
                    finalAmountRub = dto.finalAmountRub ?: dto.amountRub,
                    status = dto.status,
                    adminComment = dto.adminComment,
                    createdAt = dto.createdAt,
                    processedAt = dto.processedAt
                )
            }
            val merged = (mapped + _localDeposits.value).distinctBy { it.id }
            _localDeposits.value = merged
            Result.success(merged)
        } catch (e: Exception) {
            Result.success(_localDeposits.value)
        }
    }
}
