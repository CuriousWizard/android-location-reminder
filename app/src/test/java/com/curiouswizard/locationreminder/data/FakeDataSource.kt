package com.curiouswizard.locationreminder.data

import com.curiouswizard.locationreminder.locationreminders.data.ReminderDataSource
import com.curiouswizard.locationreminder.locationreminders.data.dto.ReminderDTO
import com.curiouswizard.locationreminder.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource(private val reminders: MutableList<ReminderDTO> = mutableListOf()) : ReminderDataSource {

    var returnError = false

    override suspend fun getReminders(): Result<List<ReminderDTO>> =
        if (returnError) {
            Result.Error("Error in getReminders()")
        } else {
            Result.Success(reminders)
        }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        reminders.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> =
        if (returnError) {
            Result.Error("Error in getReminder()")
        } else {
            val reminder = reminders.find { it.id == id}

            if (reminder != null){
                Result.Success(reminder)
            } else {
                Result.Error("Did not found reminder")
            }
        }

    override suspend fun deleteAllReminders() {
        reminders.clear()
    }
}