package com.example.domain.model

data class User(
    val id: Long = 1L,
    val fullName: String = "Али Абдулаев",
    val email: String = "student@studtool.ru",
    val balanceSc: Int = 200
)

data class DepositRequest(
    val id: Long,
    val amountRub: String,
    val eskoinsAmount: Int,
    val discountPercent: Int = 0,
    val finalAmountRub: String = "",
    val status: String, // pending, approved, rejected
    val adminComment: String? = null,
    val createdAt: String,
    val processedAt: String? = null
) {
    // Backwards compatibility getters
    val eskoins: Int get() = eskoinsAmount
    val comment: String? get() = adminComment
}

data class PaymentDetails(
    val card: String = "4274 3200 4603 6672",
    val holder: String = "Абдулаев Али Магомедрасулович",
    val bank: String = "Сбер",
    val rate: Double = 1.2,
    val minDeposit: Int = 500
)
