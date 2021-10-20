package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.nlu.OperationIntent
import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.nlu.Intent
import furhatos.nlu.common.Number
import kotlin.random.Random

fun detectEmotion(): String {
    return ""
}

fun timeLeft():Boolean {
    return true
}

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
        furhat.ask("I beg your pardon?.")
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
    onResponse{
        reentry()
    }
    onNoResponse {
        reentry()
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
/*
fun Explanation(operation: Operation): State = state(Interaction) {

}
*/
fun BriefExplanation(operation: Operation) = state(Interaction) {
    onEntry {
        val text = operation.toString()
        furhat.say("I'm going to explain: $text") // Kotlin string interpolation
        when(operation){
            Operation.ADDITION->{
                furhat.say("Let me ask you a simple question from this topic to check your understanding")
            }
        }
        goto(GaugeBriefExplanation(operation))
    }
}

fun GaugeBriefExplanation(operation : Operation) = state(Interaction) {
    var result = 0
    onEntry{
        val num1  = Random.nextInt(3,6)
        val num2 = Random.nextInt(2,num1)
        when(operation) {
            Operation.ADDITION -> {
                result = num1 + num2
                random({furhat.ask("So if I had $num1 apples, and I get $num2 more, how many do I have now?")}
                )
            }
            Operation.SUBTRACTION -> {
                result = num1 - num2
                random({furhat.ask("So if I had $num1 apples, and I lose $num2 , how many do I have now?")}
                )
            }
            Operation.MULTIPLICATION -> {
                result = num1 * num2
                random({furhat.ask("So if I had a basket with$num1 apples, and I buy $num2 such baskets, how many do I have now?")}
                )
            }
            Operation.EQUATION->{

            }
            Operation.DIVISION->{

            }

        }
        //TODO ADDING OPERATION EQUATION
    }
    onResponse<Number>{
        //furhat.say("You're answer is ${it.intent.text}")
        if(it.intent.value == result){
            val emotion = detectEmotion()
            furhat.say("Good job! Let's try another problem then")
            if(emotion == "") {
                //TODO: NEED TO IMPLEMENT EMOTION DETECTION
                goto(MediumProblem(operation))
            }
        }
        else{
            furhat.say("Hummmmm...")
            furhat.say("Unfortunately, that's not the right answer. I think it'd be a good idea to understand the concept better")
            goto(DetailedExplanation(operation))
        }
    }
    onResponse{
        furhat.say("Okay, let me try to explain the concept better")
        goto(DetailedExplanation(operation))
    }
}

fun DetailedExplanation(operation: Operation) :State = state(Interaction){
    onEntry {
        furhat.say("Let's make this an interactive learning experience.")
        delay(1000)
        val num1 = Random.nextInt(2, 5)
        print(num1)
        val num2 = Random.nextInt(1, num1)
        val num3 = Random.nextInt(1, 5)
        val limit = 10 / num3

        when (operation) {
            Operation.ADDITION -> {

                random({ furhat.say("Okay, now I want you to show $num1 using your fingers") },
                    { furhat.say("Raise $num1 fingers") }
                )
                delay(1000)
                random(
                    { furhat.say("Now raise $num2 more") }
                )
                delay(1000)
                furhat.say("Let's count the number of fingers now")
                delay(4000)
                furhat.say("You have..")
                for( i in 1..num1+num2){
                    furhat.say(i.toString())
                    delay(500)
                }
                furhat.say("Which gives us the answer of ${num1+num2} fingers. Easy")
            }
            Operation.SUBTRACTION -> {
                random({ furhat.say("Okay, now I want you to show $num1 using your fingers") },
                    { furhat.say("Raise $num1 fingers") }
                )
                delay(1000)
                random(
                    { furhat.say("Now close $num2 of them") }
                )
                delay(1000)
                furhat.say("Count the number of fingers now")
                delay(4000)
                furhat.say("Now you should have ${num1-num2} fingers. Easy")
            }
            Operation.MULTIPLICATION -> {
                random({ furhat.say("Okay, now I want you to show $num3 using your fingers") },
                    { furhat.say("Raise $num3 fingers") }
                )
                delay(1000)
                furhat.say("What you have is $num3 times 1")
                for (i in 2..limit) {
                    random({ furhat.say("Raise $num3 more fingers") },
                        { furhat.say("lift $num3 more ") },
                        { furhat.say("$num3 more") }
                    )
                    delay(300)
                    random({ furhat.say("Now you got $num3 times $i") },
                        { furhat.say("This is $num3 times $i") }
                    )
                }
            }
            else ->{

            }
        }
        goto(GaugeDetailedExplanation(operation))
    }
}

fun GaugeDetailedExplanation(operation:Operation) = state(Interaction){
    onEntry {
        var emotion = detectEmotion()
        if(emotion == "Confused"){
            goto(DetailedExplanation(operation))
        }
        else{
            furhat.say("Okay. Let's try another problem")
            delay(1000)
            goto(MediumProblem(operation))
        }
    }
}



fun MediumProblem(operation: Operation) = state(Interaction){
    var result = 0
    var num1 = 0
    var num2 = 0
    onEntry{
        num1 = Random.nextInt(2,5)
        num2 = Random.nextInt(1,num1)
        when(operation){
            Operation.ADDITION->{
                result = num1 + num2
                random({furhat.ask("If I have $num1 coins and then get $num2 more coins, how many do I finally have?")},
                    {furhat.ask("I have $num1 candies and you have $num2. Now I give you all of my candies, so how many will you end up with?")}
                )
            }
            Operation.SUBTRACTION->{
                result = num1 - num2
                random({furhat.ask("So if I had $num1 apples, and I lose $num2 , how many do I have now?") },
                    furhat.ask("I have $num2 apples, but I want $num1 apples, so how many more should I buy?")
                )
            }
            Operation.MULTIPLICATION->{
                result  = num1 * num2
                random({furhat.ask("So if I had a basket with$num1 apples, and I buy $num2 such baskets, how many do I have now?")},
                    {furhat.ask("A box of chocolates at the store has $num1 pieces, and I buy $num2 boxes, chocolates do I have in total?")}
                )
            }
            Operation.EQUATION->{

            }
            Operation.DIVISION->{

            }
        }
    }
    onResponse<Number>{
        if(it.intent.value == result){
            furhat.say("Good job! That's correct. Let's look at a more challenging problem now.")
            goto(DifficultProblem(operation))
        }
        else{
            goto(MediumProblemSolution(operation,num1,num2))
        }
    }
}


fun DifficultProblem(operation:Operation) :State= state(Interaction){
    var num1 = 0
    var num2 = 0
    var num3 = 0
    var result = 0
    onEntry{
        num1 = Random.nextInt(8,10)
        num2 = Random.nextInt(1,4)
        num3 = Random.nextInt(1,4)
        result = 0
        when(operation){
            Operation.ADDITION->{
                result = num1 + num2 + num3
                random({furhat.ask("I have $num1 bananas, $num2 oranges, and $num3 apples. How many do I have in total?")},
                    {furhat.ask("Suppose I have $num1 coins, you have $num2 coins. I then take all of your coins and a robber who has $num3 coins already with him then steals all the coins I have. How many coins does the robber have now?")})
            }
            Operation.SUBTRACTION->{
                result = num1-num2-num3
                random(
                    {furhat.ask("You have $num1 toffees. You give me $num2 of them and $num3 to another friend. How many do you have left?")}

                )
            }
            Operation.MULTIPLICATION->{
                result = num1*num2*num3
                random(
                    {furhat.ask("A box of toffees has $num1 pieces. Each carton contains $num2 boxes, and a truck can carry $num3 cartons. How many tofees can a truck carry at most?")}
                )
            }
            Operation.EQUATION->{

            }
            Operation.DIVISION->{

            }
        }

    }
    onResponse<Number>{
        if(result != it.intent.value){
            goto(DifficultProblemSolution(operation,num1,num2,num3))
        }
        else{
            if(timeLeft()){
                goto(EvaluateConditions)
            }
            else{
                goto(GiveHomework)
            }
        }
    }
}

fun MediumProblemSolution(operation:Operation, num1: Int, num2:Int) = state(Interaction){
    onEntry{
        random({furhat.say("Let's see how we should solve this problem")})
        delay(1000)
        when(operation){
            Operation.ADDITION->{
                furhat.say("The problem finally boils down to adding $num1 and $num2")
                delay(200)
                furhat.say("First draw $num1 tally marks in your notebook")
                delay(2000)
                furhat.say("Good, now draw $num2 more lines")
                delay(2000)
                furhat.say("Alright, now let's count the total number of lines")
                for(i in 0..num1+num2){
                    furhat.say(i.toString())
                    delay(100)
                }
            }
            Operation.SUBTRACTION->{
                furhat.say("The problem finally boils down to subtracting $num1 and $num2")
                delay(200)
                furhat.say("First draw $num1 tally marks in your notebook")
                delay(2000)
                furhat.say("Good, now erase $num2 of them")
                delay(2000)
                furhat.say("Alright, now let's count the remaining lines")
                for(i in 0..num1-num2){
                    furhat.say(i.toString())
                    delay(100)
                }
            }
            Operation.MULTIPLICATION->{
                furhat.say("The problem finally boils down to multiplying $num1 and $num2")
                delay(200)
                furhat.say("First draw $num1 tally marks in your notebook")
                delay(2000)
                furhat.say("Now we need to multiply this by $num2, which means we have to add $num1 more marks $num2 times")
                delay(1000)
                for(i in 1..num2){
                    if(i == 1){
                        furhat.say("So we already have $num1 times 1, which is $num1")
                        delay(300)
                    }
                    else{
                        furhat.say("Now adding $num1 more lines, we get $num1 times $i, which is")
                        delay(1000)
                        furhat.say((num1*i).toString())
                        delay(1000)
                    }
                }
            }
            Operation.EQUATION->{

            }
            Operation.DIVISION->{

            }
        }
        var emotion = detectEmotion()
        if(emotion == "confused"){
            furhat.say("Hmm, let's try out an easier problem.")
            goto(EasyProblem(operation))
        }
        else{
            furhat.say("Alrighty then. I think you've understood this solution. Let's move on to a more challenging problem now.")
            goto(DifficultProblem(operation))
        }
    }
}

fun DifficultProblemSolution(operation: Operation,num1: Int,num2: Int,num3:Int) = state(Interaction){
    onEntry{
        when(operation){
            Operation.ADDITION->{
                furhat.say("Now that you seem comfortable with the basics of addition, let's see how this can be broken down.Since there are 3 numbers to add, you first add $num1 and $num2")
                delay(1000)
                furhat.say("You should end up with a sum of ${num1+num2}")
                delay(1000)
                furhat.say("Now add $num3 to this result to get the final answer")
                delay(1000)
                furhat.say("And the answer is ${num1+num2+num3}")
            }
            Operation.SUBTRACTION->{
                furhat.say("Now that you seem comfortable with the basics of subtraction, let's see how this can be broken down. You first subtract $num1 and $num2")
                delay(1000)
                furhat.say("You should end up with a difference of ${num1-num2}")
                delay(1000)
                furhat.say("Now subtract $num3 to this result to get the final answer")
                delay(1000)
                furhat.say("And the answer is ${num1-num2-num3}")
            }
            Operation.MULTIPLICATION->{
                furhat.say("Now that you seem comfortable with the basics of multiplication, let's see how this can be broken down.Since there are 3 numbers to multiply, you first multiply $num1 and $num2")
                delay(1000)
                furhat.say("You should end up with a product of ${num1*num2}")
                delay(1000)
                furhat.say("Now multiply $num3 with this result to get the final answer")
                delay(1000)
                furhat.say("And the answer is ${num1*num2*num3}")
            }
            Operation.EQUATION->{

            }
            Operation.DIVISION->{

            }
        }
        goto(DifficultProblem(operation))
    }
}

fun EasyProblem(operation:Operation) :State = state(Interaction){
    var num1: Int
    var result = 0
    onEntry{
        num1 = Random.nextInt(1,2)
        when(operation){
            Operation.ADDITION->{
                result = num1 + num1
                furhat.ask("What would I get if I had $num1 toffees and bought $num1 more?")
            }
            Operation.SUBTRACTION->{
                result = 0
                furhat.ask("If I had $num1 toffees and I lost all, how many would I have?")
            }
            Operation.MULTIPLICATION->{
                result = num1
                furhat.ask("If a box has 1 toffee and bought $num1 boxes, how many toffees would I have in total?")
            }
            Operation.EQUATION->{

            }
            Operation.DIVISION->{

            }
        }
    }
    onResponse<Number>{
        if(result == it.intent.value){
            furhat.say("Good job! Let's try some more problems to get comfortable with the concept.")
            goto(MediumProblem(operation))
        }
        else{
            furhat.say("Hmm, I think it'd be a good idea to review some of the concepts again. Let's rewind")
            goto(DetailedExplanation(operation))
        }
    }
}

val EvaluateConditions:State = state(Interaction){
    onEntry{
        var emotion = detectEmotion()
        if(emotion == "fine"){
            goto(GetOperation)
        }
        else{
            goto(GiveHomework)
        }
    }
}

val GiveHomework:State = state(Interaction){
    onEntry{
        furhat.say("Go do your homework")
    }
}
