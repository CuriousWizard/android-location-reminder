package com.curiouswizard.locationreminder.reminderslist

import android.os.Bundle
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.swipeDown
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.curiouswizard.locationreminder.R
import com.curiouswizard.locationreminder.locationreminders.data.ReminderDataSource
import com.curiouswizard.locationreminder.locationreminders.data.dto.ReminderDTO
import com.curiouswizard.locationreminder.locationreminders.data.local.LocalDB
import com.curiouswizard.locationreminder.locationreminders.data.local.RemindersLocalRepository
import com.curiouswizard.locationreminder.locationreminders.reminderslist.ReminderListFragment
import com.curiouswizard.locationreminder.locationreminders.reminderslist.ReminderListFragmentDirections
import com.curiouswizard.locationreminder.locationreminders.reminderslist.RemindersListViewModel
import com.curiouswizard.locationreminder.util.DataBindingIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest : AutoCloseKoinTest() {

    private lateinit var repository: ReminderDataSource
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val item = ReminderDTO(
        "Test", "test", "Test place", 12.343434, 13.454545
    )

    @Before
    fun setup() {
        stopKoin()

        val app = module {
            viewModel {
                RemindersListViewModel(
                    ApplicationProvider.getApplicationContext(),
                    get() as ReminderDataSource
                )
            }

            single<ReminderDataSource> { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(ApplicationProvider.getApplicationContext()) }
        }

        startKoin {
            androidContext(ApplicationProvider.getApplicationContext())
            modules(app)
        }

        repository = get()

        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Test
    fun clickFAB_NavigateToSaveReminderFragment() {
        val scenario = launchFragmentInContainer<ReminderListFragment>(
            Bundle.EMPTY,
            R.style.Theme_LocationReminder
        )
        val navController = mock(NavController::class.java)

        scenario.onFragment {
            Navigation.setViewNavController(it.requireView(), navController)
        }

        onView(withId(R.id.addReminderFAB)).perform(click())

        verify(navController).navigate(
            ReminderListFragmentDirections.toSaveReminder()
        )
    }

    @Test
    fun reminderList_reminderIsDisplayed() {
        runBlocking {
            repository.saveReminder(item)
        }

        launchFragmentInContainer<ReminderListFragment>(
            Bundle.EMPTY,
            R.style.Theme_LocationReminder
        )

        onView(withText(item.title)).check(matches(isDisplayed()))
        onView(withText(item.description)).check(matches(isDisplayed()))
    }

    @Test
    fun checkNoData () {
        runBlocking {
            repository.saveReminder(item)
        }

        launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.Theme_LocationReminder)

        runBlocking {
            repository.deleteAllReminders()
        }

        onView(withId(R.id.refreshLayout)).perform(swipeDown())
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }

}