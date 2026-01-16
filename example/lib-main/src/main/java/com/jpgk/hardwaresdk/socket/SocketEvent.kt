package com.jpgk.hardwaresdk.socket

sealed interface SocketEvent {
    object Connected : SocketEvent
    object Disconnected : SocketEvent
    data class Message(val text: String) : SocketEvent
    data class Error(val throwable: Throwable) : SocketEvent
}