package com.jpgk.hardwaresdk.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtils {
    // 将long时间戳转换为指定格式的日期字符 串
    public static String formatTimestamp(long timestamp) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Date date = new Date(timestamp);
        return sdf.format(date);
    }


    // 转换发票日期 "104年3-4月" 或 "104年03-04月" 为 5 位数字 "10404"
    public static String convertDateToNumber(String dateRange) {
        // 假设输入格式是 "104年3-4月" 或 "104年03-04月"
        // 分离年份和月份范围
        String[] parts = dateRange.split("年|月|~");

        // 提取年份和起始月份
        String year = parts[0];  // 获取年份部分，例如 "104"
        String startMonthRange = parts[1];  // 获取月份范围部分，例如 "3-4" 或 "03-04"

        // 提取起始月份（即范围中的第一个月份），并确保月份为双数
        String startMonth = startMonthRange.split("-")[1];  // 获取 "3" 或 "03"

        // 将月份转换为双数月份（例如 "3" -> "04"）
        String formattedMonth = String.format("%02d", Integer.parseInt(startMonth));

        // 返回拼接后的 5 位数字
        return year + formattedMonth;
    }

    public static String convertDateFormat(String input) {
        SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            Date date = inputFormat.parse(input);
            return outputFormat.format(date);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String convertDateFormat(long input) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyyMMddHHmmss");
        return outputFormat.format(new Date(input));
    }

    public static String convertDateFormat2(long input) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return outputFormat.format(new Date(input));
    }

    public static String convertDateFormat3(long input) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return outputFormat.format(new Date(input));
    }

    public static String convertDateFormat4(long input) {
        SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd");
        return outputFormat.format(new Date(input));
    }
}
