package com.jpgk.hardwaresdk.iot

import com.jpgk.hardwaresdk.entity.AuthAddress


internal class DomainManager {

    fun apply(address: AuthAddress) {
        val baseUrl = address.domain ?: "http://gateway.lz517.cn/"
        val authUrl = address.authUrl ?: throw SdkException.AuthUrlEmpty

        ApiService.AUTH_URL = authUrl
    }
}