package com.jpgk.hardwaresdk.serialport;

import android.util.Log;

public class PaymentDeviceManagerV0 {
    private SerialPortManagerV0 serialPortManager;
    private double totalAmount = 0.0;
    private final int MAX_RETRY_COUNT = 5; // 最大重试次数

    public PaymentDeviceManagerV0(SerialPortManagerV0 manager) {
        this.serialPortManager = manager;
    }

    // 启用纸币接收器
    public void enableBillAcceptor(boolean enableEscrow) {
        // 构建启用命令
        byte[] enableCommand = new byte[]{
                (byte) 0x34, // MDB_BILL_ENABLE
                (byte) 0x00, // 第二字节，通常是保留字节，设为0
                (byte) 0x0F, // 启用纸币面额，启用所有面额
                (byte) 0x00, // 保留字节，设为0
                (byte) (enableEscrow ? 0x0F : 0x00) // 根据 enableEscrow 设置暂存功能
        };

        // 发送命令到纸币接收器
        serialPortManager.sendCommand(enableCommand);
        Log.d("PaymentDevice", "已发送启用纸币接收器的命令，Escrow 状态: " + (enableEscrow ? "启用" : "禁用"));
    }

    // 启用硬币接收器
    public void enableCoinAcceptor() {
//        byte[] enableCoinCommand = new byte[]{(byte) 0x09};
//        serialPortManager.sendCommand(enableCoinCommand);
        byte[] enableBillCommand = new byte[]{(byte) 0x0C, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        serialPortManager.sendCommand(enableBillCommand);
    }

    // 处理重连逻辑
    public void handleReconnect() {
        int retryCount = 0; // 初始化重试计数

        while (!serialPortManager.isConnected && retryCount < MAX_RETRY_COUNT) {
            Log.d("PaymentDevice", "尝试重新连接: 尝试次数 " + (retryCount + 1));
            try {
                serialPortManager.init("/dev/ttyS4", 9600); // 重新初始化串口
                enableBillAcceptor(false); // 重新启用纸币接收器
                enableCoinAcceptor(); // 重新启用硬币接收器
                Log.d("PaymentDevice", "重新连接成功");
            } catch (Exception e) {
                retryCount++;
                Log.d("PaymentDevice", "连接失败: " + e.getMessage());
                try {
                    Thread.sleep(3000); // 等待 3 秒后重试
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        }

        if (!serialPortManager.isConnected) {
            Log.e("PaymentDevice", "达到最大重试次数，无法连接设备");
        }
    }

    // 检查连接状态，如果断开则重连
    private boolean checkConnection() {
        if (!serialPortManager.isConnected) {
            Log.d("PaymentDevice", "检测到连接断开，开始重连...");
            handleReconnect(); // 处理重连
        }
        return serialPortManager.isConnected;
    }

    // 退回纸币（退回到客户）
    public void returnBill() {
        if (checkConnection()) {
            byte[] returnBillCommand = new byte[]{(byte) 0x35, (byte) 0x00}; // 3500h 命令: 退回纸币
            serialPortManager.sendCommand(returnBillCommand);
            Log.d("PaymentDevice", "发送退纸币命令");
        }
    }


    // 接收纸币（存入现金箱）
    public void acceptBill() {
        if (checkConnection()) {
            byte[] acceptBillCommand = new byte[]{(byte) 0x35, (byte) 0x01}; // 3501h 命令: 存入现金箱
            serialPortManager.sendCommand(acceptBillCommand);
            Log.d("PaymentDevice", "发送存纸币命令");
        }
    }

    // 退还硬币（退回指定金额的硬币）
    public void returnCoin(byte coinType) {
        if (checkConnection()) {
            byte[] returnCoinCommand = new byte[]{(byte) 0x0F,coinType,(byte) 0x50}; // 0F02命令：退还硬币
            serialPortManager.sendCommand(returnCoinCommand);
            Log.d("PaymentDevice", "发送退还硬币命令，类型：" + coinType);
        }
    }

    // 解析响应数据
    public void parseResponse(byte[] data) {
        if (data == null || data.length == 0) {
            Log.e("PaymentDevice", "没有接收到数据");
            return;
        }
        String d = bytesToHex(data);
        String d1 = "33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A ";
        String d2 = "33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A 33 30 20 30 39 0D 0A ";
        if (d1.equals(d) || d2.equals(d)){
            return;
        }

        Log.d("PaymentDevice", "解析响应数据: " + bytesToHex(data));

        handleCoinAcceptorResponse(data);

//        // 确定响应类型
//        if (data[0] == (byte) 0x30) {
//            // 纸币接收器响应
//            handleBillAcceptorResponse(data);
//        } else if (data[0] == (byte) 0x08) {
//            // 硬币接收器响应
//            handleCoinAcceptorResponse(data);
//        }  else {
//            // 处理其他未知响应
//            Log.w("PaymentDevice", "未知响应类型: " + data[0]);
//        }

        // 检查连接状态
        if (!serialPortManager.isConnected) {
            Log.e("PaymentDevice", "检测到连接断开，开始重连...");
            handleReconnect(); // 尝试重连
        }
    }


    private void handleBillAcceptorResponse(byte[] data) {
        Log.d("PaymentDevice", "纸币接收器响应: " + bytesToHex(data));

        switch (data[1]) {
            case (byte) 0x80:
                Log.d("PaymentDevice", "接收了 100 元台币");
                handleTransaction(100.0);
                break;
            case (byte) 0x81:
                Log.d("PaymentDevice", "接收了 200 元台币");
                handleTransaction(200.0);
                break;
            case (byte) 0xA1:
                Log.d("PaymentDevice", "纸币被强制移除");
                break;
            case (byte) 0x01:
                Log.d("PaymentDevice", "纸币接收器故障: 马达问题");
                break;
            case (byte) 0x09:
                Log.d("PaymentDevice", "纸币接收器被禁用");
                break;
            default:
                Log.d("PaymentDevice", "未知响应: " + bytesToHex(data));
                break;
        }
    }

    private void handleCoinAcceptorResponse(byte[] data) {
        Log.d("PaymentDevice", "硬币接收器响应 (原始字节): " + bytesToHex(data));

        // 确保数据长度足够长，以便检测模式
        if (data.length < 8) {
            Log.e("PaymentDevice", "接收到的数据长度不足，无法解析: " + data.length);
            return;
        }

        // 搜索整个字节数组，找到与硬币面额模式匹配的部分
        // 10元硬币模式: 30 38 20 35 32 20 30
        if (containsPattern(data, new byte[]{(byte) 0x30, (byte) 0x38, (byte) 0x20, (byte) 0x35, (byte) 0x32, (byte) 0x20, (byte) 0x30})) {
            Log.d("PaymentDevice", "接收到 10 元硬币");
            handleTransaction(10.0);  // 记录10元硬币交易
        }
        // 1元硬币模式: 30 38 20 35 30 20 30
        else if (containsPattern(data, new byte[]{(byte) 0x30, (byte) 0x38, (byte) 0x20, (byte) 0x35, (byte) 0x30, (byte) 0x20, (byte) 0x30})) {
            Log.d("PaymentDevice", "接收到 1 元硬币");
            handleTransaction(1.0);  // 记录1元硬币交易
        }
        // 5元硬币模式: 30 38 20 35 31 20 30
        else if (containsPattern(data, new byte[]{(byte) 0x30, (byte) 0x38, (byte) 0x20, (byte) 0x35, (byte) 0x31, (byte) 0x20, (byte) 0x30})) {
            Log.d("PaymentDevice", "接收到 5 元硬币");
            handleTransaction(5.0);  // 记录5元硬币交易
        } else {
            Log.d("PaymentDevice", "未知的硬币响应数据: " + bytesToHex(data));
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



    // 处理交易，累加金额
    private void handleTransaction(double amount) {
        totalAmount += amount;
        Log.d("PaymentDevice", "当前交易总额: $" + totalAmount);
    }

    // 将字节数组转换为十六进制字符串
    public String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }
}
