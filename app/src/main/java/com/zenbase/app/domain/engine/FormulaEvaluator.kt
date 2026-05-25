package com.zenbase.app.domain.engine

import java.util.Stack
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Nástroj pro vyhodnocování matematických výrazů (vzorců).
 * Používá běžný Shunting-yard algoritmus pro převod plně uzávorkovaného výrazu do postfixu
 * (reverzní polské notace) a následně jej vyhodnocuje s daty dodanými ze záznamu.
 */
@Singleton
class FormulaEvaluator @Inject constructor() {

    /**
     * Vyhodnotí zadaný matematický výraz v kontextu aktuálních dat záznamu.
     * @param expression Výraz k vyhodnocení (např. "cena_bez_dph * 1.21").
     * @param recordData Data aktuálního záznamu, kterými budou nahrazeny identifikátory proměnných v rovnici.
     * @return Výkon výpočtu jako typu Double.
     * @throws FormulaEvaluationException Pokud narazí na syntaktickou chybu nebo neznámou proměnnou.
     */
    fun evaluate(expression: String, recordData: Map<String, Any?>): Double {
        if (expression.isBlank()) return 0.0
        val tokens = tokenize(expression, recordData)
        val postfix = infixToPostfix(tokens)
        return evaluatePostfix(postfix)
    }

    private fun tokenize(expression: String, recordData: Map<String, Any?>): List<String> {
        val tokens = mutableListOf<String>()
        var i = 0
        var expectOperand = true

        while (i < expression.length) {
            val c = expression[i]
            if (c.isWhitespace()) {
                i++
                continue
            }

            // Rozpoznání unárního mínusu (na začátku vzorce, nebo hned po logickém operátoru či závorce)
            if (c == '-' && expectOperand) {
                val sb = java.lang.StringBuilder("-")
                i++
                // přeskočit libovolné mezery po mínusu
                while (i < expression.length && expression[i].isWhitespace()) {
                    i++
                }
                while (i < expression.length && isAllowedInIdentifier(expression[i])) {
                    sb.append(expression[i])
                    i++
                }
                val token = sb.toString()
                if (token.length == 1) { // nepodařilo se namapovat další znaky
                    tokens.add("-")
                    expectOperand = true
                } else {
                    val subToken = token.substring(1)
                    val numeric = subToken.toDoubleOrNull()
                    if (numeric != null) {
                        tokens.add((-numeric).toString())
                    } else {
                        val value = recordData[subToken] ?: throw FormulaEvaluationException("Neznámý identifikátor: \$subToken")
                        val doubleVal = getAsDouble(value, subToken)
                        tokens.add((-doubleVal).toString())
                    }
                    expectOperand = false
                }
                continue
            }

            if (c in setOf('+', '-', '*', '/', '(', ')')) {
                tokens.add(c.toString())
                expectOperand = c != ')'
                i++
                continue
            }

            if (isAllowedInIdentifier(c)) {
                val sb = java.lang.StringBuilder()
                while (i < expression.length && isAllowedInIdentifier(expression[i])) {
                    sb.append(expression[i])
                    i++
                }
                val token = sb.toString()
                if (token.toDoubleOrNull() != null) {
                    tokens.add(token)
                } else {
                    val value = recordData[token] ?: throw FormulaEvaluationException("Neznámý identifikátor: \$token")
                    tokens.add(getAsDouble(value, token).toString())
                }
                expectOperand = false
                continue
            }
            throw FormulaEvaluationException("Neznámý znak ve výrazu na pozici \$i: \$c")
        }
        return tokens
    }

    private fun isAllowedInIdentifier(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '_' || c == '.'
    }

    private fun getAsDouble(value: Any?, name: String): Double {
        return when (value) {
            is Number -> value.toDouble()
            is String -> value.toDoubleOrNull() 
                ?: throw FormulaEvaluationException("Textovou hodnotu '\$value' v poli \$name nelze použít v aritmetice.")
            is Boolean -> if (value) 1.0 else 0.0
            else -> throw FormulaEvaluationException("Typ proměnné \$name nelze použít ve výpočtu (nepodporovaný typ dat).")
        }
    }

    private fun precedence(op: String): Int {
        return when (op) {
            "+", "-" -> 1
            "*", "/" -> 2
            else -> -1
        }
    }

    private fun infixToPostfix(tokens: List<String>): List<String> {
        val result = mutableListOf<String>()
        val stack = Stack<String>()

        for (token in tokens) {
            if (token.toDoubleOrNull() != null) {
                result.add(token)
            } else if (token == "(") {
                stack.push(token)
            } else if (token == ")") {
                while (stack.isNotEmpty() && stack.peek() != "(") {
                    result.add(stack.pop())
                }
                if (stack.isNotEmpty() && stack.peek() == "(") {
                    stack.pop()
                } else {
                    throw FormulaEvaluationException("Chyba syntaxe: nespárované závorky.")
                }
            } else { 
                while (stack.isNotEmpty() && precedence(token) <= precedence(stack.peek())) {
                    result.add(stack.pop())
                }
                stack.push(token)
            }
        }
        while (stack.isNotEmpty()) {
            if (stack.peek() == "(") throw FormulaEvaluationException("Chyba syntaxe: neuzavřené závorky v rovnici.")
            result.add(stack.pop())
        }
        return result
    }

    private fun evaluatePostfix(postfix: List<String>): Double {
        val stack = Stack<Double>()
        for (token in postfix) {
            val num = token.toDoubleOrNull()
            if (num != null) {
                stack.push(num)
            } else {
                if (stack.size < 2) throw FormulaEvaluationException("Neplatný výraz (nedostatek operandů pro operátor \$token).")
                val b = stack.pop()
                val a = stack.pop()
                when (token) {
                    "+" -> stack.push(a + b)
                    "-" -> stack.push(a - b)
                    "*" -> stack.push(a * b)
                    "/" -> {
                        if (b == 0.0) throw FormulaEvaluationException("Chyba: dělení nulou není povoleno.")
                        stack.push(a / b)
                    }
                    else -> throw FormulaEvaluationException("Neznámý výpočetní operátor: \$token")
                }
            }
        }
        if (stack.size != 1) throw FormulaEvaluationException("Kritická chyba vyhodnocení, pravděpodobně nesprávný formát vstupního vzorce.")
        return stack.pop()
    }
}
