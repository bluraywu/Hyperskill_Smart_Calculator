package calculator

import java.lang.NumberFormatException
import kotlin.math.pow

val operatorRegx = Regex("[+\\-*/^]*")
val varMap = mutableMapOf<String, String>()
const val CMD_EXIT = "/exit"
const val CMD_HELP = "/help"
fun main() {
    while (true) {
        val listInput = readLine()!!.trim()
        if (listInput.isEmpty()) continue
        if (listInput.startsWith("/")) {
            try {
                when (listInput.toLowerCase()) {
                    CMD_EXIT -> {
                        println("Bye!")
                        break
                    }
                    CMD_HELP -> {
                        println("The program calculates the sum of numbers")
                        continue
                    }
                    else -> throw Exception("Unknown command")
                }
            } catch (e: Exception) {
                println(e.message)
                continue
            }
        } else {
            //detectExpression(listInput)

            try {
                detectExpression(listInput)
            } catch (e: Exception) {
                println(e.message)
            }
            //println(varMap)
        }
    }
}

fun detectExpression(expression: String) {
    val asignExpressRegx = Regex("^([a-zA-Z]+)\\s?=\\s?(-?[a-zA-Z0-9]+)$")
    val matchResult = asignExpressRegx.find(expression)
    when {
        matchResult != null -> {
            val variable = matchResult.groupValues[1]
            val value = matchResult.groupValues[2]
            when {
                value.matches(Element.VAR.regex) && varMap.containsKey(value) -> {

                    varMap[variable] = value
                }
                value.matches(Element.NUM.regex) -> {
                    varMap[variable] = value
                }
                else -> {
                    throw NumberFormatException("Invalid assignment")
                }
            }
        }
        expression.filter { it == '=' }.count() > 1 ->
            throw NumberFormatException("Invalid assignment")
        expression.filter { it == '(' }.count() != expression.filter { it == ')' }.count() ->
            throw NumberFormatException("Invalid assignment")
        else -> {
            val parsedExpress = expression.replace("(", "( ")
                .replace(")", " )")
                .split(" ")
            calculateRs(toPostfix(parsedExpress))
        }
    }
}

enum class Element(
    val precedence: Int,
    val regex: Regex
) {
    PARENTHESIS(
        4,
        Regex("[()]")
    ),
    POWER(
        3,
        Regex("[\\^]")
    ),
    MUTIDIVE(
        2,
        Regex("[*/]")
    ),
    SUMMINNUS(
        1,
        Regex("[+-]+")
    ),
    NUM(
        0,
        Regex("-?[0-9]+")
    ),
    VAR(
        0,
        Regex("[a-zA-Z]*")
    ),
}

fun detechElement(element: String): Element {
    return when {
        Element.PARENTHESIS.regex.matches(element) -> Element.PARENTHESIS
        Element.POWER.regex.matches(element) -> Element.POWER
        Element.MUTIDIVE.regex.matches(element) -> Element.MUTIDIVE
        Element.SUMMINNUS.regex.matches(element) -> Element.SUMMINNUS
        Element.NUM.regex.matches(element) -> Element.NUM
        Element.VAR.regex.matches(element) -> Element.VAR
        else -> throw NumberFormatException("Invalid expression")
    }
}

fun simplifyOperator(str: String): String {
    var theOperator = str
    if (theOperator.length > 1) {
        theOperator = when (detechElement(theOperator)) {
            Element.MUTIDIVE -> throw NumberFormatException("Invalid expression")
            Element.SUMMINNUS -> when {
                str[0] == '+' -> "+"
                str[0] == '-' && str.length % 2 == 0 -> "+"
                str[0] == '-' && str.length % 2 == 1 -> "-"
                else -> throw NumberFormatException("Invalid expression")
            }
            else -> throw NumberFormatException("Invalid expression")
        }
    }
    return theOperator
}

fun toPostfix(expression: List<String>): List<String> {
    val result = mutableListOf<String>()
    val stack = mutableListOf<String>()

    for (str in expression) {
//        println("==Before== currnet:$str")
//        println("stack: $stack")
//        println("result: $result")
        when {
            Element.NUM.regex.matches(str) || Element.VAR.regex.matches(str) -> {
                result.add(str)
            }
            stack.isEmpty() || stack.last() == "(" -> {
                stack.add(simplifyOperator(str))
            }
            operatorRegx.matches(str) -> {
                val theOperator = simplifyOperator(str)
                val topElement = detechElement(stack.last())
                val incomingElement = detechElement(theOperator)
                if (incomingElement.precedence > topElement.precedence) {
                    stack.add(theOperator)
                } else {
                    do {
                        val subTop = stack.last()
                        if (subTop != "(")
                            result.add(stack.removeLast())
                        val subTopElement = detechElement(subTop)
                    } while (stack.isNotEmpty() && (subTop != "(" && subTopElement.precedence >= incomingElement.precedence))
                    stack.add(theOperator)
                }
            }
            str == "(" -> {
                stack.add(str)
            }
            str == ")" -> {
                var top = stack.removeLast()
                while (top != "(") {
                    result.add(top)
                    top = stack.removeLast()
                }
            }
            else -> throw NumberFormatException("Invalid expression")
        }
//        println("==After==")
//        println("stack: $stack")
//        println("result: $result")
    }
    if (!stack.contains("(") || !stack.contains(")")) {
        result.addAll(stack.asReversed())
    } else {
        throw NumberFormatException("Invalid expression")
    }
    //println("Last result Stack: $result")
    return result
}

fun calculateRs(expression: List<String>) {
    val stack = mutableListOf<String>()
    for (str in expression) {
        when {
            Element.NUM.regex.matches(str) -> {
                stack.add(str)
            }
            Element.VAR.regex.matches(str) -> {
                var value = varMap[str]
                while (value?.let { Element.VAR.regex.matches(it) } == true) {
                    if (varMap.containsKey(value)) {
                        value = varMap[value]
                    } else {
                        throw NumberFormatException("Unknown variable")
                    }
                }
                if (value != null) {
                    stack.add(value)
                } else {
                    throw Exception("Unknown variable")
                }
            }
            operatorRegx.matches(str) -> {
                val right = stack.removeLast().toBigInteger()
                val left = stack.removeLast().toBigInteger()
//                println("$str|$left|$right")
                val rs = when (str) {
                    "+" -> left.plus(right)
                    "-" -> left.minus(right)
                    "*" -> left.times(right)
                    "/" -> left.div(right)
                    "^" -> left.toDouble().pow(right.toDouble()).toBigDecimal()
                    else -> throw NumberFormatException("Invalid expression")
                }
                stack.add(rs.toString())
            }
        }
//        println("$str : $stack")
    }
    if (stack.isNotEmpty())
        println(stack[0])
}
