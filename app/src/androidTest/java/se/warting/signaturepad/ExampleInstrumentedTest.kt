package se.warting.signaturepad

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import se.warting.signaturecore.CanvasChangedListener
import se.warting.signaturecore.Event
import se.warting.signaturecore.EventManager

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun useAppContext() = runTest {
        // Context of the SignaturePad-Example under test.
        val appContext = InstrumentationRegistry.getInstrumentation().targetContext

        val initialEvents: List<Event> = listOf(
            Event(timestamp = 1718546886266, action = 0, x = 83.958984f, y = 207.9419f),
            Event(timestamp = 1718546886321, action = 2, x = 90.53905f, y = 207.9419f),
            Event(timestamp = 1718546886337, action = 2, x = 123.69079f, y = 197.7146f),
            Event(timestamp = 1718546886354, action = 2, x = 175.25813f, y = 184.66736f),
            Event(timestamp = 1718546886371, action = 2, x = 213.87732f, y = 180.93262f),
            Event(timestamp = 1718546886387, action = 2, x = 259.1513f, y = 180.93262f),
            Event(timestamp = 1718546886404, action = 2, x = 298.65097f, y = 180.93262f),
            Event(timestamp = 1718546886413, action = 2, x = 302.95312f, y = 180.93262f),
            Event(timestamp = 1718546886413, action = 1, x = 302.95312f, y = 180.93262f),
        )

        val scheduler: TestCoroutineScheduler = testScheduler
        val dispatcher1 = StandardTestDispatcher(scheduler, name = "IO dispatcher")

        val eventManger = EventManager(dispatcher1, appContext, object : CanvasChangedListener {
            override fun onCanvasChanged() {
                // No-op
            }

            override fun invalidate2() {
                // No-op
            }

        })
        eventManger.setMaxSize(8, 18, velocityFilterWeight = 0.9f)
        scheduler.advanceUntilIdle()
        advanceUntilIdle()
        initialEvents.forEach {
            eventManger.addEvent(
                it
            )
        }
        eventManger.points.forEach {
            println(it)
        }
        eventManger.drawThis.forEach {
            println(it)
        }
        assertEquals("se.warting.signaturepad", appContext.packageName)
    }
}