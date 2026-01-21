package com.jpgk.hardwaresdk.hardwarelogger;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.jpgk.hardwaresdk.HardwareSDK;
import com.jpgk.hardwaresdk.utils.DateUtils;
import com.jpgk.iot.model.up.UploadLogAckUpModel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// LogUploadService.java
public class LogUploadService extends IntentService {
    private static final String TAG = "LogUploadService";
    public static final String ACTION_UPLOAD_COMPLETE = "com.jpgk.vendingmachine.UPLOAD_COMPLETE";
    public static final String EXTRA_SUCCESS_COUNT = "successCount";
    public static final String EXTRA_FAIL_COUNT = "failCount";

    private static final String UPLOAD_BASE_URL = "http://34.111.168.147/deviceLog/";

    private long startTime;
    private long endTime;
    private long expiration;
    private String securityToken;
    private String serialNo;
    private String uploadUrl;
    private String fileName1;

    public LogUploadService() {
        super("LogUploadService");
    }

    public static void startUpload(Context context, long startTime, long endTime,long expiration,String securityToken,String serialNo) {
        Intent intent = new Intent(context, LogUploadService.class);
        intent.putExtra("startTime", startTime);
        intent.putExtra("endTime", endTime);
        intent.putExtra("expiration",expiration);
        intent.putExtra("securityToken",securityToken);
        intent.putExtra("serialNo",serialNo);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        startTime = intent.getLongExtra("startTime", 0);
        endTime = intent.getLongExtra("endTime", 0);
        expiration = intent.getLongExtra("expiration",0);
        securityToken = intent.getStringExtra("securityToken");
        serialNo = intent.getStringExtra("serialNo");
        VendingMachineLogger logger = HardwareSDK.INSTANCE.getLogger();//App.instance.getLogger();
        if (logger == null){
            return;
        }
        List<File> logFiles = logger.findLogFilesBetween(startTime, endTime);

        // 按日期分组日志文件
        Map<String, List<File>> dateLogsMap = groupLogsByDate(logFiles);

        int successCount = 0;
        int failCount = 0;

        for (Map.Entry<String, List<File>> entry : dateLogsMap.entrySet()) {
            String date = entry.getKey();
            List<File> dailyLogs = entry.getValue();

            // 获取当天的崩溃日志
            List<File> crashLogs = findCrashLogsForDate(date);

            // 获取当天的系统日志
            List<File> systemLogs = getSystemLogsForDate(date);

            // 创建压缩包
            File zipFile = createDailyLogZip(date, dailyLogs, crashLogs,systemLogs);

            if (zipFile != null && uploadFile(zipFile, date)) {
                sendUpLoadSuccToIot(zipFile);
                successCount++;
            } else {
                failCount++;
            }

            // 清理临时文件
            if (zipFile != null && zipFile.exists()) {
                zipFile.delete();
            }
        }

        sendBroadcast(new Intent(ACTION_UPLOAD_COMPLETE)
                .putExtra(EXTRA_SUCCESS_COUNT, successCount)
                .putExtra(EXTRA_FAIL_COUNT, failCount));
    }

    // 按日期分组日志文件
    private Map<String, List<File>> groupLogsByDate(List<File> logFiles) {
        Map<String, List<File>> result = new HashMap<>();

        for (File file : logFiles) {
            String date = VendingMachineLogger.extractDateFromFilename(file.getName());
            if (date == null) continue;

            if (!result.containsKey(date)) {
                result.put(date, new ArrayList<>());
            }
            result.get(date).add(file);
        }

        return result;
    }

    // 查找指定日期的崩溃日志
    private List<File> findCrashLogsForDate(String dateStr) {
        List<File> crashLogs = new ArrayList<>();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
            Date targetDate = sdf.parse(dateStr);

            // 计算日期范围（当天的00:00:00 - 23:59:59）
            Calendar cal = Calendar.getInstance();
            cal.setTime(targetDate);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            long startTime = cal.getTimeInMillis();

            cal.add(Calendar.DAY_OF_MONTH, 1);
            cal.add(Calendar.MILLISECOND, -1);
            long endTime = cal.getTimeInMillis();

            File crashDir = new File(getFilesDir(), "crash_logs");
            if (!crashDir.exists() || !crashDir.isDirectory()) {
                return crashLogs;
            }

            File[] files = crashDir.listFiles();
            if (files == null) return crashLogs;

            for (File file : files) {
                if (!file.getName().startsWith("crash_") ||
                        !file.getName().endsWith(".log")) {
                    continue;
                }

                // 解析文件名中的时间戳
                String timestampStr = file.getName()
                        .replace("crash_", "")
                        .replace(".log", "");

                try {
                    long timestamp = Long.parseLong(timestampStr);
                    if (timestamp >= startTime && timestamp <= endTime) {
                        crashLogs.add(file);
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid crash filename: " + file.getName());
                }
            }
        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error", e);
        }

        return crashLogs;
    }

    // 获取指定日期的系统日志
    private List<File> getSystemLogsForDate(String dateStr) {
        List<File> systemLogs = new ArrayList<>();

        try {
            // 将 yyyyMMdd 格式转换为 yyyy-MM-dd 格式
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyyMMdd", Locale.US);
            SimpleDateFormat outputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date date = inputFormat.parse(dateStr);
            String formattedDate = outputFormat.format(date);

            // 构建系统日志文件路径
            File systemLogDir = new File(LocalLogsPathManager.INSTANCE.getLogsNewPath(), "logs/system");
            if (!systemLogDir.exists() || !systemLogDir.isDirectory()) {
                return systemLogs;
            }

            // 查找对应日期的系统日志文件
            File systemLogFile = new File(systemLogDir, formattedDate + ".log");
            if (systemLogFile.exists() && systemLogFile.isFile()) {
                systemLogs.add(systemLogFile);
                Log.i(TAG, "Found system log: " + systemLogFile.getAbsolutePath());
            } else {
                Log.i(TAG, "System log not found for date: " + formattedDate);
            }

        } catch (ParseException e) {
            Log.e(TAG, "Date parsing error", e);
        }

        return systemLogs;
    }


    // 创建每日日志压缩包
    private File createDailyLogZip(String date, List<File> dailyLogs, List<File> crashLogs,List<File> systemLogs) {
        // 创建临时目录
        File tempDir = new File(getCacheDir(), "log_zip_temp");
        if (!tempDir.exists()) {
            tempDir.mkdirs();
        }

        // 创建压缩文件
        String zipName = "logs_" + date+"_"+System.currentTimeMillis() + (crashLogs.isEmpty() ? "" : "_with_crash") + ".zip";
        File zipFile = new File(tempDir, zipName);

        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // 添加日常日志
            for (File logFile : dailyLogs) {
                addFileToZip(zos, logFile, "daily_logs/" + logFile.getName());
            }

            // 添加崩溃日志
            for (File crashFile : crashLogs) {
                addFileToZip(zos, crashFile, "crash_logs/" + crashFile.getName());
            }

            // 添加崩溃日志
            for (File systemFile : systemLogs) {
                addFileToZip(zos, systemFile, "system_logs/" + systemFile.getName());
            }

            Log.i(TAG, "Created zip: " + zipFile.getAbsolutePath() +
                    " with " + dailyLogs.size() + " daily logs and " +
                    crashLogs.size() + " crash logs");
        } catch (IOException e) {
            Log.e(TAG, "Failed to create zip file", e);
            return null;
        }

        return zipFile;
    }

    private void addFileToZip(ZipOutputStream zos, File file, String entryName) throws IOException {
        if (!file.exists()) return;

        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);

        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                zos.write(buffer, 0, length);
            }
        }

        zos.closeEntry();
    }

    private void sendUpLoadSuccToIot(File logFile) {
        UploadLogAckUpModel uploadLogAckUpModel = new UploadLogAckUpModel();
        uploadLogAckUpModel.setName(fileName1);
        uploadLogAckUpModel.setUrl(uploadUrl);
        uploadLogAckUpModel.setSerialNo(serialNo);
        //SocketClient.Companion.getInstance().sendMsg(JSON.toJSONString(uploadLogAckUpModel));
    }

    // 上传压缩文件
    private boolean uploadZipFile(File zipFile, String date) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();

        try {
            // 构建请求URL
            String url = UPLOAD_BASE_URL + date + "/" + zipFile.getName();
            Log.d(TAG, "Uploading zip to: " + url);

            // 创建请求体
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/zip"),
                    zipFile
                        );

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Authorization", "")
                    .addHeader("User-Agent", "VendingMachine/1.0")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.i(TAG, "Upload success: " + zipFile.getName());
                return true;
            } else {
                Log.w(TAG, "Upload failed: " + response.code() + " - " + zipFile.getName());
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Upload error: " + zipFile.getName(), e);
            return false;
        }
    }

    private boolean uploadFile(File logFile,String date) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(30*4, TimeUnit.SECONDS)
                .writeTimeout(30*4, TimeUnit.SECONDS)
                .build();

        try {
            // 从文件名解析日期
            String fileName = logFile.getName();
           /* String date = fileName.substring(
                    VendingMachineLogger.getLogPrefix().length(),
                    fileName.length() - VendingMachineLogger.getLogExt().length()
            );*/
            // 获取设备编号
            String machineCode = "";//App.constantsViewModelInstance.getMachineCode();
            // 构建请求URL
            String url = UPLOAD_BASE_URL + machineCode + "/" + date + "/" + fileName;
//            String url = UPLOAD_BASE_URL  + date + "/" + fileName;
            uploadUrl = url;
            fileName1 = fileName;
            Log.d(TAG, "Uploading to: " + url);

            // 创建请求体
            RequestBody body = RequestBody.create(
                    MediaType.parse("application/zip"),
                    logFile
                        );

            Request request = new Request.Builder()
                    .url(url)
                    .put(body)
                    .addHeader("Authorization", "Bearer "+securityToken)
                    .addHeader("User-Agent", "VendingMachine/1.0")
                    .build();

            Response response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                Log.i(TAG, "Upload success: " + fileName);
                return true;
            } else {
                Log.w(TAG, "Upload failed: " + response.code() + " - " + fileName);
                return false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Upload error: " + logFile.getName(), e);
            return false;
        }
    }
}