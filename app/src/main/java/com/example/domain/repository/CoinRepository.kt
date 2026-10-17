package com.example.domain.repository

import kotlinx.coroutines.flow.Flow

data class CoinPackage(
    val id: String,
    val amount: Int,
    val priceRub: Int,
    val isPopular: Boolean = false,
    val description: String
)

interface CoinRepository {
    fun getBalance(): Flow<Int>
    suspend fun deduct(amount: Int): Boolean
    suspend fun addCoins(amount: Int)
    fun getPackages(): List<CoinPackage>
}
