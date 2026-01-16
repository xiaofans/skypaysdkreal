package com.jpgk.hardwaresdk.hardwareprotocolimpl;

import android.text.TextUtils;
import android.util.Log;

import com.jpgk.hardwaresdk.utils.DLogger;
import com.vi.vioserial.COMSerial;
import com.vi.vioserial.listener.OnComDataListener;
import com.vi.vioserial.util.SerialDataUtils;

public class CashDispenserManager implements BaseHardware {

    private static final byte STX = 0x02;  // 开始字节
    private static final byte ETX = 0x03;  // 结束字节

//    public interface OnCashDispenserDataListener{
//        void onDataResponse(String hexData);
//    }
    //private CashDispenserManager.OnCashDispenserDataListener onCashDispenserDataListener;
    private String portPath;
    private int portRate;

    private BillCoinAcceptorListener onPaymentListener;
    private OnComDataListener onComDataListener;

    private boolean isCashDispenserOnLine;
    private boolean isCashDispenserError;


    public CashDispenserManager(String portPath, int portRate, BillCoinAcceptorListener onPaymentListener){
        this.portPath = portPath;
        this.portRate = portRate;
        this.onPaymentListener = onPaymentListener;
    }



    @Override
    public int openSerialPort() {
        int state = COMSerial.instance().addCOM(portPath,portRate);
        onComDataListener = new OnComDataListener(){

            @Override
            public void comDataBack(String com, String hexData) {
                if (portPath.equals(com)){
                    isCashDispenserOnLine = true;
                    parseResponse(hexData);

                }
            }
        };
        COMSerial.instance().addDataListener(onComDataListener);
        return state;
    }

    @Override
    public void closePort() {
        COMSerial.instance().removeDataListener(onComDataListener);
        COMSerial.instance().close(portPath);
    }

    @Override
    public void reconnect() {
        closePort();
        openSerialPort();
    }

    @Override
    public boolean isSerialPortOpen() {
        return COMSerial.instance().isOpen(portPath);
    }


    @Override
    public boolean isOnLine() {
        return isCashDispenserOnLine;
    }

    @Override
    public String getHardwareName() {
        return "出鈔機";
    }

    @Override
    public int getHardwareType() {
        return BaseHardware.TYPE_CASH_DISPENSER;
    }

    public boolean isCashDispenserError(){
        return isCashDispenserError;
    }


    public void testCashDispenserConnection(){
        isCashDispenserOnLine = false;
        sendCommand(SerialDataUtils.ByteArrToHex(buildQCommand()));
    }

    public void sendCommand(String hexData){
        Log.w("CashDispenser:","send:"+hexData);
        DLogger.log("CashDispenser","send:"+hexData);
        COMSerial.instance().sendHex(portPath,hexData);
    }


    public BillCoinAcceptorListener getOnPaymentListener() {
        return onPaymentListener;
    }

    public void setOnPaymentListener(BillCoinAcceptorListener onPaymentListener) {
        this.onPaymentListener = onPaymentListener;
    }

    public OnComDataListener getOnComDataListener() {
        return onComDataListener;
    }

    public void setOnComDataListener(OnComDataListener onComDataListener) {
        this.onComDataListener = onComDataListener;
    }

    public String getPortPath() {
        return portPath;
    }

    public void setPortPath(String portPath) {
        this.portPath = portPath;
    }

    public int getPortRate() {
        return portRate;
    }

    public void setPortRate(int portRate) {
        this.portRate = portRate;
    }


    // 计算校验和：BYTE0 到 BYTE7 的和
    private byte calculateChecksum(byte[] command) {
        byte checksum = 0;
        for (int i = 0; i <= 7; i++) {  // 只计算前8个字节的和
            checksum += command[i];
        }
        return checksum;
    }

    // 构建查询状态指令
    public byte[] buildSCommand() {
        byte[] command = new byte[10];
        command[0] = 0x02; // STX 开始字节
        command[1] = '0';  // ID号的第十位 '5' 的 ASCII
        command[2] = '0';  // ID号的第一位 '2' 的 ASCII
        command[3] = 'S';   // 查询状态命令
        command[4] = '0';   // 数据字节，协议要求为'0'
        command[5] = '0';   // 数据字节
        command[6] = '0';   // 数据字节
        command[7] = '0';   // 数据字节

        // 计算校验和
        command[8] = calculateChecksum(command);

        command[9] = 0x03;  // ETX 结束字节
        return command;
    }

    public byte[] buildXCommand() {
        byte[] command = new byte[10];
        command[0] = 0x02; // STX 开始字节
        command[1] = '0'; // ID号的第十位 '5' 的 ASCII
        command[2] = '0'; // ID号的第一位 '2' 的 ASCII
        command[3] = 'X';  // 'X'指令，查询校验和
        command[4] = '0';  // 数据字节，协议要求为'0'
        command[5] = '0';  // 数据字节
        command[6] = '0';  // 数据字节
        command[7] = '0';  // 数据字节

        // 计算校验和
        command[8] = calculateChecksum(command);

        command[9] = 0x03; // ETX 结束字节
        return command;
    }

    public byte[] buildQCommand() {
        byte[] command = new byte[10];
        command[0] = 0x02; // STX 开始字节
        command[1] = '0'; // ID号的第十位 '5' 的 ASCII
        command[2] = '0'; // ID号的第一位 '2' 的 ASCII
        command[3] = 'Q';  // 'X'指令，查询校验和
        command[4] = '0';  // 数据字节，协议要求为'0'
        command[5] = '0';  // 数据字节
        command[6] = '0';  // 数据字节
        command[7] = '0';  // 数据字节

        // 计算校验和
        command[8] = calculateChecksum(command);

        command[9] = 0x03; // ETX 结束字节
        return command;
    }

    public byte[] buildBCommand(int noteCount) {
        byte[] command = new byte[10];
        command[0] = 0x02; // STX 开始字节
        command[1] = '0'; // ID号的第十位 '5' 的 ASCII
        command[2] = '0'; // ID号的第一位 '2' 的 ASCII
        command[3] = 'B';  // 'B'指令，出钞命令
        command[4] = '0';  // 固定为'0'

        // 将出钞数量分成三个字节表示
        command[5] = (byte) ((noteCount / 100) + '0');  // 100位
        command[6] = (byte) (((noteCount % 100) / 10) + '0');  // 10位
        command[7] = (byte) ((noteCount % 10) + '0');  // 1位

        // 计算校验和
        command[8] = calculateChecksum(command);

        command[9] = 0x03; // ETX 结束字节
        return command;
    }

    public byte[] buildCCommand() {
        byte[] command = new byte[10];
        command[0] = 0x02; // STX 开始字节
        command[1] = 0x35; // ID号的第十位 '5' 的 ASCII
        command[2] = 0x32; // ID号的第一位 '2' 的 ASCII
        command[3] = 'C';  // 'C'指令，查询总额命令
        command[4] = '0';  // 固定为'0'
        command[5] = '0';  // 固定为'0'
        command[6] = '0';  // 固定为'0'
        command[7] = '0';  // 固定为'0'

        // 计算校验和
        command[8] = calculateChecksum(command);

        command[9] = 0x03; // ETX 结束字节
        return command;
    }

    // 构建 E 指令的方法


    public byte[] buildECommand() {
        byte[] command = new byte[10];
        command[0] = 0x02; // STX 开始字节
        command[1] = '0'; // ID号的第十位 '5' 的 ASCII
        command[2] = '0'; // ID号的第一位 '2' 的 ASCII
        command[3] = 'E';  // 'E' 指令，查询错误状态
        command[4] = '0';  // 固定为'0'
        command[5] = '0';  // 固定为'0'
        command[6] = '0';  // 固定为'0'
        command[7] = '0';  // 固定为'0'

        // 计算校验和
        command[8] = calculateChecksum(command);

        command[9] = 0x03; // ETX 结束字节
        return command;
    }


    // 解析从设备接收到的响应数据
    public void parseResponse(String hexData) {
        Log.w("CashDispenser:","RECV:"+hexData);
        DLogger.log("CashDispenser","RECV:"+hexData);
        // 检查是否为空或长度不足
        if (hexData == null || hexData.length() < 20) {
            System.out.println("Invalid response data");
            return;
        }

        // 判断是否以 "0A" 开头 去除开头换行20250728 发现带异常换行符
        if (hexData.startsWith("0A")) {
            hexData = hexData.substring(2);  // 去除前两个字符（1个字节）
        }
        // 转换 hexData 为 ASCII 字符串
        String asciiString = hexToAscii(hexData);
        System.out.println("ASCII Response: " + asciiString);
        DLogger.log("CashDispenser","ASCII Response: " + asciiString);

        // 解析各个字段
        char stx = asciiString.charAt(0);  // STX
        char id10 = asciiString.charAt(1); // ID 10th digit
        char id1 = asciiString.charAt(2);  // ID 1st digit
        char cmd = asciiString.charAt(3);  // 命令字 (例如 'E', 'S', 'C')
        String data = asciiString.substring(4, 8);  // 数据字段
        char checksum = asciiString.charAt(8);  // 校验和
        char etx = asciiString.charAt(9);  // ETX

        // 打印解析结果
        System.out.println("STX: " + stx);
        System.out.println("ID 10th: " + id10);
        System.out.println("ID 1st: " + id1);
        System.out.println("Command: " + cmd);
        System.out.println("Data: " + data);
        System.out.println("Checksum: " + checksum);
        System.out.println("ETX: " + etx);

        // 校验和验证
        if (!verifyChecksum(hexData, checksum)) {
            System.out.println("Checksum mismatch!");
            DLogger.log("CashDispenser","Checksum mismatch!");
            return;
        }

        // 根据命令进一步处理
        switch (cmd) {
            case 's':
                System.out.println('s');
                char s1 = data.charAt(0);
                char s2 = data.charAt(1);
                String errorMsg = "";
                switch (s1){
                    case 'w':
                        Log.w("CashDispenser:","出鈔機繁忙");
                        errorMsg = "出鈔機繁忙";
                        break;
                    case 'r':
                        Log.w("CashDispenser:","出钞机可用");
                        break;
                    case 'e':
                        Log.w("CashDispenser:","错误");
                        errorMsg = parseErrorCode(s2);
                        break;
                    case 't':
                        Log.w("CashDispenser:","测试模式");
                        errorMsg = "出鈔機處於測試模式";
                        break;

                }
                if (!TextUtils.isEmpty(errorMsg)){
                    isCashDispenserError = true;
                }
                if (onPaymentListener != null){
                    onPaymentListener.onCashDispenserStatus(String.valueOf(s1),String.valueOf(s2),errorMsg);
                }
                break;
            case 'b':
                Log.w("cashDispenser:","b");
                DLogger.log("CashDispenser","b");
                // 计算实际出钞数量
                int totalNotes = calculateTotalDispensedAmount(asciiString);
                Log.w("CashDispenser","实际出钞金额:"+totalNotes);
                DLogger.log("CashDispenser","实际出钞金额:"+totalNotes);
                if (onPaymentListener != null){
                    onPaymentListener.onCashDispensedSucc(totalNotes);
                }
                break;
            case 'E':
                int dispensedNotes = calculateTotalDispensedAmount(asciiString);
                char es2 = data.charAt(0);
                String errorMsg2 = parseErrorCode(es2);
                Log.w("CashDispenser:","E:已出钞数量"+dispensedNotes+" 错误信息："+errorMsg2);
                DLogger.log("CashDispenser","E:已出钞数量"+dispensedNotes+" 错误信息："+errorMsg2);
                if (onPaymentListener != null){
                    onPaymentListener.onCashDispensingError(String.valueOf(es2),errorMsg2,dispensedNotes);
                }
                break;

        }
    }

    private int calculateTotalDispensedAmount(String asciiString){
        int hundredPlace = asciiString.charAt(5) - '0';  // 100th 百位
        int tenPlace = asciiString.charAt(6) - '0';  // 10th 十位
        int onePlace = asciiString.charAt(7) - '0';  // 1st 个位
        // 计算实际出钞数量
        int totalNotes = hundredPlace * 100 + tenPlace * 10 + onePlace;
        return totalNotes * 100;
    }

    // 将 hex 转换为 ASCII 字符串
    private String hexToAscii(String hexData) {
        StringBuilder asciiString = new StringBuilder();
        for (int i = 0; i < hexData.length(); i += 2) {
            String hexPair = hexData.substring(i, i + 2); // 每两个字符作为一个hex pair
            char asciiChar = (char) Integer.parseInt(hexPair, 16); // 转换为ASCII字符
            asciiString.append(asciiChar);
        }
        return asciiString.toString();
    }

    // 校验和验证方法：校验和是 BYTE0 + BYTE1 + ... + BYTE7 的和
    private boolean verifyChecksum(String hexData, char checksum) {
        int sum = 0;
        for (int i = 0; i < 16; i += 2) { // 计算BYTE0到BYTE7
            sum += Integer.parseInt(hexData.substring(i, i + 2), 16);  // 每次取2个字符作为一个字节累加
        }
        int calculatedChecksum = sum & 0xFF;  // 取低8位

        // 打印实际的计算值和接收到的checksum值
        System.out.println("Calculated checksum: " + String.format("%02X", calculatedChecksum));
        System.out.println("Received checksum: " + String.format("%02X", (int) checksum));

        // 比较计算出的校验和与接收到的校验和
        return calculatedChecksum == (int) checksum;
    }

    // 解析错误码
    /**
     * 1: 没有纸币被出钞（由主机发出指令时没有纸币）
     * 2: 卡钞
     * 3: 链条错误
     * 4: 中途卡钞（纸币卡在一半）
     * 5: 出钞数量不足（Short count）
     * 6: 没有纸币被出钞（由开始按钮发出指令时没有纸币）
     * 7: 双张错误（一次出钞两张）
     * 8: 出钞数量超出4000张的限制
     * 9: 通信测试中接收错误
     * A: 编码器错误
     * B: 红外线左侧LED错误
     * C: 红外线右侧LED错误
     * D: 红外线左侧传感器错误
     * F: 红外线右侧传感器错误
     * G: 红外差异错误（红外传感器检测到不一致）
     * H: 纸币低位警告（BillLowLevel）
     * I: 低电压错误（Low power error）
     */
    private String parseErrorCode(char data) {
        String errorMesg = "";
        switch (data) {
            case '1':
                errorMesg = "沒有紙幣被出鈔（由主機發出指令時沒有紙幣）";
                break;
            case '2':
                errorMesg = "卡鈔";
                break;
            case '3':
                errorMesg = "鏈條错误";
                break;
            case '4':
                errorMesg = "中途卡鈔（紙鈔卡在一半）";
                break;
            case '5':
                errorMesg = "出鈔數量不足（Short count）";
                break;
            case '6':
                errorMesg = "沒有紙幣被出鈔（由開始按鈕發出指令時沒有紙幣）";
                break;
            case '7':
                errorMesg = "雙張錯誤（一次出鈔兩張）";
                break;
            case '8':
                errorMesg = "出鈔數量超出4000張的限制";
                break;
            case '9':
                errorMesg = "通訊測試中接收錯誤";
                break;
            case 'A':
                errorMesg = "編碼器錯誤";
                break;
            case 'B':
                errorMesg = "红外线左侧LED错误";
                break;
            case 'C':
                errorMesg = "紅外線右側LED錯誤";
                break;
            case 'D':
                errorMesg = "红外线左侧传感器错误";
                break;
            case 'F':
                errorMesg = "紅外線右側LED錯誤";
                break;
            case 'G':
                errorMesg = "紅外線差異錯誤（紅外線感測器偵測到不一致）";
                break;
            case 'H':
                errorMesg = "紙幣低位警告（BillLowLevel）";
                break;
            case 'I':
                errorMesg = "低電壓錯誤（Low power error）";
                break;
            default:
                System.out.println("Unknown error code");
                break;
        }
        Log.w("CashDispenser:",errorMesg);
        return errorMesg;
    }


    /**
     * 发送指令的方法
     * @param command 命令字 (I, B, K, S, C, E等)
     * @param data1 数据1
     * @param data2 数据2
     * @param data3 数据3
     * @param data4 数据4
     */
    public void sendCommand(char command, char data1, char data2, char data3, char data4) {
        // 构建指令
        byte[] cmd = new byte[10];
        cmd[0] = 0x02; // STX
        cmd[1] = '0'; // ID_10
        cmd[2] = '0'; // ID_1
        cmd[3] = (byte) command; // CMD
        cmd[4] = (byte) data1; // DATA1
        cmd[5] = (byte) data2; // DATA2
        cmd[6] = (byte) data3; // DATA3
        cmd[7] = (byte) data4; // DATA4

        // 计算校验和（BYTE0 到 BYTE7 的和，取低8位）
        cmd[8] = calculateChecksum(cmd);

        cmd[9] = 0x03; // ETX

        // 发送指令
        sendCommand(byteArrayToHex(cmd));
        System.out.println("Command sent: " + byteArrayToHex(cmd));
    }

    // 发送清除指令
    public void sendICommand(char clearKind){
        sendCommand('I','0','0','0',clearKind);
    }


    // 将字节数组转换为十六进制字符串表示，便于日志输出
    private String byteArrayToHex(byte[] byteArray) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : byteArray) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString();
    }

}
