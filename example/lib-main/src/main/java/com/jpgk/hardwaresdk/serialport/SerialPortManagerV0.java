package com.jpgk.hardwaresdk.serialport;

import android.serialport.SerialPort;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class SerialPortManagerV0 {
    private SerialPort serialPort;
    private OutputStream outputStream;
    private InputStream inputStream;
    public boolean isConnected = false; // 连接状态

    // 初始化串口连接
    public void init(String portPath, int baudRate) throws IOException {
        serialPort = new SerialPort(new File(portPath), baudRate, 0);
        outputStream = serialPort.getOutputStream();
        inputStream = serialPort.getInputStream();
        isConnected = true; // 连接成功，更新状态
    }

    // 发送数据
    public void sendCommand(byte[] command) {
        try {
            if (outputStream != null) {
                outputStream.write(command);
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
            isConnected = false; // 发送失败，更新状态
        }
    }

    // 读取响应数据
    public byte[] readResponse() {
        try {
            if (inputStream != null) {
                byte[] buffer = new byte[64];
                int size = inputStream.read(buffer);
                if (size > 0) {
                    byte[] data = new byte[size];
                    System.arraycopy(buffer, 0, data, 0, size);
                    return data;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            isConnected = false; // 读取失败，更新状态
        }
        return null;
    }

    // 关闭串口连接
    public void close() {
        try {
            if (outputStream != null) outputStream.close();
            if (inputStream != null) inputStream.close();
            if (serialPort != null) serialPort.close();
            isConnected = false; // 关闭连接，更新状态
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
