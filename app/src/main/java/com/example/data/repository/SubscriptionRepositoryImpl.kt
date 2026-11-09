package com.example.data.repository

import android.util.Log
import com.example.data.local.security.SessionManager
import com.example.data.remote.api.StudToolApiService
import com.example.data.remote.dto.CreateSubscriptionOrderRequest
import com.example.data.remote.dto.GetSubscriptionStatusRequest
import com.example.data.remote.dto.MySubscriptionsRequest
import com.example.domain.model.SubscriptionOrder
import com.example.domain.model.SubscriptionPlanType
import com.example.domain.model.SubscriptionStatus
import com.example.domain.repository.SubscriptionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SubscriptionRepositoryImpl(
    private val apiService: StudToolApiService,
    private val sessionManager: SessionManager
) : SubscriptionRepository {

    private val _statusFlow = MutableStateFlow(loadLocalStatus())
    override val statusFlow: StateFlow<SubscriptionStatus> = _statusFlow.asStateFlow()

    private val _ordersFlow = MutableStateFlow<List<SubscriptionOrder>>(emptyList())
    override val ordersFlow: StateFlow<List<SubscriptionOrder>> = _ordersFlow.asStateFlow()

    private fun loadLocalStatus(): SubscriptionStatus {
        return SubscriptionStatus(
            active = sessionManager.isSubscriptionActive(),
            hasHadSubscription = sessionManager.hasHadSubscription(),
            planType = sessionManager.getSubscriptionPlanType()?.let { SubscriptionPlanType.fromCode(it) },
            expiresAt = sessionManager.getSubscriptionExpiresAt(),
            daysLeft = sessionManager.getSubscriptionDaysLeft(),
            autoRenew = sessionManager.isSubscriptionAutoRenew()
        )
    }

    override suspend fun getSubscriptionStatus(userId: Long): SubscriptionStatus {
        return try {
            val response = apiService.getSubscriptionStatus(GetSubscriptionStatusRequest(userId = userId))
            if (response.status != "error" && response.error == null) {
                val plan = response.planType?.let { SubscriptionPlanType.fromCode(it) }
                val status = SubscriptionStatus(
                    active = response.active || sessionManager.isSubscriptionActive(),
                    hasHadSubscription = response.hasHadSubscription || sessionManager.hasHadSubscription(),
                    planType = plan ?: sessionManager.getSubscriptionPlanType()?.let { SubscriptionPlanType.fromCode(it) },
                    expiresAt = response.expiresAt ?: sessionManager.getSubscriptionExpiresAt(),
                    daysLeft = if (response.daysLeft > 0) response.daysLeft else sessionManager.getSubscriptionDaysLeft(),
                    autoRenew = response.autoRenew
                )
                saveLocalStatus(status)
                _statusFlow.value = status
                status
            } else {
                val local = loadLocalStatus()
                _statusFlow.value = local
                local
            }
        } catch (e: Exception) {
            Log.d("SUBSCRIPTION", "Fallback to local subscription status: ${e.message}")
            val local = loadLocalStatus()
            _statusFlow.value = local
            local
        }
    }

    override suspend fun createSubscriptionOrder(
        userId: Long,
        planType: SubscriptionPlanType
    ): Result<SubscriptionOrder> {
        val currentStatus = _statusFlow.value
        val amountRub = when (planType) {
            SubscriptionPlanType.MONTHLY -> SubscriptionPlanType.MONTHLY_PRICE_RUB
            SubscriptionPlanType.YEARLY -> currentStatus.yearlyPriceRub
        }

        val nowStr = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

        return try {
            val response = apiService.createSubscriptionOrder(
                CreateSubscriptionOrderRequest(
                    userId = userId,
                    planType = planType.code,
                    amountRub = amountRub
                )
            )
            val orderId = response.orderId ?: (System.currentTimeMillis() % 100000L)
            val order = SubscriptionOrder(
                id = orderId,
                planType = planType,
                amountRub = amountRub,
                status = "pending",
                createdAt = nowStr,
                cardNumber = response.card.ifBlank { "4274 3200 4603 6672" },
                cardHolder = response.holder.ifBlank { "Абдулаев Али Магомедрасулович" },
                bankName = response.bank.ifBlank { "Сбер" }
            )
            val list = _ordersFlow.value.toMutableList()
            list.add(0, order)
            _ordersFlow.value = list
            Result.success(order)
        } catch (e: Exception) {
            Log.d("SUBSCRIPTION", "Created local pending subscription order: ${e.message}")
            val order = SubscriptionOrder(
                id = System.currentTimeMillis() % 100000L,
                planType = planType,
                amountRub = amountRub,
                status = "pending",
                createdAt = nowStr
            )
            val list = _ordersFlow.value.toMutableList()
            list.add(0, order)
            _ordersFlow.value = list
            Result.success(order)
        }
    }

    override suspend fun confirmOrderPaid(orderId: Long): Result<SubscriptionOrder> {
        val list = _ordersFlow.value.toMutableList()
        val idx = list.indexOfFirst { it.id == orderId }
        val existing = if (idx >= 0) list[idx] else SubscriptionOrder(
            id = orderId,
            planType = SubscriptionPlanType.MONTHLY,
            amountRub = SubscriptionPlanType.MONTHLY_PRICE_RUB,
            status = "pending",
            createdAt = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())
        )

        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, existing.planType.durationDays)
        val expiresAtStr = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault()).format(cal.time)

        val updatedOrder = existing.copy(
            status = "pending",
            expiresAt = expiresAtStr
        )
        if (idx >= 0) {
            list[idx] = updatedOrder
        } else {
            list.add(0, updatedOrder)
        }
        _ordersFlow.value = list

        // Активируем подписку для пользователя после подтверждения оплаты
        val activeStatus = SubscriptionStatus(
            active = true,
            hasHadSubscription = true,
            planType = existing.planType,
            expiresAt = expiresAtStr,
            daysLeft = existing.planType.durationDays,
            autoRenew = true
        )
        saveLocalStatus(activeStatus)
        _statusFlow.value = activeStatus

        return Result.success(updatedOrder)
    }

    override suspend fun getMySubscriptions(userId: Long): Result<List<SubscriptionOrder>> {
        return try {
            val response = apiService.getMySubscriptions(MySubscriptionsRequest(userId = userId))
            val remoteOrders = response.orders.map { dto ->
                SubscriptionOrder(
                    id = dto.id,
                    planType = SubscriptionPlanType.fromCode(dto.planType),
                    amountRub = dto.amountRub,
                    status = dto.status,
                    createdAt = dto.createdAt,
                    expiresAt = dto.expiresAt
                )
            }
            val combined = (remoteOrders + _ordersFlow.value).distinctBy { it.id }
            _ordersFlow.value = combined
            Result.success(combined)
        } catch (e: Exception) {
            Result.success(_ordersFlow.value)
        }
    }

    override suspend fun cancelAutoRenewal(userId: Long): Result<SubscriptionStatus> {
        val updated = _statusFlow.value.copy(autoRenew = false)
        saveLocalStatus(updated)
        _statusFlow.value = updated
        return Result.success(updated)
    }

    private fun saveLocalStatus(status: SubscriptionStatus) {
        sessionManager.saveSubscriptionState(
            active = status.active,
            hasHadSubscription = status.hasHadSubscription,
            planType = status.planType?.code,
            expiresAt = status.expiresAt,
            daysLeft = status.daysLeft,
            autoRenew = status.autoRenew
        )
    }
}
