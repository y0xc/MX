package moe.fuqiuluo.mamu.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import moe.fuqiuluo.mamu.data.SeLinuxMode
import moe.fuqiuluo.mamu.data.SeLinuxStatus
import moe.fuqiuluo.mamu.data.SystemInfo
import moe.fuqiuluo.mamu.utils.RootShellExecutor
import moe.fuqiuluo.mamu.utils.ShellResult

class SystemRepository {

    fun getSystemInfo(): SystemInfo {
        return SystemInfo()
    }

    suspend fun getSeLinuxStatus(): SeLinuxStatus = withContext(Dispatchers.IO) {
        val result = RootShellExecutor.exec("getenforce")

        when (result) {
            is ShellResult.Success -> {
                val modeString = result.output.trim()
                val mode = when (modeString.uppercase()) {
                    "ENFORCING" -> SeLinuxMode.ENFORCING
                    "PERMISSIVE" -> SeLinuxMode.PERMISSIVE
                    "DISABLED" -> SeLinuxMode.DISABLED
                    else -> SeLinuxMode.UNKNOWN
                }
                SeLinuxStatus(mode, modeString)
            }
            else -> SeLinuxStatus(SeLinuxMode.UNKNOWN, "Unknown")
        }
    }

    suspend fun hasRootAccess(): Boolean = withContext(Dispatchers.IO) {
        val result = RootShellExecutor.exec("echo test")
        result is ShellResult.Success
    }
}