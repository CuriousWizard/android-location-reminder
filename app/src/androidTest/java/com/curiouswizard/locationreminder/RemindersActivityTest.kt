package com.curiouswizard.locationreminder

import android.app.Application
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.curiouswizard.locationreminder.locationreminders.ReminderDescriptionActivity
import com.curiouswizard.locationreminder.locationreminders.RemindersActivity
import com.curiouswizard.locationreminder.locationreminders.data.ReminderDataSource
import com.curiouswizard.locationreminder.locationreminders.data.local.LocalDB
import com.curiouswizard.locationreminder.locationreminders.data.local.RemindersLocalRepository
import com.curiouswizard.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.curiouswizard.locationreminder.locationreminders.reminderslist.RemindersListViewModel
import com.curiouswizard.locationreminder.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        stopKoin()//stop the original app koin
        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource}
            single { LocalDB.createRemindersDao(appContext) }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    // End to End testing
    @Test
    fun launchReminderDescription() {
        val reminder = ReminderDataItem(
            "Title", "Description", "Location", 12.343434, 13.454545
        )
        val intent = ReminderDescriptionActivity.newIntent(getApplicationContext(), reminder)
        val activityScenario = ActivityScenario.launch<ReminderDescriptionActivity>(intent)

        onView(withId(R.id.title)).check(matches(withText(reminder.title)))
        onView(withId(R.id.description)).check(matches(withText(reminder.description)))
        onView(withId(R.id.location)).check(matches(withText(reminder.location)))

        activityScenario.close()
    }

    @Test
    fun creatingReminder() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        val viewModel: SaveReminderViewModel = get()

        viewModel.reminderSelectedLocationStr.postValue("Location")
        viewModel.latitude.postValue(1.2323)
        viewModel.longitude.postValue(1.2323)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.reminderTitle)).perform(typeText("Title"))
        onView(withId(R.id.reminderDescription)).perform(typeText("Description"), closeSoftKeyboard())
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withText("Title")).check(matches(isDisplayed()))
        onView(withText("Description")).check(matches(isDisplayed()))
        onView(withText("Location")).check(matches(isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun creatingReminder_noTitle() {
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        val viewModel: SaveReminderViewModel = get()

        viewModel.reminderSelectedLocationStr.postValue("Location")
        viewModel.latitude.postValue(1.2323)
        viewModel.longitude.postValue(1.2323)

        onView(withId(R.id.addReminderFAB)).perform(click())
        onView(withId(R.id.saveReminder)).perform(click())

        onView(withId(com.google.android.material.R.id.snackbar_text))
            .check(matches(withText(R.string.err_enter_title)))

        activityScenario.close()
    }

}
