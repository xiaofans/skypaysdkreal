package com.jpgk.hardwaresdk.socket

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.Socket


internal class SocketConnection(
    private val host: String,
    private val port: Int,
    private val onMessage: (String) -> Unit,
    private val onConnected: (() -> Unit)? = null
) {


    private val scope = CoroutineScope(Job() + Dispatchers.IO)


    @Volatile
    private var running = true


    @Volatile
    private var socket: Socket? = null


    private var reader: SocketReader? = null
    private var writer: SocketWriter? = null


    private val reconnectDelay = 5_000L

    private var heartBeatJob: Job? = null


    fun start() {
        scope.launch {
            reconnectLoop()
        }
    }


    private suspend fun reconnectLoop() {
        while (running) {
            try {
                connect()
                // if connect() returns normally, break out of reconnect loop
                break
            } catch (e: Exception) {
                // emit error event (optional)
                // schedule retry
                delay(reconnectDelay)
            }
        }
    }


    private suspend fun connect() {
        // blocking network operation — run on IO dispatcher (we already on IO)
        socket = withContext(Dispatchers.IO) {
            Socket(InetAddress.getByName(host), port).apply { tcpNoDelay = true }
        }


        socket?.let { s ->
            writer = SocketWriter(s)
            reader = SocketReader(s) { msg -> onMessage(msg) }


            // start reader in coroutine
            scope.launch { reader?.start() }

            //Socket 连接完成，通知上层
            onConnected?.invoke()
            // start heartbeat
            heartBeatJob  = scope.launch { HeartBeatManager(writer!!).start() }
        } ?: throw IllegalStateException("socket is null after creation")
    }


    fun send(msg: String) {
        try {
            writer?.send(msg)
        } catch (e: Exception) {
        // swallow or log
        }
    }


    fun close() {
        running = false
        try { reader?.stop() } catch (_: Exception) {}
        try { socket?.close() } catch (_: Exception) {}
        heartBeatJob?.cancel()
        heartBeatJob = null
        socket = null
    }
}