package com.ansen.webview;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import okhttp3.Call;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Util {

    private static final String TAG = "FileUtils";
    private static String fileName = "version.txt";
    private static String path = Environment.getExternalStorageDirectory().getPath() + "/";//动态写入文件，用于更新
    private static String versionUrl = "https://www.baidu.com";
    /**
     * 创建文件
     */
    public static void createFile() {
        String strFilePath = path + fileName;
        File file = new File(strFilePath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 修改文件内容（覆盖或者添加）
     *
     * @param content 覆盖内容
     */
    public static void modifyFile(String content) {
        if (!isExternalStorageWritable()) {
            return;
        }
        try {
            String strFilePath = path + fileName;
            FileWriter fileWriter = new FileWriter(strFilePath, false);
            BufferedWriter writer = new BufferedWriter(fileWriter);
            writer.append(content);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }

    /**
     * 读取文件内容
     *
     * @return 返回内容
     */
    public static int getVersion() {
        if (!isExternalStorageWritable()) {
            return -1;
        }
        String strFilePath = path + fileName;
        File file = new File(strFilePath);
        FileInputStream inputStream = null;
        StringBuffer sb = new StringBuffer("");
        try {
            if (file.exists()) {
                inputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = null;
                inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();
                inputStreamReader.close();
                inputStream.close();
            } else {
                createFile();
                return 0;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Integer.valueOf(sb.toString());
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static boolean checkVersion() {
        OkHttpClient client = new OkHttpClient();
        FormBody formBody = new FormBody
                .Builder()
                .build();
        final Request request = new Request.Builder()
                .url(versionUrl)
                .post(formBody)
                .build();
        Call call = client.newCall(request);
        try {
            Response response = call.execute();
            final int maxVersion = Integer.valueOf(response.body().string());
            if (maxVersion > getVersion()) {
                modifyFile(String.valueOf(maxVersion));
                return true;
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//                final int maxVersion = Integer.valueOf(response.body().string());
//                if(maxVersion>getVersion()){
//                    modifyFile(String.valueOf(maxVersion));
//                }
//            }
//        });
//    }

}
