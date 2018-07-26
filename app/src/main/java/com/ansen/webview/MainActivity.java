package com.ansen.webview;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import org.json.JSONException;
import org.json.JSONObject;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;

@RuntimePermissions
public class MainActivity extends AppCompatActivity {
    private int REQUEST_CODE_SCAN = 2;//相机扫描回调

    private int REQUEST_CAMARA = 0;//相机权限
    private int REQUEST_SD = 1;//存储权限

    private WebView webView;
    private WebSettings webSettings;
    private ProgressBar progressBar;
    private TextView processTitle;

    BroadcastReceiver connectionReceiver;//网络广播
    private int quitCount = 1;//5秒内点击2次推出

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉顶部标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        //进度条
        progressBar = findViewById(R.id.progressbar);
        //进度条文本
        processTitle = findViewById(R.id.processTitle);
        processTitle.setText(R.string.load_title);
        webView = findViewById(R.id.webview);
        showBg();
        //核心设置
        webView = Webset.webSet(MainActivity.this, webView, webChromeClient, webViewClient, MainActivity.this.getApplicationContext().getCacheDir().getAbsolutePath(), true);
        webSettings = webView.getSettings();
        //启用交互
        webView.addJavascriptInterface(new JSInterface(), "Bridge");
        //初始化二维吗扫描
        ZXingLibrary.initDisplayOpinion(this);
        //注册网络监听
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        //监听网络变化
        registerReceiver(getConnnectionReceiver(), intentFilter);
        startLoad(webView);
        //小于是没有设置
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_SD);
        } else {
            MainActivityPermissionsDispatcher.needSDPmWithPermissionCheck(MainActivity.this);
        }
    }

    private void startLoad(WebView webView) {
        webView.loadUrl("file:///android_asset/release/index.html");//加载本地
    }

    private BroadcastReceiver getConnnectionReceiver() {
        connectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
                    Toast.makeText(MainActivity.this, R.string.net_disconnect, Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(MainActivity.this, R.string.net_connect, Toast.LENGTH_LONG).show();
                }
            }
        };
        return connectionReceiver;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 扫描二维码/条码回传
        if (requestCode == REQUEST_CODE_SCAN && resultCode == RESULT_OK) {
            if (null != data) {
                Bundle bundle = data.getExtras();
                if (bundle == null) {
                    return;
                }
                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    final String result = bundle.getString(CodeUtils.RESULT_STRING);
                    JSONObject j = new JSONObject();
                    String fun = "native.native.appCalljs('" + result + "')";
                    this.webView.evaluateJavascript(fun, new ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String s) {

                        }
                    });
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(MainActivity.this, R.string.msg_ewm, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    //WebViewClient主要帮助WebView处理各种通知、请求事件
    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public void onPageFinished(WebView view, String url) {//页面加载完成
            clearBg();
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {//页面开始加载
            showBg();
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            return super.shouldOverrideUrlLoading(view, url);
        }

        @TargetApi(android.os.Build.VERSION_CODES.M)
        @Override
        public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
            super.onReceivedHttpError(view, request, errorResponse);
        }
    };
    //WebChromeClient主要辅助WebView处理Javascript的对话框、网站图标、网站title、加载进度等
    private WebChromeClient webChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            if (newProgress < 100) {
                processTitle.setText(processTitle.getText().toString().split(":")[0] + ": " + newProgress + "%");
            }
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
        }
    };

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (quitCount == 1) {
            quitCount++;
            Toast.makeText(MainActivity.this, R.string.msg_quit, Toast.LENGTH_SHORT).show();
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(8000);//重置
                        quitCount = 1;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    private final class JSInterface {
        /**
         * json = {
         * type:1 复制，2获取扫描的二维码数据
         * data:"clip string"
         * }
         *
         * @param data
         */
        @JavascriptInterface
        public void callApp(String data) {
            Log.d("callApp", data);
            try {
                JSONObject jsonObject = new JSONObject(data);
                int type = jsonObject.getInt("type");
                if (type == 1) {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    cm.setText(jsonObject.getString("data"));
                } else if (type == 2) {
                    if (!lacksPermission(Manifest.permission.CAMERA)) {
                        Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                        startActivityForResult(intent, REQUEST_CODE_SCAN);
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CAMARA);
                        } else {
                            MainActivityPermissionsDispatcher.needCamPmWithPermissionCheck(MainActivity.this);
                        }
                    }
                } else if (type == 3) {

                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }

        @JavascriptInterface
        public void callJs(String data) {
            webView.loadUrl("javascript:native.native.callJs('" + data + "')");
            Log.d("callJs", data);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        MainActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
        if (requestCode == REQUEST_CAMARA) {//相机
            if (!lacksPermission(Manifest.permission.CAMERA)) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
            }
        }
        if (requestCode == REQUEST_SD) {//sd卡权限
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.PERMISSION_SD_INFO, Toast.LENGTH_LONG).show();
            }
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        webView.destroy();
        webView = null;
        if (connectionReceiver != null) {
            unregisterReceiver(connectionReceiver);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        webView.onPause();
        webView.pauseTimers();
    }

    @Override
    public void onResume() {
        super.onResume();
        webView.resumeTimers();
        webView.onResume();
    }

    private void clearBg() {
        progressBar.setVisibility(View.GONE);
        processTitle.setVisibility(View.GONE);
        webView.setBackgroundResource(0);
        webView.setBackgroundColor(Color.parseColor("#ffffff"));
    }

    private void showBg() {
        progressBar.setVisibility(View.VISIBLE);
        processTitle.setVisibility(View.VISIBLE);
        webView.setBackgroundColor(0);
        webView.setBackgroundResource(R.drawable.start);
    }

    //----------------------------------------------------------------------------------------------
    //23下，禁止后都是返回0;只要声明就返回0,询问会有弹出框

    // 单个权限
    // @NeedsPermission(Manifest.permission.CAMERA)
    // 多个权限
    @NeedsPermission(Manifest.permission.CAMERA)
    void needCamPm() {
    }

    // 被拒绝时：向用户说明为什么需要这些权限（可选）
    @OnShowRationale(Manifest.permission.CAMERA)
    void onShowCaRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.PERMISSION_REQUIRE)
                .setMessage(R.string.PERMISSION_SCAN)
                .setPositiveButton(R.string.AGREEE, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.REJECT, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.cancel();
                    }
                })
                .show();
    }

    // 用户拒绝授权回调（可选）
    @OnPermissionDenied(Manifest.permission.CAMERA)
    void onCamPmDenied() {
        Toast.makeText(MainActivity.this, R.string.PERMISSION_SCAN, Toast.LENGTH_LONG).show();
    }

    // 用户勾选了“不再提醒”时调用（可选）
    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void onCamNever() {
        Toast.makeText(MainActivity.this, R.string.PERMISSION_SET_AGAIN2, Toast.LENGTH_LONG).show();
    }

    @NeedsPermission({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void needSDPm() {
    }

    @OnShowRationale({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onShowSDPmRationale(final PermissionRequest request) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.PERMISSION_REQUIRE)
                .setMessage(R.string.PERMISSION_SD_INFO)
                .setPositiveButton(R.string.AGREEE, new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.proceed();
                    }
                })
                .setNegativeButton(R.string.REJECT, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        request.cancel();
                    }
                })
                .show();
    }

    @OnPermissionDenied({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onSDPmDenied() {
        Toast.makeText(MainActivity.this, R.string.PERMISSION_SD_INFO, Toast.LENGTH_LONG).show();
    }

    @OnNeverAskAgain({Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE})
    void onSDNever() {
        Toast.makeText(MainActivity.this, R.string.PERMISSION_SET_AGAIN1, Toast.LENGTH_LONG).show();
    }

    public boolean lacksPermission(String permission) {
        return ContextCompat.checkSelfPermission(this.getApplicationContext(), permission) ==
                PackageManager.PERMISSION_DENIED;
    }
}
