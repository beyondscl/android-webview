package com.ansen.webview;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.devspark.progressfragment.ProgressFragment;

public class LoadingActivity extends ProgressFragment {
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        // Setup content view
        setContentView(R.layout.activity_main);
        // Setup text for empty content
        setEmptyText("正在检查更新");
        // ...
        setContentShown(false);
    }
    public void shutdown(){
        setContentShown(true);
    }
}
