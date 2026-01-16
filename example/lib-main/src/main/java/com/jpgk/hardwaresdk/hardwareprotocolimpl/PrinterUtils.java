package com.jpgk.hardwaresdk.hardwareprotocolimpl;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
public class PrinterUtils {

    public static byte[] bitmapToImageData(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        int bytesPerLine = (width + 7) / 8;
        int size = bytesPerLine * height;
        byte[] imageData = new byte[size];

        int index = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x += 8) {
                byte b = 0;
                for (int bit = 0; bit < 8; bit++) {
                    int xx = x + bit;
                    if (xx < width) {
                        int pixel = bitmap.getPixel(xx, y);
                        int r = (pixel >> 16) & 0xFF;
                        int g = (pixel >> 8) & 0xFF;
                        int bColor = pixel & 0xFF;
                        int gray = (r + g + bColor) / 3;
                        if (gray < 128) { // 黑点
                            b |= (1 << (7 - bit));
                        }
                    }
                }
                imageData[index++] = b;
            }
        }

//        ByteArrayOutputStream baos = new ByteArrayOutputStream();
//        baos.write(0x1B); // ESC @ INIT
//        baos.write('@');
//
//        baos.write(0x1D); // GS v 0 m xL xH yL yH
//        baos.write('v');
//        baos.write('0');
//        baos.write(0x00);  // m = normal
//
//        baos.write(bytesPerLine & 0xFF);          // xL
//        baos.write((bytesPerLine >> 8) & 0xFF);   // xH
//        baos.write(height & 0xFF);                // yL
//        baos.write((height >> 8) & 0xFF);         // yH

//        baos.write(imageData);

        return imageData;
    }


    public static Bitmap convertPngToBlackWhite(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                int alpha = (pixel >>> 24);

                if (alpha == 0) {
                    // 透明 → 黑色
                    result.setPixel(x, y, Color.BLACK);
                } else {
                    // 非透明 → 白色
                    result.setPixel(x, y, Color.WHITE);
                }
            }
        }
        return result;
    }

    public static Bitmap convertTransparentToWhiteAndWhiteToBlack(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();

        Bitmap result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int pixel = source.getPixel(x, y);
                int alpha = (pixel >>> 24);

                if (alpha == 0) {
                    // 透明 → 白色
                    result.setPixel(x, y, Color.WHITE);
                } else {
                    // 非透明（原来如果是白色）→ 黑色
                    result.setPixel(x, y, Color.BLACK);
                }
            }
        }
        return result;
    }



    public static Bitmap cropTransparent(Bitmap source) {
        int width = source.getWidth();
        int height = source.getHeight();
        int top = 0, bottom = height - 1, left = 0, right = width - 1;

        int[] pixels = new int[width];
        boolean found = false;

        // 上边界
        for (int y = 0; y < height; y++) {
            source.getPixels(pixels, 0, width, 0, y, width, 1);
            for (int pixel : pixels) {
                if ((pixel >>> 24) != 0x00) { // 透明度不为0
                    top = y;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        // 下边界
        found = false;
        for (int y = height - 1; y >= 0; y--) {
            source.getPixels(pixels, 0, width, 0, y, width, 1);
            for (int pixel : pixels) {
                if ((pixel >>> 24) != 0x00) {
                    bottom = y;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        // 左边界
        found = false;
        for (int x = 0; x < width; x++) {
            for (int y = top; y <= bottom; y++) {
                int pixel = source.getPixel(x, y);
                if ((pixel >>> 24) != 0x00) {
                    left = x;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        // 右边界
        found = false;
        for (int x = width - 1; x >= 0; x--) {
            for (int y = top; y <= bottom; y++) {
                int pixel = source.getPixel(x, y);
                if ((pixel >>> 24) != 0x00) {
                    right = x;
                    found = true;
                    break;
                }
            }
            if (found) break;
        }

        return Bitmap.createBitmap(source, left, top, right - left + 1, bottom - top + 1);
    }


}
