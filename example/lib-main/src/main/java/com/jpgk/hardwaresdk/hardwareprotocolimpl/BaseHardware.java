package com.jpgk.hardwaresdk.hardwareprotocolimpl;

public interface BaseHardware {

    public static final int TYPE_COIN_BILL_ACCEPTER = 10;
    public static final int TYPE_COIN_ACCEPTOR = 1;
    public static final int TYPE_BILL_ACCEPTOR = 0;
    public static final int TYPE_CASH_DISPENSER = 3;
    public static final int TYPE_PRINTER = 4;
    public static final int TYPE_SCAN_CAMERA = 2;

    int openSerialPort();
    void closePort();
    void reconnect();
    boolean isSerialPortOpen();
    boolean isOnLine();
    String getHardwareName();
    int getHardwareType();
}
