package com.jpgk.hardwaresdk.hardwareprotocolimpl;

import static com.jpgk.hardwaresdk.hardwareprotocolimpl.PrinterUtils.bitmapToImageData;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;


import com.jpgk.hardwaresdk.HardwareSDK;
import com.jpgk.hardwaresdk.model.PrinterAcknowledgementReceipt;
import com.jpgk.hardwaresdk.model.PrinterBasic;
import com.jpgk.hardwaresdk.model.PrinterBottom;
import com.jpgk.hardwaresdk.model.PrinterChangeRecovery;
import com.jpgk.hardwaresdk.model.PrinterModel;
import com.jpgk.hardwaresdk.utils.DLogger;
import com.jpgk.hardwaresdk.utils.DateUtils;
import com.vi.vioserial.COMSerial;
import com.vi.vioserial.listener.OnComDataListener;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;


//KP-230H 打印机
public class PrinterManager implements BaseHardware {

    public static final int PRINT_TYPE_NOT_SPLIT_DETAILS = 0; // 不切明細
    public static final int PRINT_TYPE_SPLIT_DETAILS = 1; // 切明細
    public static final int PRINT_TYPE_DROPPED = 2;//發票作廢
    public static final int PRINT_TYPE_ONLY_RECEIPT = 3; // 不切明細
    public static final int PRINT_TYPE_OPERATE_RECORD = 4;
    public static final int PRINT_TYPE_SHOUKUAN_TONGJI_RECORD = 5;
    public static final int PRINT_TYPE_HARDWARE_TEST = 6;
    public static final int PRINT_TYPE_HARDWARE_TEST_RESULT = 7;

    public static final int PRINT_TYPE_FACTORY_TEST = 11;

    public interface OnPrinterDataListener{
        void onDataResponse(String hexData);
    }
    private OnPrinterDataListener onPrinterDataListener;
    private String portPath;
    private int portRate;

    private OnComDataListener onComDataListener;

    private boolean isPrinterOnLine;
    private boolean isPrintOK = false;
    private int printType = PRINT_TYPE_FACTORY_TEST;

    private String aesKey = "qMTbRSzPwYOTa9K6bJAQdw==";



    private Order order;
    public DDPayOrderInvoiceInfo orderInvoiceInfo;

    public PrinterModel printerModel;


    public PrinterManager(String portPath,int portRate,OnPrinterDataListener onPrinterDataListener){
        this.portPath = portPath;
        this.portRate = portRate;
        this.onPrinterDataListener = onPrinterDataListener;
    }


    public void enable(){
       openSerialPort();
    }

    public void disable(){
        closePort();
    }

    @Override
    public int openSerialPort() {
        int state = COMSerial.instance().addCOM(portPath,portRate);
        onComDataListener = new OnComDataListener() {
            @Override
            public void comDataBack(String com, String hexData) {
                if (portPath.equals(com)){
                    DLogger.log("printer data recv:"+hexData);
                    Log.w("PrinterManager","Recv data:"+hexData);
                    isPrinterOnLine = true;
                    if ("12121212121212".equals(hexData)||"92929292929292".equals(hexData)){
                        isPrintOK = true;
                    }
                    if(onPrinterDataListener != null){
                        onPrinterDataListener.onDataResponse(hexData);
                    }
                }
            }
        };
        COMSerial.instance().addDataListener(onComDataListener);
        return state;
    }

    @Override
    public void closePort() {
        COMSerial.instance().removeDataListener(onComDataListener);
        COMSerial.instance().close(portPath);
    }

    @Override
    public void reconnect() {
        closePort();
        openSerialPort();
    }

    @Override
    public boolean isSerialPortOpen() {
        return COMSerial.instance().isOpen(portPath);
    }


    @Override
    public boolean isOnLine() {
        return isPrinterOnLine;
    }

    @Override
    public String getHardwareName() {
        return "打印機";
    }

    @Override
    public int getHardwareType() {
        return BaseHardware.TYPE_PRINTER;
    }

    public boolean isPrintOk() {
        return isPrintOK;
    }

    public void setPrintOK(boolean printOK) {
        isPrintOK = printOK;
    }

    public void testPrinterConnection(){
        isPrinterOnLine = false;
        queryPrinterStatus(1);
    }



    //
    public void queryPrinterStatus(int statusType) {
        // 构造查询指令
        byte[] command = new byte[3];
        command[0] = 0x10; // DLE
        command[1] = 0x04; // EOT
        command[2] = (byte) statusType; // 状态类型

        // 发送指令到打印机
        COMSerial.instance().sendHex(portPath,bytesToHex(command));

        // 打印调试信息
        System.out.println("Query Command Sent: " + bytesToHex(command));
    }

    // 将字节数组转换为HEX字符串，方便调试和日志输出
    private String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            hexString.append(String.format("%02X ", b));
        }
        return hexString.toString().trim();
    }

    // 打印小票内容
    public void printReceipt() {
        if (printType == PRINT_TYPE_FACTORY_TEST){
            DLogger.log("Printer","打印小票");
            printFactoryTest();
//            printFactoryTest();
        }else if (printType == PRINT_TYPE_ONLY_RECEIPT){
            DLogger.log("Printer","打印小票");
            printOnlyReceipt();
        }  else {
            printOptimizedReceipt();
            DLogger.log("Printer","打印发票");
        }
    }


    private void printFactoryTest() {
        isPrintOK = false;
        if (printerModel == null)return;
        StringBuilder printData = new StringBuilder();
        printData.append("1B 40"); // 初始化命令
        printData.append("1B 61 01"); // 居中对齐
        DLogger.log("PRINTER LOGO START=========");
        Log.w("PrinterManager","PRINTER LOGO START=========");
        if (printerModel.getPrinterLogo() != null){
            // ====== 1. 打印图片 ======
            Bitmap bmp = BitmapFactory.decodeResource(HardwareSDK.INSTANCE.getApplication().getApplicationContext().getResources(), printerModel.getPrinterLogo().getLogoRes());
//            bmp = PrinterUtils.convertTransparentToWhiteAndWhiteToBlack(bmp);
            bmp = Bitmap.createScaledBitmap(bmp, 384, bmp.getHeight()*384/bmp.getWidth(), true);

            byte[] imageData = bitmapToImageData(bmp);
            int widthBytes = (bmp.getWidth() + 7) / 8;
            int height = bmp.getHeight();

            // GS v 0 m xL xH yL yH
            printData.append("1D 76 30 00 ");
            printData.append(String.format("%02X %02X ", widthBytes & 0xFF, (widthBytes >> 8) & 0xFF));
            printData.append(String.format("%02X %02X ", height & 0xFF, (height >> 8) & 0xFF));

            // image data 转 hex
            for (byte b : imageData) {
                printData.append(String.format("%02X ", b));
            }

            printData.append("1B 64 02"); // 增加2行空白
        }
        DLogger.log("PRINTER LOGO END=========");
        Log.w("PrinterManager","PRINTER LOGO END=========");
        printData.append("1D 21 00"); // 恢复默认字体
        printData.append("1B 61 01"); // 左对齐
        if (printerModel.getPrinterBasic() != null){
            PrinterBasic printerBasic = printerModel.getPrinterBasic();
            printItemLeftRight(printData,"Terminal S/N:",printerBasic.getTerminalSN());
            printItemLeftRight(printData,"Ref No:",printerBasic.getRefNO());
            printItemLeftRight(printData,"Date and Time:",DateUtils.convertDateFormat2(System.currentTimeMillis()));
        }
        printData.append("1B 61 01"); // 居中对齐
        printData.append(stringToHex("------------------------------------------------\n"));

        if (printerModel.getPrinterAcknowledgementReceipt() != null){
            printData.append("1D 21 01"); // 字体倍高倍宽
            printData.append(stringToHex("*** ACKNOWLEDGEMENT RECEIPT ***\n"));
//            printItemLeftMiddleRight(printData,"***","ACKNOWLEDGEMENT RECEIPT","***");
            printData.append("1D 21 00"); // 恢复默认字体
            printData.append("1B 61 01"); // 左对齐
            PrinterAcknowledgementReceipt acknowledgementReceipt = printerModel.getPrinterAcknowledgementReceipt();
            printItemLeftRight(printData,"Type of Transaction:",acknowledgementReceipt.getTypeOfTransaction());
            printItemLeftRight(printData,"Biller/Service:",acknowledgementReceipt.getBillerService());
            printItemLeftRight(printData,"Mobile Number:",acknowledgementReceipt.getMobileNumber());
            printItemLeftRight(printData,"Amount:",acknowledgementReceipt.getAmount());
            printItemLeftRight(printData,"Service Fee:",acknowledgementReceipt.getServiceFee());
            printItemLeftRight(printData,"VAT Amount:",acknowledgementReceipt.getVatAmount());
            printItemLeftRight(printData,"Total Amount:",acknowledgementReceipt.getTotalAmount());
            printItemLeftRight(printData,"Amount Received:",acknowledgementReceipt.getAmountReceived());

        }
        if (printerModel.getPrinterChangeRecovery() != null){
            printData.append("1D 21 00"); // 恢复默认字体
            printData.append("1D 21 01"); // 字体倍高倍宽
            printData.append("1B 61 01"); // 居中对齐
            printData.append(stringToHex("*** CHANGE RECOVERY ***\n"));
//            printItemLeftMiddleRight(printData,"***","CHANGE RECOVERY","***");
            printData.append("1D 21 00"); // 恢复默认字体
            printData.append("1B 61 01"); // 左对齐
            PrinterChangeRecovery changeRecovery = printerModel.getPrinterChangeRecovery();
            printItemLeftRight(printData,"Change Amount:",changeRecovery.getChangeAmount());
            printItemLeftRight(printData,"Action:",changeRecovery.getAction());
            printItemLeftRight(printData,"Biller/Service:",changeRecovery.getBillerService());
            printItemLeftRight(printData,"Mobile Numbe:",changeRecovery.getMobileNumber());
            printItemLeftRight(printData,"Amount:",changeRecovery.getAmount());
            printItemLeftRight(printData,"Service Fee:",changeRecovery.getServiceFee());
            printItemLeftRight(printData,"VAT Amount:",changeRecovery.getVatAmount());
            printItemLeftRight(printData,"Ref No (Change):",changeRecovery.getRefNoChange());
        }

        printData.append("1B 61 01"); // 居中对齐
        printData.append(stringToHex("------------------------------------------------\n"));
        if (printerModel.getPrinterBottom() != null){
            PrinterBottom printerBottom = printerModel.getPrinterBottom();
            printData.append("1D 21 00"); // 恢复默认字体
            printData.append("1D 21 01"); // 字体倍高倍宽
            printData.append("1B 61 01"); // 居中对齐
            printData.append(stringToHex("*** "+printerBottom.getTipsMain()+ " ***\n"));
            printData.append("1D 21 00"); // 恢复默认字体
            printData.append(stringToHex("*** "+printerBottom.getTipsSub()+ " ***\n"));
        }

        printData.append("1B 64 05"); // 增加5行空白
        printData.append("1D 56 00"); // 全切纸
        // 恢复默认字体
        COMSerial.instance().sendHex(portPath,printData.toString());
    }


    // 只打印小票
    private void printOnlyReceipt() {
        // 初始化打印机
        StringBuilder printData = new StringBuilder();
        printData.append("1B 40"); // 初始化命令
        // 恢复默认字体
        printData.append("1D 21 00"); // 恢复默认字体
        printData.append("1B 61 01"); // 居中对齐
        printData.append("1D 21 11"); // 字体倍高倍宽
        String machineCode = "jp003";//App.constantsViewModelInstance.getMachineCode();
        printData.append(stringToHex("自助付款收執聯\n"));
        printData.append("1D 21 00"); // 恢复默认字体
        // 设置左对齐
        printData.append("1B 61 00"); // 左对齐
        printData.append(stringToHex("-----------------------------------\n"));
        printData.append("1D 21 11"); // 字体倍高倍宽
        printData.append(stringToHex("取餐編號："+order.getCheckNo()+"\n"));

        printData.append(stringToHex("實收金額："+"$"+orderInvoiceInfo.getInvoiceAmountInt()+"\n"));
//        String itemLeft1 = "實收金額";
//        String itemRight1 = "$"+orderInvoiceInfo.getInvoiceAmountInt();
//        printItemLeftRight(printData,itemLeft1,itemRight1);

        printData.append("1D 21 00"); // 恢复默认字体
        //printData.append(stringToHex("門店名稱："+"\n"));
        printData.append(stringToHex("設備編號："+machineCode+"\n"));
//        printData.append(stringToHex("訂單編號："+orderInvoiceInfo.getOrderNo()+"\n"));
        printData.append(stringToHex("交易日期："+ DateUtils.convertDateFormat(orderInvoiceInfo.getNTXNDateTime())+"\n"));
        printData.append(stringToHex("付款方式："+order.getPayMethod()+"\n"));
        printData.append(stringToHex("================================\n"));
        printData.append(stringToHex("請憑此單取餐\n"));

        printData.append("1B 64 05"); // 增加5行空白
        printData.append("1D 56 00"); // 全切纸
        // 统一发送数据
        ///NormalSerial.instance().sendHex(printData.toString());
        COMSerial.instance().sendHex(portPath,printData.toString());
    }

    public void printOptimizedReceipt() {
        // 初始化打印机
        StringBuilder printData = new StringBuilder();
        printData.append("1B 40"); // 初始化命令

        // 设置字体倍高倍宽，居中对齐
        printData.append("1B 61 01"); // 居中对齐
        printData.append("1D 21 11"); // 字体倍高倍宽
        if (TextUtils.isEmpty(orderInvoiceInfo.getSellerName())){
            printData.append(stringToHex("DDPay\n電子發票證明聯\n"));
        }else {
            printData.append(stringToHex(orderInvoiceInfo.getSellerName()+"\n電子發票證明聯\n"));
        }

        printData.append(stringToHex(convertDate(orderInvoiceInfo.getEInvYm())+"\n"));
        printData.append(stringToHex(orderInvoiceInfo.getInvoiceNumber()+"\n"));

        // 恢复默认字体
        printData.append("1D 21 00"); // 恢复默认字体

        // 设置左对齐
        printData.append("1B 61 00"); // 左对齐

        // 打印日期、发票编号、随机码等
        printData.append(stringToHex(DateUtils.convertDateFormat(orderInvoiceInfo.getNTXNDateTime())+"\n"));
        printData.append(stringToHex("隨機碼 "+orderInvoiceInfo.getRandomNumer()+"    總計 $"+orderInvoiceInfo.getInvoiceAmountInt()+"\n"));
        if (TextUtils.isEmpty(orderInvoiceInfo.getBuyerId())){
            printData.append(stringToHex("賣方 "+orderInvoiceInfo.getSellerIdentifier()+"\n"));
        }else {
            printData.append(stringToHex("賣方 "+orderInvoiceInfo.getSellerIdentifier()+"    買方 "+orderInvoiceInfo.getBuyerId()+"\n"));
        }

        // 打印条码
        printBarcodeData(printData);
        printData.append("1B 64 01"); // 增加5行空白

        // 打印两个并列的二维码
        printTwoQRCodeWithUSQCommand(printData);

        // 打印条码 底部
        //printBarcodeData(printData);
        if (printType == PRINT_TYPE_NOT_SPLIT_DETAILS){
            printData.append("1B 64 01"); // 增加1行空白
            printData.append("1B 61 00"); // 左对齐
            printData.append(stringToHex("----------------------------\n"));
            printData.append("1B 61 01"); // 居中对齐
            printData.append(stringToHex("交易明細\n"));
            printData.append("1B 61 00"); // 左对齐
            printData.append(stringToHex("----------------------------\n"));
            // 设置左对齐
            printData.append("1B 61 00"); // 左对齐
            printInvoiceItemDetails(orderInvoiceInfo.getItemDetails(),printData);
            printData.append(stringToHex("----------------------------\n"));
            printSummary(orderInvoiceInfo,printData);
            printData.append(stringToHex("\n"));
            printData.append(stringToHex("----------------------------\n"));
            //printTaxTypeTotalAmount(printData);
            printData.append(stringToHex("----------------------------\n"));
        }else if (printType == PRINT_TYPE_SPLIT_DETAILS){
            printData.append("1B 64 05"); // 增加1行空白
            printData.append("1D 56 01"); // 半切纸
            // 设置字体倍高倍宽，居中对齐
            printData.append("1B 61 01"); // 居中对齐
            printData.append("1D 21 11"); // 字体倍高倍宽
            if (TextUtils.isEmpty(orderInvoiceInfo.getSellerName())){
                printData.append(stringToHex("DDPay\n交易明細\n"));
            }else {
                printData.append(stringToHex(orderInvoiceInfo.getSellerName()+"\n交易明細\n"));
            }

            // 恢复默认字体
            printData.append("1D 21 00"); // 恢复默认字体
            // 设置左对齐
            printData.append("1B 61 00"); // 左对齐
            printData.append(stringToHex(DateUtils.convertDateFormat(orderInvoiceInfo.getNTXNDateTime())+"\n"));
            printInvoiceItemDetails(orderInvoiceInfo.getItemDetails(),printData);
            printSummary(orderInvoiceInfo,printData);
        }

        // 增加空白行
        printData.append("1B 64 05"); // 增加5行空白

        // 全切纸指令
        printData.append("1D 56 00"); // 全切纸

        // 统一发送数据
        ///NormalSerial.instance().sendHex(printData.toString());
        COMSerial.instance().sendHex(portPath,printData.toString());
    }



    private void printItemLeftRight3(StringBuilder printData,String itemLeft,String itemRight){
        int targetLength = 28;
        // 遍历商品数据并格式化输出
        String leftContent = itemLeft;
        String padLeftSpace = "";
        int length = calculateCharacterCount(leftContent);
        if (length < 28){
            for (int i = 0; i < targetLength - length;i++){
                padLeftSpace =padLeftSpace+" ";
            }
        }
        leftContent = leftContent+padLeftSpace;
        String rightContent = itemRight;
        String padRightSpace = "";
        int lengthRight = calculateCharacterCount(rightContent);
        if (lengthRight < 28){
            for (int j = 0; j < targetLength - lengthRight;j++){
                padRightSpace = padRightSpace+" ";
            }
        }
        rightContent = padRightSpace+rightContent;
        String total = leftContent+rightContent+"\n";
        printData.append(stringToHex(total));
    }

    /**
     * 打印左中右对齐的内容
     * @param printData 打印数据缓冲区
     * @param itemLeft 左侧内容
     * @param itemMiddle 中间内容
     * @itemRight 右侧内容
     */
    private void printItemLeftMiddleRight(StringBuilder printData,
                                          String itemLeft,
                                          String itemMiddle,
                                          String itemRight) {
        int maxLineLength = 48; // 每行最大字符数

        // 1. 处理截断：确保各部分不会太长
        itemLeft = truncateToWidth(itemLeft, maxLineLength);
        itemMiddle = truncateToWidth(itemMiddle, maxLineLength);
        itemRight = truncateToWidth(itemRight, maxLineLength);

        // 2. 计算各部分的宽度（考虑中文字符）
        int leftWidth = calculateCharacterCount(itemLeft);
        int middleWidth = calculateCharacterCount(itemMiddle);
        int rightWidth = calculateCharacterCount(itemRight);

        // 3. 检查总长度是否超出一行
        int totalWidth = leftWidth + middleWidth + rightWidth;
        if (totalWidth > maxLineLength) {
            // 如果超出，需要调整：优先保证左右两边，中间适当截断
            int availableForMiddle = maxLineLength - leftWidth - rightWidth;
            if (availableForMiddle > 0) {
                itemMiddle = truncateToWidth(itemMiddle, availableForMiddle);
                middleWidth = calculateCharacterCount(itemMiddle);
            } else {
                // 左右已经占满一行，中间无法显示
                itemMiddle = "";
                middleWidth = 0;
            }
        }

        // 4. 计算空格填充
        StringBuilder line = new StringBuilder();

        // 左侧部分（左对齐）
        line.append(itemLeft);

        // 左侧和中间之间的空格
        int spaceBetweenLeftAndMiddle = 2; // 默认至少2个空格
        int spaceBetweenMiddleAndRight = 2; // 默认至少2个空格

        // 计算中间部分的位置：使其大致居中
        int remainingSpace = maxLineLength - leftWidth - middleWidth - rightWidth;
        if (remainingSpace > 0) {
            // 将剩余空间平均分配到两侧
            spaceBetweenLeftAndMiddle = remainingSpace / 2;
            spaceBetweenMiddleAndRight = remainingSpace - spaceBetweenLeftAndMiddle;
        }

        // 添加左侧和中间之间的空格
        for (int i = 0; i < spaceBetweenLeftAndMiddle; i++) {
            line.append(' ');
        }

        // 中间部分
        line.append(itemMiddle);

        // 添加中间和右侧之间的空格
        for (int i = 0; i < spaceBetweenMiddleAndRight; i++) {
            line.append(' ');
        }

        // 右侧部分（右对齐，这里已经是行尾了）
        line.append(itemRight);
        line.append('\n');

        // 5. 添加到打印数据
        printData.append(stringToHex(line.toString()));
    }

    private void printItemLeftRight(StringBuilder printData, String itemLeft, String itemRight) {
        int maxLineLength = 48;

        // 处理左右内容长度
        itemLeft = truncateToWidth(itemLeft, maxLineLength);
        itemRight = truncateToWidth(itemRight, maxLineLength);

        int leftWidth = calculateCharacterCount(itemLeft);
        int rightWidth = calculateCharacterCount(itemRight);

        // 如果总长度超过最大值，优先截断左侧（可根据需求调整）
        if (leftWidth + rightWidth > maxLineLength) {
            itemLeft = truncateToWidth(itemLeft, maxLineLength - rightWidth);
            leftWidth = calculateCharacterCount(itemLeft);
        }

        // 构建一行
        StringBuilder line = new StringBuilder();
        line.append(itemLeft);

        // 添加中间空格
        int spaceCount = maxLineLength - leftWidth - rightWidth;
        for (int i = 0; i < spaceCount; i++) {
            line.append(' ');
        }

        line.append(itemRight);
        line.append('\n');

        printData.append(stringToHex(line.toString()));
    }

    private String truncateToWidth(String str, int maxWidth) {
        if (calculateCharacterCount(str) <= maxWidth) {
            return str;
        }

        StringBuilder result = new StringBuilder();
        int currentWidth = 0;

        for (char c : str.toCharArray()) {
            int charWidth = isFullWidthChar(c) ? 2 : 1;

            if (currentWidth + charWidth > maxWidth) {
                // 尝试添加省略号
                if (currentWidth + 3 <= maxWidth) {
                    result.append("...");
                }
                break;
            }

            result.append(c);
            currentWidth += charWidth;
        }

        return result.toString();
    }

    private boolean isFullWidthChar(char c) {
        // 常见的全角字符范围
        return (c >= '\uFF01' && c <= '\uFF5E') ||  // 全角ASCII字符
                (c >= '\u4E00' && c <= '\u9FFF') ||  // 中文汉字
                (c >= '\u3000' && c <= '\u303F');    // 中文标点符号
    }



    public  String convertDate(String date) {
        // 提取年份和月份
        String yearStr = date.substring(0, 4);
        String monthStr = date.substring(4, 6);

        // 将年份转换为民国年
        int year = Integer.parseInt(yearStr);
        int rocYear = year - 1911;  // 民国年 = 公历年 - 1911

        // 格式化月份为两位数字
        int month = Integer.parseInt(monthStr);

        // 获取从1月到该月的范围
        String formattedMonthStart = String.format("%02d", month - 1); // 月份减1
        String formattedMonthEnd = String.format("%02d", month); // 当前月

        // 返回结果
        return String.format("%d年%s-%s月", rocYear, formattedMonthStart, formattedMonthEnd);
    }

    private void printSummary(DDPayOrderInvoiceInfo orderInvoiceInfo, StringBuilder printData) {
        if (orderInvoiceInfo == null) return;
        if (orderInvoiceInfo.getItemDetails() == null)return;
        String summary=orderInvoiceInfo.getItemDetails().size()+" 項 總計 $"+orderInvoiceInfo.getInvoiceAmount();
        printData.append(stringToHex(summary));
    }

    // 使用 US Q 指令打印两个并列的 QRCode
    public void printTwoQRCodeWithUSQCommand(StringBuilder printData) {
        // 开始双二维码指令，两个二维码，模块大小为4
        printData.append("1F 51 02 04"); // 两个二维码，模块大小为 3

        String qrContentLeft = generateQRCodeData();
        String qrContentRight = generateQRCodeData2();
        // 确保两个二维码数据长度一致
        int maxLength = Math.max(qrContentLeft.length(), qrContentRight.length());
        // 使用空格补齐较短的数据
        qrContentLeft = String.format("%-" + maxLength + "s", qrContentLeft);
        qrContentRight = String.format("%-" + maxLength + "s", qrContentRight);
        // 使用 UTF-8 编码获取字节数组
        byte[] bytes = null;
        try {
            bytes = qrContentLeft.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        // 获取字节数组的长度（字节数）
        int byteLength = bytes.length;
        // 将字节数转换为16进制字符串
        String hexLength = Integer.toHexString(byteLength).toUpperCase();
        if (hexLength.length() == 1){
            hexLength = "0"+hexLength;
        }
        // 第一个二维码：位置 32（p1H=00, p1L=20），数据长度 10 字节（l1H=00, l1L=0A），纠错等级 ECC 1，自动版本
        printData.append("00 0A 00 "+hexLength+" 01 05");  // 位置 32, 数据长度 10, ECC 1, 自动版本
        printData.append(stringToHex2(qrContentLeft)); // 数据 "0123456789" 的十六进制表示


        // 使用 UTF-8 编码获取字节数组
        byte[] bytes2 = null;
        try {
            bytes2 = qrContentRight.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        // 获取字节数组的长度（字节数）
        int byteLength2 = bytes2.length;
        // 将字节数转换为16进制字符串
        String hexLength2 = Integer.toHexString(byteLength2).toUpperCase();
        if (hexLength2.length() == 1){
            hexLength2 = "0"+hexLength2;
        }

        // 第二个二维码：位置 100（p2H=00, p2L=64），数据长度 9 字节（l2H=00, l2L=09），纠错等级 ECC 2，自动版本
        printData.append("00 c0 00 "+hexLength2+" 01 05");
        printData.append(stringToHex2(qrContentRight));

    }

    // AES 加密方法
    public String encryptData(String data) throws Exception {
        // 创建 AES 密钥
        SecretKeySpec secretKey = new SecretKeySpec(aesKey.getBytes(StandardCharsets.UTF_8), "AES");
        if (!TextUtils.isEmpty(orderInvoiceInfo.getQrcode())){
            secretKey = new SecretKeySpec(orderInvoiceInfo.getQrcode().getBytes(StandardCharsets.UTF_8), "AES");
        }
        // 创建加密器并执行加密
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedData = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));

        // 将加密后的字节数据转为 Base64 编码
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return Base64.getEncoder().encodeToString(encryptedData);
        }else {
            return "";
        }
    }

    // 拼接二维码数据并进行加密
    public String generateQRCodeData() {
        // 1. 組成發票基礎數據
        String invoiceNumber = orderInvoiceInfo.getInvoiceNumber(); // 10 碼
        String issueDate = DateUtils.convertDateToNumber(convertDate(orderInvoiceInfo.getEInvYm())); // 7 碼民國日期
        String randomCode = orderInvoiceInfo.getRandomNumer(); // 4 碼隨機碼
        String salesAmountHex = formatHexAmount(orderInvoiceInfo.getInvoiceAmountInt()); // 8 碼未稅金額
        String totalAmountHex = formatHexAmount(orderInvoiceInfo.getTaxAmountInt()); // 8 碼含稅金額

        String buyerId = "00000000";
        if (!TextUtils.isEmpty(buyerId)){
            buyerId = orderInvoiceInfo.getBuyerId();
        }
        String sellerId = orderInvoiceInfo.getSellerIdentifier(); // 8 碼

        /*String invoiceData = orderInvoiceInfo.getInvoiceNumber() + DateUtils.convertDateToNumber(convertDate(orderInvoiceInfo.getEInvYm())) + orderInvoiceInfo.getRandomNumer() +
                stringToHex(orderInvoiceInfo.getInvoiceAmount()) + stringToHex(orderInvoiceInfo.getTaxAmount()) +
                orderInvoiceInfo.getBuyerId() + orderInvoiceInfo.getSellerIdentifier();*/
        // 2. 拼接數據
        String invoiceData = invoiceNumber + issueDate + randomCode + salesAmountHex + totalAmountHex + buyerId + sellerId;

        String encryptedData = null;
        try {
            encryptedData = encryptData(orderInvoiceInfo.getInvoiceNumber() + orderInvoiceInfo.getRandomNumer());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        StringBuilder summarysb = new StringBuilder();
        summarysb.append(":");
        summarysb.append("**********");
        summarysb.append(":");
        summarysb.append("1");
        summarysb.append(":");
        summarysb.append(orderInvoiceInfo.getOrderTotalAmount()+"");
        summarysb.append(":");
        summarysb.append("1");
        summarysb.append(":");
        String qrContent = invoiceData + encryptedData + summarysb.toString();
        return qrContent;
//        return "0123456c vgvfdsx78901234567890123456789012345678901234567";
    }

    // 拼接二维码数据并进行加密
    public String generateQRCodeData2() {
      StringBuilder rightQrContent = new StringBuilder();
      rightQrContent.append("**");
      if (orderInvoiceInfo.getItemDetails() != null && !orderInvoiceInfo.getItemDetails().isEmpty()){
          //for (int i = 0; i < orderInvoiceInfo.getItemDetails().size();i++){
          DDPayOrderInvoiceItem item = orderInvoiceInfo.getItemDetails().get(0);
          rightQrContent.append(item.getItems_SName());
          rightQrContent.append(":");
          rightQrContent.append(item.getItems_Quantity());
          rightQrContent.append(":");
          rightQrContent.append(item.getItems_UnitPrice());
          rightQrContent.append("\n");
          //}
      }
      return rightQrContent.toString();
    }

    /**
     * 將金額轉換為 8 碼十六進位字串
     */
    private String formatHexAmount(int amount) {
        return String.format("%08X", amount);
    }


    // 批量处理条码的打印内容
    public void printBarcodeData(StringBuilder printData) {
       /* printData.append("1B 61 01"); // 居中对齐
        printData.append("1D 77 03"); // 条码宽度
        printData.append("1D 68 64"); // 条码高度
        printData.append("1D 48 02"); // 条码下方显示文字
        printData.append("1D 6B 49 0A"); // Code128条码
        printData.append(stringToHex("FL08572467")); // 条码内容*/
        printInvoiceBarcode(printData);
    }

    public void printInvoiceBarcode(StringBuilder printData) {
        if (orderInvoiceInfo == null) return;
        // 生成年期别（5码）
        String yearPeriod = convertYearMonthToCode2(orderInvoiceInfo.getEInvYm()); // 这里是示例数据（104年3-4月）

        // 生成统一发票字轨号（10码）
        String invoiceNumber =orderInvoiceInfo.getInvoiceNumber(); // 这里是示例数据

        // 生成随机码（4码）
        String randomCode = orderInvoiceInfo.getRandomNumer(); // 这里是示例数据

        // 拼接所有信息（19码）
        String barcodeData = yearPeriod + invoiceNumber + randomCode; // 合并为 19 位

        // 打印Code39条形码
        printBarcodeData(printData, barcodeData);
    }

    public String convertYearMonthToCode2(String yearMonth) {
       return convertYearMonthToCode(convertDate(yearMonth));
    }
    public String convertYearMonthToCode(String yearMonth) {
        // 假设输入格式为 "104年3-4月"
        // 分离年份和月份部分
        String[] parts = yearMonth.split("年|月");

        // 提取年份和月份
        String year = parts[0];  // 获取年份部分，例如 "104"
        String monthRange = parts[1];  // 获取月份部分，例如 "3-4"

        // 提取双数月份的开始（例如 "3-4" -> "04"）
        String[] months = monthRange.split("-");
        String startMonth = months[1];

        // 根据输入的月份范围，选择双数月份
        int startMonthInt = Integer.parseInt(startMonth);
        String formattedMonth = String.format("%02d", startMonthInt * 1); // 保证两位数，例如 "04"

        // 返回合并后的字符串
        return year + formattedMonth;
    }

    // 使用Code39格式打印条形码
    public void printBarcodeData(StringBuilder printData, String barcodeData) {
        // Code39条形码的开始和结束符号是 "*"（星号）
        String barcodeWithStartStop = barcodeData; // 加上开始和结束符号

        // 将条形码数据转换为 Code39 编码（这里假设打印机支持 Code39 编码）
        // 注意：此处假设打印机支持 Code39 条形码，我们使用该方法来打印条形码
        printData.append("1B 61 01"); // 居中对齐
//        printData.append("1d 6b 45 08 30 32 33 34 35 36 30 30");
//        printData.append("1d 6b 45 13 31 31 34 30 34 4d 51 32 30 30 30 30 34 32 32 34 36 32 37");
        printData.append("1D 48 00"); // 设置条形码宽度
        printData.append("1D 77 01"); // 设置条形码宽度
        printData.append("1D 68 32"); // 设置条形码高度
        // 5. 选择 Code39 条形码格式
        printData.append("1d 6b 45 13"); // 先加长度
        printData.append(stringToHex(barcodeWithStartStop));  // 条码数据
        // 将条形码数据转换为十六进制
        //printData.append(stringToHex(barcodeWithStartStop)); // 打印条形码数据
    }

    // 方法：打印发票上的商品明细
    public  void printInvoiceItemDetails(List<DDPayOrderInvoiceItem> items, StringBuilder printData) {
        int targetLength = 14;
        // 遍历商品数据并格式化输出
        for (DDPayOrderInvoiceItem item : items) {
            String leftContent = item.getItems_SName();
            String padLeftSpace = "";
            int length = calculateCharacterCount(leftContent);
            if (length < 14){
                for (int i = 0; i < targetLength - length;i++){
                    padLeftSpace =padLeftSpace+" ";
                }
            }
            leftContent = leftContent+padLeftSpace;
            String rightContent = "";
            if (TextUtils.isEmpty(item.getItems_Quantity()) || TextUtils.isEmpty(item.getItems_UnitPrice())){
                rightContent = "";
            }else {
                int amount  = Integer.parseInt(item.getItems_Quantity());
                double price = Double.parseDouble(item.getItems_UnitPrice());
                rightContent = (price*amount)+"";
            }
            String padRightSpace = "";
            int lengthRight = calculateCharacterCount(rightContent);
            if (lengthRight < 14){
                for (int j = 0; j < targetLength - lengthRight;j++){
                    padRightSpace = padRightSpace+" ";
                }
            }
            rightContent = padRightSpace+rightContent;
            String total = leftContent+rightContent+"\n";
            printData.append(stringToHex(total));
        }

    }
    public  int  calculateCharacterCount2(String input) {
        int length = 0;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            // 判断是否为中文字符（这里假设繁体中文字符的范围与简体相同）
            if (Character.toString(c).matches("[\u4e00-\u9fa5]")) {
                length += 2; // 中文字符占用两个字符
            } else {
                length += 1; // 英文字符占用一个字符
            }
        }

        return length;
    }

    public  int  calculateCharacterCount(String input) {
        int width = 0;
        for (char c : input.toCharArray()) {
            // 中文、全角、ASCII>127的字符都算2
            if (c > 127) {
                width += 2;
            } else {
                width += 1;
            }
        }
        return width;
    }


    public static String padProductName(String productName, int targetLength) {
        // 计算当前商品名称占用的长度（每个字符占两个空格）
        int currentLength = productName.length() * 2;

        // 如果商品名称的占用长度小于目标长度，补充空格
        if (currentLength < targetLength) {
            // 计算需要补充的空格数（每个字符占两个空格）
            int spacesToAdd = (targetLength - currentLength) / 2;

            // 使用 String.format 来补全空格，保证每个字符占两个空格
            return String.format("%-" + (productName.length() + spacesToAdd) + "s", productName);
        } else {
            // 如果商品名称已经满足或超出目标长度，则返回原始名称
            return productName;
        }
    }




    // 字符串转换为十六进制格式
    public String stringToHex2(String input) {
        StringBuilder hexString = new StringBuilder();
        try {
            // Big5 编码转换
            byte[] bytes = input.getBytes("UTF-8");
            for (byte b : bytes) {
                hexString.append(String.format("%02X", b)); // 将字节转换为两位十六进制
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }

    // 字符串转换为十六进制格式
  /*  public String stringToHex(String input) {
        StringBuilder hexString = new StringBuilder();
        try {
            // Big5 编码转换
            byte[] bytes = input.getBytes("Big5");
            for (byte b : bytes) {
                hexString.append(String.format("%02X", b)); // 将字节转换为两位十六进制
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hexString.toString();
    }*/

    public String stringToHex(String input) {
        StringBuilder hexString = new StringBuilder();
        // 使用 UTF-8 编码（最通用的编码，支持所有菲律宾语字符）
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        for (byte b : bytes) {
            hexString.append(String.format("%02X", b));
        }
        return hexString.toString();
    }

    public String getPortPath() {
        return portPath;
    }

    public void setPortPath(String portPath) {
        this.portPath = portPath;
    }

    public int getPortRate() {
        return portRate;
    }

    public void setPortRate(int portRate) {
        this.portRate = portRate;
    }

    public OnPrinterDataListener getOnPrinterDataListener() {
        return onPrinterDataListener;
    }

    public void setOnPrinterDataListener(OnPrinterDataListener onPrinterDataListener) {
        this.onPrinterDataListener = onPrinterDataListener;
    }

    public int getPrintType() {
        return printType;
    }

    public void setPrintType(int printType) {
        this.printType = printType;
    }



    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }


    public PrinterModel getPrinterModel() {
        return printerModel;
    }

    public void setPrinterModel(PrinterModel printerModel) {
        this.printerModel = printerModel;
    }
}
