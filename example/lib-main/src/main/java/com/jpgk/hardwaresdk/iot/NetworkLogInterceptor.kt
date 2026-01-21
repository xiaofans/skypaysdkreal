package com.jpgk.hardwaresdk.iot

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.RequestBody
import okio.Buffer
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

internal class NetworkLogInterceptor : Interceptor {

    private val TAG = "Net"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        val startNs = System.nanoTime()
        logRequest(request)

        val response = try {
            chain.proceed(request)
        } catch (e: Exception) {
            IotLogger.e(TAG, "HTTP FAILED: ${e.message}", e)
            throw e
        }

        val tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs)
        logResponse(response, tookMs)

        return response
    }

    private fun logRequest(request: okhttp3.Request) {
        val sb = StringBuilder()
        sb.append("➡️ ${request.method} ${request.url}\n")

        request.headers.forEach {
            sb.append("   ${it.first}: ${it.second}\n")
        }

        request.body?.let {
            sb.append("   body: ${bodyToString(it)}\n")
        }

        IotLogger.d(TAG, sb.toString())
    }

    private fun logResponse(response: Response, tookMs: Long) {
        val sb = StringBuilder()
        sb.append("⬅️ ${response.code} ${response.request.url} (${tookMs}ms)\n")

        response.headers.forEach {
            sb.append("   ${it.first}: ${it.second}\n")
        }

        val body = response.body
        val source = body?.source()
        source?.request(Long.MAX_VALUE)
        val buffer = source?.buffer
        val charset = body?.contentType()?.charset(Charset.forName("UTF-8")) ?: Charset.forName("UTF-8")

        buffer?.clone()?.readString(charset)?.let {
            sb.append("   body: $it\n")
        }

        IotLogger.d(TAG, sb.toString())
    }

    private fun bodyToString(body: RequestBody): String {
        return try {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        } catch (e: Exception) {
            "unable to read body"
        }
    }
}
