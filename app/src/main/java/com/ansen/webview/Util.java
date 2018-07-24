package com.ansen.webview;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.webkit.WebSettings;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class Util {

    private static final String TAG = "FileUtils";
    private static String fileName = "wwec_version.txt";
    //    private static String path = Environment.getExternalStorageDirectory().getPath() + "/";//动态写入文件，用于更新
    private static String versionUrl = "http://120.79.236.139/user/getVersion";

    /**
     * 创建文件
     */
    public static boolean createFile(Context context, String path) {
        if (!isExternalStorageWritable()) {
            return false;
        }
        String strFilePath = path + fileName;
        File file = new File(strFilePath);
        if (!file.exists()) {
            try {
                boolean f = file.createNewFile();
                Log.e(TAG, file.exists() + "");
                if (f) {
                    startSetVersion(context, path);
                    return true;
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    /**
     * 第一次进入开始更新版本号
     */
    private static void startSetVersion(Context context, final String path) {
        if (!NetworkUtil.isAvilabel(context)) {
            Log.e("network", "network is not available");
            return;
        }
        try {
            OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder()
                    .url(versionUrl)
                    .get()
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String maxVersion;
                    try {
                        maxVersion = new JSONObject(new JSONObject(response.body().string()).getString("retMsg")).getString("msg");
                        Util.modifyFile(path, maxVersion);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 修改文件内容（覆盖或者添加）
     *
     * @param content 覆盖内容
     */
    public static void modifyFile(String path, String content) {
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
            getVersion(path);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {

        }
    }

    /**
     * 判断文件是否存在
     *
     * @return 返回内容，没有权限或者被删除，默认需要更新，所以返回最小值
     */
    public static boolean isFirstEnter(String path) {
        if (!isExternalStorageWritable()) {
            return false;
        }
        String strFilePath = path + fileName;
        File file = new File(strFilePath);
        if (file.exists()) {
            return false;
        }
        return true;
    }

    /**
     * 读取文件内容
     *
     * @return 返回内容，没有权限或者被删除，默认需要更新，所以返回最小值
     */
    public static String getVersion(String path) {
        if (!isExternalStorageWritable()) {
            return "";
        }
        String strFilePath = path + fileName;
        File file = new File(strFilePath);
        FileInputStream inputStream;
        StringBuffer sb = new StringBuffer();
        try {
            if (file.exists()) {
                inputStream = new FileInputStream(file);
                InputStreamReader inputStreamReader = null;
                inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                    break;//直接退出
                }
                reader.close();
                inputStreamReader.close();
                inputStream.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    public static boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    public static void checkVersion(final Context context, final WebSettings webSettings, final Handler handler, final String path) {
        if (!NetworkUtil.isAvilabel(context)) {
            Log.e("network", "network is not available");
            return;
        }
        try {
            OkHttpClient client = new OkHttpClient();
            final Request request = new Request.Builder()
                    .url(versionUrl)
                    .get()
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    final String maxVersion;
                    try {
                        maxVersion = new JSONObject(new JSONObject(response.body().string()).getString("retMsg")).getString("msg");
                        if ("".equals(getVersion(path))) {
                            Util.modifyFile(path, maxVersion);
                            return;
                        }
                        if (!maxVersion.equals(getVersion(path))) {
                            Map v = new HashMap<String, String>();
                            v.put("type", 1);//标识哪个请求
                            v.put("version", maxVersion);
                            Message message = new Message();
                            message.obj = v;
                            handler.sendMessage(message);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void showNormalDialog(Context context, final UpdateInter updateInter) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("版本更新")
//                .setIcon(R.mipmap.ic_launcher)
                .setMessage("为了您的财产安全，我们强烈建议您更新!")
                .setPositiveButton("同意", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        updateInter.reload();
                    }
                }).setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        builder.create().show();
    }
}
