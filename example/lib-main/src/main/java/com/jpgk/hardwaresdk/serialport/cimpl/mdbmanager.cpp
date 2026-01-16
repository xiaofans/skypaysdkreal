#include "mdbmanager.h"
#include "envenum.h"
#include <QDebug>
#include <QTime>
#include <iterator>
#include <QtMath>
#include <QEventLoop>

#define Bill_Recycling_Supported (1 << 1)

void MDBmanager::pay(int payMoney)
{
    totalPayMoney = payMoney;
    checkPayMoney();
}

void MDBmanager::stopPay()
{
    totalPayMoney = 0;
}

void MDBmanager::transactionSuccess(int payMoney)
{
    totalPayMoney = 0;
    isOutting = FALSE;
    if (isUseCashless) {
        cashlessVendOutSuccess();
        cashlessVendOutComplete();
    } else {
        if (totalMoney >= payMoney) {
            totalMoney -= payMoney;
            emit cashInTotal(totalMoney, 0, 0);
        } else {
            qWarning() << "可能是非现金支付时调用了此接口";
        }
    }

}

void MDBmanager::transactionFailed()
{
    isOutting = FALSE;
    totalPayMoney = 0;
    if (isUseCashless) {
        cashleddVendOutFailure();
    }
}

void MDBmanager::transactionCancel()
{
    isOutting = FALSE;
    totalPayMoney = 0;
    cashlessVendOutCancel();
}

void MDBmanager::enableCash(bool enableEscrow)
{
    coinNumCheckTimer.start();
    startReceiveBill(enableEscrow);
    enableReceiveCash = true;
}

void MDBmanager::disableCash()
{
    stopReceiveBill();
    enableReceiveCash = false;
}

void MDBmanager::pushOutAllCash()
{
    if (!isOutting) {
        if (!isPayouting) {
            payOut(totalMoney);
            // totalMoney = 0;
            emit cashInTotal(totalMoney, 0, 0);
        } else {
            qDebug("正在退币，无法响应退币请求");
        }
    } else {
        emit errorMessage("出货中，无法执行退币动作");
        qDebug("出货中，无法执行退币动作");
    }
}

void MDBmanager::startCardPay(int payMoney, int passageId)
{
    coinNumCheckTimer.stop();
    totalPayMoney = payMoney;
    if (isCashlessEnable) {
        cashlessVendOut(payMoney, passageId);
        isUseCashless = TRUE;
    } else {
        qDebug() << "MDB刷卡器未使能";
    }
}

void MDBmanager::checkPayMoney()
{
    qDebug() << "所需支付金额 " << totalPayMoney;
    qDebug() << "已经投入金额 " << totalMoney;
    if (!isUseCashless) {
        if (totalPayMoney > 0) {
            if (totalMoney >= totalPayMoney) {
                emit paySuccess(totalPayMoney);
                isOutting = TRUE;
            }
        }
    }

}

MDBmanager::MDBmanager(QObject *parent)
    : CashPayment(parent),
      waitTimeout(0),
      quit(FALSE),
      isFull(FALSE),
      isJam(FALSE),
      seq(0),
      isSending(FALSE),
      isEnable(0),
      disableCount(0),
      isPolling(FALSE),
      currentCmdDone(FALSE),
      billScalingFactor(0),
      billDecimalPlaces(0),
      coinScalingFactor(0),
      coinDecimalPlaces(0),
      totalCoinInTubes(0),
      totalMoney(0),
      oldPay(0),
      currentReSendTimes(0),
      needMoney(0),
      isCoinFull(FALSE),
      isCoinLess(FALSE),
      isOutting(FALSE),
      totalPayMoney(0),
      isBillSupportPayout(FALSE),
      isBillPayouting(FALSE),
      totalPayoutCoin(0),
      isCoinPayouting(FALSE),
      totalPayoutBill(0),
      billPayoutType(0),
      isCheckingBillPayoutStatue(FALSE),
      totalPayoutMoney(0),
      isSupportEscrow(FALSE),
      coinMoney(0),
      billMoney(0),
      isPayouting(FALSE),
      enableReceiveCash(FALSE),
      isBillInEscrow(FALSE),
      isEscrowInOrOut(FALSE),
      escrowCmdCounter(0),
      billBusyCounter(0),
      billBusyTimer(0),
      escrowBillNum(0),
      isCashlessEnable(FALSE),
      isUseCashless(FALSE)
{
    for (int i = 0; i < 16; i++) {
        isCoinEmpty[i] = FALSE;
        billValues[i] = 0;
        coinValues[i] = 0;
    }

    setPortName(EnvEnum::getCashPort());
    connect(this, SIGNAL(response(QByteArray)), this, SLOT(gotResponse(QByteArray)),
            Qt::ConnectionType::BlockingQueuedConnection);
    connect(this, SIGNAL(timeout()), this, SLOT(responseTimeout()));
    connect(&coinNumCheckTimer, SIGNAL(timeout()), this, SLOT(checkCoin()));
    connect(this, SIGNAL(coinIdle()), this, SLOT(checkCoin()));

    coinNumCheckTimer.setInterval(10000);
    billReset();
    getBillType();
    setBillType(true);
    coinReset();
    getCoinType();
    setCoinType();
    // stopReceiveBill();
    checkCoin();
    startPoll();

    setCashlessSetupConfig();

    //    coinNumCheckTimer.start();

    coinPayoutCheckTimer = new QTimer(this);
    connect(coinPayoutCheckTimer, SIGNAL(timeout()), this, SLOT(checkCoinPayoutDone()));
    coinPayoutCheckTimer->setInterval(1000);
    coinPayoutCheckTimer->setSingleShot(true);

    escrowCmdTimer = new QTimer(this);
    connect(escrowCmdTimer, SIGNAL(timeout()), this, SLOT(checkBillEscrowDone()));
    escrowCmdTimer->setInterval(5000);
    escrowCmdTimer->setSingleShot(true);

    escrowBillTimer = new QTimer(this);
    connect(escrowBillTimer, SIGNAL(timeout()), this, SLOT(billInEscrowTimeout()));
    escrowBillTimer->setInterval(10000);
    escrowBillTimer->setSingleShot(true);

    incompleteCmdTimer = new QTimer(this);
    connect(incompleteCmdTimer, SIGNAL(timeout()), this, SLOT(waitIncompleteCmdTimeout()));
    incompleteCmdTimer->setInterval(1000);
    incompleteCmdTimer->setSingleShot(true);
}
MDBmanager::~MDBmanager()
{
    mutex.lock();
    quit = TRUE;
    coinNumCheckTimer.stop();
    cond.wakeOne();
    mutex.unlock();
    wait();
    delete coinPayoutCheckTimer;
    delete escrowCmdTimer;
    delete escrowBillTimer;
    delete incompleteCmdTimer;
}

void MDBmanager::setPortName(QString name)
{
    this->portName = name;
}

void MDBmanager::startReceiveBill(bool enableEscrow)
{
    setCoinType();
    setBillType(enableEscrow);
}

void MDBmanager::stopReceiveBill()
{
    QByteArray buf;
    qDebug() << "禁能现金设备";
    //    buf.append(MDB_BILL_ENABLE);
    //    buf.append((char)0);
    //    buf.append((char)0);
    //    buf.append((char)0);
    //    buf.append((char)0);
    //    transaction(buf);
    setBillType(false);

    buf.clear();
    buf.append(MDB_COIN_ENABLE);
    buf.append((char)0);
    buf.append((char)0);
    buf.append((char)0);
    buf.append((char)0);
    transaction(buf);
}

void MDBmanager::setBillType(bool enableEscrow)
{
    QByteArray buf;
    buf.append(MDB_BILL_ENABLE);
    buf.append((char)0);
    buf.append(0x0f);
    buf.append((char)0);
    if (enableEscrow) {
        buf.append(0x0f);
    } else {
        buf.append((char)0);
    }
    transaction(buf);
}

void MDBmanager::getBillType()
{
    QByteArray buf;

    buf.append(MDB_BILL_SETUP);
    transaction(buf);
}

void MDBmanager::setCoinType()
{
    QByteArray buf;

    buf.append(MDB_COIN_ENABLE);
    buf.append(0xff);
    buf.append(0xff);
    buf.append(0xff);
    buf.append(0xff);
    transaction(buf);
}

void MDBmanager::getCoinType()
{
    QByteArray buf;

    buf.append(MDB_COIN_SETUP);
    transaction(buf);
}

void MDBmanager::poll()
{
    QByteArray buf;
    transaction(buf);
}

void MDBmanager::checkCoin()
{
    QByteArray buf;
    buf.append(MDB_COIN_NUM);
    transaction(buf);
}

void MDBmanager::payOut(int money)
{
    totalPayoutMoney = money;
    if (money > 0) {
        qDebug() << "退钱" << money;
        isPayouting = TRUE;
        emit startPayout();
        if (isBillSupportPayout) {
            // 查询剩余币数，找最大面额开始找零，如果需要找零的金额小于纸币最小面额则进行硬币找零
            if (totalPayoutBill == -1) {
                qDebug() << "调用纸币器无法再退钱，调用硬币器退钱";
            } else {
                isBillPayouting = TRUE;
                totalPayoutBill = money;
                qDebug() << "调用纸币器退钱" << totalPayoutBill;
                checkBillExpansionDispenserStatue();
                return;
            }
        }
        int coin = getCoinCount(money);
        // 一次找零最多能找255数量的硬币（按硬币器倍率计算得出，如人民币倍率为50，1为50分（五角），欧元倍率为1，1为1分，所以很容易超范围，需要分多次找零
        // 调用函数计算每次找零的价值
        qDebug() << "coin---- " << coin;
        int curentCoinCount = getCurrentCoinCount(coin);

        qDebug() << "本次找零（count）：" << curentCoinCount;
        if (curentCoinCount != 0) {

            totalPayoutCoin = coin - curentCoinCount;
            totalMoney = totalPayoutCoin * getCoinScaling();
            emit cashInTotal(totalMoney, 0, 0);
            if (coin - curentCoinCount > 0) {
                qDebug() << "欠钱" << totalPayoutCoin *getCoinScaling();
            }
        } else {
            qDebug() << "欠钱，找不开：" << (coin - curentCoinCount) * getCoinScaling();
            if (money > 0 && coin == 0) {
                qWarning() << "未接硬币器，无法获取正确的硬币数量，按照原来的金额显示";
                totalMoney = money;
            } else {
                totalMoney = (coin - curentCoinCount) * getCoinScaling();
            }
            emit canNotRefund();
            emit cashInTotal(totalMoney, 0, 0);
            totalPayoutCoin = 0;
            totalPayoutBill = 0;
            isCoinPayouting = FALSE;
            isBillPayouting = FALSE;
            isPayouting = FALSE;
            emit completePayout();
        }

        if (curentCoinCount > 0) {
            coinPayoutCheckTimer->start(3000);
            QByteArray buf;
            buf.append(MDB_COIN_PAYOUT);
            buf.append(0x02); // 由机器自动选择找零的面额
            buf.append(curentCoinCount);
            transaction(buf);
        }
    }
}

void MDBmanager::billReset()
{
    QByteArray buf;
    buf.append(MDB_BILL_RESET);
    transaction(buf, 5000);
}

void MDBmanager::coinReset()
{
    QByteArray buf;
    buf.append(MDB_COIN_RESET);
    transaction(buf, 5000);
}

void MDBmanager::coinFull()
{
    emit error("||10");
    emit errorMessage("硬币器满");
    isCoinFull = TRUE;

    QByteArray buf;
    buf.append(MDB_COIN_ENABLE);
    buf.append((char)0);
    buf.append((char)0);
    buf.append(0xff);
    buf.append(0xff);
    transaction(buf);
}

void MDBmanager::coinNotFull()
{
    isCoinFull = FALSE;
    QByteArray buf;
    buf.append(MDB_COIN_ENABLE);
    buf.append(0xff);
    buf.append(0xff);
    buf.append(0xff);
    buf.append(0xff);
    transaction(buf);
}

void MDBmanager::setupBillSecurity()
{
    QByteArray buf;
    buf.append(MDB_BILL_SECURITY);
    buf.append(0xff);
    buf.append(0xff);
    transaction(buf);
}

void MDBmanager::getBillExpansionSetupInfo()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_IDENTIFICATION_WITH_OPTION);
    transaction(buf);
}

void MDBmanager::setBillFeatureEnable()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_FEATURE_ENABLE);
    buf.append((char)0x00);
    buf.append((char)0x00);
    buf.append((char)0x00);
    buf.append(0x02);
    transaction(buf);
}

void MDBmanager::getBillExpansionRecyclerSetupInfo()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_RECYCLER_SETUP);
    transaction(buf);
}

void MDBmanager::enableBillExpansionRecycler()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_RECYCLER_ENABLE);
    // 使能所有路径的找零功能纸币，需要针对MDB_BILL_EXPANSION_RECYCLER_SETUP回复的内容来定
    buf.append(0xff);
    buf.append(0xff);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    buf.append(0x03);
    transaction(buf);
}

// 不动作影响找零流程，需要有报错机制，报错继续使用硬币找零
void MDBmanager::checkBillExpansionDispenserStatue()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_BILL_DISPENSE_STATUS);
    transaction(buf);
}

void MDBmanager::payOutBill(int type)
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_DISPENSE_BILL);
    buf.append((char)type);
    buf.append((char)0);
    buf.append(1);
    transaction(buf);
}

void MDBmanager::getBillExpansionPayoutStatus()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_PAYOUT_STATUS);
    transaction(buf);
}

void MDBmanager::billPayoutValuePoll()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_PAYOUT_VALUE_POLL);
    transaction(buf);
}

int MDBmanager::cancelBillPayout()
{
    QByteArray buf;
    buf.append(MDB_BILL_EXPANSION);
    buf.append(MDB_BILL_EXPANSION_PAYOUT_CANCEL);
    transaction(buf);
}

void MDBmanager::letBillIn()
{
    QByteArray buf;
    buf.append(MDB_BILL_ESCROW);
    buf.append(1);
    transaction(buf, 200, 1);
    isEscrowInOrOut = TRUE;
}

void MDBmanager::letBillOut()
{
    QByteArray buf;
    buf.append(MDB_BILL_ESCROW);
    buf.append((char)0);
    transaction(buf, 200, 1);
    isEscrowInOrOut = FALSE;
}

void MDBmanager::coinEmpty(int n)
{
    int type = getCoinValue(n);
    if (type != 0) {
        qDebug() << type << "硬币空";
    }
    isCoinEmpty[n] = TRUE;
    switch (type) {
    case 50:
        emit error("||7");
        emit errorMessage("硬币器5角币空");
        break;
    case 100:
        emit error("||8");
        emit errorMessage("硬币器1元币空");
        break;
    default:
        if (type != 0) {
            emit error("||7");
            emit errorMessage(QString::number(type) + "钱管空");
        }
        break;
    }
}

void MDBmanager::coinNotEmpty(int n)
{
    qDebug() << getCoinValue(n) << "硬币不空";
    isCoinEmpty[n] = FALSE;
    emit getCoin(isCoinEmpty);
}

void MDBmanager::coinLess(int n)
{
    if (!isCoinLess) {
        qDebug() << "硬币不足，禁能纸币标志位";
        isCoinLess = TRUE;
        //        QByteArray buf;
        //        buf.append(MDB_BILL_ENABLE);
        //        buf.append((char)0);
        //        buf.append((char)0);
        //        buf.append((char)0);
        //        buf.append((char)0);
        //        transaction(buf);
    }
    emit coinNotEnough(n);
}

void MDBmanager::coinNotLess(int n)
{
    if (isCoinLess) {
        qDebug() << "硬币足够，使能纸币标志位";
        isCoinLess = FALSE;
        //        QByteArray buf;
        //        buf.append(MDB_BILL_ENABLE);
        //        buf.append((char)0);
        //        buf.append(0x0f);
        //        buf.append((char)0);
        //        buf.append((char)0);
        //        transaction(buf);
    }
    emit coinEnough(n);
}

void MDBmanager::setOldPay(int balance)
{
    if (oldPay == balance) {
        return ;
    }
    oldPay = balance;
}

int MDBmanager::getOldPay()
{
    return oldPay;
}

void MDBmanager::setCashlessSetupConfig()
{
    //110003000000
    QByteArray buf;
    buf.append(SSP_SYNC_CMD);
    buf.append(char(0));
    buf.append(0x03);
    buf.append(char(0));
    buf.append(char(0));
    buf.append(char(0));
    transaction(buf, 200, 1);
}

void MDBmanager::setCashlessSetupPrice()
{
    //1101FFFF0000
    QByteArray buf;
    buf.append(SSP_SYNC_CMD);
    buf.append(char(1));
    buf.append(0xFF);
    buf.append(0xFF);
    buf.append(char(0));
    buf.append(char(0));
    transaction(buf, 200, 1);
}

void MDBmanager::setCashlessExpansionRequest()
{
    //17004E454330303030303030303030303020202020204B5245412020200005
    QByteArray buf;
    buf.append(MDB_CASHLESS_EXPANSION);
    buf.append(char(0));
    buf.append(QByteArray::fromHex("4E454330303030303030303030303020202020204B5245412020200005"));
    transaction(buf, 500, 1);
}

void MDBmanager::setCashlessExpansionEnable()
{
    //1704000000
    QByteArray buf;
    buf.append(MDB_CASHLESS_EXPANSION);
    buf.append(char(4));
    buf.append(char(0));
    buf.append(char(0));
    buf.append(char(0));
    buf.append(0x20);
    transaction(buf, 200, 1);
}

void MDBmanager::setCashlessReaderEnable()
{
    QByteArray buf;
    buf.append(MDB_CASHLESS_READER);
    buf.append(char(1));
    transaction(buf, 200, 1);
}

void MDBmanager::cashlessVendOut(int price, int passageId)
{
    QByteArray buf;
    buf.append(MDB_CASHLESS_VEND);
    buf.append(char(0));
    buf.append(price >> 8 & 0xff);
    buf.append(price & 0xff);
    buf.append(passageId >> 8 & 0xff);
    buf.append(passageId & 0xff);
    transaction(buf, 2000, 1);
}

void MDBmanager::cashlessVendOutCancel()
{
    QByteArray buf;
    buf.append(MDB_CASHLESS_VEND);
    buf.append(0x01);
    transaction(buf, 200, 1);
}

void MDBmanager::cashlessVendOutSuccess()
{
    QByteArray buf;
    buf.append(MDB_CASHLESS_VEND);
    buf.append(0x02);
    transaction(buf, 200, 1);
}

void MDBmanager::cashleddVendOutFailure()
{
    QByteArray buf;
    buf.append(MDB_CASHLESS_VEND);
    buf.append(0x03);
    transaction(buf, 200, 1);
}

void MDBmanager::cashlessVendOutComplete()
{
    QByteArray buf;
    buf.append(MDB_CASHLESS_VEND);
    buf.append(0x04);
    transaction(buf, 200, 1);
    isUseCashless = FALSE;
}

void MDBmanager::transaction(const QByteArray &request, int timeout, int priority)
{
    QMutexLocker locker(&mutex);

    if (isSending == TRUE) {
        // 命令不是poll则缓存命令
        if (priority != 0) {
            int i = 0;
            for (i = 0; i < cmdList.size(); i++) {
                MDBCmd cmd = cmdList.at(i);
                if (priority > cmd.priority) {
                    cmdList.insert(i, MDBCmd(request, timeout, priority));
                    break;
                }
            }
            if (i == cmdList.size()) {
                cmdList.append(MDBCmd(request, timeout, priority));
            }
        } else {
            cmdList.append(MDBCmd(request, timeout, priority));   //poll 也缓存了
        }
        return;
    }
    isSending      = TRUE;
    currentCmdDone = FALSE;
    // 保持最大200ms的等待时间，但是在当前交互里保证收到数据后下一次发送间隔最少在200ms
    this->waitTimeout = timeout;
    this->request     = request;
    if (!isRunning()) {
        start();
    } else {
        cond.wakeOne();
    }
}

void MDBmanager::run()
{
    bool_t currentPortNameChanged = TRUE;
    mutex.lock();
    QString currentPortName;
    if (currentPortName != portName) {
        currentPortName        = portName;
        currentPortNameChanged = TRUE;
    }

    int        currentWaitTimeout = waitTimeout;
    QByteArray currentRequest     = request;
    mutex.unlock();
    QSerialPort serial;

    while (!quit) {
        if (currentPortNameChanged) {
            serial.close();
            serial.setPortName(currentPortName);

            if (!serial.open(QIODevice::ReadWrite)) {
                qDebug() << tr("Can't open %1, error %2")
                         .arg(portName).arg(serial.errorString());
                return;
            } else {
                qDebug() << "现金串口打开成功" << "port:" << currentPortName;
                serial.setBaudRate(QSerialPort::Baud9600);
                serial.setDataBits(QSerialPort::Data8);
                serial.setParity(QSerialPort::NoParity);
                serial.setStopBits(QSerialPort::OneStop);
            }
        }

        // write request
        QByteArray requestData = currentRequest;
        serial.write(requestData);
        if (!requestData.isEmpty() && requestData != "\n") {
            qDebug() << "发送数据" << requestData.toHex();
        }
        serial.waitForBytesWritten(waitTimeout);
        if (serial.waitForReadyRead(currentWaitTimeout)) {
            QByteArray responseData = serial.readAll();
            while (serial.waitForReadyRead(50)) {
                responseData += serial.readAll();
            }
            if (requestData != "\n") {
                qDebug() << responseData;
            }
            emit this->response(responseData);

            QEventLoop loop;
            QTimer::singleShot(currentWaitTimeout, &loop, SLOT(quit()));
            loop.exec();

            QByteArray uncatchData = serial.readAll();
            if (uncatchData.length() != 0) {
                qDebug() << "请求:" << requestData.toHex() << "等待完毕，处理未读数据:" << uncatchData;
                emit this->response(uncatchData);
            }
        }

        mutex.lock();
        if (!request.isEmpty() && requestData != "\n") {
            qDebug() << "wait..............";
        }
        // 发送完成很快返回的话就需要等待距离上一次命令200ms的时间，来让纸币器缓一会
        emit timeout();
        cond.wait(&mutex);
        if (currentPortName != portName) {
            currentPortName        = portName;
            currentPortNameChanged = TRUE;
        } else {
            currentPortNameChanged = FALSE;
        }
        currentWaitTimeout = waitTimeout;
        currentRequest     = request;
        mutex.unlock();
    }
}

void MDBmanager::cleanMoney()
{
    totalMoney = 0;
    emit gotMoney(0, 0, 0);
}

void MDBmanager::cleanNeedMoney()
{
    needMoney = 0;
}

void MDBmanager::payMoney(int money)
{
    change(money);
}

void MDBmanager::change(int money)
{
    //    totalMoney -= money;
    //    payOut(money);
    //    emit gotMoney(totalMoney, 0, 0);
}

int MDBmanager::getBillValue(int type)
{
    return billValues[type] * billScalingFactor * 100 / qPow(10, billDecimalPlaces);
}

int MDBmanager::getBillCount(int value)
{
    return value * qPow(10, billDecimalPlaces) * billScalingFactor / 100;
}

int MDBmanager::getCoinValue(int type)
{
    return coinValues[type] * coinScalingFactor * 100 / qPow(10, coinDecimalPlaces);
}

int MDBmanager::getCoinCount(int value)
{
    return value * qPow(10, coinDecimalPlaces) / coinScalingFactor / 100;
}

int MDBmanager::getBillScaling()
{
    return billScalingFactor * 100 / qPow(10, billDecimalPlaces);
}

int MDBmanager::getCoinScaling()
{
    return coinScalingFactor * 100 / qPow(10, coinDecimalPlaces);
}

void MDBmanager::checkCoinPayoutDone()
{
    if (isCoinPayouting == FALSE) {
        qWarning() << "退币失败，不退了";
        //        totalMoney = totalPayoutCoin * getCoinScaling();
        //        emit cashInTotal(totalMoney, 0, 0);
        totalPayoutCoin = 0;
        totalPayoutBill = 0;
        isPayouting = FALSE;
        emit completePayout();
    }
    if (totalPayoutCoin > 0) {
        qDebug() << "执行下一次退币";
        payOut(totalPayoutCoin);
    }
    isCoinPayouting = FALSE;
    if (totalPayoutCoin == 0) {
        qDebug() << "退币完成，重新开始检查硬币数";
        totalPayoutBill = 0;
        //        totalMoney = 0;
        //        emit cashInTotal(totalMoney, 0, 0);
        // 找零全部完成
        isPayouting = FALSE;
        emit completePayout();
        coinNumCheckTimer.start();
    }
}

void MDBmanager::startCheckBillPayoutStatue()
{
    isCheckingBillPayoutStatue = TRUE;
}

void MDBmanager::stopCheckBillPayoutStatue()
{
    isCheckingBillPayoutStatue = FALSE;
}

int MDBmanager::getCurrentCoinCount(int totalCoinCount)
{
    int maxCount = 0;
    qDebug() << "totalCoinCount " << totalCoinCount;
    qDebug() << "硬币所有硬币数" << allCoinValue;
    for (int i = allCoinValue.size() - 1; i >= 0; i--) {
        qDebug() << "硬币所有硬币数" << allCoinValue.at(i);
        if ((allCoinValue.at(i) + maxCount) <= MAX_COIN_PAYOUT_MONEY && (allCoinValue.at(i) + maxCount) <= totalCoinCount) {
            maxCount += allCoinValue.at(i);
            qDebug() << "maxCount " << maxCount;
            allCoinValue.removeAt(i);
        }
    }

    return maxCount;
}

void MDBmanager::checkBillEscrowDone()
{
    if (isBillInEscrow) {
        letBillIn();
        escrowCmdCounter = 3;
    }
}

void MDBmanager::billInEscrowTimeout()
{
    qWarning() << "容错:" <<
               "收到纸币进入暂存后10秒超时，无法再等待，认为投入成功，纸币序号：" << escrowBillNum;
    setLastBillInStack(escrowBillNum);
}

void MDBmanager::setLastBillInStack(int billNum)
{
    escrowBillTimer->stop();
    QMap<int, int>::iterator billType = escrowBillNumMap.find(billNum);
    if (billType != escrowBillNumMap.end()) {
        int type = billType.value();
        escrowBillNumMap.erase(billType);
        int billVaule = getBillValue(type);
        qDebug() << "收到纸币" << billVaule;
        isBillInEscrow = FALSE;
        escrowCmdTimer->stop();
        totalMoney += billVaule;
        oldPay += billVaule;
        qDebug() << "收到现金总额" + QString::number(totalMoney);
        emit gotMoney(totalMoney, 0, 0);
        emit cashInTotal(totalMoney, 0, 0);
        checkPayMoney();
    }
}

void MDBmanager::waitIncompleteCmdTimeout()
{
    qWarning() << "容错2:" << "不完整数据缓存超时，内容为：" << incompleteCmd;
    incompleteCmd.clear();
}

void MDBmanager::gotResponse(QByteArray buf)
{
    // 处理返回数据
    // buf.replace("\r","");
    QList<QByteArray> list = buf.split('\n');
    if (!incompleteCmd.isEmpty() || !incompleteCmd.isNull()) {
        QByteArray firstCmd = list.takeFirst();
        firstCmd = incompleteCmd + firstCmd;
        list.insert(0, firstCmd);
        incompleteCmd.clear();
        incompleteCmdTimer->stop();
        qWarning() << "容错2:" << "收到数据不完整，将这条数据接入最新收到的数据前面：" << firstCmd;
    }
    if (list.last() == "") {
        list.removeLast();
    }
    if (!list.last().endsWith('\r')) {
        incompleteCmd = list.last();
        qWarning() << "容错2:" << "收到数据不完整，缓存这条数据：" << incompleteCmd;
        list.removeLast();
        incompleteCmdTimer->start();
    }

    QList<QByteArray>::iterator i;
    currentCmdDone = FALSE;
    for (i = list.begin(); i != list.end(); i++) {
        explainResponse(*i);
    }

    //    for(int i = 0; i < list.size(); i++) {
    //        currentCmdDone = FALSE;
    //        explainResponse(list.at(i));
    //    }
}

void MDBmanager::explainPoll(QByteArray buf)
{
    QByteArray data = QByteArray::fromHex(buf);
    int len = data.size();

    if (request.isEmpty()) {
        currentCmdDone = TRUE;
    }
    qDebug() << "进入轮询响应解析流程";
    if (data.size() >= 2 && data.at(0) == MDB_COIN_HEAD) {
        for (int i = 1; i < data.size(); i++) {
            uchar eachByte = data.at(i);
            if (eachByte != 0x80) {
                // 手动退币
                if (eachByte & 0x80) {
                    int count = 0;
                    int type = 0;
                    int leftCount = 0; // 退币的金额
                    count = (eachByte & 0x70) >> 4;
                    type = eachByte & 0x0f;
                    //                    if (i + 1 <= data.size())
                    if (i + 1 < data.size()) {
                        leftCount = data.at(i + 1);
                        i++;
                    }
                }

                // 投入硬币
                else if ((eachByte & 0xC0) == 0x40) {
                    qDebug() << "投入硬币" << data.toHex();
                    int path = 0;
                    int type = 0;
                    int leftCount = 0;
                    path = (eachByte & 0x30) >> 4;
                    type = eachByte & 0x0f;
                    //                    if (i + 1 <= data.size())
                    if (i + 1 < data.size()) {
                        leftCount = data.at(i + 1);
                        i++;
                    }
                    if (path == 1 || path == 0) {   //0是到钱箱，1是到钱管
                        // 获取获取硬币面额
                        int coinVaule = getCoinValue(type);
                        qDebug() << "投入硬币" << coinVaule;
                        totalMoney += coinVaule;
                        qDebug() << "收到现金总额" + QString::number(totalMoney);
                        oldPay += coinVaule;
                        emit gotMoney(totalMoney, 0, 0);
                        emit cashInTotal(totalMoney, 0, 0);
                        checkPayMoney();
                        if (isCoinEmpty[type]) {
                            coinNotEmpty(type);
                        }
                        checkCoin();
                    }
                }

                // 投入无法识别的硬币
                else if ((eachByte & 0xE0) == 0x20) {
                    int count = 0;
                    count = eachByte & 0x1f;
                }

                else {
                    switch (eachByte) {
                    case 0x01:   qDebug() << "退币请求";
                        emit pushReturnLever();
                        checkCoin();
                        break;
                    case 0x02:   qDebug() << "退币中";
                        coinPayoutCheckTimer->start(1000);
                        break;
                    case 0x03:   qDebug() << "未知硬币面额";
                        break;
                    case 0x04:   qDebug() << "钱管感应器故障";
                        emit error("||3");
                        emit errorMessage("钱管感应器故障");
                        break;
                    case 0x05:   qDebug() << "两枚硬币到达";
                        break;
                    case 0x06:   qDebug() << "识别头被拆除";
                        break;
                    case 0x07:   qDebug() << "钱管阻塞";
                        emit error("||6");
                        emit errorMessage("钱管堵塞");
                        break;
                    case 0x08:   qDebug() << "rom校验和错误";
                        emit error("||4");
                        emit errorMessage("硬币器软体错误");
                        break;
                    case 0x09:   qDebug() << "识别的硬币没有进入钱管";
                        break;
                    case 0x0a:   qDebug() << "硬币器忙";
                        break;
                    case 0x0b:   qDebug() << "硬币器被复位";
                        break;
                    case 0x0c:   qDebug() << "卡币";
                        emit error("||5");
                        emit errorMessage("卡币");
                        break;
                    case 0x0d:   qDebug() << "可能是掏出里面的一枚硬币";
                        break;
                    default:
                        qDebug() << "硬币器发出未知故障";
                        break;
                    }
                }
            }
        }
    }

    if (data.size() >= 2 && data.at(0) == MDB_BILL_HEAD) {
        for (int i = 1; i < data.size(); i++) {
            uchar eachByte = data.at(i);
            //if (eachByte != 0x80)       打开注释 1元纸币无法识别
            {
                // 收到纸币
                if (eachByte & 0x80) {
                    billBusyCounter = 0;
                    int path = 0;
                    int type = 0;
                    path = (eachByte & 0x70) >> 4;
                    type = eachByte & 0x0f;
                    if (path == 0 || path == 3 || path == 1) { // path等于3是进入了找零设备里了
                        // 获取纸币面额
                        int billVaule = getBillValue(type);
                        qDebug() << "收到纸币" << billVaule;
                        if (isSupportEscrow && path == 1) {
                            qDebug() << "纸币进入暂存，当前收到总额：" << totalMoney << "当前现金仓：" << billMoney <<
                                     "当前硬币仓：" << coinMoney << "是否允许收币:" << enableReceiveCash;
                            if (enableReceiveCash) {
                                if (coinMoney + billMoney - totalMoney < billVaule) {
                                    qDebug() << "无法支持找零，退掉现金";
                                    letBillOut();
                                    emit payoutNotEnough();
                                } else {
                                    qDebug() << "可以支持找零，收入现金";
                                    letBillIn();
                                    setLastBillInStack(escrowBillNum);
                                    escrowBillNumMap.insert(++escrowBillNum, type);
                                    qWarning() << "容错:" <<
                                               "收到纸币进入暂存开始10秒定时器，等待10秒或当前纸币投入成功或下一次投入纸币，纸币序号："
                                               << escrowBillNum << "现在map里内容：" << escrowBillNumMap;
                                    escrowBillTimer->start();
                                }
                            } else {
                                letBillOut();
                            }
                            escrowCmdTimer->start();
                            isBillInEscrow = TRUE;
                            escrowCmdCounter = 3;
                        } else {
                            isBillInEscrow = FALSE;
                            escrowBillTimer->stop();
                            QMap<int, int>::iterator billType = escrowBillNumMap.find(escrowBillNum);
                            if (billType != escrowBillNumMap.end()) {
                                escrowBillNumMap.erase(billType);
                                qDebug() << "容错:收到纸币进入暂存后，正常收到纸币，删掉本次缓存入map的数据：" <<
                                         escrowBillNumMap;
                            }
                            escrowCmdTimer->stop();
                            totalMoney += billVaule;
                            oldPay += billVaule;
                            qDebug() << "收到现金总额" + QString::number(totalMoney);
                            emit gotMoney(totalMoney, 0, 0);
                            emit cashInTotal(totalMoney, 0, 0);
                            checkPayMoney();
                        }
                        if (isBillSupportPayout) {
                            checkBillExpansionDispenserStatue();
                        }
                    } else {
                        // 000: BILL STACKED 收入
                        // 001: ESCROW POSITION 暂存位
                        // 010: BILL RETURNED 退出
                        // 011: BILL TO RECYCLER 找零仓
                        // 100: DISABLED BILL REJECTED 不允许投入
                        // 101: BILL TO RECYCLER – MANUAL FILL 找零仓-手动投入
                        // 110: MANUAL DISPENSE 手动找出
                        // 111: TRANSFERRED FROM RECYCLER TO CASHBOX 找零仓转入钱箱
                        int billVaule = getBillValue(type);
                        isBillInEscrow = FALSE;
                        escrowCmdTimer->stop();
                        qDebug() << "收到纸币" << billVaule << "经过：" << path;
                        if (path == 2) {
                            qDebug() << "纸币" << billVaule << "被退出";
                        }
                    }

                } else if ((eachByte & 0xE0) == 0x40) { // 投入无法识别的纸币
                    int count = 0;
                    count = eachByte & 0x1f;
                    qWarning() << "投入无法识别的纸币" << count;
                    billBusyCounter = 0;
                    isBillInEscrow = FALSE;
                    escrowCmdTimer->stop();
                } else {
                    bool_t isPayoutFault = FALSE;
                    if (eachByte == 0x03) {
                        if (billBusyCounter == 0) {
                            billBusyTimer = QDateTime::currentMSecsSinceEpoch();
                        }
                        billBusyCounter++;
                        qint64 timeDiff = QDateTime::currentMSecsSinceEpoch() - billBusyTimer;
                        qDebug() << "纸币忙，可能在吃币中，进行计时计次：" << timeDiff << billBusyCounter;
                        if (billBusyCounter > 100 || timeDiff > 10000) {
                            qDebug() << "纸币忙，超时或超次，直接执行退出指令";
                            billBusyCounter = 0;
                            letBillOut();
                        }
                    } else {
                        if (billBusyCounter > 0) {
                            qDebug() << "纸币不忙，清空计数";
                            billBusyCounter = 0;
                        }
                    }
                    switch (eachByte) {
                    case 0x01:   qDebug() << "电机故障";
                        emit error("|2|");
                        emit errorMessage("纸币器电机故障");
                        isPayoutFault = TRUE;
                        break;
                    case 0x02:   qDebug() << "感应器故障";
                        emit error("|3|");
                        emit errorMessage("纸币器感应器故障");
                        isPayoutFault = TRUE;
                        break;
                    case 0x03:   qDebug() << "纸币器忙";
                        break;
                    case 0x04:   qDebug() << "rom校验和错误";
                        emit error("|4|");
                        emit errorMessage("纸币器软体错误");
                        break;
                    case 0x05:   qDebug() << "纸币器卡币";
                        emit error("|7|");
                        emit errorMessage("纸币器卡币故障");
                        isPayoutFault = TRUE;
                        break;
                    case 0x06:   qDebug() << "纸币器被复位";
                        isPayoutFault = TRUE;
                        break;
                    case 0x07:   qDebug() << "纸币被拿走";
                        isPayoutFault = TRUE;
                        break;
                    case 0x08:   qDebug() << "钱箱不在原位";
                        isPayoutFault = TRUE;
                        emit error("|5|");
                        emit errorMessage("纸币器钱箱被移走故障");
                        break;
                    case 0x09:   qDebug() << "纸币器被禁止";
                        break;
                    case 0x0a:   qDebug() << "暂存要求无效";
                        break;
                    case 0x0b:   qDebug() << "纸币被退出";
                        break;
                    case 0x0c:   qDebug() << "Possible Credited Bill Removal";
                        break;
                    case 0x21:   qDebug() << "Escrow request:存入请求";
                        break;
                    case 0x22:   qDebug() << "Dispenser Payout Busy:出币器出币忙";
                        break;
                    case 0x23:   qDebug() << "Dispenser Busy:出币器忙";
                        break;
                    case 0x24:   qDebug() << "Defective Dispenser Sensor:出币器传感器故障";
                        emit error("|3|");
                        emit errorMessage("纸币器感应器故障");
                        isPayoutFault = TRUE;
                        break;
                    case 0x25:   qDebug() << "Not used";
                        break;
                    case 0x26:   qDebug() << "Dispenser did not start / motor problem:出币器电机没动或电机问题";
                        emit error("|2|");
                        emit errorMessage("纸币器电机故障");
                        isPayoutFault = TRUE;
                        break;
                    case 0x27:   qDebug() << "Dispenser Jam:出币器卡币";
                        emit error("|7|");
                        emit errorMessage("纸币器卡币故障");
                        isPayoutFault = TRUE;
                        break;
                    case 0x28:   qDebug() << "ROM checksum error:固件校验失败";
                        break;
                    case 0x29:   qDebug() << "Dispenser disabled:出币器被禁用";
                        break;
                    case 0x2a:   qDebug() << "Bill waiting:纸币等待中";
                        break;
                    case 0x2b:   qDebug() << "Not used";
                        break;
                    case 0x2c:   qDebug() << "Not used";
                        break;
                    case 0x2d:   qDebug() << "Not used";
                        break;
                    case 0x2e:   qDebug() << "Not used";
                        break;
                    case 0x2f:   qDebug() << "Filled key pressed:Filled键被按下";
                        break;
                    default:
                        qDebug() << "纸币器发出未知故障";
                        break;
                    }
                    if (isPayoutFault && isBillSupportPayout && isBillPayouting) {
                        // 纸币找零失败，开始硬币找零
                        totalPayoutBill = -1;
                        isBillPayouting = FALSE;
                        qWarning() << "纸币找零失败，开始硬币找零";
                        payOut(totalPayoutMoney);
                    }
                }
            }
        }
    }
}

void MDBmanager::explainResponse(QByteArray buf)
{
    QByteArray data = QByteArray::fromHex(buf);
    bool_t cmdHandleDone = FALSE;
    qDebug() << "开始解析数据" << data.toHex() << "当前请求:" << request.toHex();
    int len = data.size();
    if (request != "\n") {
        qDebug() << "收到MDB数据：" << data.toHex();
    }

    if (len > 0 && !request.isEmpty()) {
        if (request != "\n") {
            qDebug() << "进入普通响应解析流程";
        }
        if (data.at(0) == 0xFF) {
            cmdHandleDone = TRUE;
        } else {
            switch (request.at(0)) {
            case MDB_BILL_SETUP:
                //01 11 56 00 64 02 00 c8 00 0f ff 01 05 0a 14 00 00 00 00 00 00 00 00 00 00 00 00 c8
                //01 00 86 00 0a 01 00 c8 ff ff ff 01 05 0a 14 00 00 00 00 00 00 00 00 00 00 00 00 7b
                //01 11 56 00 0a 01 01 f4 00 00 ff 01 05 0a 14 8b
                //02 19 78 00 64 02 01 2c ff ff ff 05 0a 14 32 00000000000000000000000078
                if (data.size() >= 11 && data.size() <= 28) {
                    qDebug() << "纸币器设置数据" << data.toHex();
                    int level = data.at(0); // level是2时，需要确认是否支持找零
                    // 如果level >= 2
                    // 查询纸币机器的各种扩展信息 37H 02H，解析每个字段内容
                    // 检查是否支持找零Bill Recycling supported
                    // 支持找零再查RECYCLER SETUP 37H 03H，获取找零路径
                    // 调用使能找零RECYCLER ENABLE 37H 04H，同硬币器一样调用获取剩余纸币数的功能
                    // BILL DISPENSE STATUS 37H 05H，获取某路径的纸币数，计算可找零金额

                    // 需要找零时调用DISPENSE BILL 37H 06H，单张找零，避免复杂找零逻辑
                    // 然后发送PAYOUT STATUS 37H 08H 确认纸币器是否收到出币指令和计算出币数量和价值

                    // 然后轮询PAYOUT VALUE POLL 37H 09H 获取已经拿走的纸币数量，当数量等于预先计划要退出的数量时，结束流程。
                    // 再查询PAYOUT STATUS 37H 08H 确认纸币器已经出币完成了
                    int country = data.at(2) + data.at(1) * 0x100;
                    billScalingFactor = data.at(4) + data.at(3) * 0x100;    //100
                    billDecimalPlaces = data.at(5); //2
                    int escrow = data.at(10);
                    isSupportEscrow = (escrow == 0xff);
                    qDebug() << "是否支持暂存" << isSupportEscrow;

                    int channalNum = (uchar)data.at(9) + (uchar)data.at(8) * 0x100; //63
                    QByteArray billValueData = data.mid(11);
                    if (channalNum == 0) {
                        channalNum = billValueData.size() - 1;
                    }
                    for (int i = 0; (i < channalNum && i < billValueData.size() - 1); i++) {
                        // 2 0 1 0 100 2 0 200 0 63 255 1
                        billValues[i] = billValueData.at(i);
                        if (getBillValue(i) != 0) {
                            qDebug() << "可收金额：" << getBillValue(i);
                        }
                    }

                    if (level >= 2) {
                        setupBillSecurity();
                    }
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_COIN_SETUP:
                //03 11 56 05 01 00 03 01 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 76
                //03 00 86 05 01 00 03 01 02 00 00 00 00 00 00 00 00 00 00 00 00 00 00 95
                //03 11 56 05 01 00 03 01 02 76
                //03 11 56 05 02 00 03 01 02 76 美元的例子，1是0.05美元，2是0.1美元
                //03 19 78 01 02 00 3f 05 0a 14 32 64 c8 0000000000000000000057
                if (data.size() >= 8 && data.size() <= 24) {
                    int level = data.at(0);
                    int country = data.at(2) + data.at(1) * 0x100;
                    coinScalingFactor = data.at(3);
                    coinDecimalPlaces = data.at(4);

                    for (int i = 0; i < 16 && i < data.size() - 8; i++) {
                        coinValues[i] = (uchar)data.at(7 + i);

                        if (getCoinValue(i) != 0) {
                            qDebug() << "可收金额：" << getCoinValue(i);
                        }
                    }
                    cmdHandleDone = TRUE;
                }
                break;
            // 49544c 303030303035333030343539 4e5631312033373920303030 0924 00000002 eb 34字节
            case MDB_BILL_EXPANSION: {
                switch (request.at(1)) {
                case MDB_BILL_EXPANSION_IDENTIFICATION_WITH_OPTION:
                    if (data.size() >= 33) {
                        QByteArray manufacturerCode = data.mid(0, 3);
                        QByteArray serialNumber = data.mid(3, 12);
                        QByteArray modelTuningRevision = data.mid(15, 12);
                        QByteArray softwareVersion = data.mid(27, 2);
                        QByteArray optionalFeatures = data.mid(29, 4);
                        qDebug() << manufacturerCode.toHex() << serialNumber << modelTuningRevision << softwareVersion.toHex() <<
                                 optionalFeatures.toHex();
                        if (optionalFeatures.at(3) & Bill_Recycling_Supported) {
                            isBillSupportPayout = TRUE;
                            qDebug() << "纸币器支持找零";
                            setBillFeatureEnable();
                        }
                        cmdHandleDone = TRUE;
                    }
                    break;
                // 使能找零功能
                case MDB_BILL_EXPANSION_FEATURE_ENABLE:
                    if (data.size() == 1 && data.at(0) == 0x00) {
                        getBillExpansionRecyclerSetupInfo();
                        cmdHandleDone = TRUE;
                    }
                    break;
                // 两个字节，按位获取可找零的通路，获取金额，以计算找零金额
                // 00 04 04
                case MDB_BILL_EXPANSION_RECYCLER_SETUP:
                    if (data.size() > 2) {
                        int supoortBillRoute = data.at(1) + data.at(0) * 0x100;
                        for (int i = 0; i < 16; i++) {
                            if (supoortBillRoute & 1 << i) {
                                qDebug() << "纸币器支持找零的面额：" << getBillValue(i);
                            }
                        }
                        cmdHandleDone = TRUE;
                        enableBillExpansionRecycler();
                    }
                    break;
                // 设置找零通路使能，回复ACK
                case MDB_BILL_EXPANSION_RECYCLER_ENABLE:
                    if (data.size() == 1 && data.at(0) == 0x00) {
                        checkBillExpansionDispenserStatue();
                        cmdHandleDone = TRUE;
                    }
                    break;
                // 34个字节，前两个字节指示找零通路是否满了，后面指示找零通路有多少纸币数
                // 00 00 00 06 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 06
                case MDB_BILL_EXPANSION_BILL_DISPENSE_STATUS:
                    if (data.size() >= 34) {
                        bool_t needPayoutMoney = FALSE;
                        int outType = 0;
                        int outCount = 0;
                        if (isBillPayouting) {
                            // 正在找零纸币，需要检查面额最高的纸币，看看能找多少，找多少。
                            needPayoutMoney = TRUE;
                        }
                        QByteArray tmp = data.mid(0, 2);
                        qDebug() << "纸币数量字段:" << tmp.toInt() << ":" << data.toHex();
                        int billSum = 0;
                        tmp = data.mid(2);
                        for (int i = 30; i >= 0; i -= 2) {
                            int num = tmp.at(i) * 0x100 + tmp.at(i + 1);
                            int billType = i / 2;
                            qDebug() << "纸币剩余：" << billType << num;
                            //计算总金额
                            int billValue = getBillValue(billType);
                            billSum += num * billValue;
                            if (needPayoutMoney && num > 0 && totalPayoutBill >= billValue) {
                                outType = billType;
                                outCount = 1;
                                break;
                            }
                        }
                        if (outCount > 0) {
                            billPayoutType = outType;
                            payOutBill(outType);
                            startCheckBillPayoutStatue();
                        }
                        // 需要找零，但是数量为0，那么说明没有纸币可以找了，需要靠硬币了
                        qDebug() << "是否需要找零" << needPayoutMoney << "当前需要找零的币数" << outCount;
                        if (needPayoutMoney && outCount == 0) {
                            totalPayoutBill = -1;
                            if (totalPayoutMoney > 0) {
                                // 纸币找零完成，开始硬币找零
                                qDebug() << "纸币找零完成，开始硬币找零";
                                isBillPayouting = FALSE;
                                payOut(totalPayoutMoney);
                            } else {
                                // 找零全部完成
                                qDebug() << "退币完成，不需要再找硬币";
                                isBillPayouting = FALSE;
                                isPayouting = FALSE;
                                emit completePayout();
                                totalMoney = 0;
                                emit cashInTotal(totalMoney, 0, 0);
                                totalPayoutBill = 0;
                            }
                        }
                        qDebug() << "检查纸币金额：" << billSum;
                        billMoney = billSum;
                        cmdHandleDone = TRUE;
                    }
                    break;
                // 找零命令回复
                case MDB_BILL_EXPANSION_DISPENSE_BILL:
                    if (data.size() == 1 && data.at(0) == 0x00) {
                        qDebug() << "开始找零";
                        cmdHandleDone = TRUE;
                    }
                    break;
                // 找零状态，指示每一个通路要出的币数，需要和之前找零命令的数量做比较
                // 00 01 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00 00
                case MDB_BILL_EXPANSION_PAYOUT_STATUS:
                    if (data.size() >= 32) {
                        int count = data.at(billPayoutType * 2) * 0x100 + data.at(billPayoutType * 2 + 1);
                        if (count > 0) {
                            checkBillExpansionDispenserStatue();
                        }
                        cmdHandleDone = TRUE;
                    }
                    break;
                // 查询找零状态，00表示没有要找零的纸币，ACK表示找零完成，其他数表示已经找零的张数
                case MDB_BILL_EXPANSION_PAYOUT_VALUE_POLL:
                    if (data.size() == 1 && data.at(0) == 0x00) {
                        // 找零完成
                        qDebug() <<
                                 "收到纸币找零完成，需要确认一下是否会收到确认币数后还会收到ACK，同一次出币收到两个正常回应会导致少退款。";
                        totalPayoutBill -= getBillValue(billPayoutType);
                        totalPayoutMoney -= getBillValue(billPayoutType);
                        stopCheckBillPayoutStatue();
                        checkBillExpansionDispenserStatue();
                        cmdHandleDone = TRUE;
                    }
                    if (data.size() >= 2) {
                        int count = data.at(1) + data.at(0) * 0x100;
                        if (count == 1) {
                            qDebug() <<
                                     "收到找出币数为1，应该是找完了，需要确认一下是否会收到确认币数后还会收到ACK，同一次出币收到两个正常回应会导致少退款。";
                            // 注释掉以下4行，在收到这个命令后不减找零金额。
                            //                        totalPayoutBill -= getBillValue(billPayoutType);
                            //                        totalPayoutMoney -= getBillValue(billPayoutType);
                            //                        stopCheckBillPayoutStatue();
                            //                        checkBillExpansionDispenserStatue();
                            // 注释以下一行，收到这个命令，继续发此指令，确认收到ACK后再处理
                            //                        cmdHandleDone = TRUE;
                        } else {
                            qDebug() << "收到找出币数为其他，数据出错，不会单次找多张。不处理";
                        }
                    }
                    break;
                // 取消找零
                case MDB_BILL_EXPANSION_PAYOUT_CANCEL:
                    cmdHandleDone = TRUE;
                    break;
                }

                break;
            }
            case MDB_BILL_ENABLE:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_COIN_ENABLE:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_BILL_RESET:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_COIN_RESET:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_COIN_PAYOUT:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    qDebug() << "退币指令已被收到，暂停检查硬币数";
                    isCoinPayouting = TRUE;
                    coinNumCheckTimer.stop();
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_COIN_NUM:
                // 00 00 01 12 00 00 00 00 00 00 00 00 00 00 00 00 00 00 13
                // 00 00 10 1c 00 00 00 00 00 00 00 00 00 00 00 00 00 00 2c
                // 00 00 00 01 01 11 1f 10 00 00 00 00 00 00 00 00 00 00 42
                // 00 00 00 00 08 03 00 00 00 00 00 00 00 00 00 00 00 00 0b
                if (data.size() == 19) {
                    QByteArray tmp = data.mid(0, 2);
                    qDebug() << "硬币数量字段:" << tmp.toInt() << ":" << data.toHex();
                    if (tmp.toInt() > 0 && isCoinFull != TRUE) {
                        coinFull();
                    } else if (tmp.toInt() <= 0 && isCoinFull == TRUE) {
                        coinNotFull();
                    }
                    int coinSum = 0;
                    allCoinValue.clear();
                    for (int i = 2; i < 16 + 2; i++) {
                        int num = (int)data.at(i);
                        if (num == 0 && !isCoinEmpty[i - 2]) {
                            coinEmpty(i - 2);
                        } else if (num != 0 && isCoinEmpty[i - 2]) {
                            coinNotEmpty(i - 2);
                        }
                        //计算总金额
                        int coinType = getCoinValue(i - 2);
                        if (num > 0) {
                            for (int j = 0; j < num; j++) {

                                allCoinValue.append(coinValues[i - 2]);
                                // qDebug() << "硬币所有硬币数" << allCoinValue.size() << coinValues[i - 2];
                            }
                        }
                        coinSum += num * coinType;
                    }
                    //剩余硬币少于20元
                    qDebug() << "检查硬币金额：" << coinSum << "当前硬币状态isCoinLess：" << isCoinLess;
                    coinMoney = coinSum;
                    //                if (coinSum < EnvEnum::getCoinThresholdValue() * 100) {
                    if (coinSum < 10 * 100) {
                        qDebug() << "硬币少了";
                        coinLess(coinSum);
                    } else if (isCoinLess) {
                        qDebug() << "硬币足了";
                        coinNotLess(coinSum);
                    }
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_BILL_SECURITY:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    getBillExpansionSetupInfo();
                    cmdHandleDone = TRUE;
                }
                break;
            case MDB_BILL_ESCROW:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    qDebug() << "暂存指令下发成功: 重发剩余次数" << escrowCmdCounter;
                    if (--escrowCmdCounter > 0) {
                        if (isEscrowInOrOut) {
                            letBillIn();
                        } else {
                            letBillOut();
                        }
                    }
                    cmdHandleDone = TRUE;
                }
                break;
            case SSP_SYNC_CMD:
                if ((data.size() == 1 && data.at(0) == 0x00) || (data.at(0) == 0x10 && data.at(1) == 0x01) || (data.at(0) == 0x01
                                                                                                               && data.at(1) == 0x01)) {
                    if (request.at(1) == 0x00) {
                        setCashlessSetupPrice();
                        cmdHandleDone = TRUE;
                    } else if (request.at(1) == 0x01) {
                        setCashlessExpansionRequest();
                        cmdHandleDone = TRUE;
                        qDebug() << "mdb刷卡器设置congfig成功";
                    }
                }

                break;
            case MDB_CASHLESS_EXPANSION:
                if (request.at(1) == 0x00) {
                    //                    if (data.at(0) == 0x10 && data.at(1) == 0x09) {
                    setCashlessExpansionEnable();
                    cmdHandleDone = TRUE;
                    //                    }
                } else if (request.at(1) == 0x04) {
                    if (data.size() == 1 && data.at(0) == 0x00) {
                        setCashlessReaderEnable();
                        cmdHandleDone = TRUE;
                    }
                }

                break;
            case MDB_CASHLESS_READER:
                if (data.size() == 1 && data.at(0) == 0x00) {
                    if (request.at(1) == 0x01) {
                        isCashlessEnable = TRUE;
                    } else if (request.at(1) == 0x00) {
                        isCashlessEnable = FALSE;
                    }
                    cmdHandleDone = TRUE;
                }

                break;
            case MDB_CASHLESS_VEND:
                cmdHandleDone = TRUE;
                break;
            }
        }

    } else if (len > 0 && request.isEmpty()) {
        if (data.at(0) == 0x10) {
            if (data.at(1) == 0x00) {
                setCashlessSetupConfig();
            } else if (data.at(1) == 0x05) {
                emit paySuccess(totalPayMoney);
                isOutting = TRUE;
                cmdHandleDone = TRUE;
            } else if (data.at(1) == 0x06) {
                cashlessVendOutComplete();
                cmdHandleDone = TRUE;
            }
        }
    }
    if (cmdHandleDone == TRUE) {
        request.clear();
        currentCmdDone = TRUE;
    } else {
        explainPoll(buf);
    }
}

void MDBmanager::responseTimeout()
{
    // 判断是否回复过，回复过就删掉，处理下一个命令，否则重发当前命令
    isSending = FALSE;
    if (currentCmdDone == TRUE) {
        if (!cmdList.isEmpty()) {
            MDBCmd cmd = cmdList.takeFirst();

            transaction(cmd.cmd, cmd.delay, cmd.priority);
        } else if (isCheckingBillPayoutStatue == TRUE) {
            billPayoutValuePoll();
        } else if (isPolling == TRUE) {
            poll();
        }
    } else {
        currentReSendTimes++;
        if (currentReSendTimes >= 5) {
            currentReSendTimes = 0;
            if (request.size() >= 2) {
                if (request.at(0) == MDB_BILL_EXPANSION && request.at(1) == MDB_BILL_EXPANSION_BILL_DISPENSE_STATUS) {
                    if (isBillSupportPayout && isBillPayouting) {
                        // 纸币找零失败，开始硬币找零
                        qWarning() << "纸币找零失败，开始硬币找零";
                        totalPayoutBill = -1;
                        isBillPayouting = FALSE;
                        payOut(totalPayoutMoney);
                    }
                }
            }
            if (!cmdList.isEmpty()) {
                MDBCmd cmd = cmdList.takeFirst();
                transaction(cmd.cmd, cmd.delay, cmd.priority);
            } else if (isCheckingBillPayoutStatue == TRUE) {
                billPayoutValuePoll();
            } else if (isPolling == TRUE) {
                poll();
            }
        } else {
            transaction(request, waitTimeout);
        }
    }

}

static int CRC_Table[8 * 32] = {
    0x0000, 0x8005, 0x800F, 0x000A, 0x801B, 0x001E, 0x0014, 0x8011,
    0x8033, 0x0036, 0x003C, 0x8039, 0x0028, 0x802D, 0x8027, 0x0022,
    0x8063, 0x0066, 0x006C, 0x8069, 0x0078, 0x807D, 0x8077, 0x0072,
    0x0050, 0x8055, 0x805F, 0x005A, 0x804B, 0x004E, 0x0044, 0x8041,
    0x80C3, 0x00C6, 0x00CC, 0x80C9, 0x00D8, 0x80DD, 0x80D7, 0x00D2,
    0x00F0, 0x80F5, 0x80FF, 0x00FA, 0x80EB, 0x00EE, 0x00E4, 0x80E1,
    0x00A0, 0x80A5, 0x80AF, 0x00AA, 0x80BB, 0x00BE, 0x00B4, 0x80B1,
    0x8093, 0x0096, 0x009C, 0x8099, 0x0088, 0x808D, 0x8087, 0x0082,
    0x8183, 0x0186, 0x018C, 0x8189, 0x0198, 0x819D, 0x8197, 0x0192,
    0x01B0, 0x81B5, 0x81BF, 0x01BA, 0x81AB, 0x01AE, 0x01A4, 0x81A1,
    0x01E0, 0x81E5, 0x81EF, 0x01EA, 0x81FB, 0x01FE, 0x01F4, 0x81F1,
    0x81D3, 0x01D6, 0x01DC, 0x81D9, 0x01C8, 0x81CD, 0x81C7, 0x01C2,
    0x0140, 0x8145, 0x814F, 0x014A, 0x815B, 0x015E, 0x0154, 0x8151,
    0x8173, 0x0176, 0x017C, 0x8179, 0x0168, 0x816D, 0x8167, 0x0162,
    0x8123, 0x0126, 0x012C, 0x8129, 0x0138, 0x813D, 0x8137, 0x0132,
    0x0110, 0x8115, 0x811F, 0x011A, 0x810B, 0x010E, 0x0104, 0x8101,
    0x8303, 0x0306, 0x030C, 0x8309, 0x0318, 0x831D, 0x8317, 0x0312,
    0x0330, 0x8335, 0x833F, 0x033A, 0x832B, 0x032E, 0x0324, 0x8321,
    0x0360, 0x8365, 0x836F, 0x036A, 0x837B, 0x037E, 0x0374, 0x8371,
    0x8353, 0x0356, 0x035C, 0x8359, 0x0348, 0x834D, 0x8347, 0x0342,
    0x03C0, 0x83C5, 0x83CF, 0x03CA, 0x83DB, 0x03DE, 0x03D4, 0x83D1,
    0x83F3, 0x03F6, 0x03FC, 0x83F9, 0x03E8, 0x83ED, 0x83E7, 0x03E2,
    0x83A3, 0x03A6, 0x03AC, 0x83A9, 0x03B8, 0x83BD, 0x83B7, 0x03B2,
    0x0390, 0x8395, 0x839F, 0x039A, 0x838B, 0x038E, 0x0384, 0x8381,
    0x0280, 0x8285, 0x828F, 0x028A, 0x829B, 0x029E, 0x0294, 0x8291,
    0x82B3, 0x02B6, 0x02BC, 0x82B9, 0x02A8, 0x82AD, 0x82A7, 0x02A2,
    0x82E3, 0x02E6, 0x02EC, 0x82E9, 0x02F8, 0x82FD, 0x82F7, 0x02F2,
    0x02D0, 0x82D5, 0x82DF, 0x02DA, 0x82CB, 0x02CE, 0x02C4, 0x82C1,
    0x8243, 0x0246, 0x024C, 0x8249, 0x0258, 0x825D, 0x8257, 0x0252,
    0x0270, 0x8275, 0x827F, 0x027A, 0x826B, 0x026E, 0x0264, 0x8261,
    0x0220, 0x8225, 0x822F, 0x022A, 0x823B, 0x023E, 0x0234, 0x8231,
    0x8213, 0x0216, 0x021C, 0x8219, 0x0208, 0x820D, 0x8207, 0x0202
};

void Update_CRC(unsigned char num, uchar *CRCH, uchar *CRCL)
{
    unsigned int table_addr;

    table_addr = (num ^ *CRCH);
    *CRCH      = (CRC_Table[table_addr] >> 8) ^ *CRCL;
    *CRCL      = (CRC_Table[table_addr] & 0x00FF);
}

ushort MDBmanager::getCRC(QByteArray buf)
{
    uchar CRCL = 0xFF, CRCH = 0xFF;

    for (int i = 1; i < buf.size(); i++) {
        Update_CRC(buf[i], &CRCH, &CRCL);      // 计算 Crc，0x7F 不参加 Crc 计算
    }
    ushort crc = (ushort)CRCL + ((ushort)CRCH << 8);
    return crc;
}

void MDBmanager::stopPoll()
{
    isPolling = FALSE;
}

void MDBmanager::startPoll()
{
    isPolling = TRUE;
}
