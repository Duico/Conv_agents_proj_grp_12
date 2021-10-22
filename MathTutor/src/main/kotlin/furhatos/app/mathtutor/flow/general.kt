package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.gaze.Gaze
import furhatos.app.mathtutor.gaze.getRandomLocation
import furhatos.event.Event
import furhatos.flow.kotlin.*
import furhatos.records.Location
import furhatos.util.Gender
import furhatos.util.Language
import khttp.async
import java.io.File
import java.time.LocalDateTime
import java.time.temporal.TemporalAmount
import java.util.*
import kotlin.random.Random

val interruptionGaze: Gaze = Gaze("/interrupted.txt")
val startSpeakingGaze: Gaze = Gaze("/start_speaking.txt")
var isInGazeSequence = false

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
//        val now = LocalDateTime.now().toString().replace(':', '-')
        val logFile = File("logs/log-${UUID.randomUUID()}.log")
        logFile.createNewFile()
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

enum class CurrentGazeStates {
    START_SPEAKING, INTERRUPT, LISTENING, SPEAKING
}

var currentGazeState = CurrentGazeStates.LISTENING

val GazeLoop: State = state {
    onEntry {
        send(OnListening())
    }

    onEvent<OnStartTalking>(cond = { currentGazeState != CurrentGazeStates.START_SPEAKING}) {
        currentGazeState = CurrentGazeStates.START_SPEAKING
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
        furhat.attend(users.current)
        send(OnSpeaking())
    }

    onEvent<OnInterrupt>(cond = { currentGazeState != CurrentGazeStates.INTERRUPT}) {
        currentGazeState = CurrentGazeStates.INTERRUPT
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
        furhat.attend(users.current)
        send(OnSpeaking())
    }

    onEvent<OnSpeaking>(cond = { currentGazeState != CurrentGazeStates.SPEAKING}) {
        currentGazeState = CurrentGazeStates.SPEAKING
        var timeLastGazed = LocalDateTime.now()
        // Random glances away while speaking
        while (true) {
            //println("onspeakingtriggered")
            val wait = Random.nextDouble(2.0, 5.0)
            if (LocalDateTime.now().minusSeconds(wait.toLong()) > timeLastGazed) {
//                delay(wait.toLong() * 100)
                timeLastGazed = LocalDateTime.now()
                val glanceLength = Random.nextInt(1, 2)
                furhat.glance(Location(getRandomLocation().x*0.5, getRandomLocation().y*0.5, getRandomLocation().z),
                    glanceLength)
            }
        }
    }

    onEvent<OnListening>(cond = { currentGazeState != CurrentGazeStates.LISTENING}) {
        currentGazeState = CurrentGazeStates.LISTENING
        print("onlisteningtriggered")
        furhat.attend(furhat.users.current)
    }
}

enum class Operation {
    ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, EQUATION
}