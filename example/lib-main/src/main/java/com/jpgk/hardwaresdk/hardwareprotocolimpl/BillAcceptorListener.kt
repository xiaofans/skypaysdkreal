package com.jpgk.hardwaresdk.hardwareprotocolimpl

interface BillAcceptorListener {
    companion object{
        val OPEN_PORT_ERROR = 1 // 开启端口失败
        val OFF_LINE_ERROR = 2 // 纸币器不在线
    }
    fun onBillAccept(status:Int,amount:Int)
    fun onEnableSucc()
    fun onError(errorCode:Int)
    fun onDisableSucc()
    fun onReleaseSucc()
}