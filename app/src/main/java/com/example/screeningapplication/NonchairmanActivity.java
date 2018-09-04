package com.example.screeningapplication;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.screeningapplication.add_play_video.PlayerActivity;
import com.example.screeningapplication.add_play_video.VideosActivity;
import com.example.screeningapplication.video_audio_transport.ScreenRecord;
import com.example.screeningapplication.video_audio_transport.TCPSocket;
import com.example.screeningapplication.video_audio_transport.UdpSocket;

public class NonchairmanActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "NonchairmanActivity";
    private LocalReceiver localReceiver;
    private IntentFilter intentFilter;
    private SocketService.SendMsgBinder mBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBinder = (SocketService.SendMsgBinder) iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };
    private Button mBtn;
    private boolean isCast;
    private static final int REQUEST_CODE_A = 2018;
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecord mScreenRecord;
    private MediaProjection mediaProjection;
    public static UdpSocket udpSocketA;
    public TCPSocket tcpSocketV;
    public static TCPSocket tcpSocketA;
    private boolean roalChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nonchairman);

        //绑定服务
        Intent bindIntent = new Intent(this, SocketService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);

        //初始化控件
        mBtn = (Button) findViewById(R.id.non_stop_btn);
        mBtn.setOnClickListener(this);

        //获取外部文件的读权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }

        //注册本地广播
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.screeningapplication.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver,intentFilter);

        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.non_stop_btn:
                if (isCast){
                    mBinder.sendStop();
                }else{
                    finish();
                }
                break;
        }
    }

    /*
    * 本地广播接收器，
    * 接收用户信息ArrayList
    * */
    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MyApplication.getContext(), "主席收到广播了", Toast.LENGTH_SHORT).show();
            int category = intent.getIntExtra("category", -1);
            String userId;
            int status;
            switch (category){
                //开始投屏
                case Category.BEGIN_CAST:
                    Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                    startActivityForResult(captureIntent, REQUEST_CODE_A);
                    tcpSocketA = new TCPSocket(6664); //音频端口,2018年8月20日09:47:47
                    mBtn.setBackground(getDrawable(R.drawable.redbutton));
                    mBtn.setText("退出投屏");
                    isCast = true;
                    break;
                //停止投屏
                case Category.STOP_CAST:
                    mScreenRecord.release();
                    tcpSocketV.stop();
                    tcpSocketA.stop();
                    mBtn.setBackground(getDrawable(R.drawable.button));
                    mBtn.setText("断开连接");
                    isCast = false;
                    break;
                //角色转移
                case Category.ROLE_TRANSFER:
                    roalChange = true;
                    ChairmanActivity.actionStart(NonchairmanActivity.this);
                    break;
                default:
                    break;
            }
        }
    }

    /*
    * 录屏权限请求结果处理，
    * 同意录屏之后，初始化录屏工作，TCP打开6665视频端口
    * */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            mediaProjection = mMediaProjectionManager.getMediaProjection(resultCode, data);
            if(mediaProjection == null){
                Toast.makeText(this,"程序发生错误:MediaProjection@1",Toast.LENGTH_SHORT).show();
                return;
            }
            //发送视频
            mScreenRecord = new ScreenRecord(this, mediaProjection);
            tcpSocketV = new TCPSocket(mScreenRecord, 6665); //2018年8月20日09:48:04
            //跳转到视频播放界面
            VideosActivity.actionStart(this);
        }
        catch (Exception e){

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch(requestCode){
            case 1:
                if (grantResults.length != 0 || permissions[0] != Manifest.permission.READ_EXTERNAL_STORAGE){
                    Toast.makeText(this, "You had refused READ_EXTERNAL_STORAGE permission ", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }

    // Activity启动函数
    public static void startActivity(Context context){
        Intent intent = new Intent(context, NonchairmanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LogUtil.i("onDestroy", "非主席活动停止了");
        if (!roalChange){
            mBinder.stopSocket();
        }
        unbindService(connection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
    }
}
