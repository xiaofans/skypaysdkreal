package com.jpgk.hardwaresdk.socket

import kotlinx.coroutines.delay
import java.io.InputStream
import java.net.Socket


internal class SocketReader(
    private val socket: Socket,
    private val onMessage: (String) -> Unit
) {


    @Volatile
    private var running = true


    suspend fun start() {
        val input: InputStream = socket.getInputStream()
        val buffer = ByteArray(2048)
        val sb = StringBuilder()


        try {
            while (running) {
                val read = input.read(buffer)
                if (read == -1) break
                if (read > 0) {
                    sb.append(String(buffer, 0, read))


                    val frames = JsonFrameDecoder.decode(sb)
                    frames?.forEach { frame ->
// deliver message
                        onMessage(frame)
                    }
                } else {
// avoid busy loop
                    delay(10)
                }
            }
        } catch (t: Throwable) {
// optionally report error
        } finally {
            try {
                input.close()
            } catch (_: Exception) {
            }
        }
    }


    fun stop() {
        running = false
        try {
            socket.shutdownInput()
        } catch (_: Exception) {
        }
    }
}