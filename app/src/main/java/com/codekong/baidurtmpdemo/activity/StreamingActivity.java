package com.codekong.baidurtmpdemo.activity;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.GestureDetectorCompat;
import android.text.TextUtils;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.recorder.api.LiveConfig;
import com.baidu.recorder.api.LiveSession;
import com.baidu.recorder.api.LiveSessionHW;
import com.baidu.recorder.api.LiveSessionSW;
import com.baidu.recorder.api.SessionStateListener;
import com.codekong.baidurtmpdemo.R;

/**
 * 录像推流处理页面
 */
public class StreamingActivity extends Activity
        implements GestureDetector.OnGestureListener, GestureDetector.OnDoubleTapListener {
    private static final String TAG = "StreamingActivity";
    //直播Session(会话)
    private LiveSession mLiveSession = null;
    //开始录像(直播按钮)
    private Button mRecorderButton = null;
    //对焦图片
    private ImageView mFocusIcon = null;
    //闪光灯状态按钮
    private Button mFlashStateButton = null;
    //美化录像状态按钮
    private Button mBeautyEffectStateButton = null;
    //录像/直播状态文字显示
    private TextView statusView = null;
    //加载中动画布局
    private LinearLayout mLoadingAnimation = null;
    //Session是否准备好
    private boolean isSessionReady = false;
    //Session是否开始
    private boolean isSessionStarted = false;
    //推流连接是否建立
    private boolean isConnecting = false;
    //停止直播之后是否需要重启
    private boolean needRestartAfterStopped = false;

    //录像/直播正在连接
    private static final int UI_EVENT_RECORDER_CONNECTING = 0;
    //开始直播
    private static final int UI_EVENT_RECORDER_STARTED = 1;
    //停止直播推流
    private static final int UI_EVENT_RECORDER_STOPPED = 2;
    //Session进入准备状态
    private static final int UI_EVENT_SESSION_PREPARED = 3;
    //隐藏对焦icon
    private static final int UI_EVENT_HIDE_FOCUS_ICON = 4;
    //重启推流
    private static final int UI_EVENT_RESTART_STREAMING = 5;
    //重新连接服务器
    private static final int UI_EVENT_RECONNECT_SERVER = 6;
    //停止推流
    private static final int UI_EVENT_STOP_STREAMING = 7;
    //展示Toast提示信息
    private static final int UI_EVENT_SHOW_TOAST_MESSAGE = 8;
    //调整摄像头预览
    private static final int UI_EVENT_RESIZE_CAMERA_PREVIEW = 9;
    //展示上传带宽
    private static final int TEST_EVENT_SHOW_UPLOAD_BANDWIDTH = 10;
    //事件处理Handler
    private Handler mUIEventHandler = null;
    //摄像头视图
    private SurfaceView mCameraView = null;
    //Session状态监听
    private SessionStateListener mStateListener = null;
    //手势检测
    private GestureDetectorCompat mDetector = null;
    //当前摄像头(前置还是后置)
    private int mCurrentCamera = -1;
    //闪光灯是否打开
    private boolean isFlashOn = false;
    //美颜是否打开
    private boolean hasBueatyEffect = false;
    //视频宽度
    private int mVideoWidth = 1280;
    //视频高度
    private int mVideoHeight = 720;
    //帧速率
    private int mFrameRate = 15;
    //码率
    private int mBitrate = 1024000;
    //推流地址
    private String mStreamingUrl = null;
    //设备横竖屏(默认竖屏)
    private boolean isOritationLanscape = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        //初始化布局
        initView();
        //获取上个Activity传入的配置(选择)参数
        initConfig();

        //UI异步消息处理
        initUIEventHandler();
        //初始化Session状态监听
        initStateListener();

        initRTMPSession(mCameraView.getHolder());
        mDetector = new GestureDetectorCompat(this, this);
        mDetector.setOnDoubleTapListener(this);
    }

    /**
     * 初始化布局
     */
    private void initView() {
        Window win = getWindow();
        //设置屏幕常亮
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        //设置全屏
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        //隐藏标题栏
        win.requestFeature(Window.FEATURE_NO_TITLE);
        //根据上个页面传入的参数设置横竖屏,加载横竖屏对应的布局
        isOritationLanscape = getIntent().getBooleanExtra("oritation_landscape", false);
        if (isOritationLanscape) {
            //设置为横屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            setContentView(R.layout.activity_streaming_landscape);
        } else {
            //设置为竖屏
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            setContentView(R.layout.activity_streaming_portrait);
        }
    }

    /**
     * 初始化配置,以及获取上个Activity传入的配置参数
     */
    private void initConfig() {
        //获取从上个Activity传入的用户选择的参数
        mVideoWidth = getIntent().getIntExtra("res_w", 1280);
        mVideoHeight = getIntent().getIntExtra("res_h", 720);
        mFrameRate = getIntent().getIntExtra("frame_rate", 15);
        mBitrate = getIntent().getIntExtra("bitrate", 1024) * 1000;
        mStreamingUrl = getIntent().getStringExtra("push_url");
        //初始化UI元素
        initUIElements();
        //初始配置摄像头为前置摄像头
        mCurrentCamera = LiveConfig.CAMERA_FACING_FRONT;
        //闪光灯关闭
        isFlashOn = false;
    }
    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "onConfigurationChanged orientation=" + newConfig.orientation);
        super.onConfigurationChanged(newConfig);
    }

    /**
     * 初始化UI元素
     */
    private void initUIElements() {
        mLoadingAnimation = (LinearLayout) findViewById(R.id.loading_anim);
        mRecorderButton = (Button) findViewById(R.id.btn_streaming_action);
        mRecorderButton.setEnabled(false);
        mCameraView = (SurfaceView) findViewById(R.id.sv_camera_preview);
        mFocusIcon = (ImageView) findViewById(R.id.iv_ico_focus);
        mFlashStateButton = (Button) findViewById(R.id.iv_flash_state);
        mBeautyEffectStateButton = (Button) findViewById(R.id.iv_effect_state);
        statusView = (TextView) findViewById(R.id.tv_streaming_action);
    }

    /**
     * UI异步消息处理
     */
    private void initUIEventHandler() {
        mUIEventHandler = new Handler() {

            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case UI_EVENT_RECORDER_CONNECTING:
                        //直播/录像正在连接推流服务器,设置UI
                        isConnecting = true;
                        mRecorderButton.setBackgroundResource(R.drawable.btn_block_streaming);
                        mRecorderButton.setPadding(0, 0, 0, 0);
                        statusView.setText("连接中");
                        mRecorderButton.setEnabled(false);
                        break;
                    case UI_EVENT_RECORDER_STARTED:
                        //开启推流服务器
                        Log.i(TAG, "Starting Streaming succeeded!");
                        //服务器失败尝试连接次数为0
                        serverFailTryingCount = 0;
                        isSessionStarted = true;
                        needRestartAfterStopped = false;
                        isConnecting = false;
                        mRecorderButton.setBackgroundResource(R.drawable.btn_stop_streaming);
                        mRecorderButton.setPadding(0, 0, 0, 0);
                        statusView.setText("停止直播");
                        mRecorderButton.setEnabled(true);
                        break;
                    case UI_EVENT_RECORDER_STOPPED:
                        //停止直播推流
                        Log.i(TAG, "Stopping Streaming succeeded!");
                        //服务器失败尝试连接次数为0
                        serverFailTryingCount = 0;
                        isSessionStarted = false;
                        needRestartAfterStopped = false;
                        isConnecting = false;
                        mRecorderButton.setBackgroundResource(R.drawable.btn_start_streaming);
                        mRecorderButton.setPadding(0, 0, 0, 0);
                        statusView.setText("开始直播");
                        mRecorderButton.setEnabled(true);
                        break;
                    case UI_EVENT_SESSION_PREPARED:
                        //Live Session已准备好
                        isSessionReady = true;
                        mLoadingAnimation.setVisibility(View.GONE);
                        mRecorderButton.setEnabled(true);
                        break;
                    case UI_EVENT_HIDE_FOCUS_ICON:
                        //隐藏对焦icon
                        mFocusIcon.setVisibility(View.GONE);
                        break;
                    case UI_EVENT_RECONNECT_SERVER:
                        //重新连接服务器
                        Log.i(TAG, "Reconnecting to server...");
                        if (isSessionReady && mLiveSession != null) {
                            //开启RtmpSession
                            mLiveSession.startRtmpSession(mStreamingUrl);
                        }
                        if (mUIEventHandler != null) {
                            //发送空消息使其处于正在连接状态
                            mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_CONNECTING);
                        }
    
                        break;
                    case UI_EVENT_STOP_STREAMING:
                        //停止直播推流
                        if (!isConnecting) {
                            Log.i(TAG, "Stopping current session...");
                            if (isSessionReady) {
                                mLiveSession.stopRtmpSession();
                            }
                            mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_STOPPED);
                        }
                        break;
                    case UI_EVENT_RESTART_STREAMING:
                        //重启直播推流
                        if (!isConnecting) {
                            Log.i(TAG, "Restarting session...");
                            isConnecting = true;
                            needRestartAfterStopped = true;
                            if (isSessionReady && mLiveSession != null) {
                                mLiveSession.stopRtmpSession();
                            }
                            if (mUIEventHandler != null) {
                                mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_CONNECTING);
                            }
    
                        }
                        break;
                    case UI_EVENT_SHOW_TOAST_MESSAGE:
                        //弹出Toast提示消息
                        String text = (String) msg.obj;
                        Toast.makeText(StreamingActivity.this, text, Toast.LENGTH_SHORT).show();
                        break;
                    case UI_EVENT_RESIZE_CAMERA_PREVIEW:
                        //调整摄像头预览
                        String hint = "注意：当前摄像头不支持您所选择的分辨率\n实际分辨率为" + mVideoWidth + "x" + mVideoHeight;
                        Toast.makeText(StreamingActivity.this, hint, Toast.LENGTH_LONG).show();
                        //根据分变率自适应预览
                        fitPreviewToParentByResolution(mCameraView.getHolder(), mVideoWidth, mVideoHeight);
                        break;
                    case TEST_EVENT_SHOW_UPLOAD_BANDWIDTH:
                        //显示上传带宽
                        if (mLiveSession != null) {
                            Log.d(TAG, "Current upload bandwidth is " + mLiveSession.getCurrentUploadBandwidthKbps()
                                    + " KBps.");
                        }
                        if (mUIEventHandler != null) {
                            mUIEventHandler.sendEmptyMessageDelayed(TEST_EVENT_SHOW_UPLOAD_BANDWIDTH, 2000);
                        }
                        break;
                    default:
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    /**
     * 初始化Session状态监听
     */
    private void initStateListener() {
        mStateListener = new SessionStateListener() {
            @Override
            public void onSessionPrepared(int code) {
                if (code == SessionStateListener.RESULT_CODE_OF_OPERATION_SUCCEEDED) {
                    if (mUIEventHandler != null) {
                        //发送Session准备消息
                        mUIEventHandler.sendEmptyMessage(UI_EVENT_SESSION_PREPARED);
                    }
                    //设置的分辨率与适配器分别率不相等则进行调整
                    int realWidth = mLiveSession.getAdaptedVideoWidth();
                    int realHeight = mLiveSession.getAdaptedVideoHeight();
                    if (realHeight != mVideoHeight || realWidth != mVideoWidth) {
                        mVideoHeight = realHeight;
                        mVideoWidth = realWidth;
                        //发送异步消息
                        mUIEventHandler.sendEmptyMessage(UI_EVENT_RESIZE_CAMERA_PREVIEW);
                    }
                }
            }

            @Override
            public void onSessionStarted(int code) {
                if (code == SessionStateListener.RESULT_CODE_OF_OPERATION_SUCCEEDED) {
                    if (mUIEventHandler != null) {
                        //发送开始推流消息
                        mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_STARTED);
                    }
                } else {
                    Log.e(TAG, "Starting Streaming failed!");
                }
            }

            @Override
            public void onSessionStopped(int code) {
                if (code == SessionStateListener.RESULT_CODE_OF_OPERATION_SUCCEEDED) {
                    if (mUIEventHandler != null) {
                        if (needRestartAfterStopped && isSessionReady) {
                            //停止直播之后需要重启推流
                            mLiveSession.startRtmpSession(mStreamingUrl);
                        } else {
                            //不需要重启,发送停止推流消息
                            mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_STOPPED);
                        }
                    }
                } else {
                    Log.e(TAG, "Stopping Streaming failed!");
                }
            }

            @Override
            public void onSessionError(int code) {
                switch (code) {
                    case SessionStateListener.ERROR_CODE_OF_OPEN_MIC_FAILED:
                        Log.e(TAG, "Error occurred while opening MIC!");
                        //打开MIC设备失败
                        onOpenDeviceFailed();
                        break;
                    case SessionStateListener.ERROR_CODE_OF_OPEN_CAMERA_FAILED:
                        Log.e(TAG, "Error occurred while opening Camera!");
                        //打开相机失败
                        onOpenDeviceFailed();
                        break;
                    case SessionStateListener.ERROR_CODE_OF_PREPARE_SESSION_FAILED:
                        Log.e(TAG, "Error occurred while preparing recorder!");
                        //准备推流失败
                        onPrepareFailed();
                        break;
                    case SessionStateListener.ERROR_CODE_OF_CONNECT_TO_SERVER_FAILED:
                        Log.e(TAG, "Error occurred while connecting to server!");
                        //连接服务器失败
                        if (mUIEventHandler != null) {
                            serverFailTryingCount++;
                            if (serverFailTryingCount > 5) {
                                //尝试自动连接服务器次数大于5,发消息停止推流
                                Message msg = mUIEventHandler.obtainMessage(UI_EVENT_SHOW_TOAST_MESSAGE);
                                msg.obj = "自动重连服务器失败，请检查网络设置";
                                mUIEventHandler.sendMessage(msg);
                                mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_STOPPED);
                            } else {
                                Message msg = mUIEventHandler.obtainMessage(UI_EVENT_SHOW_TOAST_MESSAGE);
                                msg.obj = "连接推流服务器失败，自动重试5次，当前为第" + serverFailTryingCount + "次";
                                mUIEventHandler.sendMessage(msg);
                                mUIEventHandler.sendEmptyMessageDelayed(UI_EVENT_RECONNECT_SERVER, 2000);
                            }
                            
                        }
                        break;
                    case SessionStateListener.ERROR_CODE_OF_DISCONNECT_FROM_SERVER_FAILED:
                        Log.e(TAG, "Error occurred while disconnecting from server!");
                        isConnecting = false;
                        // Although we can not stop session successfully, we still
                        // need to take it as stopped
                        if (mUIEventHandler != null) {
                            mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_STOPPED);
                        }
                        break;
                    default:
                        onStreamingError(code);
                        break;
                }
            }
        };
    }

    //服务器连接尝试次数
    int serverFailTryingCount = 0;

    /**
     * 打开设备失败
     */
    private void onOpenDeviceFailed() {
        if (mUIEventHandler != null) {
            Message msg = mUIEventHandler.obtainMessage(UI_EVENT_SHOW_TOAST_MESSAGE);
            msg.obj = "摄像头或MIC打开失败！请确认您已开启相关硬件使用权限！";
            mUIEventHandler.sendMessage(msg);
        }
    }

    /**
     * 直播准备失败
     */
    private void onPrepareFailed() {
        isSessionReady = false;
    }
    //网络微弱提示次数
    int mWeakConnectionHintCount = 0;

    /**
     * 当推流失败时
     * @param errno
     */
    private void onStreamingError(int errno) {
        Message msg = mUIEventHandler.obtainMessage(UI_EVENT_SHOW_TOAST_MESSAGE);
        switch (errno) {
            //连接被服务器拒绝/服务器内部错误
            case SessionStateListener.ERROR_CODE_OF_PACKET_REFUSED_BY_SERVER:
            case SessionStateListener.ERROR_CODE_OF_SERVER_INTERNAL_ERROR:
                msg.obj = "因服务器异常，当前直播已经中断！正在尝试重新推流...";
                if (mUIEventHandler != null) {
                    mUIEventHandler.sendMessage(msg);
                    //发送重启服务器消息
                    mUIEventHandler.sendEmptyMessage(UI_EVENT_RESTART_STREAMING);
                }
                break;
            case SessionStateListener.ERROR_CODE_OF_WEAK_CONNECTION:
                Log.i(TAG, "Weak connection...");
                msg.obj = "当前网络不稳定，请检查网络信号！";
                mWeakConnectionHintCount++;
                if (mUIEventHandler != null) {
                    mUIEventHandler.sendMessage(msg);
                    if (mWeakConnectionHintCount >= 5) {
                        mWeakConnectionHintCount = 0;
                        //网络微弱提示次数大于5,重启推流
                        mUIEventHandler.sendEmptyMessage(UI_EVENT_RESTART_STREAMING);
                    }
                }
                break;
            case SessionStateListener.ERROR_CODE_OF_CONNECTION_TIMEOUT:
                //网络连接超时
                Log.i(TAG, "Timeout when streaming...");
                msg.obj = "连接超时，请检查当前网络是否畅通！我们正在努力重连...";
                if (mUIEventHandler != null) {
                    mUIEventHandler.sendMessage(msg);
                    mUIEventHandler.sendEmptyMessage(UI_EVENT_RESTART_STREAMING);
                }
                break;
            default:
                Log.i(TAG, "Unknown error when streaming...");
                msg.obj = "未知错误，当前直播已经中断！正在重试！";
                if (mUIEventHandler != null) {
                    mUIEventHandler.sendMessage(msg);
                    mUIEventHandler.sendEmptyMessageDelayed(UI_EVENT_RESTART_STREAMING, 1000);
                }
                break;
        }
    }

    /**
     * 初始化RTMP Session,配置参数
     * @param sh
     */
    private void initRTMPSession(SurfaceHolder sh) {
        int orientation = isOritationLanscape ? LiveConfig.ORIENTATION_LANDSCAPE : LiveConfig.ORIENTATION_PORTRAIT;
        LiveConfig liveConfig = new LiveConfig.Builder().setCameraId(LiveConfig.CAMERA_FACING_FRONT) // 选择摄像头为前置摄像头
                .setCameraOrientation(orientation) // 设置摄像头为竖向
                .setVideoWidth(mVideoWidth) // 设置推流视频宽度, 需传入长的一边
                .setVideoHeight(mVideoHeight) // 设置推流视频高度，需传入短的一边
                .setVideoFPS(mFrameRate) // 设置视频帧率
                .setInitVideoBitrate(mBitrate) // 设置视频码率，单位为bit per seconds
                .setAudioBitrate(64 * 1000) // 设置音频码率，单位为bit per seconds
                .setAudioSampleRate(LiveConfig.AUDIO_SAMPLE_RATE_44100) // 设置音频采样率
                .setGopLengthInSeconds(2) // 设置I帧间隔，单位为秒
                .build();
        Log.d(TAG, "Calling initRTMPSession..." + liveConfig.toString());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            //硬编码
            mLiveSession = new LiveSessionHW(this, liveConfig);
        } else {
            //软编码
            mLiveSession = new LiveSessionSW(this, liveConfig);
        }
        //设置状态监听
        mLiveSession.setStateListener(mStateListener);
        //设置预览View
        mLiveSession.bindPreviewDisplay(sh);
        //启动音视频采集设备(即相机和 MIC)
        mLiveSession.prepareSessionAsync();
    }

    /**
     * 点击退出
     * @param v
     */
    public void onClickQuit(View v) {
        if (isSessionStarted) {
            Toast.makeText(this, "直播过程中不能返回，请先停止直播！", Toast.LENGTH_SHORT).show();
        } else {
            this.finish();
        }
    }

    /**
     * 点击切换闪光灯
     * @param v
     */
    public void onClickSwitchFlash(View v) {
        //后置摄像头才可开启闪光灯
        if (mCurrentCamera == LiveConfig.CAMERA_FACING_BACK) {
            mLiveSession.toggleFlash(!isFlashOn);
            isFlashOn = !isFlashOn;
            //根据状态设置不同的背景图片
            if (isFlashOn) {
                mFlashStateButton.setBackgroundResource(R.drawable.btn_flash_on);
            } else {
                mFlashStateButton.setBackgroundResource(R.drawable.btn_flash_off);
            }
        }
    }

    /**
     * 点击切换摄像头
     * @param v
     */
    public void onClickSwitchCamera(View v) {
        if (mLiveSession.canSwitchCamera()) {
            if (mCurrentCamera == LiveConfig.CAMERA_FACING_BACK) {
                //将后置摄像头切换为前置摄像头,若闪光灯开着应该将其关闭
                mCurrentCamera = LiveConfig.CAMERA_FACING_FRONT;
                mLiveSession.switchCamera(mCurrentCamera);
                if (isFlashOn) {
                    mFlashStateButton.setBackgroundResource(R.drawable.btn_flash_off);
                }
            } else {
                mCurrentCamera = LiveConfig.CAMERA_FACING_BACK;
                mLiveSession.switchCamera(mCurrentCamera);
                if (isFlashOn) {
                    mFlashStateButton.setBackgroundResource(R.drawable.btn_flash_on);
                }
            }
        } else {
            Toast.makeText(this, "抱歉！该分辨率下不支持切换摄像头！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 点击美颜
     * @param v
     */
    public void onClickSwitchBeautyEffect(View v) {
        hasBueatyEffect = !hasBueatyEffect;
        mLiveSession.enableDefaultBeautyEffect(hasBueatyEffect);
        mBeautyEffectStateButton
                .setBackgroundResource(hasBueatyEffect ? R.drawable.btn_effect_on : R.drawable.btn_effect_off);
    }

    /**
     * 点击直播推流摄像头
     * @param v
     */
    public void onClickStreamingButton(View v) {
        if (!isSessionReady) {
            return;
        }
        if (!isSessionStarted && !TextUtils.isEmpty(mStreamingUrl)) {
            //开启直播Session并开始建立连接
            if (mLiveSession.startRtmpSession(mStreamingUrl)) {
                Log.i(TAG, "Starting Streaming in right state!");
            } else {
                Log.e(TAG, "Starting Streaming in wrong state!");
            }
            mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_CONNECTING);
        } else {
            if (mLiveSession.stopRtmpSession()) {
                Log.i(TAG, "Stopping Streaming in right state!");
            } else {
                Log.e(TAG, "Stopping Streaming in wrong state!");
            }
            mUIEventHandler.sendEmptyMessage(UI_EVENT_RECORDER_CONNECTING);
        }
    }

    /**
     * 返回键的处理
     */
    @Override
    public void onBackPressed() {
        if (isSessionStarted) {
            Toast.makeText(this, "直播过程中不能返回，请先停止直播！", Toast.LENGTH_SHORT).show();
        } else {
            finish();
        }
    }

    @Override
    public void onStart() {
        Log.i(TAG, "===========> onStart()");
        super.onStart();
    }

    @Override
    protected void onStop() {
        Log.i(TAG, "===========> onStop()");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i(TAG, "===========> onDestroy()");
        mUIEventHandler.removeCallbacksAndMessages(null);
        if (isSessionStarted) {
            mLiveSession.stopRtmpSession();
            isSessionStarted = false;
        }
        if (isSessionReady) {
            mLiveSession.destroyRtmpSession();
            mLiveSession = null;
            mStateListener = null;
            mUIEventHandler = null;
            isSessionReady = false;
        }
        super.onDestroy();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.mDetector.onTouchEvent(event)) {
            //消费掉触摸事件
            return true;
        }
        // Be sure to call the superclass implementation
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onDown(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onFling(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent arg0) {
    }

    @Override
    public boolean onScroll(MotionEvent arg0, MotionEvent arg1, float arg2, float arg3) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent arg0) {
    }

    @Override
    public boolean onSingleTapUp(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onDoubleTap(MotionEvent arg0) {
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent arg0) {
        if (mLiveSession != null && !mLiveSession.zoomInCamera()) {
            Log.e(TAG, "Zooming camera failed!");
            mLiveSession.cancelZoomCamera();
        }
        return true;
    }

    /**
     * 单击屏幕处理对焦
     * @param arg0
     * @return
     */
    @Override
    public boolean onSingleTapConfirmed(MotionEvent arg0) {
        if (mLiveSession != null) {
            //获得单击位置,计算出显示对焦图片的位置
            mLiveSession.focusToPosition((int) arg0.getX(), (int) arg0.getY());
            mFocusIcon.setX(arg0.getX() - mFocusIcon.getWidth() / 2);
            mFocusIcon.setY(arg0.getY() - mFocusIcon.getHeight() / 2);
            mFocusIcon.setVisibility(View.VISIBLE);
            //一秒之后隐藏对焦的图片
            mUIEventHandler.sendEmptyMessageDelayed(UI_EVENT_HIDE_FOCUS_ICON, 1000);
        }
        return true;
    }

    /**
     * 根据分辨率及屏幕宽高动态调整适应宽高
     * @param holder
     * @param width
     * @param height
     */
    private void fitPreviewToParentByResolution(SurfaceHolder holder, int width, int height) {
        //动态调整SurfaceView的外观
        int screenHeight = getWindow().getDecorView().getRootView().getHeight();
        int screenWidth = getWindow().getDecorView().getRootView().getWidth();
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            //如果是竖屏,我们应该交换宽和高(后面配置需要)
            width = width ^ height;
            height = width ^ height;
            width = width ^ height;
        }
        //调整视频的宽高
        int adjustedVideoHeight;
        int adjustedVideoWidth;
        if (width * screenHeight > height * screenWidth) {
            //width/height > screenWidth/screenHeight
            //Fit width
            adjustedVideoHeight = height * screenWidth / width;
            adjustedVideoWidth = screenWidth;
        } else {
            // Fit height
            adjustedVideoHeight = screenHeight;
            adjustedVideoWidth = width * screenHeight / height;
        }
        //设置调整后的值
        holder.setFixedSize(adjustedVideoWidth, adjustedVideoHeight);
    }
}
