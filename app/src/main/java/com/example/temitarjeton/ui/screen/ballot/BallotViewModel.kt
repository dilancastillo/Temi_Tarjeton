package com.example.temitarjeton.ui.screen.ballot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.temitarjeton.domain.model.SelectionResult
import com.example.temitarjeton.domain.usecase.EvaluateSelectionUseCase
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

class BallotViewModel(
    private val evaluate: EvaluateSelectionUseCase = EvaluateSelectionUseCase()
) : ViewModel() {

    private val _state = MutableStateFlow<BallotUiState>(
        BallotUiState.Ready(
            showLoseDialog = false,
            loseMessage = null
        )
    )
    val state: StateFlow<BallotUiState> = _state

    private val _events = MutableSharedFlow<BallotEvent>(extraBufferCapacity = 64)
    val events: SharedFlow<BallotEvent> = _events

    fun onNumberPressed(n: Int) {
        val current = _state.value as BallotUiState.Ready

        when (val result = evaluate(n)) {
            SelectionResult.Win -> viewModelScope.launch {
                _events.tryEmit(BallotEvent.PlayWin)
                _events.tryEmit(BallotEvent.Speak("¡Excelente! Has elegido el número correcto. Reproduciendo el mensaje del candidato."))
                _events.tryEmit(BallotEvent.NavigateToVideo)
            }

            is SelectionResult.Lose -> viewModelScope.launch {
                val msg = "¡Uy! Elegiste $n, pero el número correcto era el 5. " +
                        "Recuerda: marca el 5. ¡Vota por John Amaya!"
                _state.value = current.copy(
                    showLoseDialog = true,
                    loseMessage = msg
                )
                _events.tryEmit(BallotEvent.PlayLose)
                _events.tryEmit(BallotEvent.Speak(msg))
            }
        }
    }

    fun dismissLoseDialog() {
        val current = _state.value as BallotUiState.Ready
        _state.value = current.copy(showLoseDialog = false)
    }

    fun staffReset() {
        _state.value = BallotUiState.Ready(
            showLoseDialog = false,
            loseMessage = null
        )
    }
}