package com.wwec.wallet;

public interface UpdateInter {
    String versionUrl = "http://120.79.236.139/user/getVersion";//test

    //    String versionUrl = "https://wallet.wwec.top/user/getVersion";//prod
    void reload();
}
