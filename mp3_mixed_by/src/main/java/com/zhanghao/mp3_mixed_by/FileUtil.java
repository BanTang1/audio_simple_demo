package com.zhanghao.mp3_mixed_by;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 文件操作工具类
 */
public class FileUtil {

    /**
     * 将assets目录下的文件复制到外部存储应用程序私有目录中;
     * 覆盖模式
     *
     * @param context       上下文
     * @param assetFileNames  任意个文件名
     */
    public static void copyAssetFilesToExternalStorage(Context context, String... assetFileNames) {
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = null;
        FileOutputStream outputStream = null;

        try {
            // 获取应用程序的外部私有目录
            File externalDir = context.getExternalFilesDir(null);

            if (externalDir != null) {
                for (String assetFileName : assetFileNames) {
                    try {
                        // 从 assets 目录中打开输入流
                        inputStream = assetManager.open(assetFileName);

                        // 创建输出文件
                        File outputFile = new File(externalDir, assetFileName);

                        // 打开输出流
                        outputStream = new FileOutputStream(outputFile);

                        // 将数据从输入流复制到输出流
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }

                        // 输出完成
                        outputStream.flush();
                    } finally {
                        // 关闭输出流
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // 关闭输入流
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * 将ByteBuffer写入文件, 使用try-with-resources 语法，
     * 这种语法确保在 try 块结束时自动关闭资源，无需手动调用 close() 方法。
     * 追加模式
     *
     * @param buffer        ByteBuffer
     * @param filePath      目标文件路径
     */
    public static void writeByteBufferToFile(ByteBuffer buffer, String filePath) {
        try (FileOutputStream fos = new FileOutputStream(filePath,true);
             FileChannel channel = fos.getChannel()) {

            // 将 ByteBuffer 中的数据写入文件
            channel.write(buffer);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

