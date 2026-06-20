package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val dao: GameStateDao) {
    val gameStateFlow: Flow<GameState?> = dao.getGameStateFlow()

    suspend fun getGameState(): GameState? = dao.getGameState()

    suspend fun saveGameState(state: GameState) = dao.saveGameState(state)
}
