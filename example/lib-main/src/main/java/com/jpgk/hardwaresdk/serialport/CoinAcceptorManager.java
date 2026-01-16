package com.jpgk.hardwaresdk.serialport;

import android.util.Log;

public class CoinAcceptorManager implements PaymentDeviceConstant{
    private SerialPortManager serialPortManager;
    private CoinAcceptorListener listener;

    // 构造函数
    public CoinAcceptorManager(SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    // 设置回调监听器
    public void setCoinAcceptorListener(CoinAcceptorListener listener) {
        this.listener = listener;
    }

    // 启用硬币接收器
    public void enableCoinAcceptor() {
        byte[] enableCommand = new byte[]{MDB_COIN_ENABLE, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        serialPortManager.sendCommand(enableCommand);
        if (listener != null) {
            listener.onCoinAcceptorEnabled();
        }
    }

    // 禁用硬币接收器
    public void disableCoinAcceptor() {
        byte[] disableCommand = new byte[]{(byte) 0x0C, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        serialPortManager.sendCommand(disableCommand);
        if (listener != null) {
            listener.onCoinAcceptorDisabled();
        }
    }
    // 退还硬币（退回指定金额的硬币）50 > 1 51 > 5 52 -> 10 53 -> 50
    public void returnCoinAuto() {
        byte[] returnCoinCommand = new byte[]{MDB_COIN_PAYOUT,(byte)0x02,0x50}; // 0F02命令：退还硬币
        serialPortManager.sendCommand(returnCoinCommand);
        Log.d("PaymentDevice", "发送退还硬币命令：自动");
    }

    // 退还硬币（退回指定金额的硬币）50 > 1 51 > 5 52 -> 10 53 -> 50
    public void returnCoin(byte count) {
        byte[] returnCoinCommand = new byte[]{(byte) 0x0F,(byte)0x02,count}; // 0F02命令：退还硬币
        serialPortManager.sendCommand(returnCoinCommand);
        Log.d("PaymentDevice", "发送退还硬币命令：自动");
    }

    // 处理硬币接收器的响应
    public void handleResponse(byte[] data) {
        if (data[0] == (byte) 0x30 && listener != null) {
            int amount = 0;
            Log.d("PaymentDevice", "硬币接收器响应 (原始字节): " + SerialPortUtils.bytesToHex(data));

            // 确保数据长度足够长，以便检测模式
            if (data.length < 8) {
                Log.e("PaymentDevice", "接收到的数据长度不足，无法解析: " + data.length);
                return;
            }
            // 搜索整个字节数组，找到与硬币面额模式匹配的部分
            // 50元硬币模式: 30 38 20 35 33 20 30
            if (containsPattern(data, new byte[]{(byte) 0x30, (byte) 0x38, (byte) 0x20, (byte) 0x35, (byte) 0x33, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 50 元硬币");
                //handleTransaction(10.0);  // 记录50元硬币交易
                amount = 50;
            }
            // 10元硬币模式: 30 38 20 35 32 20 30
            if (containsPattern(data, new byte[]{(byte) 0x30, (byte) 0x38, (byte) 0x20, (byte) 0x35, (byte) 0x32, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 10 元硬币");
                //handleTransaction(10.0);  // 记录10元硬币交易
                amount = 10;
            }
            // 1元硬币模式: 30 38 20 35 30 20 30
            else if (containsPattern(data, new byte[]{(byte) 0x30, (byte) 0x38, (byte) 0x20, (byte) 0x35, (byte) 0x30, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 1 元硬币");
                //handleTransaction(1.0);  // 记录1元硬币交易
                amount = 1;
            }
            // 5元硬币模式: 30 38 20 35 31 20 30
            else if (containsPattern(data, new byte[]{(byte) 0x30, (byte) 0x38, (byte) 0x20, (byte) 0x35, (byte) 0x31, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 5 元硬币");
//                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 5;
            } else {
                Log.d("PaymentDevice", "未知的硬币响应数据: " + SerialPortUtils.bytesToHex(data));
            }
            listener.onCoinReceived(amount);  // 通知监听器收到硬币
        }
    }

    // 在数据中查找模式，返回是否找到匹配模式
    private boolean containsPattern(byte[] data, byte[] pattern) {
        // 遍历整个字节数组，尝试找到与 pattern 匹配的部分
        for (int i = 0; i <= data.length - pattern.length; i++) {
            boolean match = true;
            for (int j = 0; j < pattern.length; j++) {
                if (data[i + j] != pattern[j]) {
                    match = false;
                    break;
                }
            }
            if (match) {
                return true; // 找到匹配的模式
            }
        }
        return false; // 未找到匹配的模式
    }
}
