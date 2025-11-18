@file:Suppress("KotlinJniMissingFunction")
package moe.fuqiuluo.mamu

import android.app.Application
import android.util.Log
import com.tencent.mmkv.MMKV
import kotlin.system.exitProcess

private const val TAG = "MamuApplication"

class MamuApplication : Application() {
    companion object {
        lateinit var instance: MamuApplication
            private set

        init {
            System.loadLibrary("mamu_core")
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // 初始化 MMKV
        MMKV.initialize(this)

        if (!initMamuCore()) {
            Log.e(TAG, "Failed to initialize Mamu Core")
            exitProcess(1)
        }

        Log.d(TAG, "MamuApplication initialized")
    }

    override fun onTerminate() {
        super.onTerminate()
        Log.d(TAG, "MamuApplication terminated")
    }

    /**
     * 初始化 Mamu Core 库
     * @return 初始化是否成功
     */
    private external fun initMamuCore(): Boolean
}