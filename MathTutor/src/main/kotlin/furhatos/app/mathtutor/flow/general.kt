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

const val DISABLE_GAZE = false
const val ENABLE_ATTEND_BUSY = true

val interruptionGaze: Gaze = Gaze("/interrupted.txt")
val startSpeakingGaze: Gaze = Gaze("/start_speaking.txt")
const val DELAY_TIME = 10L

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
            goto(GazeLoop)
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
            send(OnInterrupt())
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

val GazeLoop: State = state {
    var attendBusy = false
    var lookingAway = false
    var lookingAwayWithPriority = false

    fun setLookingAway(isLookingAway: Boolean, priority: Boolean = false){
        lookingAway = isLookingAway
        if(!isLookingAway || priority)
            lookingAwayWithPriority = isLookingAway
    }

    fun Furhat.gazeFromSample(tr: TriggerRunner<*>, sample: BooleanArray?) {
        if (sample != null) { // Do nothing if, for some reason, the resource file cannot be found
            attendBusy = true
            for (gazeState in sample) {
                // Sample data is in 10ms buckets, so this loop should only run at that frequency
                    if (!gazeState) { // Check if we should be looking away (!gazeState)                            lookingAway = true
                        if (!lookingAwayWithPriority) { // Only find a new spot to look at if furhat is currently looking at the user
                            // Get some random spot to look at
                            val absoluteLocation = Location(getRandomLocation().x, getRandomLocation().y, getRandomLocation().z)
                            attend(absoluteLocation)
                            setLookingAway(true, true)
                        }
                    } else {
                        attend(users.current)
                        setLookingAway(false)
                    }
                tr.delay(DELAY_TIME)
                //}
            }
            attendBusy = false
        }
    }

    onEntry {
        send(OnListening())
    }

    onEvent<OnStartTalking>() {
        val sample = startSpeakingGaze.getRandomSample()
        furhat.gazeFromSample(this, sample)
        furhat.attend(users.current)
        setLookingAway(false)
        send(OnSpeaking())
    }

    onEvent<OnInterrupt>() {
        val sample = interruptionGaze.getRandomSample()
        furhat.gazeFromSample(this, sample)
        furhat.attend(users.current)
        setLookingAway(false)
        send(OnSpeaking())
    }

    onEvent<OnSpeaking>() {
        // Random glances away while speaking
        val preWait = Random.nextDouble(.5,2.0)
        delay((preWait*1000).toLong())
        while(true) {
            val postWait = Random.nextDouble(4.0, 8.0)
            if(ENABLE_ATTEND_BUSY) {
                if (attendBusy) {
                    println("attendBusy")
                    continue
                }
            }
            //if (Duration.between(Instant.now(), timeLastGazed).seconds > wait) {
                if (!lookingAway) {
                    setLookingAway(true)
                    furhat.attend(Location(getRandomLocation().x*0.3, getRandomLocation().y*0.3, getRandomLocation().z))
                } else {
                    setLookingAway(true)
                    furhat.attend(furhat.users.current)
                }
            //}
            delay((postWait*1000).toLong())
        }
    }

    onEvent<OnListening>() {
        furhat.attend(furhat.users.current)
        lookingAway = false
    }

/*    onEvent<Event>{
        //print(it.event_name)
    }*/
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