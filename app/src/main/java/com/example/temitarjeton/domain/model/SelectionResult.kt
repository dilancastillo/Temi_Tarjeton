package com.example.temitarjeton.domain.model

sealed interface SelectionResult {
    data object Win : SelectionResult
    data class Lose(val selected: Int) : SelectionResult
}