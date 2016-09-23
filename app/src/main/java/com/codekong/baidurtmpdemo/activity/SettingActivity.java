package com.codekong.baidurtmpdemo.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.codekong.baidurtmpdemo.R;

public class SettingActivity extends Activity {
    private static final String TAG = "SettingActivity";

    // TODO update your rtmp here url
    //默认推流地址
    private String mStreamKey = "rtmp://push.bcelive.com/live/3a4bbpgvqptbkcswkr";
    //页面加载中动画布局
    private LinearLayout mLoadingAnimation = null;
    //推流地址输入对话框
    private EditText mStreamUrlET = null;
    //开始推流按钮
    private Button mStartButton = null;
    //存储/读取用户推流设置参数
    private SharedPreferences mSharedPreferences = null;
    //是否进行横竖屏切换(默认值为竖屏)
    private boolean isOritationSwitcherChecked = false;

    //支持的码率参数
    private int mSupportedBitrateValues[] = new int[] { 2000, 1200, 800, 600 };

    //支持的视频分辨率(推流视频宽度和高度,例1920*1080)
    private int mSupportedResolutionValues[] = new int[] { 1920, 1080, 1280, 720, 640, 480, 480, 360 };
    private int mSelectedResolutionIndex = 1;

    //支持的帧速率
    private int mSupportedFramerateValues[] = new int[] { 18, 15, 15, 15 };
    //UI异步更新Handler
    private Handler mUIEventHandler = null;
    //隐藏跳转到推流Activity
    private static final int UI_EVENT_SHOW_STREAMING_ACTIVITY = 0;
    //上一次按返回键的时间
    private long mLastPressBackTime = 0;
    //一秒内连续两次点击返回键则退出程序
    private static final int INTERVAL_OF_TWO_CLICK_TO_QUIT = 1000; // 1 seconde

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "Calling onCreate()");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        //获得本地存储/读取实例
        mSharedPreferences = getApplication().getSharedPreferences("BCELive", Context.MODE_PRIVATE);
        //初始化UIHandler
        initUIHandler();
        //初始化UI元素
        initUIElements();
    }

    /**
     * 1秒之内双击返回键退出应用
     */
    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - mLastPressBackTime < INTERVAL_OF_TWO_CLICK_TO_QUIT) {
            //保存用户选择的推流参数
            saveStreamParams();
            //此处是自己设置逻辑把第一次按back键产生的finish()给吞了,使第一次按back键不退出应用
            Intent intent = new Intent();
            intent.putExtra("has_logout", false);
            setResult(RESULT_OK, intent);
            finish();
        } else {
            Toast.makeText(this, "再次按下返回键将退出应用！", Toast.LENGTH_SHORT).show();
            mLastPressBackTime = System.currentTimeMillis();
        }
    }

    @Override
    public void onStart() {
        Log.i(TAG, "Calling onStart()");
        super.onStart();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (resultCode) {
        case RESULT_OK:
            break;
        default:
            break;
        }
    }

    @Override
    public void onStop() {
        //保存用户选择的推流参数
        saveStreamParams();
        super.onStop();
    }

    @Override
    public void onDestroy() {
        //Activity销毁时移除回调及消息
        mUIEventHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }

    /**
     * 初始化UI元素
     */
    private void initUIElements() {
        //获取本地存储的推流参数
        fetchStreamParams();
        //将默认的推流地址设置到输入框中
        mStreamUrlET = (EditText) findViewById(R.id.et_streamurl);
        mStreamUrlET.setText(mStreamKey);
        //横竖屏选择结果判断
        RadioGroup orientationRadioGroup = (RadioGroup) findViewById(R.id.radioGroup1);
        final RadioButton radioLandscape = (RadioButton) findViewById(R.id.radioLandscape);
        final RadioButton radioPortrait = (RadioButton) findViewById(R.id.radioPortrait);
        orientationRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.radioLandscape) {
                    //横屏
                    isOritationSwitcherChecked = true;
                    radioLandscape.setTextColor(Color.WHITE);
                    radioPortrait.setTextColor(0xff666666);
                } else {
                    //竖屏
                    isOritationSwitcherChecked = false;
                    radioLandscape.setTextColor(0xff666666);
                    radioPortrait.setTextColor(Color.WHITE);
                }
            }
        });
        //设置单选按钮的选择样式状态
        if (isOritationSwitcherChecked) {
            radioLandscape.setChecked(true);
            radioLandscape.setTextColor(Color.WHITE);
        } else {
            radioPortrait.setChecked(true);
            radioPortrait.setTextColor(Color.WHITE);
        }
        //设置分辨率
        RadioGroup resolutionRadioGroup = (RadioGroup) findViewById(R.id.radioGroup0);
        final RadioButton radio1080P = (RadioButton) findViewById(R.id.radio1080p);
        final RadioButton radio720P = (RadioButton) findViewById(R.id.radio720p);
        final RadioButton radio480P = (RadioButton) findViewById(R.id.radio480p);
        final RadioButton radio360P = (RadioButton) findViewById(R.id.radio360p);
        resolutionRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            //根据是否选中来设置样式,同时设置mSelectedResolutionIndex(分辨率对应在数组中的索引)
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                radio1080P.setTextColor(0xff666666);
                radio720P.setTextColor(0xff666666);
                radio480P.setTextColor(0xff666666);
                radio360P.setTextColor(0xff666666);
                switch (checkedId) {
                case R.id.radio1080p:
                    mSelectedResolutionIndex = 0;
                    radio1080P.setTextColor(Color.WHITE);
                    break;
                case R.id.radio720p:
                    mSelectedResolutionIndex = 1;
                    radio720P.setTextColor(Color.WHITE);
                    break;
                case R.id.radio480p:
                    mSelectedResolutionIndex = 2;
                    radio480P.setTextColor(Color.WHITE);
                    break;
                case R.id.radio360p:
                    mSelectedResolutionIndex = 3;
                    radio360P.setTextColor(Color.WHITE);
                    break;
                }
            }
        });

        //将分辨率选择全部重置为初始状态
        switch (mSelectedResolutionIndex) {
        case 0:
            radio1080P.setChecked(true);
            radio1080P.setTextColor(Color.WHITE);
            break;
        case 1:
            radio720P.setChecked(true);
            radio720P.setTextColor(Color.WHITE);
            break;
        case 2:
            radio480P.setChecked(true);
            radio480P.setTextColor(Color.WHITE);
            break;
        case 3:
            radio360P.setChecked(true);
            radio360P.setTextColor(Color.WHITE);
            break;
        }

        mStartButton = (Button) findViewById(R.id.btn_start);
    }

    /**
     * 初始化UIHandler
     */
    private void initUIHandler() {
        //实例化UIHandler
        mUIEventHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case UI_EVENT_SHOW_STREAMING_ACTIVITY:
                    //跳转到录像直播页面
                    Intent intent = new Intent(SettingActivity.this, StreamingActivity.class);
                    saveStreamParams();
                    //带用户选择的参数传递到下一个activity
                    //推流url
                    intent.putExtra("push_url", mStreamKey.trim());
                    //设备宽度(取支持分辨率的偶数元素作为宽度)
                    intent.putExtra("res_w", mSupportedResolutionValues[mSelectedResolutionIndex * 2]);
                    //设备高度(取支持分辨率的偶数元素作为高度)
                    intent.putExtra("res_h", mSupportedResolutionValues[mSelectedResolutionIndex * 2 + 1]);
                    //帧速率
                    intent.putExtra("frame_rate", mSupportedFramerateValues[mSelectedResolutionIndex]);
                    //支持的码率
                    intent.putExtra("bitrate", mSupportedBitrateValues[mSelectedResolutionIndex]);
                    //设备横竖屏
                    intent.putExtra("oritation_landscape", isOritationSwitcherChecked);
                    startActivityForResult(intent, 0);
                    break;
                default:
                    break;
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 点击退出,同时保存用户设置的推流参数到本地
     * @param v
     */
    public void onClickQuit(View v) {
        saveStreamParams();
    }

    /**
     *获取推流参数
     */
    private void fetchStreamParams() {
        //默认视频分辨率1920*1080
        mSelectedResolutionIndex = mSharedPreferences.getInt("resolution", 1);
        //横竖屏(默认为竖屏)
        isOritationSwitcherChecked = mSharedPreferences.getBoolean("oritation_landscape", false);
    }


    /**
     * 保存用户选择的推流参数
     */
    private void saveStreamParams() {
        Editor editor = mSharedPreferences.edit();
        //保存分辨率索引
        editor.putInt("resolution", mSelectedResolutionIndex);
        //保存横竖屏
        editor.putBoolean("oritation_landscape", isOritationSwitcherChecked);
        editor.commit();
    }

    /**
     * 开始推流按钮的点击事件
     * @param v
     */
    public void onClickStart(View v) {
        //获得输入框中的推流地址
        mStreamKey = mStreamUrlET.getText().toString();
        if (!TextUtils.isEmpty(mStreamKey)) {
            //发送跳转到推流页面的消息
            mUIEventHandler.sendEmptyMessage(UI_EVENT_SHOW_STREAMING_ACTIVITY);
        } else {
            Toast.makeText(this, "注意：推流地址不能为空！！", Toast.LENGTH_SHORT).show();
        }
    }
}
