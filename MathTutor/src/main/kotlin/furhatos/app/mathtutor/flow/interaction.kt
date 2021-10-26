package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.nlu.*
import furhatos.nlu.common.*
import furhatos.flow.kotlin.*
import furhatos.nlu.common.Number
import java.net.HttpURLConnection
import java.net.URL
import java.time.Duration
import java.time.Instant
import kotlin.random.Random

var learntOperation: MutableMap<Operation, Boolean> = mutableMapOf()
var prevOp : Operation? = null
var entryTime : Instant = Instant.now()

var binaryOpNum1: Int? = null
var binaryOpNum2: Int? = null

fun detectEmotion(): String {
    val url = URL("http://localhost:9999/detect")
    with(url.openConnection() as HttpURLConnection) {
        requestMethod = "GET"  // optional default is GET
        //print("\nSent 'GET' request to URL : $url; Response Code : $responseCode")
        return responseMessage
    }
}

fun timeLeft(): Boolean {
    var currTime = Instant.now()
    print("Duration (sec): "+Duration.between(entryTime, currTime).seconds)
    return Duration.between(entryTime, currTime).seconds < 180
}

val Greeting : State = state(Interaction) {

    onEntry {
        if(!DISABLE_GAZE) {
            parallel(abortOnExit = false) {
                goto(GazeLoop)
            }
        }
        send(OnStartTalking())
        entryTime = Instant.now()
        furhat.say("Hello. I'm MathTutor, your AI math teacher.")
        goto(GetOperation(false, false))
    }


//    onResponse{
//        goto(GetOperation)
//    }
}

fun GetOperation(askProblem: Boolean = false, secondTry: Boolean = false) : State = state(Interaction) {
    onEntry {
            send(OnStartTalking())
            if(secondTry){
                    furhat.ask("I beg your pardon?")
            }else{
                if(askProblem)
                    furhat.ask("What math problem are you stuck on?")
                else
                    furhat.listen()
            }
        send(OnListening())
        }
//    onReentry {
//        furhat.ask("I beg your pardon?")
//    }

    fun handleOperationIntent(tr: TriggerRunner<*>, op: Operation){
        if(prevOp == op){
            tr.furhat.say("Well, you just finished that topic, but it's okay if you want to go over it again!")
        }
        prevOp = op
        tr.goto(BriefExplanation(op))
    }
    onResponse<OperationIntent> {
        if(it.intent.operationEntity != null) {
            if(it.intent.operationEntity!!.binaryOperation != null && it.intent.operationEntity!!.binaryOperation!!.operand != null) {
                val binaryOp = it.intent.operationEntity!!.binaryOperation!!
                val op = it.intent.operationEntity!!.binaryOperation!!.operand!!.operation
                if(op == null ){
                    reentry()
                }else {
                    binaryOpNum1 = binaryOp.op1?.value
                    binaryOpNum2 = binaryOp.op2?.value
                    handleOperationIntent(this, op)
                }
            }
        }else{
            goto(GetOperation(true, true))
        }
    }
    fun handleVerbalOperationIntent(triggerRunner: TriggerRunner<*>, intent: VerbalOperationIntent){
        val op = intent.operation
        handleOperationIntent(triggerRunner, op)
    }
    onResponse<VerbalAddition>{
        handleVerbalOperationIntent(this, it.intent)
    }
    onResponse<VerbalSubtraction>{
        handleVerbalOperationIntent(this, it.intent)
    }
    onResponse<VerbalMultiplication>{
        handleVerbalOperationIntent(this, it.intent)
    }
    onResponse<VerbalDivision>{
        handleVerbalOperationIntent(this, it.intent)
    }
    onResponse<Greeting>{
        var afterGreetingUtterance = ""
        random(
                {afterGreetingUtterance = "Let's start!"},
                {afterGreetingUtterance = "Let's begin!"},
                {afterGreetingUtterance = "Alright, let's start!"},
                {afterGreetingUtterance = "Cool, let's get started!"}
        )

        furhat.say(afterGreetingUtterance)
        goto(GetOperation(true, false))
    }

    onResponse{
        goto(GetOperation(true, true))
    }
    onNoResponse {
        goto(GetOperation(true, true))
    }
}

val GetOperationSecond: State = state(GetOperation()){
    onEntry {
        send(OnStartTalking())
        furhat.ask("Is there any other problem you want to discuss?")
    }

    onResponse<Greeting> {
        reentry()
    }
}

fun BriefExplanation(operation: Operation) = state(Interaction) {
    onEntry {
        send(OnStartTalking())
        val text = operation.toString()
        var introSentence : String = ""
        random({ introSentence = "Ok, you have some troubles with $text."},
                {introSentence = "Got it! You have some doubts with $text."},
                {introSentence = "Ohh, $text! That's an easy one!"})
        furhat.say(introSentence)
        //TODO smile

        furhat.say("But before I begin, let me ask you a simple question from this topic to check your understanding")

        goto(GaugeBriefExplanation(operation))
    }
}

fun GaugeBriefExplanation(operation : Operation) = state(Interaction) {
    var result = 0
    var num1 : Int
    var num2 : Int
    var question : String? = null

    fun generateQuestion(tr: FlowControlRunner){
        num1  = binaryOpNum1 ?: Random.nextInt(3,6)
        num2 =  binaryOpNum2 ?: Random.nextInt(2,num1)

        when(operation) {
            Operation.ADDITION -> {
                result = num1 + num2
                tr.random({question ="So if I had $num1 apples, and I get $num2 more, how many do I have now?"}
                )
            }
            Operation.SUBTRACTION -> {
                result = num1 - num2
                tr.random({question="So if I had $num1 apples, and I lose $num2 , how many do I have now?"}
                )
            }
            Operation.MULTIPLICATION -> {
                result = num1 * num2
                tr.random({question = "So if I had a basket with $num1 apples, and I buy $num2 such baskets, how many apples do I have now?"}
                )
            }
            else->{

            }

        }
        //TODO ADD OPERATION EQUATION
    }

    onEntry{
        send(OnStartTalking())
        if(question == null)
            generateQuestion(this)
        furhat.ask(question!!)
    }
    onResponse<Number>{
        if(it.intent.value == result){
            val emotion = detectEmotion()
            furhat.say("Good job! Let's try another problem then")
            if(emotion == "happy" || emotion == "neutral" || emotion == "surprise") {
                furhat.say("You seem pleased!")
                goto(MediumProblem(operation))
            }
        }
        else{
            furhat.say("Hummm...")
            furhat.say("Unfortunately, that's not the right answer. I think it'd be a good idea to understand the concept better")
            goto(DetailedExplanation(operation))
        }
    }
    onResponse{
        reentry()
    }

}

fun DetailedExplanation(operation: Operation) :State = state(Interaction){
    onEntry {
        send(OnStartTalking())
        furhat.say("Let's make this an interactive learning experience.")
        delay(1000)
        val num1 = Random.nextInt(2, 5)
        val num2 = Random.nextInt(1, num1)
        val num3 = Random.nextInt(1, 5)
        val limit = 10 / num3

        when (operation) {
            Operation.ADDITION -> {
                random({furhat.say("To understand this concept better, let's take an example of adding $num1 and $num2")},{furhat.say("To get a grasp of the concept, let's see how to add $num1 and $num2")})
                delay(1000)
                random({ furhat.say("Okay, now I want you to show $num1 using your fingers") },
                    { furhat.say("Raise $num1 fingers") }
                )
                delay(2000)
                random(
                    { furhat.say("Now raise $num2 more") }
                )
                delay(3000)
                furhat.say("Let's count the number of fingers now")
                delay(2000)

                send(OnListening())
                furhat.say("You have..")
                for( i in 1..num1+num2){
                    furhat.say(i.toString())
                    delay(500)
                }
                furhat.say("Which gives us the answer of ${num1+num2} fingers. Easy")
            }
            Operation.SUBTRACTION -> {
                random({furhat.say("To understand this concept better, let's take an example of subtracting $num1 and $num2")},{furhat.say("To get a grasp of the concept, let's see how to subtract $num1 and $num2")})
                delay(2000)
                random({ furhat.say("Okay, now I want you to show $num1 using your fingers") },
                    { furhat.say("Raise $num1 fingers") }
                )
                delay(2000)
                furhat.say("Let's count down from $num1 fingers to perform subtraction")
                delay(1000)
                for(i in num1-1 downTo num1-num2) {
                    random(
                        { furhat.say("after closing one more finger, we have $i") }
                    )
                    delay(1000)
                }
                furhat.say("Count the number of fingers now")
                delay(4000)
                furhat.say("Now you should have ${num1-num2} fingers. Easy")
                delay(1000)
            }
            Operation.MULTIPLICATION -> {
                random({furhat.say("To understand this concept better, let's take an example of multiplying $num1 and $num2")},{furhat.say("To get a grasp of the concept, let's see how to multiplying $num1 and $num2")})
                random({ furhat.say("Okay, now I want you to show $num3 using your fingers") },
                    { furhat.say("Raise $num3 fingers") }
                )
                delay(3000)
                furhat.say("What you have is $num3 times 1")
                for (i in 2..limit) {
                    random({ furhat.say("Raise $num3 more ") },
                        { furhat.say("lift $num3 more ") },
                        { furhat.say("$num3 more") }
                    )
                    delay(1000)
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
        send(OnStartTalking())
        var emotion = detectEmotion()
        if((emotion == "sad" || emotion=="disgust" || emotion=="fear" || emotion =="angry")   && timeLeft()){
            furhat.say("You don't seem satisfied. Let's look at the explanation again")
            goto(DetailedExplanation(operation))
        }
        else if(timeLeft() == false){
            if(prevOp != null){
                learntOperation[prevOp!!] = true
            }
            goto(Encouragement(operation))
        }
        else{
            furhat.say("Okay. Let's try another problem")
            delay(1000)
            goto(MediumProblem(operation))
        }
    }
}

fun Encouragement(operation: Operation) = state(Interaction){
    onEntry{
        random({furhat.say("I understand that you aren't very happy with your performance today")},
            {furhat.say("Don't worry though! Through practice, you will definitely master the concepts!")})
        var emotion = detectEmotion()
        while(emotion == "fear" || emotion == "disgust" || emotion == "angry" || emotion == "sad"){
            furhat.say("C'mon! Cheer up!")
            reentry()
        }
        goto(GiveHomework)
    }
}



fun MediumProblem(operation: Operation) = state(Interaction){
    var result = 0
    var num1 = 0
    var num2 = 0
    var question: String? = null

    fun generateQuestion(tr: FlowControlRunner) {
        num1 = Random.nextInt(2, 5)
        num2 = if(operation==Operation.MULTIPLICATION) Random.nextInt(2,5) else Random.nextInt(1, num1)
        when (operation) {
            Operation.ADDITION -> {
                result = num1 + num2
                tr.random({question = "If I have $num1 coins and then get $num2 more, how many do I finally have?"},
                        {question = "I have $num1 candies and you have $num2. Now I give you all of my candies, so how many will you end up with?"}
                )
            }
            Operation.SUBTRACTION -> {
                result = num1 - num2
                tr.random({ question = "So if I had $num1 apples, and I lose $num2 , how many do I have now?" },
                        {question = "I have $num2 apples, but I want $num1, so how many more should I buy?"}
                )
            }
            Operation.MULTIPLICATION -> {
                result = num1 * num2
                tr.random({question = "So if I had a basket with $num1 apples, and I buy $num2 such baskets, how many do I have now?"},
                        {question = "A box of chocolates at the store has $num1 pieces, and I buy $num2 boxes, how many chocolates do I have in total?"}
                )
            }
            else -> {
                question = ""
            }

        }
    }
    onEntry {
        send(OnStartTalking())
        generateQuestion(this)
        furhat.ask(question!!)
    }

    onReentry {
        if(question == null)
            generateQuestion(this)
        furhat.ask(question!!)
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

    onResponse<RepeatQuestion>{
        reentry()
    }
    onResponse{
        goto(MediumProblemSolution(operation, num1, num2))
    }

}


fun DifficultProblem(operation:Operation) : State = state(Interaction){
    var num1 = 0
    var num2 = 0
    var num3 = 0
    var result = 0
    var question: String? = null

    fun generateQuestion(tr: FlowControlRunner) {
        num1 = Random.nextInt(8, 10)
        num2 = if (operation==Operation.MULTIPLICATION) Random.nextInt(2,4) else Random.nextInt(1, 6)
        num3 = if (operation==Operation.MULTIPLICATION) Random.nextInt(2,4) else Random.nextInt(1, 6)
        when (operation) {
            Operation.ADDITION -> {
                result = num1 + num2 + num3
                tr.random({ question = "I have $num1 bananas, $num2 oranges, and $num3 apples. How many do I have in total?" },
                        { question = "Suppose I have $num1 coins, you have $num2 coins. I then take all of your coins and a robber who has $num3 coins already with him then steals all the coins I have. How many coins does the robber have now?" })
            }
            Operation.SUBTRACTION -> {
                result = num1 - num2 - num3
                tr.random(
                        { question = "You have $num1 toffees. You give me $num2 of them and $num3 to another friend. How many do you have left?" }

                )
            }
            Operation.MULTIPLICATION -> {
                result = num1 * num2 * num3
                tr.random(
                        { question = "A box of toffees has $num1 pieces. Each carton contains $num2 boxes, and a truck can carry $num3 cartons. How many tofees can a truck carry at most?" }
                )
            }
            else->{
                question=""
            }
        }
    }

    onEntry{
        send(OnStartTalking())
        generateQuestion(this)
        furhat.ask(question!!)

    }

    onReentry {
        if(question == null)
            generateQuestion(this)
        furhat.ask(question!!)
    }


    onResponse<Number>{
        if(result != it.intent.value){
            goto(DifficultProblemSolution(operation,num1,num2,num3))
        }
        else{
            if(prevOp!=null)
                learntOperation[prevOp!!] = true
            if(timeLeft()){
                goto(EvaluateConditions)
            }
            else{
                goto(GiveHomework)
            }
        }
    }
    onResponse<RepeatQuestion>{
        reentry()
    }
    onResponse{
        goto(DifficultProblemSolution(operation, num1, num2, num3))
    }
}

fun MediumProblemSolution(operation:Operation, num1: Int, num2:Int) = state(Interaction){
    onEntry{
        send(OnStartTalking())
        random({furhat.say("Umm, no. Let's see how we should solve this problem")})
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
                for(i in 1..num1+num2){
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
                for(i in 1..num1-num2){
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
            else->{

            }
        }
        var emotion = detectEmotion()
        if(emotion == "sad" || emotion =="fear" || emotion == "disgust" || emotion == "angry"){
            furhat.say("Hmm you don't seem happy, let's try out an easier problem.")
            goto(EasyProblem(operation))
        }
        else{
            furhat.say("Alright then, you look confident. I think you've understood this solution. Let's move on to a more challenging problem now.")
            goto(DifficultProblem(operation))
        }
    }
}

fun DifficultProblemSolution(operation: Operation,num1: Int,num2: Int,num3:Int) = state(Interaction){
    onEntry{
        send(OnStartTalking())
        when(operation){
            Operation.ADDITION->{
                furhat.say("Incorrect! Now that you seem comfortable with the basics of addition, let's see how this can be broken down. Since there are 3 numbers to add, you first add $num1 and $num2")
                delay(1000)
                furhat.say("You should end up with a sum of ${num1+num2}")
                delay(1000)
                furhat.say("Now add $num3 to this result to get the final answer")
                delay(1000)
                furhat.say("And the answer is ${num1+num2+num3}")
            }
            Operation.SUBTRACTION->{
                furhat.say("Incorrect! Now that you seem comfortable with the basics of subtraction, let's see how this can be broken down. You first subtract $num1 and $num2")
                delay(1000)
                furhat.say("You should end up with a difference of ${num1-num2}")
                delay(1000)
                furhat.say("Now subtract $num3 to this result to get the final answer")
                delay(1000)
                furhat.say("And the answer is ${num1-num2-num3}")
            }
            Operation.MULTIPLICATION->{
                furhat.say("Incorrect! Now that you seem comfortable with the basics of multiplication, let's see how this can be broken down.Since there are 3 numbers to multiply, you first multiply $num1 and $num2")
                delay(1000)
                furhat.say("You should end up with a product of ${num1*num2}")
                delay(1000)
                furhat.say("Now multiply $num3 with this result to get the final answer")
                delay(1000)
                furhat.say("And the answer is ${num1*num2*num3}")
            }
            else->{

            }
        }
        goto(MediumProblem(operation))
    }
}
fun EasyProblem(operation:Operation) :State = state(Interaction){
    var num1: Int
    var result = 0
    var question : String? = null

    fun generateQuestion(tr: FlowControlRunner) {
        num1 = Random.nextInt(1, 2)
        when(operation){
            Operation.ADDITION->{
                result = num1 + num1
                tr.random( {question = "What would I get if I had $num1 toffees and bought $num1 more?"} )
            }
            Operation.SUBTRACTION->{
                result = 0
                tr.random( {question = "If I had $num1 toffees and I lost all of them, how many would I have?"} )
            }
            Operation.MULTIPLICATION->{
                result = num1
                tr.random( {question = "If a box has 1 toffee and bought $num1 boxes, how many toffees would I have in total?"} )
            }
            else->{
                question = ""
            }
        }
    }


    onEntry{
        send(OnStartTalking())
        generateQuestion(this)
        furhat.ask(question!!)
    }

    onReentry {
        if(question == null)
            generateQuestion(this)
        furhat.ask(question!!)
    }

    onResponse<Number>{
        if(it.intent.value == result){
            furhat.say("Good job! Let's try some more problems to get comfortable with the concept.")
            goto(MediumProblem(operation))
        }
        else{
            furhat.say("Hmm, I think it'd be a good idea to review some of the concepts again. Let's rewind")
            goto(DetailedExplanation(operation))
        }
    }
    onResponse<RepeatQuestion> {
        reentry()
    }
    onResponse{
        goto(DetailedExplanation(operation))
    }
}

val EvaluateConditions:State = state(Interaction){
    onEntry{
        send(OnStartTalking())
        var emotion = detectEmotion()
        if(timeLeft() && (emotion == "happy" || emotion =="neutral" || emotion == "surprise")){
            var learnMore = furhat.askYN("Looks like we still have some time left plus you seem confident and happy. Would you like to learn a new concept?")
            if(learnMore == true){
                goto(GetOperationSecond)
            }else {
                furhat.say("Haha, I'm sorry, but I can't let you leave class this early. Let's solve some more problems!")
                if (prevOp != null) {
                    goto(MediumProblem(prevOp!!))

                }
            }
        }
        else{
            goto(GiveHomework)
        }
    }
}

val GiveHomework:State = state(Interaction){
    onEntry{
        send(OnStartTalking())
        var operationsText = ""
        var firstOperation = true
        print("Learnt operations: ")
        println(learntOperation)
        learntOperation.filterValues { v->v }.forEach { (op: Operation, _) ->
            run {
                if (firstOperation) {
                    operationsText += "$op"
                    firstOperation = false
                } else
                    operationsText += ", $op"
            }
        }
        furhat.say("Well done! Today we learned about $operationsText.")
        var satisfied = furhat.askYN("Did you enjoy today's session?")
        if (satisfied == true) {
            furhat.say("That's nice to hear!")
        } else {
            furhat.say("I'm sorry. I'll try to do better next time!")
        }
        furhat.say("Bye!")
    }
}
