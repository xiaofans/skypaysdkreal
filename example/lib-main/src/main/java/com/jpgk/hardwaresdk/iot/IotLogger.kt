package com.jpgk.hardwaresdk.iot

import android.util.Log

object IotLogger {


    var enable: Boolean = true
    var globalTag: String = "IOT-SDK"


    fun d(tag: String, msg: String) {
        if (enable) Log.d(globalTag, "[$tag] $msg")
    }


    fun i(tag: String, msg: String) {
        if (enable) Log.i(globalTag, "[$tag] $msg")
    }


    fun w(tag: String, msg: String) {
        if (enable) Log.w(globalTag, "[$tag] $msg")
    }


    fun e(tag: String, msg: String, tr: Throwable? = null) {
        if (enable) Log.e(globalTag, "[$tag] $msg", tr)
    }
}