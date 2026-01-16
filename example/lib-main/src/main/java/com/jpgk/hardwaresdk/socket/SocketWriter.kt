package com.jpgk.hardwaresdk.socket

import java.io.OutputStream
import java.net.Socket


internal class SocketWriter(socket: Socket) {
    private val out: OutputStream = socket.getOutputStream()
    private val lock = Any()


    fun send(msg: String) {
        synchronized(lock) {
            out.write(msg.toByteArray())
            out.flush()
        }
    }


    fun close() {
        try { out.close() } catch (_: Exception) {}
    }
}