package com.jpgk.hardwaresdk.socket

import com.alibaba.fastjson.JSON
import com.jpgk.hardwaresdk.HardwareSDK
import com.jpgk.hardwaresdk.iot.IotLogger
import com.jpgk.iot.model.up.HealthUpModel
import kotlinx.coroutines.delay
import java.util.Date


internal class HeartBeatManager(private val writer: SocketWriter) {


    private val interval = 20_000L
    private var running = true
    var msgId = 0

    suspend fun start() {
        while (running) {
            try {
                var heartUpModel = HealthUpModel()
                heartUpModel.imei = SdkSocket.machineCode
                heartUpModel.machineCode = SdkSocket.machineCode
                heartUpModel.timestamp = Date()
                heartUpModel.maxId = msgId
                heartUpModel.version = HardwareSDK.appVersion
                msgId++
                val heartJson = JSON.toJSON(heartUpModel).toString()
                IotLogger.w("SocketSDK","Heart JSON:${heartJson}")
                writer.send(heartJson)
            } catch (_: Exception) {
// ignore send error — reader/connection handles reconnection
            }
            delay(interval)
        }
    }


    fun stop() { running = false }
}