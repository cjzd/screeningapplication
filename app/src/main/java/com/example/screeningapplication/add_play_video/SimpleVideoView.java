package com.example.screeningapplication.add_play_video;

/**
 * Created by tpf on 2018-08-21.
 */

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.VideoView;

import com.example.screeningapplication.ChairmanActivity;
import com.example.screeningapplication.LogUtil;
import com.example.screeningapplication.R;
import com.example.screeningapplication.video_audio_transport.VoiceRTPPacket;

public class SimpleVideoView extends RelativeLayout implements OnClickListener{
    private static final String TAG = "SimpleVideoView";

    private Context context;
    private View mView;
    private VideoView mVideoView;//视频控件
//    private ImageView mBigPlayBtn;//大的播放按钮
    private ImageView mPlayBtn;//播放按钮
    private ImageView mFullScreenBtn;//全屏按钮
    private SeekBar mPlayProgressBar;//播放进度条
    private TextView mPlayTime;//播放时间
    private LinearLayout mControlPanel;

    private String path = null;

    private Animation outAnima;//控制面板出入动画
    private Animation inAnima;//控制面板出入动画

    private int mVideoDuration;//视频毫秒数
    private int mCurrentProgress;//毫秒数

    private Runnable mUpdateTask;
    private Thread mUpdateThread;

    private final int UPDATE_PROGRESS = 0;
    private final int EXIT_CONTROL_PANEL = 1;
    private boolean stopThread = true;//停止更新进度线程标志

    private Point screenSize = new Point();//屏幕大小
    private boolean mIsFullScreen = false;//是否全屏标志

    private int mWidth;//控件的宽度
    private int mHeigth;//控件的高度

    private VoiceRTPPacket voiceRTPPacket;
    public static boolean timeChanged;

    public SimpleVideoView(Context context){
        super(context);
        Log.e(TAG, "SimpleVideoView1: 初始化" );
        init(context, null, 0);
    }
    public SimpleVideoView(Context context, AttributeSet attrs){
        super(context, attrs);
        Log.e(TAG, "SimpleVideoView2: 初始化" );
        init(context, attrs, 0);

    }
    public SimpleVideoView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.e(TAG, "SimpleVideoView3: 初始化" );
        init(context, attrs, defStyleAttr);
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case UPDATE_PROGRESS:
                    mPlayProgressBar.setProgress(mCurrentProgress);
                    setPlayTime(mCurrentProgress);
                    break;
                case EXIT_CONTROL_PANEL:
                    //执行退出动画
                    if(mControlPanel.getVisibility() != View.GONE){
                        mControlPanel.startAnimation(outAnima);
                        mControlPanel.setVisibility(View.GONE);
                    }
                    break;
            }
        }
    };

    //初始化控件
    private void init(final Context context, AttributeSet attrs, int defStyleAttr){
        this.context = context;
        //加载simple_video_view
        mView = LayoutInflater.from(context).inflate(R.layout.simple_video_view, this);
//        mBigPlayBtn = (ImageView) mView.findViewById(R.id.big_play_button);
        mPlayBtn = (ImageView) mView.findViewById(R.id.play_button);
        mFullScreenBtn = (ImageView) mView.findViewById(R.id.full_screen_button);
        mPlayProgressBar = (SeekBar) mView.findViewById(R.id.progress_bar);
        mPlayTime = (TextView) mView.findViewById(R.id.time);
        mControlPanel = (LinearLayout) mView.findViewById(R.id.control_panel);
        mVideoView = (VideoView) mView.findViewById(R.id.video_view);
        //获取屏幕大小
        ((Activity) context).getWindowManager().getDefaultDisplay().getSize(screenSize);
        //加载动画
        outAnima = AnimationUtils.loadAnimation(context, R.anim.exit_from_bottom);
        inAnima = AnimationUtils.loadAnimation(context, R.anim.enter_from_bottom);
        //设置控制面板初始不可见
        mControlPanel.setVisibility(View.GONE);
        //设置大的播放按钮可见
//        mBigPlayBtn.setVisibility(View.VISIBLE);
        //设置媒体控制器
//		mMediaController = new MediaController(context);
//		mMediaController.setVisibility(View.GONE);
//		mVideoView.setMediaController(mMediaController);
        mVideoView.setOnPreparedListener(new OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                //视频加载完成后才能获取视频时长
                initVideo();
                voiceRTPPacket = new VoiceRTPPacket(path, ChairmanActivity.udpSocketA, mVideoView);
                mVideoView.setBackground(null);
                if(mUpdateThread == null || !mUpdateThread.isAlive()) {
                    //开始更新进度线程
                    mUpdateThread = new Thread(mUpdateTask);
                    stopThread = false;
                    mUpdateThread.start();
                }
                mp.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
                    @Override
                    public void onSeekComplete(MediaPlayer mediaPlayer) {
                        voiceRTPPacket.setMediaextractorTime(Long.valueOf(mediaPlayer.getCurrentPosition() + "000"));
                        Log.e(TAG, "onSeekComplete: " );
                    }
                });
                mVideoView.start();
                mVideoView.startAnimation(inAnima);
                mPlayBtn.setImageResource(R.drawable.pause_icon);
            }
        });
        //视频播放完成监听器
        mVideoView.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                Log.e("setOnCompletionListener", "sadf");
                mPlayBtn.setImageResource(R.drawable.play_icon);
                mVideoView.seekTo(0);
                mPlayProgressBar.setProgress(0);
                setPlayTime(0);
                stopThread = true;
                sendHideControlPanelMessage();
            }
        });

        mView.setOnClickListener(this);
        Log.e(TAG, "init: "+ "初始化控件" );
    }

    //初始化视频，设置视频时间和进度条最大值
    private void initVideo(){
        Log.e(TAG, "initVideo: " + "初始化播放器" );
        //初始化时间和进度条
        mVideoDuration = mVideoView.getDuration();//毫秒数
        int seconds = mVideoDuration/1000;
        mPlayTime.setText("00:00/"+
                ((seconds/60>9)?(seconds/60):("0"+seconds/60))+":"+
                ((seconds%60>9)?(seconds%60):("0"+seconds%60)));
        mPlayProgressBar.setMax(mVideoDuration);
        mPlayProgressBar.setProgress(0);
        //更新进度条和时间任务
        mUpdateTask = new Runnable(){
            @Override
            public void run(){
                while(!stopThread){
                    mCurrentProgress = mVideoView.getCurrentPosition();
                    handler.sendEmptyMessage(UPDATE_PROGRESS);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
//        mBigPlayBtn.setOnClickListener(this);
        mPlayBtn.setOnClickListener(this);
        mFullScreenBtn.setOnClickListener(this);
        //进度条进度改变监听器
        mPlayProgressBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                handler.sendEmptyMessageDelayed(EXIT_CONTROL_PANEL, 3000);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeMessages(EXIT_CONTROL_PANEL);
            }
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress,
                                          boolean fromUser) {
                if (fromUser){
                    LogUtil.i("onProgressChanged", "SeekBar  "+progress);
                    mVideoView.seekTo(progress);//设置视频
                    setPlayTime(progress);//设置时间
//                    voiceRTPPacket.setMediaextractorTime(Long.valueOf(progress + "000"));
                    LogUtil.i("onProgressChanged", "MediaextractorTime  "+voiceRTPPacket.getMediaextractorTime());
                    timeChanged = true;
                }
            }
        });
        mWidth = this.getWidth();
        mHeigth = this.getHeight();
    }

    @Override
    public void onClick(View v) {
        if(v == mView){
//            if(mBigPlayBtn.getVisibility() == View.VISIBLE){
//                return;
//            }
            if(mControlPanel.getVisibility() == View.VISIBLE){
                //执行退出动画
                mControlPanel.startAnimation(outAnima);
                mControlPanel.setVisibility(View.GONE);
            }else {
                //执行进入动画
                mControlPanel.startAnimation(inAnima);
                mControlPanel.setVisibility(View.VISIBLE);
                sendHideControlPanelMessage();
            }
        }
//        else if(v.getId() == R.id.big_play_button){//大的播放按钮
////            mBigPlayBtn.setVisibility(View.GONE);
//            mVideoView.setBackground(null);
//            if(!mVideoView.isPlaying()){
//                mVideoView.start();
//                mPlayBtn.setImageResource(R.drawable.pause_icon);
//                //开始更新进度线程
//                mUpdateThread = new Thread(mUpdateTask);
//                stopThread = false;
//                mUpdateThread.start();
//            }
//        }
        else if(v.getId() == R.id.play_button){//播放/暂停按钮
            if(mVideoView.isPlaying()){
                mVideoView.pause();
                mPlayBtn.setImageResource(R.drawable.play_icon);
            }else{
                mVideoView.setBackground(null);
                if(mUpdateThread == null || !mUpdateThread.isAlive()){
                    //开始更新进度线程
                    mUpdateThread = new Thread(mUpdateTask);
                    stopThread = false;
                    mUpdateThread.start();
                }
                mVideoView.start();
                mPlayBtn.setImageResource(R.drawable.pause_icon);
            }
            sendHideControlPanelMessage();
        }
        else if(v.getId() == R.id.full_screen_button){//全屏
            if(context.getResources().getConfiguration().orientation
                    == Configuration.ORIENTATION_PORTRAIT){
                setFullScreen();
            }else{
                setNoFullScreen();
            }
            sendHideControlPanelMessage();
        }
    }

    //设置当前时间
    private void setPlayTime(int millisSecond){
        int currentSecond = millisSecond/1000;
        String currentTime = ((currentSecond/60>9)?(currentSecond/60+""):("0"+currentSecond/60))+":"+
                ((currentSecond%60>9)?(currentSecond%60+""):("0"+currentSecond%60));
        StringBuilder text = new StringBuilder(mPlayTime.getText().toString());
        text.replace(0,  text.indexOf("/"), currentTime);
        mPlayTime.setText(text);
    }
    //设置控件的宽高
    private void setSize(){
        ViewGroup.LayoutParams lp = this.getLayoutParams();
        if(mIsFullScreen){
            lp.width = screenSize.y;
            lp.height = screenSize.x;
        }else{
            lp.width = mWidth;
            lp.height = mHeigth;
        }
        this.setLayoutParams(lp);
    }

    //两秒后隐藏控制面板
    private void sendHideControlPanelMessage(){
        handler.removeMessages(EXIT_CONTROL_PANEL);
        handler.sendEmptyMessageDelayed(EXIT_CONTROL_PANEL, 3000);
    }

    //设置视频路径
    public void setVideoUri(String paht){
        this.path = paht;
        mVideoView.setVideoURI(Uri.parse(paht));
    }
    //获取视频路径
    public String getVideoPath(){
        return path;
    }

    //设置视频初始画面
    public void setInitPicture(Drawable d){
        mVideoView.setBackground(d);
    }
    //挂起视频
    public void suspend(){
        if(mVideoView != null){
            mVideoView.suspend();
        }
    }

    //继续视频
    public void resume(){
        if(mVideoView != null){
            mVideoView.resume();
        }
    }
    //设置视频进度
    public void setVideoProgress(int millisSecond,boolean isPlaying){
        mVideoView.setBackground(null);
//        mBigPlayBtn.setVisibility(View.GONE);
        mPlayProgressBar.setProgress(millisSecond);
        setPlayTime(millisSecond);
        if(mUpdateThread == null || !mUpdateThread.isAlive()){
            mUpdateThread = new Thread(mUpdateTask);
            stopThread = false;
            mUpdateThread.start();
        }
        mVideoView.seekTo(millisSecond);
        if(isPlaying){
            mVideoView.start();
            mPlayBtn.setImageResource(R.drawable.pause_icon);
        }else{
            mVideoView.pause();
            mPlayBtn.setImageResource(R.drawable.play_icon);
        }
    }
    //获取视频进度
    public int getVideoProgress(){
        return mVideoView.getCurrentPosition();
    }
    //判断视频是否正在播放
    public boolean isPlaying(){
        return mVideoView.isPlaying();
    }
    //判断是否为全屏状态
    public boolean isFullScreen(){
        return mIsFullScreen;
    }
    //设置竖屏
    public void setNoFullScreen(){
//        this.mIsFullScreen = false;
//        Activity ac = (Activity)context;
//        ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
//        ac.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        setSize();
//        voiceRTPPacket.cleanPcmData();
        Activity ac = (Activity)context;
        ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        ac.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setSize();
    }
    //设置横屏
    public void setFullScreen(){
        this.mIsFullScreen = true;
        Activity ac = (Activity)context;
        ac.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        ac.getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setSize();
    }

    public void stopPlayer(){
        stopThread = true;
    }
}
