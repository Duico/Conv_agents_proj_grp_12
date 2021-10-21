package gaze

import furhatos.app.mathtutor.gaze.Gaze
import furhatos.app.mathtutor.gaze.getRandomLocation
import org.junit.Test
import org.junit.jupiter.api.Assertions

internal class GazeTest {

    @Test
    fun `Test getRandomSample with interrupted text file`() {
        val interruptionGaze = Gaze("/interrupted.txt")

        val samples = interruptionGaze.samples?.samples
        if (samples != null) {
            Assertions.assertFalse(samples.isEmpty())
            for (sample in samples) {
                Assertions.assertEquals(350, sample.size) // should all be 3.5 seconds of gaze data
            }
        }
    }

    @Test
    fun `Test getRandomSample with start_speaking text file`() {
        val interruptionGaze = Gaze("/start_speaking.txt")

        val samples = interruptionGaze.samples?.samples
        if (samples != null) {
            Assertions.assertFalse(samples.isEmpty())
            for (sample in samples) {
                Assertions.assertEquals(350, sample.size) // should all be 3.5 seconds of gaze data
            }
        }
    }

    @Test
    fun `Test getRandomLocation`() {
        for (i in 0..1000) {
            val location = getRandomLocation()
            Assertions.assertEquals(location.z, 0.0)
            Assertions.assertTrue(location.x >= -2.5)
            // Assert random location isn't too close to middle
            Assertions.assertFalse(location.x > -1 && location.x < 1)
            Assertions.assertTrue(location.x <= 2.5)
            Assertions.assertTrue(location.y >= -0.5)
            Assertions.assertTrue(location.y <= 0.0)
        }
    }
}