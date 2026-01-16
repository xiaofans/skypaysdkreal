package com.jpgk.hardwaresdk.model

import com.jpgk.hardwaresdk.R

class PrinterModel {
    var printerLogo:PrinterLogo?= null
    var printerBasic:PrinterBasic?=null
    var printerAcknowledgementReceipt:PrinterAcknowledgementReceipt?=null
    var printerChangeRecovery:PrinterChangeRecovery?=null
    var printerBottom:PrinterBottom?=null

    fun getTestPrintData(){
        printerLogo= PrinterLogo()
        printerLogo?.run {
            logoRes = R.mipmap.skypay_logo
        }
        printerBasic = PrinterBasic()
        printerBasic?.run {
            terminalSN = "SKYPAY26-000001"
            refNO = "398855645676445"
            dateAndTime = "2025-10-02 12:58:19"
        }
        printerAcknowledgementReceipt = PrinterAcknowledgementReceipt()
        printerAcknowledgementReceipt?.run {
            typeOfTransaction = "Cash In"
            billerService = "GCASH"
            mobileNumber = "0912xxx7890"
            amount = "4,000.00"
            serviceFee = "10.00"
            vatAmount = "1.20"
            totalAmount = "4,011.20"
            amountReceived = "4050.00"
        }
        printerChangeRecovery = PrinterChangeRecovery()

        printerChangeRecovery?.run {
            changeAmount = "38.80"
            action = "CREDITED TO WALLET"
            billerService = "GCASH"
            mobileNumber = "0912xxx7890"
            amount = "27.60"
            serviceFee = "10.00"
            vatAmount = "1.20"
            refNoChange = "59885645676445"
        }

        printerBottom = PrinterBottom()
        printerBottom?.run {
            tipsMain = "THIS DOCUMENT IS NOT VALID FOR CLAIM OF INPUT TAX"
            tipsSub = "For assistance,please visit skypay.com.ph and send us a message."
        }

    }

    fun getTestPrintDataNoChangeRecovery(){
        printerLogo= PrinterLogo()
        printerLogo?.run {
            logoRes = R.mipmap.skypay_logo
        }

        printerBasic = PrinterBasic()
        printerBasic?.run {
            terminalSN = "SKYPAY26-000001"
            refNO = "398855645676445"
            dateAndTime = "2025-10-02 12:58:19"
        }
        printerAcknowledgementReceipt = PrinterAcknowledgementReceipt()
        printerAcknowledgementReceipt?.run {
            typeOfTransaction = "Cash In"
            billerService = "GCASH"
            mobileNumber = "0912xxx7890"
            amount = "4,000.00"
            serviceFee = "10.00"
            vatAmount = "1.20"
            totalAmount = "4,011.20"
            amountReceived = "4050.00"
        }

        printerBottom = PrinterBottom()
        printerBottom?.run {
            tipsMain = "THIS DOCUMENT IS NOT VALID FOR CLAIM OF INPUT TAX"
            tipsSub = "For assistance,please visit skypay.com.ph and send us a message."
        }
    }
}
