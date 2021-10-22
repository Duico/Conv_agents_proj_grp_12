package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.gaze.Gaze
import furhatos.app.mathtutor.gaze.getRandomLocation
import furhatos.event.Event
import furhatos.event.EventSystem
import furhatos.event.EventSystem.send
import furhatos.flow.kotlin.*
import furhatos.records.Location
import furhatos.util.Gender
import furhatos.util.Language
import java.io.File
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

val interruptionGaze: Gaze = Gaze("/interrupted.txt")
val startSpeakingGaze: Gaze = Gaze("/start_speaking.txt")
const val DELAY_TIME = 10L * 1000000L

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

class OnStartSpeaking : Event()
class OnInterrupt : Event()
class OnListening : Event()
class OnSpeaking : Event()

enum class CurrentGazeStates {
    START_SPEAKING, INTERRUPT, LISTENING, SPEAKING
}

var currentGazeState = CurrentGazeStates.LISTENING
var timeLastGazed: LocalDateTime = LocalDateTime.now()
var lookingAtSpeaker = true
var wait = Random.nextLong(4L, 8L)

var lookingAway = false
var lastSample: LocalDateTime = LocalDateTime.now()
var sampleBucket = 0

val GazeLoop: State = state {
    fun Furhat.gazeFromSample(sample: BooleanArray?) {
        if (sample != null) { // Do nothing if, for some reason, the resource file cannot be found
            // Sample data is in 10ms buckets, so this loop should only run at that frequency
            if (LocalDateTime.now().minusNanos(DELAY_TIME) > lastSample) {
                lastSample = LocalDateTime.now()
                if (!sample[sampleBucket]) { // Check if we should be looking away (!gazeState)
                    if (!lookingAway) { // Only find a new spot to look at if furhat is currently looking at the user
                        // Get some random spot to look at
                        val absoluteLocation = getRandomLocation()
                        attend(absoluteLocation)
                        lookingAway = true
                    }
                } else {
                    attend(users.current)
                }
                sampleBucket += 1
            }
        }
    }

    onEntry {
        send(OnListening())
    }

    onEvent<OnStartSpeaking> {
        if (currentGazeState != CurrentGazeStates.START_SPEAKING) {
            EventSystem.clearQueue()
            currentGazeState = CurrentGazeStates.START_SPEAKING
            lookingAway = false
            lastSample = LocalDateTime.now()
            sampleBucket = 0
        }
        val sample = startSpeakingGaze.getRandomSample()
        furhat.gazeFromSample(sample)
        if (sample != null && sampleBucket >= sample.size) { // switch to normal speaking gaze if sample completed
            EventSystem.clearQueue()
            send(OnSpeaking())
        } else {
            send(OnStartSpeaking())
        }
    }

    onEvent<OnInterrupt> {
        if (currentGazeState != CurrentGazeStates.INTERRUPT) {
            EventSystem.clearQueue()
            currentGazeState = CurrentGazeStates.INTERRUPT
            lookingAway = false
            lastSample = LocalDateTime.now()
        }
        val sample = interruptionGaze.getRandomSample()
        furhat.gazeFromSample(sample)
        if (sample != null && sampleBucket < sample.size) { // if we did the whole sample then we're done
            send(OnStartSpeaking())
        }
    }

    onEvent<OnSpeaking> {
        EventSystem.clearQueue()
        if (currentGazeState != CurrentGazeStates.SPEAKING) {
            furhat.attend(furhat.users.current)
            timeLastGazed = LocalDateTime.now()
            lookingAtSpeaker = true
            wait = Random.nextLong(4L, 8L)
        }

        if (LocalDateTime.now().minusSeconds(wait) > timeLastGazed) {
            timeLastGazed = LocalDateTime.now()
            if (lookingAtSpeaker) {
                lookingAtSpeaker = false
                furhat.attend(Location(getRandomLocation().x*0.5, getRandomLocation().y*0.5, getRandomLocation().z))
            } else {
                lookingAtSpeaker = true
                furhat.attend(furhat.users.current)
            }
        }
        send(OnSpeaking())
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