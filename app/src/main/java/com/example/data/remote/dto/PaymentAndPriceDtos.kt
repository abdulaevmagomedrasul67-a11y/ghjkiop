package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class PaymentInfoResponse(
    @Json(name = "card")
    val card: String = "4274 3200 4603 6672",
    @Json(name = "holder")
    val holder: String = "Абдулаев Али Магомедрасулович",
    @Json(name = "bank")
    val bank: String = "Сбер",
    @Json(name = "rate")
    val rate: Double = 1.2,
    @Json(name = "min_deposit")
    val minDeposit: Int = 500
)

@JsonClass(generateAdapter = true)
data class WorkPricesResponse(
    @Json(name = "referat")
    val referat: Int = 200,
    @Json(name = "doklad")
    val doklad: Int = 120,
    @Json(name = "kursovaya")
    val kursovaya: Int = 480,
    @Json(name = "esse")
    val esse: Int = 320,
    @Json(name = "kontrolnaya")
    val kontrolnaya: Int = 160
)
