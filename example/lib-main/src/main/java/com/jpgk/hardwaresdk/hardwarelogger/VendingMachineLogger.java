package com.jpgk.hardwaresdk.hardwarelogger;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class VendingMachineLogger {
    // 基础配置
    private static final String LOG_DIR = "vm_logs";
    private static final String LOG_PREFIX = "vm_log_";
    private static final String LOG_EXT = ".txt";
    private static final String TRANS_LOG = "transactions.csv";
    private static final int BUFFER_SIZE = 8192; // 8KB缓冲

    // 时间格式
    private final SimpleDateFormat fileDateFormat =
            new SimpleDateFormat("yyyyMMdd", Locale.getDefault());
    private final SimpleDateFormat logDateFormat =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    // 系统组件
    private final Context context;
    private HandlerThread logHandlerThread;
    private Handler logHandler;

    // 流管理
    private BufferedWriter logWriter;
    private FileOutputStream logFileStream;
    private long lastFlushTime = System.currentTimeMillis();

    // 日期轮转检测
    private final Handler dateRolloverHandler = new Handler();
    private static final long DATE_CHECK_INTERVAL = 5 * 60 * 1000; // 5分钟检查一次
    private String currentLogDate; // 当前日志日期

    public VendingMachineLogger(Context context) {
        this.context = context.getApplicationContext();
        initLoggerThread();
        cleanOldLogs(30); // 保留7天日志

        // 初始化当前日期
        currentLogDate = fileDateFormat.format(new Date());

        // 启动日期轮转检测
        dateRolloverHandler.post(dateCheckRunnable);

    }


    private void initLoggerThread() {
        logHandlerThread = new HandlerThread("VMLoggerWorker",
                Process.THREAD_PRIORITY_BACKGROUND);
        logHandlerThread.start();
        logHandler = new Handler(logHandlerThread.getLooper());
    }

    // 日期轮转检测Runnable
    private final Runnable dateCheckRunnable = new Runnable() {
        @Override
        public void run() {
            checkDateRollover();
            // 定期检查日期变化
            dateRolloverHandler.postDelayed(this, DATE_CHECK_INTERVAL);
        }
    };

    // 基础日志方法
    public void log(String level, String tag, String message) {
        logHandler.post(() -> {
            try {
                checkDateRollover();
                writeLogEntry(level, tag, message);
            } catch (Exception e) {
                Log.e("Logger", "日志记录失败", e);
            }
        });
    }

    // 交易专用日志
    public void logTransaction(String itemId, double price, int quantity) {
        logHandler.post(() -> {
            String record = String.format(Locale.US, "%s,%s,%.2f,%d\n",
                    logDateFormat.format(new Date()),
                    itemId, price, quantity);

            writeToCsvFile(record);
        });
    }

    private void writeLogEntry(String level, String tag, String message) {
        File logFile = getCurrentLogFile();
        if (logFile == null) return;

        try (FileChannel channel = new RandomAccessFile(logFile, "rw").getChannel()) {
            FileLock lock = channel.lock();
            try {
                // 确保文件存在
                if (!logFile.exists()) {
                    logFile.createNewFile();
                }

                // 初始化或重新初始化流
                if (logFileStream == null || logWriter == null) {
                    initLogWriter(logFile);
                }

                String entry = String.format("%s [%s] %s: %s\n",
                        logDateFormat.format(new Date()), level, tag, message);
                logWriter.write(entry);

                // 每5秒或缓冲区满时自动flush
                if (System.currentTimeMillis() - lastFlushTime > 5000) {
                    logWriter.flush();
                    lastFlushTime = System.currentTimeMillis();
                }
            } finally {
                lock.release();
            }
        } catch (IOException e) {
            handleIOError(e);
        }
    }

    private void initLogWriter(File logFile) throws IOException {
        // 先关闭旧流防止泄漏
        closeCurrentStreams();

        // 创建新的文件流
        logFileStream = new FileOutputStream(logFile, true);
        OutputStreamWriter osw = new OutputStreamWriter(logFileStream);
        logWriter = new BufferedWriter(osw, BUFFER_SIZE);
    }

    private void writeToCsvFile(String record) {
        File csvFile = new File(LocalLogsPathManager.INSTANCE.getLogsNewPath() + File.separator + "etc" + File.separator, TRANS_LOG);
        try (FileChannel channel = new RandomAccessFile(csvFile, "rw").getChannel()) {
            FileLock lock = channel.lock();
            try (BufferedWriter writer = new BufferedWriter(
                    new FileWriter(csvFile, true))) {
                writer.write(record);
            } finally {
                lock.release();
            }
        } catch (IOException e) {
            handleIOError(e);
        }
    }

    private File getCurrentLogFile() {
        File parentFile = new File(LocalLogsPathManager.INSTANCE.getLogsNewPath() + File.separator + "etc" + File.separator);
        File logDir = new File(parentFile, LOG_DIR);
        if (!logDir.exists() && !logDir.mkdirs()) {
            Log.e("Logger", "无法创建日志目录");
            return null;
        }
        return new File(logDir, LOG_PREFIX + currentLogDate + LOG_EXT);
    }

    // 修改日期轮转检查方法
    private void checkDateRollover() {
        String today = fileDateFormat.format(new Date());

        // 如果日期没有变化，直接返回
        if (today.equals(currentLogDate)) {
            return;
        }

        Log.i("Logger", "检测到日期变化: " + currentLogDate + " → " + today);

        try {
            // 关闭当前流
            closeCurrentStreams();

            // 更新当前日期
            currentLogDate = today;

            // 获取新的日志文件
            File newLogFile = getCurrentLogFile();
            if (newLogFile != null) {
                Log.i("Logger", "创建新日志文件: " + newLogFile.getName());

                // 确保目录存在
                File parentDir = newLogFile.getParentFile();
                if (!parentDir.exists()) {
                    parentDir.mkdirs();
                }

                // 创建新文件（但不立即打开流）
                if (!newLogFile.exists()) {
                    newLogFile.createNewFile();
                }
            }
        } catch (IOException e) {
            Log.e("Logger", "日期轮转时创建文件失败", e);
        }
    }

    private void cleanOldLogs(int keepDays) {
        ExecutorService cleaner = Executors.newSingleThreadExecutor();
        cleaner.execute(() -> {
            File logDir = new File(LocalLogsPathManager.INSTANCE.getLogsNewPath() + File.separator + "etc" + File.separator, LOG_DIR);
            if (!logDir.exists()) return;

            long cutoff = System.currentTimeMillis() - keepDays * 86400000L;
            File[] oldFiles = logDir.listFiles(file ->
                    file.getName().startsWith(LOG_PREFIX) &&
                            file.lastModified() < cutoff
            );

            // 压缩三天前的日志
            File[] toCompress = logDir.listFiles(file ->
                    file.lastModified() < System.currentTimeMillis() - 3 * 86400000L &&
                            !file.getName().endsWith(".zip")
            );

            if (toCompress != null) {
                for (File file : toCompress) {
                    zipLogFile(file);
                }
            }

            if (oldFiles != null) {
                for (File file : oldFiles) {
                    file.delete();
                }
            }
        });
        cleaner.shutdown();
    }

    private void zipLogFile(File logFile) {
        File zipFile = new File(logFile.getParent(), logFile.getName() + ".zip");
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            zos.putNextEntry(new ZipEntry(logFile.getName()));
            try (FileInputStream fis = new FileInputStream(logFile)) {
                byte[] buffer = new byte[1024];
                int length;
                while ((length = fis.read(buffer)) > 0) {
                    zos.write(buffer, 0, length);
                }
            }
            zos.closeEntry();
            logFile.delete();
        } catch (IOException e) {
            Log.w("Logger", "压缩失败: " + logFile.getName());
        }
    }

    private void handleIOError(IOException e) {
        Log.e("Logger", "IO错误: " + e.getMessage());
        closeCurrentStreams();
    }

    private void closeCurrentStreams() {
        try {
            if (logWriter != null) {
                logWriter.flush();
                logWriter.close();
            }
            if (logFileStream != null) {
                logFileStream.getFD().sync();
                logFileStream.close();
            }
        } catch (IOException ex) {
            Log.e("Logger", "关闭流失败", ex);
        } finally {
            logWriter = null;
            logFileStream = null;
        }
    }

    public void shutdown() {
        // 停止日期检查定时器
        dateRolloverHandler.removeCallbacks(dateCheckRunnable);

        logHandler.post(() -> {
            closeCurrentStreams();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
                logHandlerThread.quitSafely();
            }
        });
    }

    // 添加公共方法用于文件名解析
    public static String getLogPrefix() {
        return LOG_PREFIX;
    }

    public static String getLogExt() {
        return LOG_EXT;
    }

    public static String extractDateFromFilename(String filename) {
        if (filename == null || !filename.startsWith(LOG_PREFIX)) {
            return null;
        }
        return filename.replace(LOG_PREFIX, "").replace(LOG_EXT, "");
    }

    /**
     * 查找指定时间区间内的日志文件
     * @param startTime 开始时间（包含）
     * @param endTime 结束时间（包含）
     * @return 按日期排序的文件列表（从旧到新）
     */
    public List<File> findLogFilesBetween(Date startTime, Date endTime) {
        List<File> result = new ArrayList<>();

        // 获取日志目录
        File logDir = new File(LocalLogsPathManager.INSTANCE.getLogsNewPath() + File.separator + "etc" + File.separator, LOG_DIR);
        if (!logDir.exists() || !logDir.isDirectory()) {
            return result;
        }

        // 创建日期解析器
        SimpleDateFormat filenameFormat = new SimpleDateFormat("yyyyMMdd", Locale.getDefault());

        // 遍历目录文件
        File[] files = logDir.listFiles();
        if (files == null) return result;

        for (File file : files) {
            // 跳过非日志文件
            if (!file.getName().startsWith(LOG_PREFIX) ||
                    !file.getName().endsWith(LOG_EXT)) {
                continue;
            }

            try {
                // 从文件名提取日期
                String dateStr = file.getName()
                        .replace(LOG_PREFIX, "")
                        .replace(LOG_EXT, "");

                Date fileDate = filenameFormat.parse(dateStr);

                // 检查日期范围
                if (!fileDate.before(startTime) &&
                        !fileDate.after(endTime)) {
                    result.add(file);
                }
            } catch (ParseException e) {
                Log.w("Logger", "无效日志文件名: " + file.getName());
            }
        }

        // 按日期排序
        Collections.sort(result, (f1, f2) -> {
            String n1 = f1.getName().replaceAll("\\D", "");
            String n2 = f2.getName().replaceAll("\\D", "");
            return n1.compareTo(n2);
        });

        return result;
    }

    // 添加日期边界处理扩展方法（可选）
    public List<File> findLogFilesBetween(long startTimestamp, long endTimestamp) {
        return findLogFilesBetween(new Date(startTimestamp), new Date(endTimestamp));
    }
}
