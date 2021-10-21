package furhatos.app.mathtutor.flow

import furhatos.app.mathtutor.gaze.Gaze
import furhatos.app.mathtutor.gaze.getRandomLocation
import furhatos.flow.kotlin.*
import furhatos.util.Gender
import furhatos.util.Language

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
        furhat.param.interruptableOnAsk = true                  //to make all states interruptible. This can be done individually for each state as well
// Make Furhat interruptable during all furhat.say(...)
        furhat.param.interruptableOnSay = true
        furhat.param.interruptableWithoutIntents = true          //to make states that implement onResponse { } interruptible as well
        parallel {
            goto(StartTalking)
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

    onResponse(instant = true, cond = {it.interrupted}) {
        parallel{
            goto(Interrupted)
        }
    }


}

val Interrupted: State = state {
    onEntry {
        var lookingAway = false
        val sample = interruptionGaze.getRandomSample()
        print(sample)
        print("interrupted")
        if (sample != null) { // Do nothing if, for some reason, the resource file cannot be found
            for (gazeState in sample) {
                if (!gazeState) { // Check if we should be looking away (!gazeState)
                    if (!lookingAway) { // Only find a new spot to look at if furhat is currently looking at the user
                        // Get some random spot to look at
                        val absoluteLocation = getRandomLocation()
                        // Relative to the current user
                        println(absoluteLocation)

                        //val relativeLocation = absoluteLocation.add(Location(users.current))
                        furhat.attend(absoluteLocation)
                        lookingAway = true
                    }
                } else {
                    furhat.attend(users.current)
                }
                delay(10) // Sample data is in 100ms buckets, so this loop should only run at that frequency
            }
        }
    }

    onExit {
        furhat.attend(users.current)
    }
}

val StartTalking: State = state {
    onEntry {
        var lookingAway = false
        val sample = startSpeakingGaze.getRandomSample()

        if (sample != null) { // Do nothing if, for some reason, the resource file cannot be found
            for (gazeState in sample) {
                if (!gazeState) { // Check if we should be looking away (!gazeState)
                    if (!lookingAway) { // Only find a new spot to look at if furhat is currently looking at the user
                        // Get some random spot to look at
                        val absoluteLocation = getRandomLocation()
                        // Relative to the current user
                        //val relativeLocation = absoluteLocation.add(Location(users.current))
                        print("start talking")
                        print(users.current.fields)

                        furhat.attend(absoluteLocation)
                        lookingAway = true
                    }
                } else {
                    furhat.attend(users.current)
                }
                delay(10) // Sample data is in 100ms buckets, so this loop should only run at that frequency
            }
        }
    }

    onExit {
        furhat.attend(users.current)
    }
}

enum class Operation {
    ADDITION, SUBTRACTION, MULTIPLICATION, DIVISION, EQUATION,EMPTY
}