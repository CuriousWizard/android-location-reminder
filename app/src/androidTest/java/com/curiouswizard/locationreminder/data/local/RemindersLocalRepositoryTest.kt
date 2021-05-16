package com.curiouswizard.locationreminder.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.curiouswizard.locationreminder.locationreminders.data.dto.ReminderDTO
import com.curiouswizard.locationreminder.locationreminders.data.dto.Result
import com.curiouswizard.locationreminder.locationreminders.data.local.RemindersDatabase
import com.curiouswizard.locationreminder.locationreminders.data.local.RemindersLocalRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    private lateinit var db: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    private val reminder = ReminderDTO(
        "Test",
        "testing",
        "test",
        22.1354982,
        12.325486
    )

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setup(){
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        repository = RemindersLocalRepository(db.reminderDao(), Dispatchers.Main)
    }

    @After
    fun closeDb() = db.close()

    @Test
    fun saveReminder_1Reminder_successfullySaved() = runBlocking {
        repository.saveReminder(reminder)

        val resultReminderDTO = (repository.getReminder(reminder.id) as Result.Success).data

        assertThat(resultReminderDTO.id, `is`(reminder.id))
        assertThat(resultReminderDTO.title, `is`(reminder.title))
        assertThat(resultReminderDTO.description, `is`(reminder.description))
        assertThat(resultReminderDTO.location, `is`(reminder.location))
        assertThat(resultReminderDTO.latitude, `is`(reminder.latitude))
        assertThat(resultReminderDTO.longitude, `is`(reminder.longitude))
    }

    @Test
    fun deleteAllReminders_successfullyDeleted() = runBlocking {
        repository.saveReminder(reminder)

        repository.deleteAllReminders()
        val reminderList = (repository.getReminders() as Result.Success).data

        assertThat(reminderList.isEmpty(), `is`(true))
    }

    @Test
    fun getReminder_queryWrongId_returnsError() = runBlocking {
        repository.saveReminder(reminder)

        val result = repository.getReminder("123") as Result.Error

        assertThat(result.message, `is`("Reminder not found!"))
    }
}