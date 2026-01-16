package com.jpgk.hardwaresdk.iot

import android.content.Context
import com.jpgk.hardwaresdk.socket.SdkSocket
import com.jpgk.hardwaresdk.socket.SocketListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object IotSdk {

    private lateinit var appContext: Context
    private lateinit var authManager: AuthManager

    private val sdkScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun init(
        context: Context,
        machineCode: String,
        listener: IotInitListener
    ) {
        appContext = context.applicationContext

        // 初始化 SDK 内部依赖
        val apiService = RetrofitFactory.create(ApiService::class.java)
        val domainManager = DomainManager()
        val retryPolicy = RetryPolicy()

        authManager = AuthManager(apiService, domainManager, retryPolicy)

        sdkScope.launch {
            try {
                val result: AuthResult = authManager.start(machineCode)

                withContext(Dispatchers.Main) {
                    SdkSocket.init(appContext,machineCode,result.device.tcpHost,result.device.tcpPort.toInt(),result.device.token)
                    listener.onSuccess(result.device, result.machine)
                }
            } catch (e: SdkException) {
                withContext(Dispatchers.Main) {
                    listener.onAuthFail(e)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    listener.onFatal(e)
                }
            }
        }
    }
}
