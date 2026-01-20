package com.jpgk.hardwaresdk.model

import com.jpgk.hardwaresdk.R
import com.jpgk.hardwaresdk.utils.DateUtils

class PrinterModel {
    var printerLogo:PrinterLogo?= null
    var printerBasic:PrinterBasic?=null
    var printerAcknowledgementReceipt:PrinterAcknowledgementReceipt?=null
    var printerChangeRecovery:PrinterChangeRecovery?=null
    var printerLargeBottom:PrinterBottom?=null
    var printerSmallBottom:PrinterBottom?=null

    fun getTestPrintData(){
        printerLogo= PrinterLogo()
        printerLogo?.run {
            logoRes = R.mipmap.skypay_logo
        }
        printerBasic = PrinterBasic().apply {
            data["terminalSN"] = "SKYPAY26-000001"
            data["refNO"] = "398855645676445"
            data["dateAndTime"] = DateUtils.convertDateFormat2(System.currentTimeMillis())
        }
        printerAcknowledgementReceipt = PrinterAcknowledgementReceipt().apply {
            data["typeOfTransaction"] = "Cash In"
            data["billerService"] = "GCASH"
            data["mobileNumber"] = "0912xxx7890"
            data["amount"] = "4,000.00"
            data["serviceFee"] = "10.00"
            data["vatAmount"] = "1.20"
            data["totalAmount"] = "4,011.20"
            data["amountReceived"] = "4050.00"
        }

        printerChangeRecovery = PrinterChangeRecovery().apply {
            data["changeAmount"] = "38.80"
            data["action"] = "CREDITED TO WALLET"
            data["billerService"] = "GCASH"
            data["mobileNumber"] = "0912xxx7890"
            data["amount"] = "27.60"
            data["serviceFee"] = "10.00"
            data["vatAmount"] = "1.20"
            data["refNoChange"] = "59885645676445"
        }

        printerLargeBottom = PrinterBottom().apply {
            data.add( "*** THIS DOCUMENT IS NOT VALID FOR CLAIM OF ")
            data.add("INPUT TAX ***")
        }
        printerSmallBottom = PrinterBottom().apply {
            data.add("*** For assistance,please visit skypay.com.ph ")
            data.add("and send us a message. ***")
        }

    }

    fun getTestPrintDataNoChangeRecovery() {
        printerLogo= PrinterLogo()
        printerLogo?.run {
            logoRes = R.mipmap.skypay_logo
        }
        printerBasic = PrinterBasic().apply {
            data["terminalSN"] = "SKYPAY26-000001"
            data["refNO"] = "398855645676445"
            data["dateAndTime"] = DateUtils.convertDateFormat2(System.currentTimeMillis())
        }
        printerAcknowledgementReceipt = PrinterAcknowledgementReceipt().apply {
            data["typeOfTransaction"] = "Cash In"
            data["billerService"] = "GCASH"
            data["mobileNumber"] = "0912xxx7890"
            data["amount"] = "4,000.00"
            data["serviceFee"] = "10.00"
            data["vatAmount"] = "1.20"
            data["totalAmount"] = "4,011.20"
            data["amountReceived"] = "4050.00"
        }


        printerLargeBottom = PrinterBottom().apply {
            data.add( "*** THIS DOCUMENT IS NOT VALID FOR CLAIM OF ")
            data.add("INPUT TAX ***")
        }
        printerSmallBottom = PrinterBottom().apply {
            data.add("*** For assistance,please visit skypay.com.ph ")
            data.add("and send us a message. ***")
        }
    }

}
