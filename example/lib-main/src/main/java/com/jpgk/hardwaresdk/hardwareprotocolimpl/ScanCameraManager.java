package com.jpgk.hardwaresdk.hardwareprotocolimpl;

import android.util.Log;

import com.vi.vioserial.COMSerial;
import com.vi.vioserial.NormalSerial;
import com.vi.vioserial.listener.OnComDataListener;
import com.vi.vioserial.util.SerialDataUtils;

//NLS-EM20-80 反扫器
public class ScanCameraManager implements BaseHardware {


    public interface OnCameraDataListener{
        void onDataResponse(String hexData);
    }
    private OnCameraDataListener onCameraDataListener;
    private String portPath;
    private int portRate;

    private OnComDataListener onComDataListener;

    private Boolean isScanCameraOnLine = false;

    public ScanCameraManager(String portPath, int portRate, OnCameraDataListener onCameraDataListener){
        this.portPath = portPath;
        this.portRate = portRate;
        this.onCameraDataListener = onCameraDataListener;
    }

    @Override
    public int openSerialPort() {
        int state = COMSerial.instance().addCOM(portPath,portRate);
        onComDataListener = new OnComDataListener(){

            @Override
            public void comDataBack(String com, String hexData) {
                if (portPath.equals(com)){
                    isScanCameraOnLine = true;
                    if(onCameraDataListener != null){
                        onCameraDataListener.onDataResponse(hexData);
                    }
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
        return isScanCameraOnLine;
    }

    @Override
    public String getHardwareName() {
        return "反掃器";
    }

    @Override
    public int getHardwareType() {
        return BaseHardware.TYPE_SCAN_CAMERA;
    }

    public void enableScan(){
        String commandStr = "~<SOH>0000@SCNENA1;<ETX>";
        byte[] command = convertCommandToHex(commandStr);
        Log.w("PaymentDevice","port is open:"+NormalSerial.instance().isOpen());
        Log.d("PaymentDevice", "发送指令:"+SerialDataUtils.ByteArrToHex(command));
//        NormalSerial.instance().sendHex(SerialDataUtils.ByteArrToHex(command));
        COMSerial.instance().sendHex(portPath,SerialDataUtils.ByteArrToHex(command));
    }

    public void disableSan(){
        String commandStr = "~<SOH>0000@SCNENA0;<ETX>";
        byte[] command = convertCommandToHex(commandStr);
        Log.w("PaymentDevice","port is open:"+NormalSerial.instance().isOpen());
        Log.d("PaymentDevice", "发送指令:"+SerialDataUtils.ByteArrToHex(command));
//        NormalSerial.instance().sendHex(SerialDataUtils.ByteArrToHex(command));
        COMSerial.instance().sendHex(portPath,SerialDataUtils.ByteArrToHex(command));
    }


    public void testScanCameraConnection(){
        String commandStr = "~<SOH>0000@C11*;<ETX>";
        byte[] command = convertCommandToHex(commandStr);
        Log.w("PaymentDevice","port is open:"+NormalSerial.instance().isOpen());
        Log.d("PaymentDevice", "发送指令:"+SerialDataUtils.ByteArrToHex(command));
//        NormalSerial.instance().sendHex(SerialDataUtils.ByteArrToHex(command));
        COMSerial.instance().sendHex(portPath,SerialDataUtils.ByteArrToHex(command));
    }


    public  byte[] convertCommandToHex(String command) {
        StringBuilder hexCommand = new StringBuilder();

        // 遍历每个字符并转换为HEX值
        for (int i = 0; i < command.length(); i++) {
            char c = command.charAt(i);
            if (c == '~') {
                hexCommand.append("7E ");
            } else if (command.startsWith("<SOH>", i)) {
                hexCommand.append("01 ");
                i += 4; // 跳过"<SOH>"
            } else if (command.startsWith("<ETX>", i)) {
                hexCommand.append("03 ");
                i += 4; // 跳过"<ETX>"
            } else {
                // 将字符转换为对应的HEX值
                hexCommand.append(String.format("%02X ", (int) c));
            }
        }

        // 去掉最后一个空格
        String hexString = hexCommand.toString().trim();

        // 打印转换后的HEX字符串用于调试
        System.out.println("Converted HEX String: " + hexString);

        // 将字符串转换为字节数组
        String[] hexArray = hexString.split(" ");
        byte[] hexBytes = new byte[hexArray.length];
        for (int i = 0; i < hexArray.length; i++) {
            hexBytes[i] = (byte) Integer.parseInt(hexArray[i], 16);
        }

        return hexBytes;
    }

    public  String convertHexToString(byte[] hexBytes) {
        StringBuilder result = new StringBuilder();

        // 遍历每个字节，将其转为相应的字符
        for (byte hexByte : hexBytes) {
            // 将每个字节转回字符
            int unsignedByte = hexByte & 0xFF;  // 将 byte 转为无符号整型
            result.append((char) unsignedByte);  // 将无符号整型转为字符
        }

        return result.toString();
    }

    public OnCameraDataListener getOnCameraDataListener() {
        return onCameraDataListener;
    }

    public void setOnCameraDataListener(OnCameraDataListener onCameraDataListener) {
        this.onCameraDataListener = onCameraDataListener;
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
}
