package com.jpgk.hardwaresdk.hardwarelogger
import android.os.Environment

object LocalLogsPathManager {

    fun getLogsNewPath():String{
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).absolutePath
    }

    fun getLogsOldPath():String{
        return ""
    }


}