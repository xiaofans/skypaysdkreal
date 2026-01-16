package com.jpgk.vendingmachine.lib_base.entity


data class DeviceEntity(
    val machineCode: String,
    val machineName: String,
    val machineSign: String,
    val token: String,
    val httpDomain: String,
    val tcpHost: String,
    val tcpPort: String,
    val ossEndPoint: String,
    val msgMaxId: Int,
    //心跳间隔时间
    val healthDuration: Int,
    //出货超时时间
    val openDoorTimeout: Int,
    val ossType: String,
    val ossRootPath: String,
    val ossDomain: String,
    val extend: Extend
)

data class Extend(
    val merchantCode: String,
    val merchantSecret: String,
)


