package com.zenbase.app.domain.engine

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FormulaEvaluatorTest {
    private val evaluator = FormulaEvaluator()

    @Test
    fun testSimpleAddition() {
        val formula = "2 + 2"
        val recordJson = "{}"
        val result = evaluator.evaluate(formula, recordJson)
        assertEquals(4.0, result)
    }

    @Test
    fun testVariableSubstitution() {
        val formula = "price * quantity"
        val recordJson = """{"price": 10, "quantity": 5}"""
        val result = evaluator.evaluate(formula, recordJson)
        assertEquals(50.0, result)
    }
}
