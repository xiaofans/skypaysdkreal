package com.jpgk.hardwaresdk.hardwareprotocolimpl

import android.os.Handler
import android.util.Log
import androidx.core.os.postDelayed
import com.jpgk.hardwaresdk.serialport.PaymentDeviceConstant
import com.jpgk.hardwaresdk.serialport.PaymentDeviceConstant.MDB_BILL_EXPANSION
import com.jpgk.hardwaresdk.serialport.PaymentDeviceConstant.MDB_BILL_EXPANSION_IDENTIFICATION_WITH_OPTION
import com.jpgk.hardwaresdk.serialport.SerialPortUtils
import com.jpgk.hardwaresdk.setting.SettingConstants
import com.jpgk.hardwaresdk.utils.DLogger

import com.vi.vioserial.COMSerial
import com.vi.vioserial.listener.OnComDataListener
import com.vi.vioserial.util.SerialDataUtils


open class BillCoinAcceptorManager(var portPath: String?,
                                   var portRate: Int,
                                   var billAcceptorListener: BillAcceptorListener?):BaseHardware {
    interface OnBillCoinDataListener {
        fun onDataResponse(hexData: String?)
    }

    val TAG = "BillCoinMgr"
    private var onComDataListener: OnComDataListener? = null

    var coinConfig:CoinAcceptorConfig? = null
    var tubeStatus:TubeStatusConfig? = null

    var isCoinAcceptorOnLine = false
    var isBillAcceptorOnLine = false

    var isBillAcceptorEnableCommandAccepted = false
    var isCoinAcceptorEnabled = false


    var billAcceptorHandler:Handler = Handler()


    var onBillCoinDataListener = object:BillCoinAcceptorManager.OnBillCoinDataListener{
        override fun onDataResponse(hexData: String?) {
            hexData?.run {
                var data = SerialDataUtils.HexToByteArr(this)
                Log.w("DDPayingVM","接收到数据hex:"+ hexData)
                DLogger.log("DDPayingVM","接收到数据hex:"+ hexData)
                val asciiString = hexData.chunked(2).map { it.toInt(16).toChar() }.joinToString("")
                // Split ASCII string by space
                val asciidata = asciiString.split(" ")
                Log.w("DDPayingVM","接收到数据ascii:"+ asciiString)
                DLogger.log("DDPayingVM","接收到数据ascii:"+ asciiString)
//                val responseBytes = hexData.split(" ").map { it.toInt(16) }
//                val responseBytes = hexData.chunked(2).map { it.toInt(16) }
                Log.w("DDPayingVM","响应字节:$asciidata 长度:${asciidata.size}")
                DLogger.log("DDPayingVM","响应字节:$asciidata 长度:${asciidata.size}")

                isBillAcceptorOnLine = true
               /* if (asciidata[0].equals("03") && asciidata[1].equals("19")){
                    LogUtils.w("isCoinAcceptorOnLine:"+isCoinAcceptorOnLine)
                    DLogger.log("isCoinAcceptorOnLine:"+isCoinAcceptorOnLine)
                    isCoinAcceptorOnLine = true
                }
                if (asciidata[0].equals("01") && asciidata[1].equals("19")){
                    isBillAcceptorOnLine = true
                    LogUtils.w("isBillAcceptorOnLine:"+isBillAcceptorOnLine)
                    DLogger.log("isBillAcceptorOnLine:"+isBillAcceptorOnLine)
                }*/
                val responseBytesSize = asciidata.size
                if (data != null) {
//                        Log.e("响应数据:",SerialPortUtils.bytesToHex(data) );
                    // 根据设备类型处理响应
                    var splitCmdList =parseMdbCommandsHex(this)
                    for (cmd in splitCmdList){
                        var data1 = SerialDataUtils.HexToByteArr(cmd.trim())
                        if (isBillData(data1)) {
                            handleBillResponse(data1,responseBytesSize)
                        } else if (isCoinData(data1)) {
                            val cmdAsciiString = cmd.trim().chunked(2).map { it.toInt(16).toChar() }.joinToString("")
                            val cmdAsciidata = cmdAsciiString.split(" ")
                            val cmdResponseBytesSize = cmdAsciidata.size
                            if (cmdAsciidata.size == 2){
                                if (asciidata[0] == "00"){
                                    isBillAcceptorEnableCommandAccepted = true
                                    DLogger.log("DDPayingVM","纸币器正常启用")
                                    Log.w("DDPayingVM","纸币器正常启用")
                                }
                            }
                            if (cmdAsciidata.size == 4){
                                if (cmdAsciidata[0] == "03" && cmdAsciidata[1] == "00" && cmdAsciidata[2]=="03"){
                                    isCoinAcceptorEnabled = true
                                    DLogger.log("DDPayingVM","硬幣器正常启用")
                                    Log.w("DDPayingVM","硬幣器正常启用")
                                }
                            }
                            DLogger.log("cmdAsciiString:${cmdAsciiString},cmdAsciidata:${cmdAsciidata},cmdResponseBytesSize:${cmdResponseBytesSize}")
                            handleCoinResponse(data1,cmdResponseBytesSize,cmd.trim())
                        }
                    }

                }

            }
        }
    }

    fun parseMdbCommandsHex(hexData: String): List<String> {
        val commands = mutableListOf<String>()

        // 将 0D0A 转换为十六进制形式，方便查找分隔符
        val delimiter = "0D0A"
        var start = 0
        while (start < hexData.length) {
            val index = hexData.indexOf(delimiter, start)
            if (index == -1) {
                // 最后一条指令
                val cmd = hexData.substring(start).trim()
                if (cmd.isNotEmpty()) commands.add(cmd)
                break
            }
            // 截取到 delimiter 前的指令
            val cmd = hexData.substring(start, index).trim()
            if (cmd.isNotEmpty()) commands.add(cmd)
            start = index + delimiter.length
        }

        return commands
    }

    fun parseMdbCommands(hexData: String): List<String> {
        val commands: MutableList<String> = ArrayList()

        // 每两个字符表示一个字节
        val len = hexData.length
        val sb = java.lang.StringBuilder()
        var i = 0
        while (i < len) {
            val byteHex = hexData.substring(i, i + 2)
            val value = byteHex.toInt(16)
            sb.append(value.toChar())
            i += 2
        }

        // 按 0D0A 分割
        val parts = sb.toString().split("\r\n".toRegex()).dropLastWhile { it.isEmpty() }
            .toTypedArray()
        for (part in parts) {
            if (!part.trim { it <= ' ' }.isEmpty()) {
                commands.add(part)
            }
        }

        return commands
    }
    fun hexStringToAscii(hexString: String): String {
        return hexString.chunked(2)
            .map {
                val charValue = it.toInt(16)
                charValue.toChar()
            }
            .joinToString("")
    }
    fun hexStringToAsciiJava(hexString: String): String {
        val output = StringBuilder()
        for (i in 0 until hexString.length step 2) {
            val str = hexString.substring(i, i + 2)
            val charValue = str.toInt(16)
            if (charValue != 0x0D && charValue != 0x0A) {  // Ignore carriage return and line feed
                output.append(charValue.toChar())
            }
        }
        return output.toString()
    }

    override fun openSerialPort(): Int {
        val state = COMSerial.instance().addCOM(portPath, portRate)
        onComDataListener = OnComDataListener { com, hexData ->
            if (portPath == com) {
                if (onBillCoinDataListener != null) {
                    onBillCoinDataListener!!.onDataResponse(hexData)
                }
            }
        }
        COMSerial.instance().addDataListener(onComDataListener)
        return state
    }

    override fun closePort() {
        COMSerial.instance().removeDataListener(onComDataListener)
        COMSerial.instance().close(portPath)
    }

    override fun reconnect() {
        closePort()
        openSerialPort()
    }

    override fun isSerialPortOpen(): Boolean {
        return COMSerial.instance().isOpen(portPath)
    }

    override fun isOnLine(): Boolean {
        return isBillAcceptorOnLine && isCoinAcceptorOnLine
    }

    override fun getHardwareName(): String {
        return "硬幣器紙幣器"
    }

    override fun getHardwareType(): Int {
        return BaseHardware.TYPE_COIN_BILL_ACCEPTER
    }


    open fun sendCommand(command: ByteArray?) {
        Log.d("PaymentDevice", "发送指令:"+SerialDataUtils.ByteArrToHex(command))
        DLogger.log("PaymentDevice", "发送指令:"+SerialDataUtils.ByteArrToHex(command))
        COMSerial.instance().sendHex(portPath, SerialDataUtils.ByteArrToHex(command))
    }


    // 判断是否为纸币数据
    private fun isBillData(data: ByteArray): Boolean {
        // 根据协议判断数据是否为纸币响应
        return data[0] == PaymentDeviceConstant.MDB_BILL_HEAD // 示例判断
    }

    // 判断是否为硬币数据
    private fun isCoinData(data: ByteArray): Boolean {
        // 根据协议判断数据是否为硬币响应
        return data[0] == PaymentDeviceConstant.MDB_COIN_HEAD // 示例判断
    }

    // 处理纸币接收器的响应
    fun handleBillResponse(data: ByteArray,responseBytesSize:Int) {
        if (data[0] == 0x33.toByte()) {
            var amount = 0
            var status:Int = 1
            Log.d("PaymentDevice", "纸币接收器响应 (原始字节): " + SerialPortUtils.bytesToHex(data))
            DLogger.log("PaymentDevice", "纸币接收器响应 (原始字节): " + SerialPortUtils.bytesToHex(data))

            // 确保数据长度足够长，以便检测模式
            if (data.size < 2) {
                Log.e("PaymentDevice", "接收到的数据长度不足，无法解析: " + data.size)
                DLogger.log("PaymentDevice", "接收到的数据长度不足，无法解析: " + data.size)
                return
            }
            // 搜索整个字节数组，找到与硬币面额模式匹配的部分
            // 100元纸币模式: 33 30 20 38 30 20 30
            //暂存取100元纸币 33 30 20 30 39 20 30 状态
            if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x38.toByte(),
                        0x30.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 20 元纸币")
                DLogger.log("PaymentDevice", "接收到 20 元纸币")
                //handleTransaction(10.0);  // 记录100元纸币交易
                amount = 20
                //200元纸币模式: 33 30 20 38 31 20 30
                status = 1
                recordBillAcceptAmount(amount)
            } else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x38.toByte(),
                        0x31.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 50 元纸币")
                DLogger.log("PaymentDevice", "接收到 50 元纸币")
                //handleTransaction(10.0);  // 记录20元硬币交易
                amount = 50
                status = 1
                recordBillAcceptAmount(amount)
            } else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x38.toByte(),
                        0x32.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 100 元纸币")
                DLogger.log("PaymentDevice", "接收到 100 元纸币")
                //handleTransaction(1.0);  // 记录1元硬币交易
                amount = 100
                status = 1
                recordBillAcceptAmount(amount)
            } else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x38.toByte(),
                        0x33.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 200 元纸币")
                DLogger.log("PaymentDevice", "接收到 200 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 200
                status = 1
                recordBillAcceptAmount(amount)
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x38.toByte(),
                        0x34.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 500 元纸币")
                DLogger.log("PaymentDevice", "接收到 500 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 500
                status = 1
                recordBillAcceptAmount(amount)
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x38.toByte(),
                        0x35.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 1000 元纸币")
                DLogger.log("PaymentDevice", "接收到 1000 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 1000
                status = 1
                recordBillAcceptAmount(amount)
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x39.toByte(),
                        0x30.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "暂存区 20 元纸币")
                DLogger.log("PaymentDevice", "暂存区 20 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 20
                status = 2
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x39.toByte(),
                        0x31.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "暂存区 50 元纸币")
                DLogger.log("PaymentDevice", "暂存区 50 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 50
                status = 2
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x39.toByte(),
                        0x32.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "暂存区 100 元纸币")
                DLogger.log("PaymentDevice", "暂存区 100 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 100
                status = 2
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x39.toByte(),
                        0x33.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "暂存区 200 元纸币")
                DLogger.log("PaymentDevice", "暂存区 200 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 200
                status = 2
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x39.toByte(),
                        0x34.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "暂存区 500 元纸币")
                DLogger.log("PaymentDevice", "暂存区 500 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 500
                status = 2
            }else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x33.toByte(),
                        0x30.toByte(),
                        0x20.toByte(),
                        0x39.toByte(),
                        0x35.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "暂存区 1000 元纸币")
                DLogger.log("PaymentDevice", "暂存区 1000 元纸币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 1000
                status = 2
            } else {
                Log.d("PaymentDevice", "未知的纸币响应数据: " + SerialPortUtils.bytesToHex(data))
                DLogger.log("PaymentDevice", "未知的纸币响应数据: " + SerialPortUtils.bytesToHex(data))
            }
            billAcceptorListener?.onBillAccept(status,amount)
        }
    }

    // 处理硬币接收器的响应
    fun handleCoinResponse(data: ByteArray,responseBytesSize:Int,hexData: String) {
        var isHandleBySzie = handleCoinResponseBySize(data,responseBytesSize,hexData)
        if (data[0] == 0x30.toByte() && !isHandleBySzie) {
            var amount = 0
            Log.d("PaymentDevice", "硬币接收器响应 (原始字节): " + SerialPortUtils.bytesToHex(data))
            DLogger.log("PaymentDevice", "硬币接收器响应 (原始字节): " + SerialPortUtils.bytesToHex(data))

            // 确保数据长度足够长，以便检测模式
            if (data.size < 2) {
                Log.e("PaymentDevice", "接收到的数据长度不足，无法解析: " + data.size)
                DLogger.log("PaymentDevice", "接收到的数据长度不足，无法解析: " + data.size)
                return
            }
            // 搜索整个字节数组，找到与硬币面额模式匹配的部分
            // 50元硬币模式: 30 38 20 35 33 20 30
            if (containsPattern(
                    data,
                    byteArrayOf(
                        0x30.toByte(),
                        0x38.toByte(),
                        0x20.toByte(),
                        0x35.toByte(),
                        0x34.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 50 元硬币")
                DLogger.log("PaymentDevice", "接收到 50 元硬币")
                //handleTransaction(10.0);  // 记录50元硬币交易
                amount = 50
            }else
            if (containsPattern(
                data,
                byteArrayOf(
                    0x30.toByte(),
                    0x38.toByte(),
                    0x20.toByte(),
                    0x34.toByte(),
                    0x34.toByte(),
                )
            )){
                Log.d("PaymentDevice", "溢币区接收到 50 元硬币")
                DLogger.log("PaymentDevice", "溢币区接收到 50 元硬币")
                amount = 50
                recordYBQAmount(amount)

            }else
            // 10元硬币模式: 30 38 20 35 32 20 30
            if (containsPattern(
                    data,
                    byteArrayOf(
                        0x30.toByte(),
                        0x38.toByte(),
                        0x20.toByte(),
                        0x35.toByte(),
                        0x32.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 10 元硬币")
                DLogger.log("PaymentDevice", "接收到 10 元硬币")
                //handleTransaction(10.0);  // 记录10元硬币交易
                amount = 10
            }else if (containsPattern(
                data,
                byteArrayOf(
                    0x30.toByte(),
                    0x38.toByte(),
                    0x20.toByte(),
                    0x34.toByte(),
                    0x32.toByte(),
                )
            )){
                amount = 10
                recordYBQAmount(amount)
                Log.d("PaymentDevice", "溢币区接收到 10 元硬币")
                DLogger.log("PaymentDevice", "溢币区接收到 10 元硬币")
            }
            else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x30.toByte(),
                        0x38.toByte(),
                        0x20.toByte(),
                        0x35.toByte(),
                        0x30.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 1 元硬币")
                DLogger.log("PaymentDevice", "接收到 1 元硬币")
                //handleTransaction(1.0);  // 记录1元硬币交易
                amount = 1
            }else if (containsPattern(
                data,
                byteArrayOf(
                    0x30.toByte(),
                    0x38.toByte(),
                    0x20.toByte(),
                    0x34.toByte(),
                    0x30.toByte(),
                )
            )){
                Log.d("PaymentDevice", "溢币区接收到 1 元硬币")
                DLogger.log("PaymentDevice", "溢币区接收到 1 元硬币")
                amount = 1
                recordYBQAmount(amount)
            } else if (containsPattern(
                    data,
                    byteArrayOf(
                        0x30.toByte(),
                        0x38.toByte(),
                        0x20.toByte(),
                        0x35.toByte(),
                        0x31.toByte(),
                    )
                )
            ) {
                Log.d("PaymentDevice", "接收到 5 元硬币")
                DLogger.log("PaymentDevice", "接收到 5 元硬币")
                //                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 5
            }else if (containsPattern(
                data,
                byteArrayOf(
                    0x30.toByte(),
                    0x38.toByte(),
                    0x20.toByte(),
                    0x34.toByte(),
                    0x31.toByte(),
                )
            )){
                amount = 5
                recordYBQAmount(amount)
                Log.d("PaymentDevice", "溢币区接收到 5 元硬币")
                DLogger.log("PaymentDevice", "溢币区接收到 5 元硬币")
            } else {
                Log.d("PaymentDevice", "未知的硬币响应数据: " + SerialPortUtils.bytesToHex(data))
            }
//            onPaymentListener?.onCoinAccept(amount)
        }
    }


    fun handleCoinResponseBySize(data: ByteArray,responseBytesSize:Int,hexData:String):Boolean{
        when(responseBytesSize){
            18 ->{
                // PAY OUT STATUS
                val totalPayOutAmount = parsePayoutStatus(hexData)
                Log.w("BillCoinMgr:","pay out amount:$totalPayOutAmount")
                DLogger.log("BillCoinMgr:","pay out amount:$totalPayOutAmount")
//                onPaymentListener?.onPayOutCoinAmount(totalPayOutAmount.toInt())
            }
            20 ->{
                //TUBE STATUS
                this.tubeStatus = parseTubeStatus(hexData)
//                onPaymentListener?.onTubeStatusChange()
                Log.w("BillCoinMgr:","tube status:"+this.tubeStatus.toString())
                DLogger.log("BillCoinMgr:","tube status:"+this.tubeStatus.toString())
                return true
            }
            25->{
                // SET_UP
                this.coinConfig = parseCoinAcceptorSetup(hexData)
                Log.w("BillCoinMgr:","coin setup config:"+this.coinConfig.toString())
                DLogger.log("BillCoinMgr:","coin setup config:"+this.coinConfig.toString())
                return true
            }

        }
        return false
    }

    // 在数据中查找模式，返回是否找到匹配模式
    private fun containsPattern(data: ByteArray, pattern: ByteArray): Boolean {
        // 遍历整个字节数组，尝试找到与 pattern 匹配的部分
        for (i in 0..data.size - pattern.size) {
            var match = true
            for (j in pattern.indices) {
                if (data[i + j] != pattern[j]) {
                    match = false
                    break
                }
            }
            if (match) {
                return true // 找到匹配的模式
            }
        }
        return false // 未找到匹配的模式
    }


    fun enableBillAcceptor(enableEscrow: Boolean){
        closePort()
        var openState =  openSerialPort()
        if (openState == 0){
            enableBillAcceptorCMD(enableEscrow)
            retryCoinAcceptorConfig(1)
            retryCoinAcceptorConfig(2)
            retryCoinAcceptorConfig(3)
            checkIfenableSucc()
        }else{
            billAcceptorListener?.onError(BillAcceptorListener.OPEN_PORT_ERROR)
        }
    }
    fun checkIfenableSucc(){
        if (!isBillAcceptorOnLine){
            billAcceptorHandler.postDelayed({
                billAcceptorListener?.onEnableSucc()
            },4000)
        }else{
            billAcceptorListener?.onError(BillAcceptorListener.OFF_LINE_ERROR)
        }
    }

    fun retryCoinAcceptorConfig(delaySecods:Int){
        billAcceptorHandler?.postDelayed({
            enableBillAcceptorCMD(true)
        },delaySecods * 1000L)
    }

    fun disableBillAcceptor(){
        disableBillAcceptorCMD()
        billAcceptorHandler.postDelayed({
            billAcceptorListener?.onDisableSucc()
        },1000)
    }

    fun releaseBillAcceptor(){
        billAcceptorHandler.postDelayed({
            disableBillAcceptorCMD()
        },1000)
        billAcceptorHandler.postDelayed({
            closePort()
            billAcceptorListener?.onReleaseSucc()
            billAcceptorListener = null
            billAcceptorHandler.removeCallbacksAndMessages(null)

        },2000)
    }


    // 启用纸币接收器（全额全部打开）
    private fun enableBillAcceptorCMD(enableEscrow: Boolean) {
        val enableCommand = byteArrayOf(
            0x34.toByte(),            // MDB_BILL_ENABLE 固定 0x34
            0xFF.toByte(),            // 保留 or 启用位
            0xFF.toByte(),            // 所有面额启用
            0xFF.toByte(),            // 保留
            (if (enableEscrow) 0xFF else 0x00).toByte()   // escrow 全开启 或全关闭
        )
        sendCommand(enableCommand)
    }

    // 禁用纸币接收器
   private fun disableBillAcceptorCMD() {
        val disableCommand = byteArrayOf(
            PaymentDeviceConstant.MDB_BILL_ENABLE,
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte(),
            0x00.toByte()
        )
        sendCommand(disableCommand)
    }


    //纸币存入到钱箱
    fun pushBillIn() {
        val putBillInCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_BILL_ESCROW, 0x01.toByte()) // 0F02命令：退还硬币
        sendCommand(putBillInCommand)
        Log.d("PaymentDevice", "发送存入纸币命令")
        DLogger.log("PaymentDevice", "发送存入纸币命令")
    }

    //纸币从暂存区退出
    fun pollBillOut() {
        val putBillInCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_BILL_ESCROW, 0x00.toByte()) // 0F02命令：退还硬币
        sendCommand(putBillInCommand)
        Log.d("PaymentDevice", "发送纸币从暂存区弹出命令")
        DLogger.log("PaymentDevice", "发送纸币从暂存区弹出命令")
    }

    // 退还纸币（退回指定金额的纸币）80 > 100 81 > 200 82 -> 500 83 -> 1000
    fun returnBillAuto() {
        val returnCoinCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_BILL_ESCROW, 0x00.toByte()) // 0F02命令：退还硬币
        sendCommand(returnCoinCommand)
        Log.d("PaymentDevice", "发送退还硬币命令：自动")
        DLogger.log("PaymentDevice", "发送退还硬币命令：自动")
    }


    // 执行退纸币操作
    fun returnBill(type: Int) {
        val payOutCommand = byteArrayOf(
            PaymentDeviceConstant.MDB_BILL_EXPANSION,  // 扩展命令
            PaymentDeviceConstant.MDB_BILL_ESCROW,  // 退纸币命令
            type.toByte(),  // 纸币类型（通道）
            0x00.toByte(),  // 保留字节，设为0
            0x01.toByte() // 退还1张纸币
        )

        // 发送命令到纸币器
        sendCommand(payOutCommand)
    }


    // 启用硬币接收器
    fun enableCoinAcceptor() {
        val enableCommand = byteArrayOf(
            PaymentDeviceConstant.MDB_COIN_ENABLE,
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte(),
            0xFF.toByte()
        )
        sendCommand(enableCommand)
    }

    // 重置硬幣接收器
    fun resetCoinAcceptor(){
        val resetCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_COIN_RESET)
        sendCommand(resetCommand)
    }

    // 禁用硬币接收器
    fun disableCoinAcceptor() {
        val disableCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_COIN_ENABLE, 0x00.toByte(), 0x00.toByte(), 0x00.toByte(), 0x00.toByte())
        sendCommand(disableCommand)
    }

    // 退还硬币（退回指定金额的硬币）50 > 1 51 > 5 52 -> 10 53 -> 50
    fun returnCoinAuto() {
        val returnCoinCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_COIN_PAYOUT, 0x02.toByte(), 0x50) // 0F02命令：退还硬币
        sendCommand(returnCoinCommand)
        Log.d("PaymentDevice", "发送退还硬币命令：自动")
        DLogger.log("PaymentDevice", "发送退还硬币命令：自动")
    }

    // 退还硬币（退回指定金额的硬币）50 > 1 51 > 5 52 -> 10 53 -> 50
    fun returnCoin(count: Byte) {
        val returnCoinCommand = byteArrayOf(0x0F.toByte(), 0x02.toByte(), count) // 0F02命令：退还硬币
        sendCommand(returnCoinCommand)
        Log.d("PaymentDevice", "发送退还硬币命令：自动")
        DLogger.log("PaymentDevice", "发送退还硬币命令：自动")
    }

    // 退还硬币
    fun payOutCoin(amount:Int){
        if(coinConfig==null){
            return
        }
        val scalingFactorValue = amount / coinConfig!!.scalingFactor
        val payoutValue = scalingFactorValue.toByte()
        Log.w("BillCoin","amount:$amount,payOutValue:$payoutValue coinConfig!!.scalingFactor:${scalingFactorValue}")
        DLogger.log("BillCoin","amount:$amount,payOutValue:$payoutValue")
        val returnCoinCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_COIN_PAYOUT, 0x02.toByte(), payoutValue) // 0F02命令：退还硬币
        sendCommand(returnCoinCommand)
        Log.d("PaymentDevice", "发送退还硬币命令：自动")
        DLogger.log("PaymentDevice", "发送退还硬币命令：自动")
    }
    // 退回硬币状态
    fun payOutCoinStatus(){
        val returnCoinCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_COIN_PAYOUT, 0x03.toByte()) // 0F02命令：退还硬币
        sendCommand(returnCoinCommand)
        Log.d("PaymentDevice", "发送退还硬币状态指令")
        DLogger.log("PaymentDevice", "发送退还硬币状态指令")
    }


    // 硬幣器啟用狀態
    fun getCoinAcceptorIsEnabledStatus(){
        val returnCoinCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_COIN_PAYOUT, 0x05.toByte())
        sendCommand(returnCoinCommand)
        Log.d("PaymentDevice", "發送硬幣器啟用狀態查詢指令")
        DLogger.log("PaymentDevice", "發送硬幣器啟用狀態查詢指令")
    }

    // 查询硬币总额
    fun checkCoin(){
        val checkCoinCommand = byteArrayOf(0x0A.toByte())
        sendCommand(checkCoinCommand)
    }

    // 硬币器配置项
    fun getCoinSetupInfo(){
        val checkCoinCommand = byteArrayOf(PaymentDeviceConstant.MDB_COIN_SETUP)
        sendCommand(checkCoinCommand)
    }

    // 纸币器器配置项
    fun getBillSetupInfo(){
        val checkCoinCommand = byteArrayOf(PaymentDeviceConstant.MDB_BILL_SETUP)
        sendCommand(checkCoinCommand)
    }

    // 发送36H 对账指令
     fun sendStackerStatus() {
        val cmd = byteArrayOf(0x36) // 文档里定义的对账命令
        sendCommand(cmd)
    }



    // 硬币器配置项
    fun getCoinTubeStatus(){
        val checkCoinCommand = byteArrayOf(PaymentDeviceConstant.MDB_COIN_NUM)
        sendCommand(checkCoinCommand)
    }

    fun getBillExpansionSetupInfo(){
        val command = byteArrayOf(MDB_BILL_EXPANSION, MDB_BILL_EXPANSION_IDENTIFICATION_WITH_OPTION)
        sendCommand(command)
    }

    fun queryCoinStatus(){
        val returnCoinCommand =
            byteArrayOf(PaymentDeviceConstant.MDB_COIN_PAYOUT, 0x02.toByte(), 0x50) // 0F02命令：退还硬币
        sendCommand(returnCoinCommand)
        Log.d("PaymentDevice", "发送退还硬币命令：自动")
        DLogger.log("PaymentDevice", "发送退还硬币命令：自动")
    }

    // 测试硬币器是否在线
    fun testCoinAcceptorConnection(){
        isCoinAcceptorOnLine = false
        getCoinSetupInfo()
    }

    // 测试纸币器是否在线
    fun tesetBillAcceptorConnection(){
        isBillAcceptorOnLine = false
        getBillSetupInfo()
    }


    // --------------------------------------硬币器SET UP/TUBE STATUS 配置相关信息---------------------
    fun parseCoinAcceptorSetup(response: String): CoinAcceptorConfig {
        // Convert HEX string to ASCII string
        val asciiString = response.chunked(2).map { it.toInt(16).toChar() }.joinToString("")

        // Split ASCII string by space
        val data = asciiString.split(" ")

        // Extract values from the data list
        val featureLevel = data[0].toInt(16)
        val countryCode = ((data[1].toInt(16) and 0xFF) shl 8) or (data[2].toInt(16) and 0xFF)
        val scalingFactor = data[3].toInt(16)
        val decimalPlaces = data[4].toInt(16)
        val coinRouting = ((data[5].toInt(16) and 0xFF) shl 8) or (data[6].toInt(16) and 0xFF)
        val coinCredits = data.slice(7..22).map { it.toInt(16) }

        // Create a configuration object
        return CoinAcceptorConfig(
            featureLevel = featureLevel,
            countryCode = countryCode,
            scalingFactor = scalingFactor,
            decimalPlaces = decimalPlaces,
            coinRouting = coinRouting,
            coinCredits = coinCredits
        )
    }

    data class CoinAcceptorConfig(
        val featureLevel: Int,
        val countryCode: Int,
        val scalingFactor: Int,
        val decimalPlaces: Int,
        val coinRouting: Int,
        val coinCredits: List<Int>
    )

    fun parseTubeStatusOld(response: String): TubeStatusConfig {
        // Convert HEX string to ASCII string
        val asciiString = response.chunked(2).map { it.toInt(16).toChar() }.joinToString("")

        // Split ASCII string by space
        val data = asciiString.split(" ")

        // Extract values from the data list
        val tubeFullStatus = ((data[0].toInt(16) and 0xFF) shl 8) or (data[1].toInt(16) and 0xFF)
        val tubeStatus = data.slice(2..17).map { it.toInt(16) }

        // Create a configuration object
        return TubeStatusConfig(
            tubeFullStatus = tubeFullStatus,
            tubeStatus = tubeStatus
        )
    }

    fun parseTubeStatus(response: String): TubeStatusConfig {
        // 转换 HEX -> ASCII
        var asciiString = response.chunked(2).map { it.toInt(16).toChar() }.joinToString("")

        // 去除换行、回车符号
        asciiString = asciiString.replace("\r", " ").replace("\n", " ")

        // 按空格切割并过滤掉空值
        val data = asciiString.split(" ").filter { it.isNotBlank() }

        // 安全校验数据长度
        if (data.size < 18) {
            Log.e("BillCoinMgr", "parseTubeStatus 数据长度异常: ${data.size}, 内容: $data")
            DLogger.log("BillCoinMgr", "parseTubeStatus 数据长度异常: ${data.size}, 内容: $data")
            return TubeStatusConfig(0, emptyList())
        }

        // 正常解析
        val tubeFullStatus = ((data[0].toInt(16) and 0xFF) shl 8) or (data[1].toInt(16) and 0xFF)
        val tubeStatus = data.slice(2..17).map { it.toInt(16) }

        return TubeStatusConfig(
            tubeFullStatus = tubeFullStatus,
            tubeStatus = tubeStatus
        )
    }


    data class TubeStatusConfig(
        val tubeFullStatus: Int,
        val tubeStatus: List<Int>
    )

    fun calculateTotalAmount(): Double {
        var totalAmount = 0.0
        if (coinConfig == null || tubeStatus == null){
            return totalAmount.toDouble()
        }
        // Iterate over each coin type
        for (i in tubeStatus!!.tubeStatus.indices) {
            val coinCount = tubeStatus!!.tubeStatus[i]
            if (i < coinConfig!!.coinCredits.size) {
                val coinValue = coinConfig!!.coinCredits[i] * coinConfig!!.scalingFactor
                totalAmount += coinValue * coinCount
            }
        }

        // Adjust for decimal places
        return totalAmount / Math.pow(10.0, coinConfig!!.decimalPlaces.toDouble())
    }

    fun getProvideChangeDebugInfo():String{
        if (coinConfig == null || tubeStatus == null){
            Log.w("BillCoinMgr","可用硬币:null")
            DLogger.log("BillCoinMgr","可用硬币:null")
            return ""
        }

        val scalingFactor = coinConfig!!.scalingFactor
        val decimalPlaces = coinConfig!!.decimalPlaces

        // Create a list of available coins with their counts
        val availableCoins = mutableListOf<Pair<Int, Int>>()
        for (i in tubeStatus!!.tubeStatus.indices) {
            if (i < coinConfig!!.coinCredits.size) {
                val coinValue = coinConfig!!.coinCredits[i] * scalingFactor
                val coinCount = tubeStatus!!.tubeStatus[i]
                if (coinValue > 0 && coinCount > 0) {
                    availableCoins.add(Pair(coinValue, coinCount))
                }
            }
        }

        // Sort available coins in descending order of value
        availableCoins.sortByDescending { it.first }
        Log.w("BillCoinMgr","可用硬币: $availableCoins")
        DLogger.log("BillCoinMgr","可用硬币: $availableCoins")
        return "硬幣總額:${calculateTotalAmount()},可用找零硬幣:${availableCoins.toString()}"
    }

    fun getCoinInfo():MutableList<Pair<Int, Int>>{
        if (coinConfig == null || tubeStatus == null){
            Log.w("BillCoinMgr","可用硬币:null")
            DLogger.log("BillCoinMgr","可用硬币:null")
            return mutableListOf<Pair<Int, Int>>()
        }

        val scalingFactor = coinConfig!!.scalingFactor
        val decimalPlaces = coinConfig!!.decimalPlaces

        // Create a list of available coins with their counts
        val availableCoins = mutableListOf<Pair<Int, Int>>()
        for (i in tubeStatus!!.tubeStatus.indices) {
            if (i < coinConfig!!.coinCredits.size) {
                val coinValue = coinConfig!!.coinCredits[i] * scalingFactor
                val coinCount = tubeStatus!!.tubeStatus[i]
                if (coinValue > 0) {
                    availableCoins.add(Pair(coinValue, coinCount))
                }
            }
        }

        // Sort available coins in descending order of value
        availableCoins.sortByDescending { it.first }
        Log.w("BillCoinMgr","可用硬币: $availableCoins")
        DLogger.log("BillCoinMgr","可用硬币: $availableCoins")
        return availableCoins
    }

    fun canProvideChange(changeAmount: Double): Boolean {
        if (coinConfig == null || tubeStatus == null){
            return false
        }
        Log.w("BillCoinMgr","检查是否可以提供找零: $changeAmount, 使用硬币配置: $coinConfig 和管状态: $tubeStatus")
        DLogger.log("BillCoinMgr","检查是否可以提供找零: $changeAmount, 使用硬币配置: $coinConfig 和管状态: $tubeStatus")
        val scalingFactor = coinConfig!!.scalingFactor
        val decimalPlaces = coinConfig!!.decimalPlaces
        val requiredAmount = (changeAmount * Math.pow(10.0, decimalPlaces.toDouble())).toInt()

        // Create a list of available coins with their counts
        val availableCoins = mutableListOf<Pair<Int, Int>>()
        for (i in tubeStatus!!.tubeStatus.indices) {
            if (i < coinConfig!!.coinCredits.size) {
                val coinValue = coinConfig!!.coinCredits[i] * scalingFactor
                val coinCount = tubeStatus!!.tubeStatus[i]
                if (coinValue > 0 && coinCount > 0) {
                    availableCoins.add(Pair(coinValue, coinCount))
                }
            }
        }

        // Sort available coins in descending order of value
        availableCoins.sortByDescending { it.first }
        Log.w("BillCoinMgr","可用硬币: $availableCoins")
        DLogger.log("BillCoinMgr","可用硬币: $availableCoins")

        var remainingAmount = requiredAmount

        // Try to provide change using available coins
        for ((coinValue, coinCount) in availableCoins) {
            val numCoinsToUse = remainingAmount / coinValue
            val coinsUsed = minOf(numCoinsToUse, coinCount)
            remainingAmount -= coinsUsed * coinValue
            Log.w("BillCoinMgr","使用了 $coinsUsed 个面值为 $coinValue 的硬币, 剩余金额: $remainingAmount")
            DLogger.log("BillCoinMgr","使用了 $coinsUsed 个面值为 $coinValue 的硬币, 剩余金额: $remainingAmount")
            if (remainingAmount <= 0) {
                Log.w("BillCoinMgr","可以提供找零。")
                return true
            }
        }

        Log.w("BillCoinMgr","无法提供找零。剩余金额: $remainingAmount")
        DLogger.log("BillCoinMgr","无法提供找零。剩余金额: $remainingAmount")
        return remainingAmount <= 0
    }

    fun parsePayoutStatus(hexData: String): Double {
        // 将HEX字符串转换为字节列表
        val asciiString = hexData.chunked(2).map { it.toInt(16).toChar() }.joinToString("")

        // Split string and filter out empty or invalid data
        val data = asciiString.split(" ").filter { it.isNotBlank() && it.all { char -> char.isDigit() || char in 'A'..'F' } }

        // 提取每种硬币的退出数量，确保不会处理空数据
        val payoutCounts = data.map { it.toInt(16) }

        var totalAmount = 0.0

        // 根据 CoinAcceptorConfig 计算总金额
        for (i in payoutCounts.indices) {
            if (i < coinConfig!!.coinCredits.size) {
                val coinValue = coinConfig!!.coinCredits[i] * coinConfig!!.scalingFactor // 硬币面值
                val coinCount = payoutCounts[i] // 退出硬币的数量
                val coinTotal = coinValue * coinCount
                totalAmount += coinTotal
                println("硬币类型: $i, 退出数量: $coinCount, 面值: $coinValue, 小计: $coinTotal")
                DLogger.log("硬币类型: $i, 退出数量: $coinCount, 面值: $coinValue, 小计: $coinTotal")
            }
        }

        // 根据小数位调整总金额
        return totalAmount / Math.pow(10.0, coinConfig!!.decimalPlaces.toDouble())
    }
    // --------------------------------------硬币器SET UP/TUBE STATUS 配置相关信息---------------------

    private val ybqLock = Any()
    private fun recordYBQAmount(amount: Int) {
        synchronized(ybqLock) {
            var recordedAmount = loadMMKVValueByKey(SettingConstants.ybq_yibiqu_amount_key)
            setMMKVValueByKey(SettingConstants.ybq_yibiqu_amount_key,recordedAmount+amount)
        }
    }

    private fun setMMKVValueByKey(key:String,amount:Int){
    }

    private fun loadMMKVValueByKey(key:String):Int{
        return 0
    }


    private val zbqLock = Any()
    fun recordBillAcceptAmount(amount: Int) {
        synchronized(zbqLock){
            when(amount){
                100->{
                    var localAcceptNum = loadMMKVValueByKey(SettingConstants.zbq_100_amount_key)
                    setMMKVValueByKey(SettingConstants.zbq_100_amount_key,localAcceptNum+1)
                }
                500->{
                    var localAcceptNum = loadMMKVValueByKey(SettingConstants.zbq_500_amount_key)
                    setMMKVValueByKey(SettingConstants.zbq_500_amount_key,localAcceptNum+1)
                }
                1000->{
                    var localAcceptNum = loadMMKVValueByKey(SettingConstants.zbq_1000_amount_key)
                    setMMKVValueByKey(SettingConstants.zbq_1000_amount_key,localAcceptNum+1)
                }
            }
        }

    }



}