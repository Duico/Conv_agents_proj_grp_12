package furhatos.app.mathtutor.nlu

import furhatos.app.mathtutor.flow.Operation
import furhatos.nlu.*
import furhatos.util.Language
import furhatos.nlu.common.Number
import furhatos.nlu.grammar.Grammar
import furhatos.nlu.kotlin.grammar

class OperationIntent(
        val operationEntity : OperationEntity? = null
) : Intent() {
    override fun getExamples(lang: Language): List<String> {
        return listOf("I can't solve @operationEntity", "How to solve @operationEntity?", "Can you help me with @operationEntity?", "@operationEntity")
    }
}

/*class OperationEntity(val operation: Operation, val binaryOperation: BinaryOperation? = null, val equation: Equation? = null): ComplexEnumEntity() {
    override fun getEnum(lang: Language): List<String> {
//        val binaryOperation: BinaryOperation
//        val equation: Equation
        return listOf("@binaryOperation","@equation")
    }
}*/

class OperationEntity(val binaryOperation: BinaryOperation? = null, val equation: Equation? = null): ComplexEnumEntity() {
    override fun getEnum(lang: Language): List<String> {
//        val binaryOperation: BinaryOperation
//        val equation: Equation
        return listOf("@binaryOperation","@equation")
    }
}

class Operand(val operation: Operation? = null): GrammarEntity() {
    override fun getGrammar(lang: Language): Grammar {
        return grammar {
            rule(public = true){
                ruleref("oper")  tag{Operand(operation=ref["oper"] as Operation)}
            }
            rule("oper") {
                choice {
                    +("+" / "plus") tag { Operation.ADDITION }
                    +("-" / "minus") tag { Operation.SUBTRACTION }
                    +("*" / "times" / "multiplied by") tag { Operation.MULTIPLICATION }
                    +("/" / "divided" / "over") tag { Operation.DIVISION }
                }
            }
        }
    }

}

class BinaryOperation(
        val op1: Number? = null,
        val op2: Number? = null,
        val operand: Operand? = null
) : ComplexEnumEntity() {
        override fun getEnum(lang: Language): List<String> {
            return listOf("@op1 @operand @op2")
    }
}

class Equation(val lhs: String? = null, val rhs: String? = null): EnumEntity() {
    override fun getEnum(lang: Language): List<String> {
        return listOf("@lhs = @rhs")
    }
}


//class Expression: GrammarEntity

/*
val BinaryOperationGrammar =
        grammar {
            rule(public = true) {
                group {
                        "@op1"
                        ruleref("type")
                        "@op2"
                        tag { BinaryOperation(op1type=ref["type"] as String) }
                    }
                }
                rule("operation") {
                    +"+" tag { Operation.ADDITION}
                    + "plus" tag {Operation.ADDITION}
                    +"-" tag {Operation.SUBTRACTION}
                    + "minus" tag {Operation.SUBTRACTION}
                    +"*" tag {Operation.MULTIPLICATION}
                    + "times" tag {Operation.MULTIPLICATION}
                    +"/" tag{Operation.DIVISION}
                    + "divided" tag{Operation.DIVISION}
                    + "over" tag{Operation.DIVISION}
                }
            }
*/
//class AdditionEntity(override val op1: Number? = null, override val op2: Number? = null): BinaryOperation(op1, op2, Operation.ADDITION) {
//    override fun getEnum(lang: Language): List<String> {
//        return listOf("@op1 + @op2", "@op1 plus @op2")
//    }
//}

//class SubtractionEntity(override val op1: Number, override val op2: Number): BinaryOperation(op1, op2, Operation.SUBTRACTION) {
//    override fun getEnum(lang: Language): List<String> {
//        return listOf("@op1 - @op2", "@op1 minus @op2")
//    }
//}
//
//class MultiplicationEntity(override val op1: Number, override val op2: Number): BinaryOperation(op1, op2, Operation.SUBTRACTION) {
//    override fun getEnum(lang: Language): List<String> {
//        return listOf("@op1 * @op2", "@op1 times @op2", "@op1 multiplied by @op2")
//    }
//}
