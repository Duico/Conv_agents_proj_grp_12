package furhatos.app.mathtutor.gaze

data class Samples(val samples: Array<BooleanArray>) {
    val size: Int = samples.size

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Samples

        if (!samples.contentDeepEquals(other.samples)) return false

        return true
    }

    override fun hashCode(): Int {
        return samples.contentDeepHashCode()
    }

    operator fun get(i: Int): BooleanArray {
        return samples[i]
    }
}
