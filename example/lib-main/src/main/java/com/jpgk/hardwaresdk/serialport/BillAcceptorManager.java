package com.jpgk.hardwaresdk.serialport;

import android.util.Log;

public class BillAcceptorManager implements PaymentDeviceConstant{
    private SerialPortManager serialPortManager;
    private BillAcceptorListener listener;

    public BillAcceptorManager(SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
    }

    // 设置回调监听器
    public void setBillAcceptorListener(BillAcceptorListener listener) {
        this.listener = listener;
    }

    // 启用纸币接收器
    public void enableBillAcceptor(boolean enableEscrow) {
        // 构建启用命令
        byte[] enableCommand = new byte[]{
                MDB_BILL_ENABLE, // MDB_BILL_ENABLE
                (byte) 0x00, // 第二字节，通常是保留字节，设为0
                (byte) 0x0F, // 启用纸币面额，启用所有面额
                (byte) 0x00, // 保留字节，设为0
                (byte) (enableEscrow ? 0x0F : 0x00) // 根据 enableEscrow 设置暂存功能
        };
        serialPortManager.sendCommand(enableCommand);
        if (listener != null) {
            listener.onBillAcceptorEnabled(enableEscrow);
        }
    }

    // 禁用纸币接收器
    public void disableBillAcceptor() {
        byte[] disableCommand = new byte[]{
                (byte) MDB_BILL_ENABLE,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00,
                (byte) 0x00
        };
        serialPortManager.sendCommand(disableCommand);
        if (listener != null) {
            listener.onBillAcceptorDisabled();
        }
    }

    //纸币存入到钱箱
    public void pushBillIn(){
        byte[] putBillInCommand = new byte[]{MDB_BILL_ESCROW,(byte)0x01}; // 0F02命令：退还硬币
        serialPortManager.sendCommand(putBillInCommand);
        Log.d("PaymentDevice", "发送存入纸币命令");
    }

    //纸币从暂存区退出
    public void pollBillOut(){
        byte[] putBillInCommand = new byte[]{MDB_BILL_ESCROW,(byte)0x00}; // 0F02命令：退还硬币
        serialPortManager.sendCommand(putBillInCommand);
        Log.d("PaymentDevice", "发送纸币从暂存区弹出命令");
    }


    // 退还纸币（退回指定金额的纸币）80 > 100 81 > 200 82 -> 500 83 -> 1000
    public void returnBillAuto() {
        byte[] returnCoinCommand = new byte[]{MDB_BILL_ESCROW,(byte)0x00}; // 0F02命令：退还硬币
        serialPortManager.sendCommand(returnCoinCommand);
        Log.d("PaymentDevice", "发送退还硬币命令：自动");
    }

    // 执行退纸币操作
    public void returnBill(int type) {
        byte[] payOutCommand = new byte[]{
                MDB_BILL_EXPANSION,                      // 扩展命令
                MDB_BILL_ESCROW,        // 退纸币命令
                (byte) type,                             // 纸币类型（通道）
                (byte) 0x00,                             // 保留字节，设为0
                (byte) 0x01                              // 退还1张纸币
        };

        // 发送命令到纸币器
        serialPortManager.sendCommand(payOutCommand);

        // 通知监听器退纸币操作已执行（可以添加回调逻辑）
        if (listener != null) {
            listener.onBillDispensed(type, 1); // 回调通知，退还的纸币类型和数量
        }
    }

    // 处理纸币接收器的响应
    public void handleResponse(byte[] data) {
        if (data[0] == (byte) 0x33 && listener != null) {
            int amount = 0;
            Log.d("PaymentDevice", "纸币接收器响应 (原始字节): " + SerialPortUtils.bytesToHex(data));

            // 确保数据长度足够长，以便检测模式
            if (data.length < 8) {
                Log.e("PaymentDevice", "接收到的数据长度不足，无法解析: " + data.length);
                return;
            }
            // 搜索整个字节数组，找到与硬币面额模式匹配的部分
            // 100元纸币模式: 33 30 20 38 30 20 30
            if (containsPattern(data, new byte[]{(byte) 0x33, (byte) 0x30, (byte) 0x20, (byte) 0x38, (byte) 0x30, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 100 元纸币");
                //handleTransaction(10.0);  // 记录100元纸币交易
                amount = 100;
                //200元纸币模式: 33 30 20 38 31 20 30
            }else if (containsPattern(data, new byte[]{(byte) 0x33, (byte) 0x30, (byte) 0x20, (byte) 0x38, (byte) 0x31, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 200 元纸币");
                //handleTransaction(10.0);  // 记录20元硬币交易
                amount = 200;
            }
            // 500元纸币模式: 33 30 20 38 32 20 30
        else if (containsPattern(data, new byte[]{(byte) 0x33, (byte) 0x30, (byte) 0x20, (byte) 0x38, (byte) 0x32, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 500 元纸币");
                //handleTransaction(1.0);  // 记录1元硬币交易
                amount = 500;
            }
            // 1000元纸币模式: 30 38 20 35 33 20 30
            else if (containsPattern(data, new byte[]{(byte) 0x33, (byte) 0x30, (byte) 0x20, (byte) 0x38, (byte) 0x33, (byte) 0x20, (byte) 0x30})) {
                Log.d("PaymentDevice", "接收到 1000 元纸币");
//                handleTransaction(5.0);  // 记录5元硬币交易
                amount = 1000;
            } else {
                Log.d("PaymentDevice", "未知的纸币响应数据: " + SerialPortUtils.bytesToHex(data));
            }
            listener.onBillReceived(amount);  // 通知监听器收到硬币
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

