package furhatos.app.mathtutor.gaze

import furhatos.nlu.kotlin.logger
import furhatos.records.Location
import kotlin.random.Random


fun getRandomLocation(): Location {

    var x = Random.nextDouble(0.25, 1.0)   // Look right by default
    val direction = Random.nextInt(0, 2)
    if (direction == 0) { // Look left if `direction` is 0
        x = -x
    }
    val y = Random.nextDouble(-0.5, 0.2) // Random double between -0.5 and 0.5
    return Location(x, y, 3.0)
}

class Gaze(type: String) {
    init {
        if (type[0] != '/') {
            logger.warn("Resource filename must begin with a '/' character.")
        }
    }

    val samples: Samples? = getSampleData(type)

    fun getRandomSample(): BooleanArray? {
        if (samples == null) {
            return null
        }
        val sampleNumber = Random.nextInt(0, samples.size)
        return samples[sampleNumber]
    }

    private fun getSampleData(type: String): Samples? {
        val rawSampleData = this.javaClass.getResource(type)
        if (rawSampleData != null) {
            val text = rawSampleData.readText()
            val sampleArray: ArrayList<BooleanArray> = ArrayList(text.lines().count())
            for (line in text.lines()) {
                if (line.isNotEmpty()) {
                    val currentSample = BooleanArray(line.length)
                    for ((j, c) in line.withIndex()) {
                        currentSample[j] = c == 'g' // True if we should be gazing at the person
                    }
                    sampleArray.add(currentSample)
                }
            }
            return Samples(sampleArray.toTypedArray())
        }
        logger.warn("Cannot find a resource file by the name $type.")
        return null
    }
}