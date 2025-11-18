package moe.fuqiuluo.mamu.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import moe.fuqiuluo.mamu.data.DriverInfo
import moe.fuqiuluo.mamu.data.SeLinuxStatus
import moe.fuqiuluo.mamu.data.SystemInfo
import moe.fuqiuluo.mamu.repository.DriverRepository
import moe.fuqiuluo.mamu.repository.SystemRepository

data class MainUiState(
    val isLoading: Boolean = true,
    val systemInfo: SystemInfo = SystemInfo(),
    val driverInfo: DriverInfo? = null,
    val seLinuxStatus: SeLinuxStatus? = null,
    val hasRootAccess: Boolean = false,
    val error: String? = null
)

class MainViewModel(
    private val systemRepository: SystemRepository = SystemRepository(),
    private val driverRepository: DriverRepository = DriverRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            try {
                val systemInfo = systemRepository.getSystemInfo()
                val hasRoot = systemRepository.hasRootAccess()
                val seLinuxStatus = systemRepository.getSeLinuxStatus()
                val driverInfo = driverRepository.getDriverInfo()

                _uiState.value = MainUiState(
                    isLoading = false,
                    systemInfo = systemInfo,
                    driverInfo = driverInfo,
                    seLinuxStatus = seLinuxStatus,
                    hasRootAccess = hasRoot
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
}