package furhatos.app.mathtutor

import furhatos.app.mathtutor.flow.*
import furhatos.skills.Skill
import furhatos.flow.kotlin.*

class MathtutorSkill : Skill() {
    override fun start() {
        Flow().run(Idle)
    }
}

fun main(args: Array<String>) {
    Skill.main(args)
}
