package com.jpgk.hardwaresdk.socket


import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.jpgk.hardwaresdk.HardwareSDK
import com.jpgk.hardwaresdk.hardwarelogger.LogUploadService
import com.jpgk.hardwaresdk.iot.IOTListener
import com.jpgk.hardwaresdk.iot.IotLogger
import com.jpgk.iot.enums.DownCommandEnum
import com.jpgk.iot.model.up.AuthenticationUpModel
import com.jpgk.iot.model.up.ReceiveAckUpModel
import kotlinx.coroutines.*
import kotlinx.coroutines.Dispatchers.Main
import org.json.JSONObject
import java.util.Date

object SdkSocket {

    private  var HOST = "192.168.2.28"
    private  var PORT = 20000
    private  var machineCode = "jp003"
    private  var token = ""

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mainScope = CoroutineScope(SupervisorJob() + Main)

    private val listeners = mutableSetOf<IOTListener>()

    private var connection: SocketConnection? = null
    private var initialized = false


    fun init(context: Context? = null,machineCode:String,host:String,port:Int,token: String?) {
        if (initialized) return
        initialized = true
        HOST = host
        PORT = port
        this.machineCode = machineCode
        connection = SocketConnection(HOST, PORT, onMessage ={ msg ->
            IotLogger.w("SocketMsg",msg)
            // 切回 UI 线程分发给监听器
            val jsonObject = JSONObject(msg)
            var type = jsonObject.optString("command")
            if (type == "UPLOAD_LOG"){
                val jsoObj1 = jsonObject.optString("ossCredentials")
                val serialNo = jsonObject.optString("serialNo")
                if (jsoObj1 != null){
                    val jsobObj2 = JSONObject(jsoObj1)
                    val expiration = jsobObj2.optLong("expiration")
                    val securityToken = jsobObj2.optString("securityToken")
                    val startDate = jsonObject.optLong("beginDate")
                    val endDate = jsonObject.optLong("endDate")
                    LogUploadService.startUpload(HardwareSDK.application,startDate,endDate,expiration,securityToken,serialNo)
                }
            }
            if (!jsonObject.isNull("ack") && jsonObject.getBoolean("ack")) {
                sendAckToServer(type, jsonObject.optString("serialNo"))
            }
            mainScope.launch {
                listeners.forEach { it.onMessage(msg) }
            }

        },
            onConnected = {
                // ✅ Socket 连接完成后自动认证
                sendTokenMsg(token)
            })

        ioScope.launch {
            connection?.start()
        }
    }
    /**
     * 收到服务返回的消息，回传给服务
     *
     * */
    fun sendAckToServer(commandType: String, seriNo: String) {
        var receiveAckUpModel = ReceiveAckUpModel()
        receiveAckUpModel.imei =machineCode
        receiveAckUpModel.machineCode =machineCode
        receiveAckUpModel.timestamp = Date()
        receiveAckUpModel.serialNo = seriNo
        receiveAckUpModel.downCommand = DownCommandEnum.valueOf(commandType)

        var data1 = Gson().toJson(receiveAckUpModel).toString()
        IotLogger.w("SdkSocket", "sendAckToServer${data1}")
        send(data1)
    }

    private fun sendTokenMsg(token: String?) {
        val authentication = AuthenticationUpModel().apply {
            imei = machineCode
            machineCode = this@SdkSocket.machineCode
            timestamp = Date()
            this.token = token
        }

        // ⚠️ 注意：这里直接调用 connection.send
        ioScope.launch {
            connection?.send(Gson().toJson(authentication).toString())
        }
    }


    /** UI 线程可安全调用 */
    fun send(msg: String) {
        if (!initialized) init(null, machineCode,HOST, PORT, token)

        ioScope.launch {
            connection?.send(msg)
        }
    }


    fun addListener(listener: IOTListener) {
        listeners.add(listener)
    }

    fun removeListener(listener: IOTListener) {
        listeners.remove(listener)
    }


    fun close() {
        ioScope.launch {
            connection?.close()
        }
        initialized = false
    }
}
