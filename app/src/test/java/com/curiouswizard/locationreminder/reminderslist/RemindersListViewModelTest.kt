package com.curiouswizard.locationreminder.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.curiouswizard.locationreminder.MainCoroutineRule
import com.curiouswizard.locationreminder.data.FakeDataSource
import com.curiouswizard.locationreminder.getOrAwaitValue
import com.curiouswizard.locationreminder.locationreminders.data.dto.ReminderDTO
import com.curiouswizard.locationreminder.locationreminders.reminderslist.RemindersListViewModel
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

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
class RemindersListViewModelTest {

    private lateinit var fakeDataSource: FakeDataSource
    private lateinit var remindersListViewModel: RemindersListViewModel
    private lateinit var dataItem: ReminderDTO

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setup() {
        fakeDataSource = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(
            ApplicationProvider.getApplicationContext(),
            fakeDataSource
        )

        dataItem = ReminderDTO(
            "It's pizza time!",
            "Let's get some food",
            "Best Pizza Here",
            25.262823,
            12.123456
        )
    }

    @After
    fun clear() {
        stopKoin()
    }

    @Test
    fun loadReminders_loadingReminders_showLoadingTrueThenFalse() {
        mainCoroutineRule.pauseDispatcher()

        remindersListViewModel.loadReminders()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(true))

        mainCoroutineRule.resumeDispatcher()
        assertThat(remindersListViewModel.showLoading.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_1validReminder_showNoDataIsFalse() = runBlockingTest {
        // Given
        fakeDataSource.saveReminder(dataItem)
        // When
        remindersListViewModel.loadReminders()
        // Then
        assertThat(remindersListViewModel.showNoData.getOrAwaitValue(), `is`(false))
    }

    @Test
    fun loadReminders_noReminders_remindersListIsEmpty() = runBlockingTest {
        // When
        remindersListViewModel.loadReminders()
        // Then
        assertThat(remindersListViewModel.remindersList.getOrAwaitValue().isEmpty(),`is`(true))
    }

    @Test
    fun loadReminder_remindersUnavailableOrEmpty_showsError() = runBlockingTest {
        // Given
        fakeDataSource.returnError = true
        // When
        remindersListViewModel.loadReminders()
        // Then
        assertThat(remindersListViewModel.showSnackBar.getOrAwaitValue(), `is`("Error in getReminders()"))
    }
}