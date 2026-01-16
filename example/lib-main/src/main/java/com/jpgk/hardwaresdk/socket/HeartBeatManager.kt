package com.jpgk.hardwaresdk.socket

import com.google.gson.Gson
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
                heartUpModel.imei = "jp003"
                heartUpModel.machineCode = "jp003"
                heartUpModel.timestamp = Date()
                heartUpModel.maxId = msgId
                heartUpModel.version = "1.7.7"
                msgId++
                val heartJson = Gson().toJson(heartUpModel)
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