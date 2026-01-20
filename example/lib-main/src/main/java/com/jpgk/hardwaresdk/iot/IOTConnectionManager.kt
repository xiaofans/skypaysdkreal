package com.jpgk.hardwaresdk.iot

import com.google.gson.Gson
import com.jpgk.hardwaresdk.HardwareSDK
import com.jpgk.hardwaresdk.socket.SdkSocket
import com.jpgk.iot.enums.InvoiceTypeEnum
import com.jpgk.iot.enums.OrderTypeEnum
import com.jpgk.iot.enums.PaymentTypeEnum
import com.jpgk.iot.model.up.OrderUpModel
import java.math.BigDecimal

class IOTConnectionManager private constructor() {

    companion object {

        @Volatile
        private var instance: IOTConnectionManager? = null

        fun getInstance(): IOTConnectionManager {
            return instance ?: synchronized(this) {
                instance ?: IOTConnectionManager().also { instance = it }
            }
        }
    }

    private var machineCode: String? = null
    private var inited = false


    fun init(machineCode: String,iotInitListener: IotInitListener) {
        if (!HardwareSDK.initialized){
            throw Exception("请先初始化HardwareSdk!")
        }else{
            IotSdk.init(HardwareSDK.application!!.applicationContext,machineCode,iotInitListener)
        }

    }


    fun addListener(listener: IOTListener) {
        SdkSocket.addListener(listener)
    }

    fun removeListener(listener: IOTListener) {
       SdkSocket.removeListener(listener)
    }

    fun send(msg:String){
        SdkSocket.send(msg)
    }

    fun disconnect(){
        SdkSocket.close()
    }

    fun sendOrderStatusTOIot(orderNum:String, amount:Double, orderType: OrderTypeEnum, paymentType: PaymentTypeEnum, invoiceType: InvoiceTypeEnum, payStatus:Boolean){
        var orderUpModel = OrderUpModel()
        orderUpModel.orderNo = orderNum
        orderUpModel.amount = BigDecimal(amount)
        orderUpModel.orderType = orderType
        orderUpModel.currency="TW"
        orderUpModel.paymentType = paymentType
        orderUpModel.phoneNumber = ""
        orderUpModel.paymentFlag= payStatus
        orderUpModel.invoiceType = invoiceType
        //SocketClient.getInstance().sendMsg(JSON.toJSONString(orderUpModel))
        send(Gson().toJson(orderUpModel))
    }
}