package com.jpgk.hardwaresdk.upgrade

import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

object ApkDownloader {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun downloadApk(
        url: String,
        downloadDir: File,
        maxRetry: Int = 3,
        retryDelayMillis: Long = 2000,
        onProgress: (progress: Int) -> Unit,
        onSuccess: (filePath: String) -> Unit,
        onError: (Exception) -> Unit
    ) {

        Thread {
            var attempt = 0
            var lastException: Exception? = null

            while (attempt < maxRetry) {
                try {
                    attempt++

                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs()
                    }

                    val fileName = extractFileName(url)
                    val targetFile = File(downloadDir, fileName)

                    val request = Request.Builder()
                        .url(url)
                        .build()

                    client.newCall(request).execute().use { response ->

                        if (!response.isSuccessful) {
                            throw Exception("Download failed: ${response.code}")
                        }

                        val body = response.body
                            ?: throw Exception("Empty response body")

                        val total = body.contentLength()

                        body.byteStream().use { inputStream ->
                            FileOutputStream(targetFile).use { outputStream ->

                                val buffer = ByteArray(8 * 1024)
                                var bytesRead: Int
                                var downloaded = 0L

                                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                                    outputStream.write(buffer, 0, bytesRead)
                                    downloaded += bytesRead

                                    if (total > 0) {
                                        val progress =
                                            (downloaded * 100 / total).toInt()
                                        onProgress(progress)
                                    }
                                }

                                outputStream.flush()
                            }
                        }
                    }

                    onSuccess(targetFile.absolutePath)
                    return@Thread

                } catch (e: Exception) {
                    lastException = e

                    if (attempt < maxRetry) {
                        Thread.sleep(retryDelayMillis)
                    }
                }
            }

            onError(lastException ?: Exception("Unknown error"))
        }.start()
    }

    private fun extractFileName(url: String): String {
        val cleanUrl = url.substringBefore("?")
        val fileName = cleanUrl.substringAfterLast("/")
        return URLDecoder.decode(fileName, "UTF-8")
            .ifBlank { "download_${System.currentTimeMillis()}.apk" }
    }
}