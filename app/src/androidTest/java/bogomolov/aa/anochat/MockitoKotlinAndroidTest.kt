package bogomolov.aa.anochat

import org.mockito.Mockito

class MockitoKotlinAndroidTest {
    companion object {
        /**
         * Matcher that returns null
         */
        inline fun <reified T> any(): T = Mockito.any<T>()

        /**
         * Matcher never returns null
         */
        inline fun <reified T> any(type: Class<T>): T = Mockito.any(type)
    }
}