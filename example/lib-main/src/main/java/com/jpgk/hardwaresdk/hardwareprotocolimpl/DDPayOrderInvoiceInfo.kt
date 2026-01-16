package com.jpgk.hardwaresdk.hardwareprotocolimpl

import android.text.TextUtils

class DDPayOrderInvoiceInfo {
    var invoiceInfoId: Int = 0
    var storeId: String? = null
    var orderId:String? = null
    var orderNo:String? = null
    var invoicelSh:String? = null
    var invoiceCode:String? = null
    var qrcode:String? = null
    var businessType:Int = 0
    var paydate:Long = 0
    var invoiceAmount:String? = null
    var invoiceNumber:String? = null
    var randomNumer:String? = null
    var printMark:String? = null
    var carrierType1:String? = null
    var carrierType2:String? = null
    var npoban:String? = null
    var invoiceDate:String? = null
    var buyerId:String? = null
    var buyerType:String? = null
    var sellerName:String? = null
    var sellerIdentifier:String? = null
    var carrierType:String? = null
    var invoiceState:Int = 0
    var invoiceType:Int = 0
    var eInvYm:String? = null
    var invoiceNumberEnd:String? = null
    var invoiceNumberLength:Int = 0
    var invoiceCreateDate:Int = 0
    var isNoOnlineInvoicing:Int = 0
    var n_TXN_Amount:String? = null
    var TaxAmount:String? = null
    var itemDetails:MutableList<DDPayOrderInvoiceItem>? = null

    var nTXNDateTime:String? = null


    fun getInvoiceAmountInt():Int{
        if (TextUtils.isEmpty(invoiceAmount)){
            return 0
        }
        if (invoiceAmount!!.contains(".")){
            return invoiceAmount!!.split(".")[0].toInt()
        }else{
            return invoiceAmount!!.toInt()
        }
    }

    fun getTaxAmountInt():Int{
        if (TextUtils.isEmpty(TaxAmount)){
            return 0
        }
        if (TaxAmount!!.contains(".")){
            return TaxAmount!!.split(".")[0].toInt()
        }else{
            return TaxAmount!!.toInt()
        }
    }
    fun getOrderTotalAmount():Int{
        return itemDetails?.size?:0
    }
    fun getOrderTaxTotalAmount():Int{
        var taxTotalAmount:Int = 0
        itemDetails?.run {
            for (item in this){
                if ((item.Items_Taxtype ?: "") == "1"){
                    taxTotalAmount+= 1
                }
            }
        }
        return taxTotalAmount
    }

    // 計算不同稅種的金額
    fun getTotalAmountByTaxType(taxType:String):Double{
        var taxTypeTotalAmount:Double = 0.0
        itemDetails?.run {
            for (item in this){
                if ((item.Items_Taxtype ?: "") == taxType){
                    val amount: Int = item.Items_Quantity!!.toInt()
                    val price: Double = item.Items_UnitPrice!!.toDouble()
                    taxTypeTotalAmount+= price*amount
                }
            }
        }
        return taxTypeTotalAmount
    }

}