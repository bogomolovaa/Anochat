package bogomolov.aa.anochat

import org.mockito.ArgumentCaptor

class MockitoKotlinTest {

    companion object {
        fun <T> capture(argumentCaptor: ArgumentCaptor<T>): T = argumentCaptor.capture()
    }
}