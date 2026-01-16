package com.jpgk.hardwaresdk.hardwareprotocolimpl

import java.io.Serializable


/****
 * {
 *   "code": 0,
 *   "msg": "操作成功",
 *   "orderStatus": 0,
 *   "orderNo": "10030241214000001",
 *   "totalConsume": 4.0,
 *   "outstandingAmount": 4.0,
 *   "orderInvoiceInfo": {
 *     "invoiceInfoId": null,
 *     "storeId": "",
 *     "orderId": "5969e2fe-a7f5-40c3-88f3-80084d841cb7",
 *     "orderNo": "10030241214000001",
 *     "invoicelSh": "",
 *     "invoiceCode": "",
 *     "qrcode": "FW/r9aRrrpKwfWM19jvJbg==",
 *     "businessType": 1,
 *     "paydate": 0,
 *     "invoiceAmount": 0.0,
 *     "invoiceNumber": "HR10010223",
 *     "randomNumer": "4500",
 *     "printMark": "Y",
 *     "carrierType1": "",
 *     "carrierType2": "",
 *     "npoban": "",
 *     "invoiceDate": "",
 *     "buyerId": "",
 *     "buyerType": "",
 *     "sellerName": "",
 *     "sellerIdentifier": "",
 *     "carrierType": "EJ0002",
 *     "invoiceState": 0,
 *     "invoiceType": 2,
 *     "eInvYm": "202412",
 *     "invoiceNumberEnd": "",
 *     "invoiceNumberLength": 0,
 *     "invoiceCreateDate": 1734105600000,
 *     "isNoOnlineInvoicing": 0
 *   }
 * }
 */
class OrderInvoiceInfoModel: Serializable {

    var invoiceInfoId:String? = null
    var storeId:String? = null
    var orderId:String? = null
    var orderNo:String? = null
    var invoicelSh:String? = null
    var invoiceCode:String? = null
    var qrcode:String? = null
    var paydate:String? = null
    var invoiceAmount:Double? = null
    var invoiceNumber:String? = null
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
    var invoiceState:String? = null
    var invoiceType:String? = null
    var eInvYm:String? = null


    var invoiceNumberEnd:String? = null


    var invoiceNumberLength:String? = null
    var invoiceCreateDate:String? = null
    var isNoOnlineInvoicing:Int = 0

}