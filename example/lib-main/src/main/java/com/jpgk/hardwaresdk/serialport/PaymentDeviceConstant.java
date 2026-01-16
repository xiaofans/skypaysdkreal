package com.jpgk.hardwaresdk.serialport;

public interface PaymentDeviceConstant {

    public static  String S_PORT_PATH = "/dev/ttyS4";
    public static  int S_PORT_RATE = 9600;

    public static  String SCAN_CAMERA_PORT_PATH = "/dev/ttyS5";
    public static  int SCAN_CAMERA_PORT_RATE = 9600;


    public static final byte MDB_BILL_SETUP = 0x31;
    public static final byte MDB_COIN_SETUP = 0x09;
    public static final byte MDB_BILL_ENABLE = 0x34;
    public static final byte MDB_COIN_ENABLE = 0x0C;
    public static final byte MDB_BILL_RESET = 0x30;
    public static final byte MDB_COIN_RESET = 0x08;
    public static final byte MDB_COIN_PAYOUT = 0x0f;
    public static final byte MDB_COIN_NUM = 0x0A;
    public static final byte MDB_BILL_SECURITY = 0x32;
    public static final byte MDB_BILL_ESCROW = 0x35;

    public static final byte MDB_BILL_EXPANSION = 0x37;
    public static final byte MDB_BILL_EXPANSION_IDENTIFICATION_WITHOUT_OPTION = 0x00;
    public static final byte MDB_BILL_EXPANSION_FEATURE_ENABLE = 0x01;
    public static final byte MDB_BILL_EXPANSION_IDENTIFICATION_WITH_OPTION = 0x02;
    public static final byte MDB_BILL_EXPANSION_RECYCLER_SETUP = 0x03;
    public static final byte MDB_BILL_EXPANSION_RECYCLER_ENABLE = 0x04;
    public static final byte MDB_BILL_EXPANSION_BILL_DISPENSE_STATUS = 0x05;
    public static final byte MDB_BILL_EXPANSION_DISPENSE_BILL = 0x06;
    public static final byte MDB_BILL_EXPANSION_DISPENSE_VALUE = 0x07;
    public static final byte MDB_BILL_EXPANSION_PAYOUT_STATUS = 0x08;
    public static final byte MDB_BILL_EXPANSION_PAYOUT_VALUE_POLL = 0x09;
    public static final byte MDB_BILL_EXPANSION_PAYOUT_CANCEL = 0x0A;

    public static final byte MDB_BILL_HEAD = 0x33;
    public static final byte MDB_COIN_HEAD = 0x30;
    public static final byte MDB_CASHLESS_HEAD = 0x10;

    public static final byte MDB_CASHLESS_EXPANSION = 0X17;
    public static final byte MDB_CASHLESS_READER = 0X14;
    public static final byte MDB_CASHLESS_VEND = 0X13;

    public static final byte SSP_SYNC_CMD = 0x11;
    public static final byte SSP_RESET_CMD = 0x01;
    public static final byte SSP_SETINHIBITS_CMD = 0x02;
    public static final byte SSP_SETUP_CMD = 0x05;
    public static final byte SSP_POLL_CMD = 0x07;
    public static final byte SSP_ENABLE_CMD = 0x0A;
    public static final byte SSP_DISABLE_CMD = 0x09;


    public static final int SSP_OK_RESP = 0xF0;
    public static final int SSP_BOXREMOVED_RESP = 0xE3;  // 钱箱被取走
    public static final int SSP_BOXREPLACED_RESP = 0xE4; // 钱箱被放回
    public static final int SSP_STARKERFULL_RESP = 0xE7; // 钱箱满
    public static final int SSP_UNSAFEJAM_RESP = 0xE9; // 卡币
    public static final int SSP_SAFEJAM_RESP = 0xEA; // 卡币
    public static final int SSP_CREDIT_RESP = 0xEE;  // 收到纸币了
    public static final int SSP_READ_RESP = 0xEF; // 读到有纸币
    public static final int SSP_DISABLED_RESP = 0xE8; // 纸币器禁能
    public static final int SSP_FRAUD_RESP = 0xE8; // 钓鱼，欺诈

    public static final int SSP_STX = 0x7f;


}
