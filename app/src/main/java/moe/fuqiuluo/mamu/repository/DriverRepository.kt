package moe.fuqiuluo.mamu.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.fuqiuluo.mamu.data.DriverInfo
import moe.fuqiuluo.mamu.data.DriverStatus
import moe.fuqiuluo.mamu.driver.WuwaDriver

class DriverRepository {

    suspend fun getDriverInfo(): DriverInfo = withContext(Dispatchers.IO) {
        try {
            val loaded = WuwaDriver.loaded
            val status = if (loaded) DriverStatus.LOADED else DriverStatus.NOT_LOADED

            if (loaded) {
                DriverInfo(
                    status = status,
                    isProcessBound = WuwaDriver.isProcessBound,
                    boundPid = WuwaDriver.currentBindPid
                )
            } else {
                DriverInfo(status = status)
            }
        } catch (e: Exception) {
            DriverInfo(
                status = DriverStatus.ERROR,
                errorMessage = e.message
            )
        }
    }
}