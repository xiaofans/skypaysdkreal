#ifndef MDBMANAGER_H
#define MDBMANAGER_H
#include <QThread>
#include <QMutex>
#include <QTimer>
#include <QWaitCondition>
#include <QMap>
#include "qserialport.h"
#include "CashPayment.h"

#define TRUE (1 == 1)
#define FALSE (!TRUE)
#define MAX_COIN_PAYOUT_MONEY 200

typedef uint8_t bool_t;

enum SSPBillDef {
    MDB_BILL_SETUP = 0x31,
    MDB_COIN_SETUP = 0x09,
    MDB_BILL_ENABLE = 0x34,
    MDB_COIN_ENABLE = 0x0C,
    MDB_BILL_RESET = 0x30,
    MDB_COIN_RESET = 0x08,
    MDB_COIN_PAYOUT = 0x0f,
    MDB_COIN_NUM = 0x0A,
    MDB_BILL_SECURITY = 0x32,
    MDB_BILL_ESCROW = 0x35,

    MDB_BILL_EXPANSION = 0x37,
    MDB_BILL_EXPANSION_IDENTIFICATION_WITHOUT_OPTION = 0x00,
    MDB_BILL_EXPANSION_FEATURE_ENABLE = 0x01,
    MDB_BILL_EXPANSION_IDENTIFICATION_WITH_OPTION = 0x02,
    MDB_BILL_EXPANSION_RECYCLER_SETUP = 0x03,
    MDB_BILL_EXPANSION_RECYCLER_ENABLE = 0x04,
    MDB_BILL_EXPANSION_BILL_DISPENSE_STATUS = 0x05,
    MDB_BILL_EXPANSION_DISPENSE_BILL = 0x06,
    MDB_BILL_EXPANSION_DISPENSE_VALUE = 0x07,
    MDB_BILL_EXPANSION_PAYOUT_STATUS = 0x08,
    MDB_BILL_EXPANSION_PAYOUT_VALUE_POLL = 0x09,
    MDB_BILL_EXPANSION_PAYOUT_CANCEL = 0x0A,

    MDB_BILL_HEAD = 0x30,
    MDB_COIN_HEAD = 0x08,
    MDB_CASHLESS_HEAD = 0x10,

    MDB_CASHLESS_EXPANSION = 0X17,
    MDB_CASHLESS_READER = 0X14,
    MDB_CASHLESS_VEND = 0X13,

    SSP_SYNC_CMD = 0x11,
    SSP_RESET_CMD = 0x01,
    SSP_SETINHIBITS_CMD = 0x02,
    SSP_SETUP_CMD = 0x05,
    SSP_POLL_CMD = 0x07,
    SSP_ENABLE_CMD = 0x0A,
    SSP_DISABLE_CMD = 0x09,


    SSP_OK_RESP = 0xF0,
    SSP_BOXREMOVED_RESP = 0xE3,  // 钱箱被取走
    SSP_BOXREPLACED_RESP = 0xE4, // 钱箱被放回
    SSP_STARKERFULL_RESP = 0xE7, // 钱箱满
    SSP_UNSAFEJAM_RESP = 0xE9, // 卡币
    SSP_SAFEJAM_RESP = 0xEA, // 卡币
    SSP_CREDIT_RESP = 0xEE,  // 收到纸币了
    SSP_READ_RESP = 0xEF, // 读到有纸币
    SSP_DISABLED_RESP = 0xE8, // 纸币器禁能
    SSP_FRAUD_RESP = 0xE8, // 钓鱼，欺诈

    SSP_STX = 0x7f,
};

class MDBmanager : public CashPayment
{
    Q_OBJECT
public:

    // 支付接口，应传入支付金额
    // 调用支付接口就同时使能刷卡器
    void pay(int payMoney); // 分为单位
    void stopPay();
    void transactionSuccess(int payMoney); // 分为单位
    void transactionFailed();
    void transactionCancel(); //取消支付
    void enableCash(bool enableEscrow);
    void disableCash();
    void pushOutAllCash();
    void startCardPay(int payMoney, int passageId); // 分为单位

    explicit MDBmanager(QObject *parent = 0);
    ~MDBmanager();
    void setPortName(QString name);
    void startReceiveBill(bool enableEscrow);
    void stopReceiveBill();
    void setBillType(bool enableEscrow);
    void getBillType();
    void setCoinType();
    void getCoinType();
    void poll();
    void payOut(int money);
    void transaction(const QByteArray &request, int timeout = 200, int priority = 0);

    void run();

    void cleanMoney();
    void cleanNeedMoney();
    void payMoney(int money);
    void change(int money);

    void billReset();
    void coinReset();

    void coinFull();
    void coinNotFull();

    void setupBillSecurity();

    void getBillExpansionSetupInfo();
    void setBillFeatureEnable();
    void getBillExpansionRecyclerSetupInfo();
    void enableBillExpansionRecycler();
    void checkBillExpansionDispenserStatue();
    void payOutBill(int type); // 纸币器退币，退币类型，每次退一张
    void getBillExpansionPayoutStatus();
    void billPayoutValuePoll();
    int cancelBillPayout();
    void letBillIn();
    void letBillOut();

    void coinEmpty(int type);
    void coinNotEmpty(int n);

    void coinLess(int n);
    void coinNotLess(int n);

    void setOldPay(int balance);
    int getOldPay();

    void setCashlessSetupConfig();
    void setCashlessSetupPrice();
    void setCashlessExpansionRequest();
    void setCashlessExpansionEnable();
    void setCashlessReaderEnable();
    void cashlessVendOut(int price, int passageId);
    void cashlessVendOutCancel();
    void cashlessVendOutSuccess();
    void cashleddVendOutFailure();
    void cashlessVendOutComplete();


signals:
    void response(QByteArray s);
    void error(const QString s);
    void timeout();
    void gotMoney(quint32 total, quint32, quint32); // 分为单位
    void readData();
    void coinNotEnough(int count);
    void coinEnough(int count);
    void returnLever();
    void coinIdle();
    void getCoin(bool_t *p_coinStatus);

public slots:
private slots:
    void checkCoin();

    void gotResponse(QByteArray buf);
    void explainResponse(QByteArray buf);
    void explainPoll(QByteArray buf);

    void responseTimeout();
    ushort getCRC(QByteArray buf);
    void stopPoll();
    void startPoll();


    void checkPayMoney();
    int getBillValue(int type); // 根据纸币器返回的类型获取纸币金额（分）
    int getBillCount(int value); // 根据上层需要转换的金额获取对应纸币器退币的计数
    int getCoinValue(int type); // 根据硬币器返回的类型获取硬币金额（分）
    int getCoinCount(int value); // 根据上层需要转换的金额获取对应硬币器退币的计数
    int getBillScaling();
    int getCoinScaling();

    void checkCoinPayoutDone();
    void startCheckBillPayoutStatue();
    void stopCheckBillPayoutStatue();
    int getCurrentCoinCount(int totalCoinCount);
    void checkBillEscrowDone();
    void billInEscrowTimeout();
    void setLastBillInStack(int billNum);
    void waitIncompleteCmdTimeout();
private:
    QString portName;
    QByteArray request;
    int waitTimeout;
    QMutex mutex;
    QWaitCondition cond;
    bool_t quit;
    int isFull; // 是否满
    int isJam;  // 是否卡币
    uchar seq;
    volatile int isSending;
    int isEnable;
    int disableCount;
    int isPolling;
    bool_t currentCmdDone;
    int confirmCount;
    int billValues[16];
    int coinValues[16];
    int billScalingFactor;
    int billDecimalPlaces;
    int coinScalingFactor;
    int coinDecimalPlaces;
    class MDBCmd
    {
    public:
        MDBCmd(QByteArray cmd, int delay, int priority)
        {
            this->cmd = cmd;
            this->delay = delay;
            this->priority = priority;
        }

        QByteArray cmd;
        int delay;
        int priority;
    };
    QList<MDBCmd> cmdList;
    int totalCoinInTubes;
    int totalMoney;
    int oldPay;
    int currentReSendTimes;
    int needMoney;
    int isCoinFull;
    bool_t isCoinEmpty[16];
    bool_t isCoinLess; // 硬币是否不足：1 未初始化 2 初始化足够 3 足够 4 不足
    bool_t isOutting;
    int totalPayMoney;

    QTimer coinNumCheckTimer;

    bool_t isBillSupportPayout; // 是否支持找零，上层调用找零接口时，判断此标志，确定是否启动纸币找零
    bool_t isBillPayouting; // 是否正在找零纸币，正在找零纸币时，需要监控找零状态，如果超时需要取消找零纸币

    int totalPayoutCoin; // 总需要找零金额(分)
    bool_t isCoinPayouting; // 正在找零
    QTimer *coinPayoutCheckTimer;
    int totalPayoutBill; // 总需要找零纸币金额(分)
    int billPayoutType; // 当前找零的纸币类型，找完后需要减掉总找零数
    bool_t isCheckingBillPayoutStatue; // 开启查询纸币找零状态，找完后就可以不再轮询了
    int totalPayoutMoney; // 总需要找零的金额，包含纸币硬币，主要是为了解决找完纸币后，继续找硬币的总额问题
    bool_t isSupportEscrow; // 是否支持暂存
    int coinMoney; // 找零硬币仓总额
    int billMoney; // 找零纸币仓总额
    QList<int> allCoinValue; // 所有硬币的价值，欧元就是1对1，人民币就是1对50
    bool_t isPayouting;
    bool_t enableReceiveCash;
    bool_t isBillInEscrow;
    bool_t isEscrowInOrOut;
    int escrowCmdCounter;
    int billBusyCounter;
    qint64 billBusyTimer;
    QTimer *escrowCmdTimer;

    QTimer *escrowBillTimer;
    int escrowBillNum;
    QMap<int, int> escrowBillNumMap;

    QTimer *incompleteCmdTimer;
    QByteArray incompleteCmd;

    bool isCashlessEnable;
    bool isUseCashless;
};

#endif // MDBMANAGER_H
