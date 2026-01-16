package com.jpgk.hardwaresdk.hardwareprotocolimpl

interface BillCoinAcceptorListener {
    fun onBillAccept(status:Int,amount:Int)
    fun onCoinAccept(amount:Int)
    fun onPayOutCoinAmount(amount:Int)
    fun onConnectStatus()
    fun paySucc(needPayOutCoinAmount:Int,needPayOutCashAmount:Int)
    fun payFail(failCode:Int)
    fun onPayContinue()
    fun onTubeStatusChange()

    // 出钞机状态
    fun onCashDispenserStatus(status:String,errorCode:String?,errorMsg:String?)
    // 出钞机错误
    fun onCashDispensingError(errorCode:String?,errorMsg: String?,dispensedCashAmount:Int)
    //出钞机出钞成功
    fun onCashDispensedSucc(dispensedCashAmount: Int)

    // 找零超時
    fun onChangeTimeOut()
}