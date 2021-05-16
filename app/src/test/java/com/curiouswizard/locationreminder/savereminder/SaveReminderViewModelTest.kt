package com.curiouswizard.locationreminder.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.curiouswizard.locationreminder.MainCoroutineRule
import com.curiouswizard.locationreminder.data.FakeDataSource
import com.curiouswizard.locationreminder.getOrAwaitValue
import com.curiouswizard.locationreminder.locationreminders.reminderslist.ReminderDataItem
import com.curiouswizard.locationreminder.locationreminders.savereminder.SaveReminderViewModel

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.`is`
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
class SaveReminderViewModelTest {

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var saveReminderviewModel: SaveReminderViewModel

    @get:Rule
    val instantTaskExecRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        fakeDataSource = FakeDataSource()
        saveReminderviewModel = SaveReminderViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )
    }

    @After
    fun clear() {
        stopKoin()
    }

    @Test
    fun showLoading_savingReminders_showLoadingTrueThenFalse() {
        val reminderDataItem = ReminderDataItem(
            "Test",
            "test",
            "test",
            25.262823,
            12.123456
        )

        mainCoroutineRule.pauseDispatcher()
        saveReminderviewModel.saveReminder(reminderDataItem)
        assertThat(saveReminderviewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(saveReminderviewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun validateEnteredData_nullOrEmptyTitle_returnFalse() {
        // Given
        val dataItemNullTitle = ReminderDataItem(
            null,
            "test",
            "test",
            25.262823,
            12.123456
        )
        val dataItemEmptyTitle = ReminderDataItem(
            "",
            "test",
            "test",
            25.262823,
            12.123456
        )

        // When
        val valueNull = saveReminderviewModel.validateEnteredData(dataItemNullTitle)
        val valueEmpty = saveReminderviewModel.validateEnteredData(dataItemEmptyTitle)

        // Then
        assertThat(valueNull, `is`(false))
        assertThat(valueEmpty, `is`(false))
    }

    @Test
    fun validateEnteredData_nullOrEmptyLocation_returnFalse() {
        // Given
        val dataItemNullLocation = ReminderDataItem(
            "test",
            "test",
            null,
            25.262823,
            12.123456
        )
        val dataItemEmptyLocation = ReminderDataItem(
            "test",
            "test",
            "",
            25.262823,
            12.123456
        )

        // When
        val valueNull = saveReminderviewModel.validateEnteredData(dataItemNullLocation)
        val valueEmpty = saveReminderviewModel.validateEnteredData(dataItemEmptyLocation)

        // Then
        assertThat(valueNull, `is`(false))
        assertThat(valueEmpty, `is`(false))
    }

    @Test
    fun showToast_savingReminder_notEmptyAfterSave() = runBlockingTest {
        // Given
        val dataItem = ReminderDataItem(
            "Test",
            "test",
            "test",
            25.262823,
            12.123456
        )
        // When
        saveReminderviewModel.saveReminder(dataItem)

        // Then
        assertThat(saveReminderviewModel.showToast.getOrAwaitValue().isNotEmpty(), `is`(true))
    }
}