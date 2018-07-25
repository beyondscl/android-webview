package com.ansen.webview;

import android.app.Application;
import android.os.Environment;

import com.tencent.bugly.Bugly;
import com.tencent.bugly.beta.Beta;

public class WalletApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Bugly.init(getApplicationContext(), "f1b80e426b", false);
        Beta.checkUpgrade(false,false);
        /**
         * true表示app启动自动初始化升级模块；
         * false不自动初始化
         */
        Beta.autoInit = true;

        /**
         * true表示初始化时自动检查升级
         * false表示不会自动检查升级，需要手动调用Beta.checkUpgrade()方法
         */
        Beta.autoCheckUpgrade = true;

        /**
         * 设置升级周期为60s（默认检查周期为0s），60s内SDK不重复向后天请求策略
         */
        Beta.initDelay = 1 * 1000;

        /**
         * 设置通知栏大图标，largeIconId为项目中的图片资源；
         */
        Beta.largeIconId = R.mipmap.ic_launcher;

        /**
         * 设置状态栏小图标，smallIconId为项目中的图片资源id;
         */
        Beta.smallIconId = R.mipmap.ic_launcher;

        /**
         * 设置更新弹窗默认展示的banner，defaultBannerId为项目中的图片资源Id;
         * 当后台配置的banner拉取失败时显示此banner，默认不设置则展示“loading“;
         */
        Beta.defaultBannerId = R.mipmap.ic_launcher;

        /**
         * 设置sd卡的Download为更新资源保存目录;
         * 后续更新资源会保存在此目录，需要在manifest中添加WRITE_EXTERNAL_STORAGE权限;
         */
        Beta.storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);

        /**
         * 点击过确认的弹窗在APP下次启动自动检查更新时会再次显示;
         */
        Beta.showInterruptedStrategy = true;

        /**
         * 设置是否显示消息通知
         */
        Beta.enableNotification = true;

        /**
         * 使用默认弹窗
         */
        Beta.canShowApkInfo = true;
        /**
         * 关闭或开启热更新能力,默认开启
         */
        Beta.enableHotfix = true;

        /**
         * 只允许在MainActivity上显示更新弹窗，其他activity上不显示弹窗;
         * 不设置会默认所有activity都可以显示弹窗;
         */
        //  Beta.canShowUpgradeActs.add(MainActivity.class);
        // 不在登录activity上显示弹窗
        //Beta.canNotShowUpgradeActs.add(MainActivity.class);
    }
}
