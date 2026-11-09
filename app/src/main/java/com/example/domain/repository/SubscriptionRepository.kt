package com.example.domain.repository

import com.example.domain.model.SubscriptionOrder
import com.example.domain.model.SubscriptionPlanType
import com.example.domain.model.SubscriptionStatus
import kotlinx.coroutines.flow.StateFlow

interface SubscriptionRepository {
    val statusFlow: StateFlow<SubscriptionStatus>
    val ordersFlow: StateFlow<List<SubscriptionOrder>>

    fun observeSubscriptionStatus(): StateFlow<SubscriptionStatus> = statusFlow
    fun observeOrders(): StateFlow<List<SubscriptionOrder>> = ordersFlow

    suspend fun getSubscriptionStatus(userId: Long): SubscriptionStatus
    suspend fun createSubscriptionOrder(
        userId: Long,
        planType: SubscriptionPlanType
    ): Result<SubscriptionOrder>

    suspend fun confirmOrderPaid(orderId: Long): Result<SubscriptionOrder>
    suspend fun confirmOrderPaid(userId: Long, orderId: Long): Result<SubscriptionOrder> = confirmOrderPaid(orderId)
    suspend fun getMySubscriptions(userId: Long): Result<List<SubscriptionOrder>>
    suspend fun cancelAutoRenewal(userId: Long): Result<SubscriptionStatus>
    suspend fun cancelAutoRenew(userId: Long): Result<SubscriptionStatus> = cancelAutoRenewal(userId)
}
