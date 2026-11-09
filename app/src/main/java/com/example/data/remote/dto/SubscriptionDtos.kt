package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GetSubscriptionStatusRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "action") val action: String = "get_subscription_status"
)

@JsonClass(generateAdapter = true)
data class SubscriptionStatusResponse(
    @Json(name = "active") val active: Boolean = false,
    @Json(name = "has_had_subscription") val hasHadSubscription: Boolean = false,
    @Json(name = "plan_type") val planType: String? = null, // "month" or "year"
    @Json(name = "expires_at") val expiresAt: String? = null, // e.g. "31.12.2026"
    @Json(name = "days_left") val daysLeft: Int = 0,
    @Json(name = "auto_renew") val autoRenew: Boolean = true,
    @Json(name = "status") val status: String? = null,
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class CreateSubscriptionOrderRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "plan_type") val planType: String, // "month" or "year"
    @Json(name = "amount_rub") val amountRub: Int,
    @Json(name = "action") val action: String = "create_subscription_order"
)

@JsonClass(generateAdapter = true)
data class CreateSubscriptionOrderResponse(
    @Json(name = "success") val success: Boolean = true,
    @Json(name = "order_id") val orderId: Long? = null,
    @Json(name = "status") val status: String = "pending",
    @Json(name = "amount_rub") val amountRub: Int = 1800,
    @Json(name = "card") val card: String = "4274 3200 4603 6672",
    @Json(name = "holder") val holder: String = "Абдулаев Али Магомедрасулович",
    @Json(name = "bank") val bank: String = "Сбер",
    @Json(name = "error") val error: String? = null
)

@JsonClass(generateAdapter = true)
data class MySubscriptionsRequest(
    @Json(name = "user_id") val userId: Long,
    @Json(name = "action") val action: String = "my_subscriptions"
)

@JsonClass(generateAdapter = true)
data class SubscriptionOrderDto(
    @Json(name = "id") val id: Long,
    @Json(name = "plan_type") val planType: String,
    @Json(name = "amount_rub") val amountRub: Int,
    @Json(name = "status") val status: String, // "pending", "approved", "active", "rejected"
    @Json(name = "created_at") val createdAt: String,
    @Json(name = "expires_at") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class MySubscriptionsResponse(
    @Json(name = "orders") val orders: List<SubscriptionOrderDto> = emptyList(),
    @Json(name = "status") val status: String? = "success"
)
