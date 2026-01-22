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
            data["Terminal S/N:"] = "SKYPAY26-000001"
            data["Ref No:"] = "398855645676445"
            data["Date and Time:"] = DateUtils.convertDateFormat2(System.currentTimeMillis())
        }
        printerAcknowledgementReceipt = PrinterAcknowledgementReceipt().apply {
            data["Type of Transaction:"] = "Cash In"
            data["Biller/Service:"] = "GCASH"
            data["Mobile Number:"] = "0912xxx7890"
            data["Amount:"] = "4,000.00"
            data["Service Fee:"] = "10.00"
            data["VAT Amount:"] = "1.20"
            data["Total Amount:"] = "4,011.20"
            data["Amount Received:"] = "4050.00"
        }

        printerChangeRecovery = PrinterChangeRecovery().apply {
            data["Change Amount:"] = "38.80"
            data["Action:"] = "CREDITED TO WALLET"
            data["Biller/Service:"] = "GCASH"
            data["Mobile Number:"] = "0912xxx7890"
            data["Amount:"] = "27.60"
            data["Service Fee:"] = "10.00"
            data["VAT Amount:"] = "1.20"
            data["Ref No (Change):"] = "59885645676445"
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
