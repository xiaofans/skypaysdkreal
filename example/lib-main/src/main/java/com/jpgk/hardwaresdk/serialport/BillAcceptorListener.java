package com.jpgk.hardwaresdk.serialport;

public interface BillAcceptorListener {
    // 当纸币接收器启用时回调
    void onBillAcceptorEnabled(boolean enableEscrow);

    // 当纸币接收器禁用时回调
    void onBillAcceptorDisabled();

    // 当收到纸币时回调，参数为接收到的纸币金额
    void onBillReceived(int amount);

    // 当退回暂存的纸币时回调
    void onEscrowBillReturned();

    // 当接收到未知的响应时回调
    void onBillUnknownResponse(byte[] data);

    // 退回纸币数量
    void onBillDispensed(int type,int amount);
}
