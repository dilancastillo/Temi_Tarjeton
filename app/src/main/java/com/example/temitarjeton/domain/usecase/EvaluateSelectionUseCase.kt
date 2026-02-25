package com.example.temitarjeton.domain.usecase

import com.example.temitarjeton.domain.model.SelectionResult

class EvaluateSelectionUseCase(
    private val winningNumber: Int = 5
) {
    operator fun invoke(selected: Int): SelectionResult =
        if (selected == winningNumber) SelectionResult.Win
        else SelectionResult.Lose(selected)
}