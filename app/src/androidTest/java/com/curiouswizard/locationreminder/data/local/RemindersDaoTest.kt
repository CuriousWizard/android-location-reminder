package com.curiouswizard.locationreminder.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.curiouswizard.locationreminder.locationreminders.data.dto.ReminderDTO
import com.curiouswizard.locationreminder.locationreminders.data.local.RemindersDao
import com.curiouswizard.locationreminder.locationreminders.data.local.RemindersDatabase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    private lateinit var database: RemindersDatabase
    private lateinit var dao: RemindersDao

    private val reminder = ReminderDTO(
        "Test",
        "testing",
        "test",
        22.1354982,
        12.325486
    )

    // Executes each task synchronously using Architecture Components.
    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun initDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
        dao = database.reminderDao()
    }

    @After
    fun closeDb() = database.close()

    @Test
    fun saveReminder_1Reminder_successfullySaved() = runBlockingTest {
        // Given that reminder is saved to the database
        dao.saveReminder(reminder)

        // When query its data
        val data = dao.getReminderById(reminder.id)

        // Then
        assertThat(data as ReminderDTO, notNullValue())
        assertThat(data.title, `is`(reminder.title))
        assertThat(data.description, `is`(reminder.description))
        assertThat(data.location, `is`(reminder.location))
        assertThat(data.latitude, `is`(reminder.latitude))
        assertThat(data.longitude, `is`(reminder.longitude))
    }

    @Test
    fun getReminders_1Reminder_returnsList() = runBlockingTest {
        // Given
        dao.saveReminder(reminder)

        // When
        val reminderList = dao.getReminders()

        // Then
        assertThat(reminderList.size,`is`(1))
        assertThat(reminderList.contains(reminder),`is`(true))
    }

    @Test
    fun deleteAllReminders_clearDatabase_deletedSuccessfully() = runBlockingTest {
        // Given that reminder is saved to the database
        dao.saveReminder(reminder)

        // When
        dao.deleteAllReminders()

        // Then
        assertThat(dao.getReminders().isEmpty(), `is`(true))
    }
}