package com.jpgk.hardwaresdk.iot

import com.jpgk.hardwaresdk.entity.AuthAddress
import com.jpgk.iot.util.SHA256Util
import com.jpgk.vendingmachine.lib_base.entity.DeviceEntity
import com.jpgk.vendingmachine.lib_base.entity.MachineEntity

internal class AuthManager(
    private val apiService: ApiService,
    private val domainManager: DomainManager,
    private val retryPolicy: RetryPolicy
) {

    private val TAG = "AuthManager"

    suspend fun start(machineCode: String): AuthResult {
        return retryPolicy.run {
            IotLogger.d(TAG, "Step1: getAuthAddress")
            val address = getAuthAddress(machineCode)

            IotLogger.d(TAG, "Step2: apply domain, base=${address.domain}, auth=${address.authUrl}")
            domainManager.apply(address)

            IotLogger.d(TAG, "Step3: auth")
            val device = auth(machineCode)

            //IotLogger.d(TAG, "Step4: getMachineDetail")
            //val machine = getMachineDetail(machineCode)

            AuthResult(device, MachineEntity())
        }
    }

    private suspend fun getAuthAddress(machineCode: String): AuthAddress {
        val resp = apiService.getAuthAddress(
            url = ApiService.ADDRESS_URL,
            machineCode = machineCode
        )

        IotLogger.d(TAG, "AuthAddress response: $resp")

        if (resp.ok != true || resp.result == null) {
            throw SdkException.AuthAddressError
        }
        return resp.result
    }

    private suspend fun auth(machineCode: String): DeviceEntity {
        val timestamp = System.currentTimeMillis()
        val sign = SHA256Util.digest("${machineCode}${timestamp}${ApiService.AUTH_KEY}")

        val resp = apiService.auth(
            url = ApiService.AUTH_URL,
            machineCode = machineCode,
            sign = sign,
            timestamp = timestamp
        )

        IotLogger.d(TAG, "Auth response: $resp")

        if (resp.ok != true || resp.result == null) {
            throw SdkException.AuthFailed
        }



        return resp.result
    }

    private suspend fun getMachineDetail(machineCode: String): MachineEntity {
        val resp = apiService.getMachineDetail(machineCode)
        IotLogger.d(TAG, "MachineDetail response: $resp")

        if (resp.ok != true || resp.result == null) {
            throw SdkException.MachineDetailError
        }
        return resp.result
    }
}
