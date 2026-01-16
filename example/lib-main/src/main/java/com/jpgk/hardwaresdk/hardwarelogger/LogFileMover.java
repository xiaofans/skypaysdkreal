package com.jpgk.hardwaresdk.hardwarelogger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class LogFileMover {

    public static void copyLogsAndDeleteOld(String oldDirPath, String newDirPath) {
        File oldDir = new File(oldDirPath);
        File newDir = new File(newDirPath);

        if (!oldDir.exists() || !oldDir.isDirectory()) {
            System.out.println("舊目錄不存在: " + oldDirPath);
            return;
        }

        if (!newDir.exists()) {
            newDir.mkdirs();
        }

        copyDirectory(oldDir, newDir);

        // 全部拷貝完成後刪除舊目錄
        //deleteDirectory(oldDir);

        System.out.println("已將所有日誌從 " + oldDirPath + " 拷貝到 " + newDirPath + "，並刪除舊目錄。");
    }

    private static void copyDirectory(File source, File destination) {
        File[] files = source.listFiles();
        if (files == null) return;

        for (File file : files) {
            File newFile = new File(destination, file.getName());
            if (file.isDirectory()) {
                newFile.mkdirs();
                copyDirectory(file, newFile);
            } else {
                copyFile(file, newFile);
            }
        }
    }

    private static void copyFile(File src, File dest) {
        try (FileInputStream fis = new FileInputStream(src);
             FileOutputStream fos = new FileOutputStream(dest)) {

            byte[] buffer = new byte[4096];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                fos.write(buffer, 0, length);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void deleteDirectory(File dir) {
        if (dir == null || !dir.exists()) return;

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    file.delete();
                }
            }
        }
        dir.delete();
    }
}

