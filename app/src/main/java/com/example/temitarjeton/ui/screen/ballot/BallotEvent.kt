package com.example.temitarjeton.ui.screen.ballot

sealed interface BallotEvent {
    data class Speak(val text: String) : BallotEvent
    data object PlayWin : BallotEvent
    data object PlayLose : BallotEvent
    data object NavigateToVideo : BallotEvent
}