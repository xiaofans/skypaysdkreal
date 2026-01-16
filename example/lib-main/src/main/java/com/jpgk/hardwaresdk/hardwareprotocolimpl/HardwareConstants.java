package com.jpgk.hardwaresdk.hardwareprotocolimpl;

public class HardwareConstants {

    public static  String SCAN_CAMERA_PORT_PATH = "/dev/ttyS3";
    public static  int SCAN_CAMERA_PORT_RATE = 9600;

    public static  String BILL_COIN_ACCEPTOR_PORT_PATH = "/dev/ttyS4";
    public static  int BILL_COIN_ACCEPTOR_PORT_RATE = 9600;


    public static  String PRINTER_PORT_PATH = "/dev/ttyS5";
    public static  int PRINTER_PORT_RATE = 9600;

    public static  String CASH_DISPENSER_PORT_PATH = "/dev/ttyS7";
    public static  int CASH_DISPENSER_PORT_RATE = 9600;


    public static  int PAYING_TIMEOUT = 100;
    public static  int CHANGING_TIMEOUT = 100;

    public static long HARDWARE_CONNECTION_TIME_OUT = 6000;

    public static long HARDWARE_CONNECTION_SCAN_CAMERA_TIME_OUT = 7000;


}

