package com.jpgk.hardwaresdk.serialport;

public class SerialPortUtils {

    // 辅助方法：将字节数组转换为十六进制字符串
    public static  final String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString().trim();
    }

    public static String hexToString(String hexData) {
        StringBuilder output = new StringBuilder();

        // 每两个字符代表一个字节（8位），将其转为对应的字符
        for (int i = 0; i < hexData.length(); i += 2) {
            // 取两个字符作为一个 HEX 字节
            String str = hexData.substring(i, i + 2);

            // 将 HEX 转换为整数，再转为字符
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }
}
