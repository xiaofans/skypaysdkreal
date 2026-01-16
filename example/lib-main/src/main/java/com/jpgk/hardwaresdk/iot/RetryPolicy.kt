package com.jpgk.hardwaresdk.iot

import kotlinx.coroutines.delay

internal class RetryPolicy(
    private val maxRetry: Int = 10,
    private val retryDelayMs: Long = 2000
) {

    suspend fun <T> run(block: suspend () -> T): T {
        var retry = 0
        while (true) {
            try {
                return block()
            } catch (e: SdkException) {
                retry++
                if (retry >= maxRetry) {
                    throw e
                }
                delay(retryDelayMs)
            }
        }
    }
}