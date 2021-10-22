package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.gaze.Gaze
import furhatos.app.mathtutor.gaze.getRandomLocation
import furhatos.event.Event
import furhatos.flow.kotlin.*
import furhatos.records.Location
import furhatos.util.Gender
import furhatos.util.Language
import java.io.File
import java.util.*
import kotlin.random.Random

val interruptionGaze: Gaze = Gaze("/interrupted.txt")
val startSpeakingGaze: Gaze = Gaze("/start_speaking.txt")

val Idle: State = state {

    init {
        furhat.setVoice(Language.ENGLISH_US, Gender.MALE)
        if (users.count > 0) {
            furhat.attend(users.random)
            goto(Greeting)
        }
    }

    onEntry {
        furhat.attendNobody()
    }

    onUserEnter {
        furhat.attend(it)
        goto(Greeting)
    }
}

val Interaction: State = state {
    init {
        //to make all states interruptible. This can be done individually for each state as well
        furhat.param.interruptableOnAsk = true
        // Make Furhat interruptable during all furhat.say(...)
        furhat.param.interruptableOnSay = false
        //to make states that implement onResponse { } interruptible as well
        furhat.param.interruptableWithoutIntents = true

        // Set up flow logging
        //val now = LocalDateTime.now()
        val logFile = File("logs/log_${UUID.randomUUID()}.log")
        logFile.createNewFile()
        //println("logs/log_${now}_${UUID.randomUUID()}.txt")
        //println(status)
        flowLogger.start(logFile) //Start the logger
        parallel(abortOnExit = false){
            goto(GazeLoop) // Start parallel gaze loop which catches Events
        }
    }

    onUserLeave(instant = true) {
        if (users.count > 0) {
            if (it == users.current) {
                furhat.attend(users.other)
                goto(Greeting)
            } else {
                furhat.glance(it)
            }
        } else {
            goto(Idle)
        }
    }

    onUserEnter(instant = true) {
        furhat.glance(it)
    }

    onResponse(instant = true) {
        if(it.interrupted == true) {
            parallel {
                send(OnInterrupt())
            }
            reentry()
        }
        else{
            reentry()
        }
    }

}

class OnStartTalking : Event()
class OnInterrupt : Event()
class OnListening : Event()
class OnSpeaking : Event()

val GazeLoop: State = state {
    onEntry {
        send(OnListening())
    }

    onEvent<OnStartTalking> {
        var lookingAway = false
        val sample = startSpeakingGaze.getRandomSample()

        if (sample != null) { // Do nothing if, for some reason, the resource file cannot be found
            for (gazeState in sample) {
                if (!gazeState) { // Check if we should be looking away (!gazeState)
                    if (!lookingAway) { // Only find a new spot to look at if furhat is currently looking at the user
                        // Get some random spot to look at
                        val absoluteLocation = getRandomLocation()
                        furhat.attend(absoluteLocation)
                        lookingAway = true
                    }
                } else {
                    furhat.attend(users.current)
                }
                // Sample data is in 10ms buckets, so this loop should only run at that frequency
                delay(10)
            }
        }
        send(OnSpeaking())
    }

    onEvent<OnInterrupt> {
        var lookingAway = false
        val sample = interruptionGaze.getRandomSample()
        if (sample != null) { // Do nothing if, for some reason, the resource file cannot be found
            for (gazeState in sample) {
                if (!gazeState) { // Check if we should be looking away (!gazeState)
                    if (!lookingAway) { // Only find a new spot to look at if furhat is currently looking at the user
                        // Get some random spot to look at
                        val absoluteLocation = getRandomLocation()
                        furhat.attend(absoluteLocation)
                        lookingAway = true
                    }
                } else {
                    furhat.attend(users.current)
                }
                delay(10) // Sample data is in 10ms buckets, so this loop should only run at that frequency
            }
        }

        send(OnSpeaking())
    }

    onEvent<OnSpeaking> {

        // Random glances away while speaking
        while (true) {
            //println("onspeakingtriggered")
            val wait = Random.nextDouble(1.5, 5.0)
            delay(wait.toLong() * 100)
            val glanceLength = Random.nextInt(1, 3)
            furhat.glance(Location(getRandomLocation().x*0.5, getRandomLocation().y*0.5, getRandomLocation().z),
                glanceLength)
        }
    }

    onEvent<OnListening> {
        print("onlisteningtriggered")
        furhat.attend(furhat.users.current)
    }
}

enum class Operation {
    ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, EQUATION
}