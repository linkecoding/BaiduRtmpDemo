package com.codekong.baidurtmpdemo.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Toast;

import com.codekong.baidurtmpdemo.R;
import com.codekong.baidurtmpdemo.util.NetworkUtils;

import java.util.TimerTask;

public class SplashActivity extends Activity {
    private static final String TAG = "SplashActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        if (!checkNetwork()) {
            //没有进行网络连接则不进入
            return;
        }
        new Handler().postDelayed(new TimerTask() {
            @Override
            public void run() {
                SplashActivity.this.startActivityForResult(new Intent(SplashActivity.this, SettingActivity.class), 0);
            }
        }, 700);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //当退出的时候直接finish掉页面
        finish();
    }

    /**
     * 检查网络状态
     * @return
     */
    private boolean checkNetwork() {
        if (!NetworkUtils.isConnected(this)) {
            Toast.makeText(this, "请检查网络状态！", Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }
}
