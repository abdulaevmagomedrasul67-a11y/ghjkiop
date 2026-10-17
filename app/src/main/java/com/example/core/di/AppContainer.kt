package com.example.core.di

import android.content.Context
import com.example.data.docx.DocxGenerator
import com.example.data.local.AppDatabase
import com.example.data.local.security.PinManager
import com.example.data.local.security.SessionManager
import com.example.data.remote.LlmConnectionChecker
import com.example.data.remote.api.ApiClient
import com.example.data.remote.api.StudToolApiService
import com.example.data.remote.llm.VibeCodeApiClient
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.CoinRepositoryImpl
import com.example.data.repository.DepositRepository
import com.example.data.repository.HistoryRepositoryImpl
import com.example.data.repository.LlmRepositoryImpl
import com.example.data.repository.ProfileRepositoryImpl
import com.example.data.repository.SettingsRepositoryImpl
import com.example.data.repository.SupportRepository
import com.example.data.repository.UniversityProfileRepositoryImpl
import com.example.data.repository.WorkRepositoryImpl
import com.example.domain.pipeline.SequentialGenerationController
import com.example.domain.repository.AuthRepository
import com.example.domain.repository.CoinRepository
import com.example.domain.repository.HistoryRepository
import com.example.domain.repository.LlmRepository
import com.example.domain.repository.ProfileRepository
import com.example.domain.repository.SettingsRepository
import com.example.domain.repository.UniversityProfileRepository
import com.example.domain.repository.WorkRepository
import com.example.domain.usecase.BuildDocxUseCase
import com.example.domain.usecase.CreateSupportTicketUseCase
import com.example.domain.usecase.GenerateWorkUseCase
import com.example.domain.usecase.GetMySupportTicketsUseCase

class AppContainer(private val context: Context) {

    val sessionManager: SessionManager by lazy {
        SessionManager(context.applicationContext)
    }

    val pinManager: PinManager by lazy {
        PinManager(context.applicationContext)
    }

    val appDatabase: AppDatabase by lazy {
        AppDatabase.getInstance(context.applicationContext)
    }

    val apiService: StudToolApiService by lazy {
        ApiClient.apiService
    }

    val depositRepository: DepositRepository by lazy {
        DepositRepository(apiService, sessionManager)
    }

    val llmConnectionChecker: LlmConnectionChecker by lazy {
        LlmConnectionChecker()
    }

    val supportRepository: SupportRepository by lazy {
        SupportRepository(apiService, sessionManager)
    }

    val createSupportTicketUseCase: CreateSupportTicketUseCase by lazy {
        CreateSupportTicketUseCase(supportRepository)
    }

    val getMySupportTicketsUseCase: GetMySupportTicketsUseCase by lazy {
        GetMySupportTicketsUseCase(supportRepository)
    }

    val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(sessionManager)
    }

    val coinRepository: CoinRepository by lazy {
        CoinRepositoryImpl(sessionManager)
    }

    val universityProfileRepository: UniversityProfileRepository by lazy {
        UniversityProfileRepositoryImpl()
    }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(sessionManager)
    }

    val workRepository: WorkRepository by lazy {
        WorkRepositoryImpl()
    }

    val historyRepository: HistoryRepository by lazy {
        HistoryRepositoryImpl(workRepository)
    }

    val profileRepository: ProfileRepository by lazy {
        ProfileRepositoryImpl(sessionManager)
    }

    val docxGenerator: DocxGenerator by lazy {
        DocxGenerator(context.applicationContext)
    }

    val buildDocxUseCase: BuildDocxUseCase by lazy {
        BuildDocxUseCase(docxGenerator)
    }

    val vibeCodeApiClient: VibeCodeApiClient by lazy {
        VibeCodeApiClient()
    }

    val llmRepository: LlmRepository by lazy {
        LlmRepositoryImpl(vibeCodeApiClient)
    }

    val sequentialGenerationController: SequentialGenerationController by lazy {
        SequentialGenerationController(llmRepository)
    }

    val generateWorkUseCase: GenerateWorkUseCase by lazy {
        GenerateWorkUseCase(sequentialGenerationController)
    }
}
