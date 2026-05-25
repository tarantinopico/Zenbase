package com.example.domain.engine

/**
 * Výjimka vyhazovaná při neočekávaných chybách během vyhodnocování matematického výrazu.
 */
class FormulaEvaluationException(message: String) : Exception(message)
