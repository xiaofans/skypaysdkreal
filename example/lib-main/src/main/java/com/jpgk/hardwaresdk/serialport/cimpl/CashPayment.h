#ifndef CASHPAYMENT_H
#define CASHPAYMENT_H

#include <QThread>

class CashPayment : public QThread
{
    Q_OBJECT

public:
    CashPayment(QObject *parent = nullptr);
    virtual ~CashPayment() = 0;

    // 支付接口，应传入支付金额
    // 调用支付接口就同时使能刷卡器
    virtual void pay(int payMoney) = 0; // 开始支付 分为单位
    virtual void stopPay() = 0; // 开始支付 分为单位
    virtual void transactionSuccess(int payMoney) = 0; // 出货成功，扣款 分为单位
    virtual void transactionFailed() = 0; // 出货全失败
    virtual void transactionCancel() = 0; //取消支付
    virtual void enableCash(bool enableEscrow) = 0; // 使能现金
    virtual void disableCash() = 0; // 禁能现金
    virtual void pushOutAllCash() = 0; // 退出所有现金
    virtual void startCardPay(int payMoney, int passageId) = 0;

signals:
    void cashReady(); // 现金准备完毕
    void cashEnabled(); // 现金使能完毕
    void cashDisabled(); // 现金禁能完毕
    void paySuccess(int payMoney); // 支付成功 分为单位
    void cashInTotal(int total, int bill, int coin); // 通知收到金额有变化 分为单位
    void errorMessage(QString msg); // 通知报错
    void pushReturnLever(); // 通知用户按了退币按钮
    void startPayout(); // 开始退币
    void completePayout(); // 退币完成
    void payoutNotEnough(); // 找零不足
    void canNotRefund(); // 无法再找零

};

#endif // CASHPAYMENT_H
