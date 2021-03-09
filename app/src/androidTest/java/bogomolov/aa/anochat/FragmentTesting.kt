package bogomolov.aa.anochat

import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import androidx.lifecycle.ViewModelStore
import androidx.navigation.NavHostController
import androidx.navigation.Navigation
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.core.internal.deps.dagger.internal.Preconditions
import bogomolov.aa.anochat.features.settings.SettingsFragment
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

inline fun <reified T> launchFragmentInHiltContainer(
    fragmentArgs: Bundle? = null,
    navHostController: NavHostController? = null,
    crossinline action: T.() -> Unit = {}
): ActivityScenario<HiltTestActivity> {

    val startActivityIntent = Intent.makeMainActivity(
        ComponentName(
            ApplicationProvider.getApplicationContext(),
            HiltTestActivity::class.java
        )
    )

    return ActivityScenario.launch<HiltTestActivity>(startActivityIntent).onActivity { activity ->

        val fragment: Fragment = activity.supportFragmentManager.fragmentFactory.instantiate(
            Preconditions.checkNotNull(T::class.java.classLoader)!!,
            T::class.java.name
        )

        fragment.arguments = fragmentArgs

        navHostController?.let {
            fragment.viewLifecycleOwnerLiveData.observeForever { viewLifecycleOwner ->
                if (viewLifecycleOwner != null) {
                    Navigation.setViewNavController(fragment.requireView(), it)
                }
            }
        }

        (fragment as T).action()

        activity.supportFragmentManager
            .beginTransaction()
            .add(android.R.id.content, fragment, "")
            .commitNow()

    }
}

inline fun <reified T> navigateTo(destination: Int): T {
    val testNavHostController =
        TestNavHostController(ApplicationProvider.getApplicationContext())
    testNavHostController.setViewModelStore(ViewModelStore())

    return runBlocking {
        return@runBlocking suspendCoroutine<T> {
            launchFragmentInHiltContainer<T>(navHostController = testNavHostController) {
                testNavHostController.setGraph(R.navigation.nav_graph)
                testNavHostController.setCurrentDestination(destination)
                it.resume(this)
            }
        }
    }
}