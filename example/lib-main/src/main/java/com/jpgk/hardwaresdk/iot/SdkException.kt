package com.jpgk.hardwaresdk.iot

sealed class SdkException(message: String) : RuntimeException(message) {
    object AuthAddressError : SdkException("获取认证地址失败")
    object AuthUrlEmpty : SdkException("认证URL为空")
    object AuthFailed : SdkException("设备认证失败")
    object MachineDetailError : SdkException("获取设备详情失败")
}
