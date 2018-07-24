package com.ansen.webview;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
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

import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private WebSettings webSettings;
    private ProgressBar progressBar;
    private TextView processTitle;
    private int REQUEST_CODE_SCAN = 2;
    private int REQUEST_CODE_SCAN_GET = 3;//申请相机
    BroadcastReceiver connectionReceiver;//网络广播
    private boolean isLoaded = false;//是否已经加载,第一次断网进入，重新加载
    private boolean disConnect = false;//是否断开连接
    private int quitCount = 1;//5秒内点击2次推出
    private boolean isLoadDisconn = false;//是否标识加载disconnect图片，这个必定成功
    private boolean isFirstEnter = false;//是否第一次进入app
    private String path = null;//文件存储地址

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //去掉顶部标题栏
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        //版本信息存储地址，与app同生共死
        path = this.getApplicationContext().getCacheDir().getAbsolutePath() + "/";
        //进度条
        progressBar = findViewById(R.id.progressbar);
        //进度条文本
        processTitle = findViewById(R.id.processTitle);
        processTitle.setText(R.string.load_title);
        isFirstEnter = Util.isFirstEnter(path);
        //第一次创建版本信息,有那么必定有缓存
        if (isFirstEnter) {
            processTitle.setText(R.string.first_enter);
            Util.createFile(path);
        }
        webView = findViewById(R.id.webview);
        showBg();
        //核心设置
        webView = Webset.webSet(MainActivity.this, webView, webChromeClient, webViewClient, MainActivity.this.getApplicationContext().getCacheDir().getAbsolutePath(), isFirstEnter);
        webSettings = webView.getSettings();
        //启用交互
        webView.addJavascriptInterface(new JSInterface(), "Bridge");
        //初始化二维吗扫描
        ZXingLibrary.initDisplayOpinion(this);
        //注册网络监听
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(getConnnectionReceiver(), intentFilter);
        //监听网络变化
        startLoad(webView);
        final Handler updateHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                final Map map = (Map) msg.obj;
                if (Integer.valueOf(map.get("type").toString()) == 1) {
                    final String version = map.get("version").toString();
                    Util.showNormalDialog(MainActivity.this, new UpdateInter() {
                        @Override
                        public void reload() {
                            Util.modifyFile(path, version);
                            webSettings.setCacheMode(webSettings.LOAD_NO_CACHE);
                            processTitle.setText(R.string.update_title);
                            startLoad(webView);
                        }
                    });
                }
            }
        };
        //检查更新
        if (!isFirstEnter) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Util.checkVersion(MainActivity.this, webSettings, updateHandler, path);
                }
            }).start();
        }
    }

    private void startLoad(WebView webView) {
        isLoaded = true;
        isLoadDisconn = false;
//        webView.loadUrl("file:///android_asset/release/index.html");//加载本地
//        webView.loadUrl("http://192.168.2.113:8080/wwwallet/index.html");//加载url
//        webView.loadUrl("http://120.79.236.139");//加载url
        webView.loadUrl("https://wallet.wwec.top");//加载url
    }

    private BroadcastReceiver getConnnectionReceiver() {
        connectionReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                ConnectivityManager connectMgr = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
                NetworkInfo mobNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
                NetworkInfo wifiNetInfo = connectMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

                if (!mobNetInfo.isConnected() && !wifiNetInfo.isConnected()) {
                    disConnect = true;
                    Toast.makeText(MainActivity.this, R.string.net_disconnect, Toast.LENGTH_LONG).show();
                } else {
                    if (disConnect) {
                        disConnect = false;
                        Toast.makeText(MainActivity.this, R.string.net_connect, Toast.LENGTH_LONG).show();
                    }
                    if (!isLoaded) {
                        if (isFirstEnter) {//第一次无网进入，需要加载远程
                            webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE);
                        }
                        startLoad(webView);
                    }
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
        if (requestCode == REQUEST_CODE_SCAN_GET) {

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
            // 这个方法在6.0才出现
            int statusCode = errorResponse.getStatusCode();
            if (404 == statusCode || 500 == statusCode) {
                startLoad(webView);
            }
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
            // android 6.0 以下通过title获取
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                if (title.contains("404") || title.contains("500") || title.contains("Error") || title.contains("网页无法打开")) {
                    setDisConn();
                }
            }
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
                    //申请权限
                    if (!checkPermissions()) {
                        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_SCAN);
                        return;
                    }
                    Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                    startActivityForResult(intent, REQUEST_CODE_SCAN);
                } else if (type == 3) {
                    // 不去掉背景，fps只有10左右。
                    //webView.setBackgroundColor(Color.parseColor("#ffffff")); //ok 不会闪黑屏
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
        if (requestCode == REQUEST_CODE_SCAN) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
            } else {
                Toast.makeText(this, "请在应用管理中打开“相机”访问权限！", Toast.LENGTH_LONG).show();
                finish();
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

    private boolean checkPermissions() {
        try {
            if (Build.VERSION.SDK_INT < 23) {//一般android6以下会在安装时自动获取权限,但在小米机上，可能通过用户权限管理更改权限
                return true;
            } else {
                if (getApplicationInfo().targetSdkVersion < 23) {
                    //targetSdkVersion<23时 即便运行在android6及以上设备 ContextWrapper.checkSelfPermission和Context.checkSelfPermission失效
                    //返回值始终为PERMISSION_GRANTED
                    //此时必须使用PermissionChecker.checkSelfPermission
                    if (PermissionChecker.checkPermission(this, Manifest.permission.CAMERA, Binder.getCallingPid(), Binder.getCallingUid(), getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        return true;
                    } else {
                        return false;
                    }
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    private void clearBg() {
        progressBar.setVisibility(View.INVISIBLE);
        processTitle.setVisibility(View.INVISIBLE);
        webView.setBackgroundResource(0);
        webView.setBackgroundColor(Color.parseColor("#ffffff"));
        if (isLoadDisconn) {
            setReConn();
        }
    }

    private void showBg() {
        progressBar.setVisibility(View.VISIBLE);
        processTitle.setVisibility(View.VISIBLE);
        webView.setBackgroundColor(0);
        webView.setBackgroundResource(R.drawable.start);
    }

    private void setReConn() {
        progressBar.setVisibility(View.VISIBLE);
        processTitle.setVisibility(View.VISIBLE);
        processTitle.setText(R.string.wait_net);
        processTitle.setTextColor(Color.BLACK);
    }

    private void setDisConn() {
        isLoaded = false;
        isLoadDisconn = true;
        webView.loadUrl("file:///android_asset/imgs/disconnnect.png");
    }
}
