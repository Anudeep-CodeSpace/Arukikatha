package com.arukikatha.data

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    fun history(): Flow<List<SessionHistoryEntity>>
    suspend fun save(item: SessionHistoryEntity)
}

class SessionRepositoryImpl(private val dao: SessionDao) : SessionRepository {
    override fun history(): Flow<List<SessionHistoryEntity>> = dao.observeAll()

    override suspend fun save(item: SessionHistoryEntity) {
        dao.insert(item)
    }
}
