package com.tmc.mobile.data.attendance

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Transaction

@Entity(tableName = "attendance_records", primaryKeys = ["employeeId", "date"], indices = [Index("employeeId", "month")])
data class AttendanceRecordEntity(
    val employeeId: String,
    val month: String,
    val date: String,
    val gateId: String?,
    val entry: String?,
    val exit: String?,
    val source: String,
)

@Entity(tableName = "cached_months", primaryKeys = ["employeeId", "month"])
data class CachedMonthEntity(
    val employeeId: String,
    val month: String,
)

@Dao
interface AttendanceDao {

    @Query("SELECT * FROM attendance_records WHERE employeeId = :employeeId AND month = :month")
    suspend fun records(employeeId: String, month: String): List<AttendanceRecordEntity>

    @Query("SELECT COUNT(*) FROM cached_months WHERE employeeId = :employeeId AND month = :month")
    suspend fun isCached(employeeId: String, month: String): Int

    @Query("DELETE FROM attendance_records WHERE employeeId = :employeeId AND month = :month")
    suspend fun deleteMonth(employeeId: String, month: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecords(records: List<AttendanceRecordEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markCached(month: CachedMonthEntity)

    @Transaction
    suspend fun replaceMonth(employeeId: String, month: String, records: List<AttendanceRecordEntity>) {
        deleteMonth(employeeId, month)
        insertRecords(records)
        markCached(CachedMonthEntity(employeeId, month))
    }

    @Query("DELETE FROM attendance_records")
    suspend fun deleteAllRecords()

    @Query("DELETE FROM cached_months")
    suspend fun deleteAllMonths()

    @Transaction
    suspend fun deleteAll() {
        deleteAllRecords()
        deleteAllMonths()
    }
}

@Database(entities = [AttendanceRecordEntity::class, CachedMonthEntity::class], version = 1, exportSchema = false)
abstract class AttendanceDatabase : RoomDatabase() {

    abstract fun attendanceDao(): AttendanceDao
}
