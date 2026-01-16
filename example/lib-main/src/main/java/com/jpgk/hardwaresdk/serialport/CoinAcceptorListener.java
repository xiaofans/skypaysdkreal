package com.jpgk.hardwaresdk.serialport;

public interface CoinAcceptorListener {
    // 当硬币接收器启用时回调
    void onCoinAcceptorEnabled();

    // 当硬币接收器禁用时回调
    void onCoinAcceptorDisabled();

    // 当收到硬币时回调，参数为接收到的硬币金额
    void onCoinReceived(int amount);

    // 当硬币找零或退币完成时回调
    void onCoinsDispensed();

    // 当接收到未知的响应时回调
    void onCoinUnknownResponse(byte[] data);
}