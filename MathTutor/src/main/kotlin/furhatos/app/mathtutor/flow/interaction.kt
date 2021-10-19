package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.nlu.OperationIntent
import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.nlu.Intent

val Greeting : State = state(Interaction) {

    onEntry {
        furhat.ask("Hello. I'm MathTutor, your AI math teacher.")
    }

    onResponse<Greeting>{
        var afterGreetingUtterance = ""
        random(
                {afterGreetingUtterance = ""},
                {afterGreetingUtterance = "Let's start!"},
                {afterGreetingUtterance = "Let's start(1)"},
                {afterGreetingUtterance = "Let's start(2)"},
                {afterGreetingUtterance = "Let's start(3)"}
        )
        furhat.say(afterGreetingUtterance)
        goto(GetOperation)
    }

    onResponse{
        goto(GetOperation)
    }
}

val GetOperation : State = state(Interaction) {
    onEntry {
            furhat.ask("What math problem are you stuck on?")
        }
    onReentry {
        furhat.ask("I did not understand that, try with something else.")
    }

    onResponse<OperationIntent> {
        if(it.intent.operationEntity != null) {
            if(it.intent.operationEntity!!.binaryOperation != null && it.intent.operationEntity!!.binaryOperation!!.operand != null) {
                val op = it.intent.operationEntity!!.binaryOperation!!.operand!!.operation
                if(op == null ){
                    reentry()
                }else {
                    goto(BriefExplanation(op))
                }
            }
        }else{
            reentry()
        }
    }
//
//    onResponse<No>{
//        furhat.say("That's sad.")
//    }
//    onResponse{
//        reentry()
//    }
}

val GetOperationSecond: State = state(GetOperation){
    onEntry {
        furhat.ask("Is there any other problem you want to discuss?")
    }
}

fun Explanation(operation: Operation): State = state(Interaction) {

}

fun BriefExplanation(operation: Operation) = state(Explanation(operation)) {
    onEntry {
        val text = operation.toString()
        furhat.say("I'm going to explain: $text") // Kotlin string interpolation
    }
}
