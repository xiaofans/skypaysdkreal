package com.jpgk.hardwaresdk.iot

import com.jpgk.hardwaresdk.entity.AuthAddress
import com.jpgk.vendingmachine.lib_base.entity.DeviceEntity
import com.jpgk.vendingmachine.lib_base.entity.MachineEntity
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Url

interface ApiService {

    companion object {
        const val ADDRESS_URL = "https://iot.jpgk.cn/iotweb/api/terminal/address"
        const val AUTH_KEY = "ThisIsASecretKey"
        var AUTH_URL = "https://iot.jpgk.cn/"
    }

    @GET
    suspend fun getAuthAddress(
        @Url url: String?,
        @Query("machineCode") machineCode: String?
    ): ApiResponse<AuthAddress>

    @POST
    suspend fun auth(
        @Url url: String?,
        @Query("machineCode") machineCode: String?,
        @Query("sign") sign: String?,
        @Query("timestamp") timestamp: Long?
    ): ApiResponse<DeviceEntity>

    @GET("/machine/machine/machineDetail")
    suspend fun getMachineDetail(
        @Query("machineCode") machineCode: String?
    ): ApiResponse<MachineEntity>
}
