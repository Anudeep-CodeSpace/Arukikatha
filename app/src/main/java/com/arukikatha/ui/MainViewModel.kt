package com.arukikatha.ui

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.arukikatha.data.AppDatabase
import com.arukikatha.data.SessionHistoryEntity
import com.arukikatha.data.SessionRepositoryImpl
import com.arukikatha.domain.ArukikathaPhase
import com.arukikatha.session.ArukikathaSessionService
import com.arukikatha.session.SessionOrchestrator
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val db = Room.databaseBuilder(application, AppDatabase::class.java, "arukikatha.db").build()
    private val repository = SessionRepositoryImpl(db.sessionDao())

    val sessionState = SessionOrchestrator.state
    val history = repository.history().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var completionSaved = false

    init {
        viewModelScope.launch {
            sessionState.collect { state ->
                if (state.phase == ArukikathaPhase.COMPLETED && !completionSaved) {
                    completionSaved = true
                    repository.save(
                        SessionHistoryEntity(
                            timestamp = System.currentTimeMillis(),
                            completedBriskCount = state.completedBriskCount,
                            completedNormalCount = state.completedNormalCount,
                            successfulMinutes = state.successfulMinutes,
                            resetsTriggered = state.resetsTriggered,
                            totalPauses = state.totalPauses,
                            goalCompleted = state.successfulMinutes >= 30
                        )
                    )
                }
                if (!state.isRunning && state.phase == ArukikathaPhase.BRISK) {
                    completionSaved = false
                }
            }
        }
    }

    fun start() {
        completionSaved = false
        sendServiceAction(ArukikathaSessionService.ACTION_START)
    }

    fun initService() = sendServiceAction("com.arukikatha.INIT")

    fun stop() = sendServiceAction(ArukikathaSessionService.ACTION_STOP)

    fun pause() = sendServiceAction(ArukikathaSessionService.ACTION_PAUSE)

    fun resume() = sendServiceAction(ArukikathaSessionService.ACTION_RESUME)

    private fun sendServiceAction(action: String) {
        val app = getApplication<Application>()
        val intent = Intent(app, ArukikathaSessionService::class.java).apply { this.action = action }
        app.startForegroundService(intent)
    }
}
