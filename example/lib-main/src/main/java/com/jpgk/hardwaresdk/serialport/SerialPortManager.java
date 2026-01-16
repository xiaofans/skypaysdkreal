package com.jpgk.hardwaresdk.serialport;

import android.serialport.SerialPort;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class SerialPortManager implements PaymentDeviceConstant{
    private static final String TAG = "SerialPortManager";


    private SerialPort serialPort;
    private OutputStream outputStream;
    private InputStream inputStream;

    // 构造函数
    public SerialPortManager() {
        // 初始化串口和输入输出流
    }

    // 连接到串口
    public boolean connect() {
        try {
            // 这里替换为实际串口配置
            serialPort = new SerialPort(new File(S_PORT_PATH), 9600, 0); // 设备名和波特率
            outputStream = serialPort.getOutputStream();
            inputStream = serialPort.getInputStream();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "串口连接失败: " + e.getMessage());
            return false;
        }
    }

    /**
     * try {
     *             if (mInputStream != null) {
     *                 mInputStream.close();
     *             }
     *             if (mOutputStream != null) {
     *                 mOutputStream.close();
     *             }
     *         } catch (IOException e) {
     *             e.printStackTrace();
     *         }
     *
     *         if (mReadThread != null) {
     *             mReadThread.interrupt();
     *         }
     *         if (mSerialPort != null) {
     *             mSerialPort.close();
     *             mSerialPort = null;
     *         }
     *         if (mHandlerThread != null) {
     *             mHandlerThread.quit();
     *             mHandlerThread = null;
     *         }
     *         _isOpen = false;
     */
    // 断开连接
    public void disconnect() {
        if (serialPort != null) {
            try {
               inputStream.close();
               inputStream = null;
               outputStream.close();
               outputStream = null;
            }catch (IOException e){
                e.printStackTrace();
            }
            serialPort.close();
            serialPort = null;
        }
    }

    // 发送数据
    public void sendCommand(byte[] command) {
        try {
            if (outputStream != null) {
                outputStream.write(command);
                outputStream.flush();
                Log.d(TAG, "发送命令: " + bytesToHex(command));
            } else {
                Log.e(TAG, "输出流未初始化，无法发送命令");
            }
        } catch (IOException e) {
            Log.e(TAG, "发送命令失败: " + e.getMessage());
        }
    }

    // 接收数据
    public byte[] receiveData() {
        try {
            if (inputStream != null) {
                byte[] buffer = new byte[1024]; // 缓冲区
                int size = inputStream.read(buffer); // 阻塞读取
                if (size > 0) {
                    byte[] data = new byte[size];
                    System.arraycopy(buffer, 0, data, 0, size);
                    Log.d(TAG, "接收到数据: " + bytesToHex(data));
                    return data;
                }
            } else {
                Log.e(TAG, "输入流未初始化，无法接收数据");
            }
        } catch (IOException e) {
            Log.e(TAG, "接收数据失败: " + e.getMessage());
        }
        return null;
    }

    // 将字节数组转换为十六进制字符串
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }
}

