package com.tmc.mobile.ui.attendance

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmc.mobile.domain.attendance.GetMonthlyAttendance
import com.tmc.mobile.domain.attendance.MonthlyAttendance
import com.tmc.mobile.domain.attendance.NoAttendanceDataException
import com.tmc.mobile.domain.port.SessionExpiredException
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AttendanceContent {

    data object Loading : AttendanceContent

    data class Loaded(val attendance: MonthlyAttendance) : AttendanceContent

    data object NoData : AttendanceContent

    data class Error(val message: String) : AttendanceContent
}

data class AttendanceUiState(
    val months: List<YearMonth>,
    val selected: YearMonth,
    val content: AttendanceContent = AttendanceContent.Loading,
)

@HiltViewModel
class AttendanceViewModel @Inject constructor(
    private val getMonthlyAttendance: GetMonthlyAttendance,
    clock: Clock,
) : ViewModel() {

    private val months = YearMonth.now(clock).let { current -> List(MONTHS_SHOWN) { current.minusMonths(it.toLong()) } }
    private val mutableState = MutableStateFlow(AttendanceUiState(months, months.first()))
    private var loading: Job? = null

    val state: StateFlow<AttendanceUiState> = mutableState.asStateFlow()

    init {
        load(months.first())
    }

    fun select(month: YearMonth) {
        if (month in months) {
            load(month)
        }
    }

    private fun load(month: YearMonth) {
        loading?.cancel()
        mutableState.update { it.copy(selected = month, content = AttendanceContent.Loading) }
        loading = viewModelScope.launch {
            val content = try {
                AttendanceContent.Loaded(getMonthlyAttendance(month))
            } catch (e: NoAttendanceDataException) {
                AttendanceContent.NoData
            } catch (e: SessionExpiredException) {
                AttendanceContent.Error(SESSION_EXPIRED)
            }
            mutableState.update { it.copy(content = content) }
        }
    }

    private companion object {
        const val MONTHS_SHOWN = 3
        const val SESSION_EXPIRED = "Your session has expired. Sign in again."
    }
}
