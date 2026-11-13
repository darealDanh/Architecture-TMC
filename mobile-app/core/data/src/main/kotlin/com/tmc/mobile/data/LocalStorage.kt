package com.tmc.mobile.data

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.tmc.mobile.data.attendance.AttendanceDatabase
import com.tmc.mobile.data.attendance.RoomAttendanceCache
import com.tmc.mobile.data.session.DataStoreCertificateStore
import com.tmc.mobile.data.session.DataStoreSessionStore
import com.tmc.mobile.domain.port.AttendanceCache
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.SessionStore

class LocalStorage(context: Context) {

    private val preferences = PreferenceDataStoreFactory.create {
        context.preferencesDataStoreFile(PREFERENCES_FILE)
    }

    private val database = Room.databaseBuilder(context, AttendanceDatabase::class.java, DATABASE_FILE).build()

    val sessionStore: SessionStore = DataStoreSessionStore(preferences)

    val certificateStore: CertificateStore = DataStoreCertificateStore(preferences)

    val attendanceCache: AttendanceCache = RoomAttendanceCache(database.attendanceDao())

    private companion object {
        const val PREFERENCES_FILE = "tmc_device"
        const val DATABASE_FILE = "attendance.db"
    }
}
