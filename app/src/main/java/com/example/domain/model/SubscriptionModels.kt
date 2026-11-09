package com.example.domain.model

enum class SubscriptionPlanType(
    val code: String,
    val titleRu: String,
    val typeLabelRu: String,
    val durationDays: Int
) {
    MONTHLY(
        code = "month",
        titleRu = "Подписка на месяц",
        typeLabelRu = "Месячная",
        durationDays = 30
    ),
    YEARLY(
        code = "year",
        titleRu = "Подписка на год",
        typeLabelRu = "Годовая",
        durationDays = 365
    );

    companion object {
        const val MONTHLY_PRICE_RUB = 1800
        const val YEARLY_FIRST_TIME_PRICE_RUB = 14000
        const val YEARLY_RENEWAL_PRICE_RUB = 21600
        const val YEARLY_SAVINGS_RUB = 7600

        fun fromCode(code: String?): SubscriptionPlanType {
            return if (code?.lowercase()?.startsWith("year") == true) YEARLY else MONTHLY
        }
    }
}

data class SubscriptionStatus(
    val active: Boolean = false,
    val hasHadSubscription: Boolean = false,
    val planType: SubscriptionPlanType? = null,
    val expiresAt: String = "31.12.2026",
    val daysLeft: Int = 0,
    val autoRenew: Boolean = true
) {
    val daysRemaining: Int
        get() = daysLeft

    val yearlyPriceRub: Int
        get() = if (hasHadSubscription) {
            SubscriptionPlanType.YEARLY_RENEWAL_PRICE_RUB
        } else {
            SubscriptionPlanType.YEARLY_FIRST_TIME_PRICE_RUB
        }

    val yearlyButtonText: String
        get() = if (hasHadSubscription) {
            "Продлить за 21 600 ₽"
        } else {
            "Оформить за 14 000 ₽"
        }
}

data class SubscriptionOrder(
    val id: Long,
    val planType: SubscriptionPlanType,
    val amountRub: Int,
    val status: String, // "pending", "approved", "active", "rejected"
    val createdAt: String,
    val expiresAt: String? = null,
    val cardNumber: String = "4274 3200 4603 6672",
    val cardHolder: String = "Абдулаев Али Магомедрасулович",
    val bankName: String = "Сбер"
) {
    val priceRub: Int
        get() = amountRub
}
