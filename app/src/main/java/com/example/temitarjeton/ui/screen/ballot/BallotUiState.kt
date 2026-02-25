package com.example.temitarjeton.ui.screen.ballot

sealed interface BallotUiState {
    data class Ready(
        val showLoseDialog: Boolean,
        val loseMessage: String?,
    ) : BallotUiState
}