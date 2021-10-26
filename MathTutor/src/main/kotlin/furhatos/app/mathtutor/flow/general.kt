package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.gaze.Gaze
import furhatos.app.mathtutor.gaze.getRandomLocation
import furhatos.event.Event
import furhatos.flow.kotlin.*
import furhatos.records.Location
import furhatos.util.Gender
import furhatos.util.Language
import java.io.File
import java.time.LocalDateTime
import java.util.*
import kotlin.random.Random

const DISABLE_GAZE = false

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

class OnStartTalking : Event()
class OnInterrupt : Event()
class OnListening : Event()
class OnSpeaking : Event()

enum class CurrentGazeStates {
    START_SPEAKING, INTERRUPT, LISTENING, SPEAKING
}

var currentGazeState = CurrentGazeStates.LISTENING

val GazeLoop: State = state {
    fun Furhat.gazeFromSample(sample: BooleanArray?) {
        if (sample != null) { // Do nothing if, for some reason, the resource file cannot be found
            var lookingAway = false
            var lastSample = LocalDateTime.now()
            for (gazeState in sample) {
                // Sample data is in 10ms buckets, so this loop should only run at that frequency
                if (LocalDateTime.now().minusNanos(DELAY_TIME) > lastSample) {
                    lastSample = LocalDateTime.now()
                    if (!gazeState) { // Check if we should be looking away (!gazeState)
                        if (!lookingAway) { // Only find a new spot to look at if furhat is currently looking at the user
                            // Get some random spot to look at
                            val absoluteLocation = getRandomLocation()
                            if (!DISABLE_GAZE) {
                                attend(absoluteLocation)
                            }
                            lookingAway = true
                        }
                    } else {
                        if (!DISABLE_GAZE) {
                            attend(users.current)
                        }
                    }
                }
            }
        }
    }

    onEntry {
        send(OnListening())
    }

    onEvent<OnStartTalking>(cond = { currentGazeState != CurrentGazeStates.START_SPEAKING}) {
        currentGazeState = CurrentGazeStates.START_SPEAKING
        val sample = startSpeakingGaze.getRandomSample()
        furhat.gazeFromSample(sample)
        if (!DISABLE_GAZE) {
            furhat.attend(users.current)
        }
        send(OnSpeaking())
    }

    onEvent<OnInterrupt>(cond = { currentGazeState != CurrentGazeStates.INTERRUPT}) {
        currentGazeState = CurrentGazeStates.INTERRUPT
        val sample = interruptionGaze.getRandomSample()
        furhat.gazeFromSample(sample)
        if (!DISABLE_GAZE) {
            furhat.attend(users.current)
        }
        send(OnSpeaking())
    }

    onEvent<OnSpeaking>(cond = { currentGazeState != CurrentGazeStates.SPEAKING}) {
        currentGazeState = CurrentGazeStates.SPEAKING
        var timeLastGazed = LocalDateTime.now()
        var lookingAtSpeaker = true
        // Random glances away while speaking
        while (true) {
            //println("onspeakingtriggered")
            val wait = Random.nextDouble(4.0, 8.0)
            if (LocalDateTime.now().minusSeconds(wait.toLong()) > timeLastGazed) {
                timeLastGazed = LocalDateTime.now()
                if (lookingAtSpeaker) {
                    lookingAtSpeaker = false
                    if (!DISABLE_GAZE) {
                        furhat.attend(Location(getRandomLocation().x*0.5, getRandomLocation().y*0.5, getRandomLocation().z))
                    }
                } else {
                    lookingAtSpeaker = true
                    if (!DISABLE_GAZE) {
                        furhat.attend(furhat.users.current)
                    }
                }
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
    ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, EQUATION;

    override fun toString(): String {
        return when(this) {
            ADDITION -> "addition"
            SUBTRACTION -> "subtraction"
            MULTIPLICATION -> "multiplication"
            DIVISION -> "division"
            EQUATION -> "equations"
        }

    }
}
