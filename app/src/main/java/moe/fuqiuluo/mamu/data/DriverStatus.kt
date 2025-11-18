package moe.fuqiuluo.mamu.data

enum class DriverStatus {
    LOADED,
    NOT_LOADED,
    ERROR
}

data class DriverInfo(
    val status: DriverStatus,
    val isProcessBound: Boolean = false,
    val boundPid: Int = -1,
    val errorMessage: String? = null
)