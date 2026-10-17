package com.example.data.repository

import com.example.data.local.security.SessionManager
import com.example.domain.repository.CoinPackage
import com.example.domain.repository.CoinRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class CoinRepositoryImpl(
    private val sessionManager: SessionManager
) : CoinRepository {

    private val balanceFlow = MutableStateFlow(sessionManager.getCoinBalance())

    private val packages = listOf(
        CoinPackage("pack_250", 250, 199, false, "1 реферат или эссе"),
        CoinPackage("pack_600", 600, 399, true, "1 стандартная курсовая (до 35 стр.)"),
        CoinPackage("pack_1500", 1500, 899, false, "1 дипломная работа или 2-3 курсовых"),
        CoinPackage("pack_3000", 3000, 1590, false, "Максимальный пакет для диссертаций")
    )

    override fun getBalance(): Flow<Int> = balanceFlow.asStateFlow()

    override suspend fun deduct(amount: Int): Boolean {
        val success = sessionManager.deductCoins(amount)
        if (success) {
            balanceFlow.value = sessionManager.getCoinBalance()
        }
        return success
    }

    override suspend fun addCoins(amount: Int) {
        sessionManager.addCoins(amount)
        balanceFlow.value = sessionManager.getCoinBalance()
    }

    override fun getPackages(): List<CoinPackage> = packages
}
