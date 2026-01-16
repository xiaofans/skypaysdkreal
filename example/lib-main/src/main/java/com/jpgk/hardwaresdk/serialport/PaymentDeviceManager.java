package com.jpgk.hardwaresdk.serialport;

import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

public class PaymentDeviceManager implements PaymentDeviceConstant{
    private SerialPortManager serialPortManager;
    private BillAcceptorManager billAcceptorManager;
    private CoinAcceptorManager coinAcceptorManager;
//    private HandlerThread handlerThread;
//    private Handler handler;
    private boolean isConnected = false; // 连接状态标志

    // Handler 和线程用于发送命令和管理连接
    private HandlerThread commandHandlerThread;
    private Handler commandHandler;

    // Handler 和线程用于读取响应数据
    private HandlerThread responseHandlerThread;
    private Handler responseHandler;

    public PaymentDeviceManager(SerialPortManager serialPortManager) {
        this.serialPortManager = serialPortManager;
        billAcceptorManager = new BillAcceptorManager(serialPortManager);
        coinAcceptorManager = new CoinAcceptorManager(serialPortManager);

        // 初始化 HandlerThread 用于处理命令
        commandHandlerThread = new HandlerThread("PaymentCommandThread");
        commandHandlerThread.start();
        commandHandler = new Handler(commandHandlerThread.getLooper());

        // 初始化 HandlerThread 用于处理响应读取
        responseHandlerThread = new HandlerThread("PaymentResponseThread");
        responseHandlerThread.start();
        responseHandler = new Handler(responseHandlerThread.getLooper());
    }

    public void setBillAcceptorListener(BillAcceptorListener listener) {
        billAcceptorManager.setBillAcceptorListener(listener);
    }

    public void setCoinAcceptorListener(CoinAcceptorListener listener) {
        coinAcceptorManager.setCoinAcceptorListener(listener);
    }

    // 启动连接
    public void startConnection() {
        commandHandler.post(() -> {
            if (!isConnected) {
                if (serialPortManager.connect()) {
                    isConnected = true;
                    Log.d("PaymentDeviceManager", "设备连接成功");
                    enableBillAcceptor(true);
                    enableCoinAcceptor();
                    startReadingResponse(); // 开始读取响应数据
                } else {
                    Log.e("PaymentDeviceManager", "设备连接失败，尝试重连...");
                    attemptReconnect();
                }
            }
        });
    }

    // 读取响应数据
    private void startReadingResponse() {
        responseHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isConnected) {
                    byte[] data = serialPortManager.receiveData(); // 从串口读取数据
                    if (data != null) {
//                        Log.e("响应数据:",SerialPortUtils.bytesToHex(data) );
                        // 根据设备类型处理响应
                        if (isBillData(data)) {
                            billAcceptorManager.handleResponse(data);
                        } else if (isCoinData(data)) {
                            coinAcceptorManager.handleResponse(data);
                        }
                    }
                    // 继续读取
                    startReadingResponse();
                }
            }
        },1000);
    }

    // 判断是否为纸币数据
    private boolean isBillData(byte[] data) {
        // 根据协议判断数据是否为纸币响应
        return data[0] == MDB_BILL_HEAD; // 示例判断
    }

    // 判断是否为硬币数据
    private boolean isCoinData(byte[] data) {
        // 根据协议判断数据是否为硬币响应
        return data[0] == MDB_COIN_HEAD; // 示例判断
    }

    // 尝试重连
    private void attemptReconnect() {
        commandHandler.postDelayed(() -> {
            Log.d("PaymentDeviceManager", "正在尝试重连...");
            if (serialPortManager.connect()) {
                isConnected = true;
                Log.d("PaymentDeviceManager", "设备重连成功");
                startReadingResponse(); // 重连后开始读取响应数据
            } else {
                Log.e("PaymentDeviceManager", "设备重连失败，继续尝试...");
                attemptReconnect(); // 继续尝试重连
            }
        }, 5000); // 每隔5秒尝试重连
    }

    // 启用纸币器
    public void enableBillAcceptor(final boolean enableEscrow) {
        commandHandler.post(() -> {
            if (isConnected) {
                billAcceptorManager.enableBillAcceptor(enableEscrow);
            } else {
                Log.e("PaymentDeviceManager", "设备未连接，无法启用纸币接收器");
            }
        });
    }

    // 启用硬币器
    public void enableCoinAcceptor() {
        commandHandler.post(() -> {
            if (isConnected) {
                coinAcceptorManager.enableCoinAcceptor();
            } else {
                Log.e("PaymentDeviceManager", "设备未连接，无法启用硬币接收器");
            }
        });
    }

    // 关闭连接
    public void closeConnection() {
        commandHandler.post(() -> {
            serialPortManager.disconnect();
            isConnected = false;
            Log.d("PaymentDeviceManager", "设备已断开连接");
        });

        // 停止读取响应的 Handler
        if (responseHandlerThread != null) {
            responseHandlerThread.quitSafely(); // 安全地停止线程
            responseHandlerThread = null;
        }
    }


    //将暂存区的钱存入钱箱
    public void pushBillIn(){
        commandHandler.post(() -> {
            if (isConnected) {
                billAcceptorManager.pushBillIn();
            } else {
                Log.e("PaymentDeviceManager", "设备未连接，纸币器无法将钱存入钱箱");
            }
        });
    }

    //将暂存区的钱退回
    public void pollBillOut(){
        commandHandler.post(() -> {
            if (isConnected) {
                billAcceptorManager.pollBillOut();
            } else {
                Log.e("PaymentDeviceManager", "设备未连接，纸币器无法将钱退出");
            }
        });
    }
    
    // 退还硬币
    public void returnCoinAuto() {
        commandHandler.post(() -> {
            if (isConnected) {
                coinAcceptorManager.returnCoinAuto();
            } else {
                Log.e("PaymentDeviceManager", "设备未连接，硬币接收器无法退币");
            }
        });
    }

    //退还纸币
    public void returnBillAuto() {
        commandHandler.post(() -> {
            if (isConnected) {
                billAcceptorManager.returnBillAuto();
            } else {
                Log.e("PaymentDeviceManager", "设备未连接，纸币接收器无法退币");
            }
        });
    }



}

