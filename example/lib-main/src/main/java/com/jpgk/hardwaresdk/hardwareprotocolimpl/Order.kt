package com.jpgk.hardwaresdk.hardwareprotocolimpl

import com.google.gson.annotations.SerializedName
import com.jpgk.iot.enums.InvoiceTypeEnum
import java.io.Serializable
import java.math.BigDecimal

class Order:Serializable {

    var code:Int = 0
    var msg:String? = null
    var type:String? = null
    var channel:String? = null
    // 0未结订单 1已结订单 2 反结订单 3 无效订单 4 挂单订单 5 退单 6 桌台未下单保留 7 微信的待确认订单
    var orderStatus:Int = 0

    @SerializedName("orderNo")
    var orderNum:String? = null
    var totalConsume:Double? = 0.0
    @SerializedName("outstandingAmount")
    var orderAmount:Double? = 0.0

    var orderInvoiceInfo:OrderInvoiceInfoModel? = null

    var invoiceType:InvoiceTypeEnum = InvoiceTypeEnum.NONE

    var checkNo:String? = null
    var payMethod:String? = null

}