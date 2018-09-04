package com.example.screeningapplication;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.screeningapplication.add_play_video.VideosActivity;
import com.example.screeningapplication.video_audio_transport.ScreenRecord;
import com.example.screeningapplication.video_audio_transport.TCPSocket;
import com.example.screeningapplication.video_audio_transport.UdpSocket;

import java.util.ArrayList;
import java.util.List;

public class ChairmanActivity extends AppCompatActivity implements View.OnClickListener{

    private static final String TAG = "ChairmanActivity";
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private List<UserInfo> usersInfoArrayList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private Button mBtn;
    private ProgressDialog dialog;
    private UserInfoAdapter mAdapter;
    private SocketService.SendMsgBinder mBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBinder = (SocketService.SendMsgBinder) iBinder;
            LogUtil.i("onServiceConnected", "chairman");
            //绑定服务之后，获得用户信息数组
            usersInfoArrayList = mBinder.getUsersInfo();
            //遍历当前用户，如果用正在投屏的，就改变主席的状态
            for (UserInfo userInfo : usersInfoArrayList){
                if (userInfo.getStatus() == 1){
                    isCast = true;
                    mBtn.setBackground(getDrawable(R.drawable.redbutton));
                    mBtn.setText("退出投屏");
                    break;
                }
            }
            //创建一个UserInfoAdapter的实例，将用户信息数组传进去
            mAdapter = new UserInfoAdapter(usersInfoArrayList);
            //绑定RecyclerView
            mRecyclerView.setAdapter(mAdapter);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtil.i("onServiceDisconnected", "解除了主席界面的服务绑定");
        }
    };
    private boolean isCast;
    private static final int REQUEST_CODE_A = 2018;
    private MediaProjectionManager mMediaProjectionManager;
    private ScreenRecord mScreenRecord;
    private MediaProjection mediaProjection;
    public static UdpSocket udpSocketA;
    public TCPSocket tcpSocketV;
    public static TCPSocket tcpSocketA;
    public static boolean roalChange;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chairman);

        //绑定服务
        Intent bindIntent = new Intent(ChairmanActivity.this, SocketService.class);
        bindService(bindIntent, connection, BIND_AUTO_CREATE);

        //初始化控件
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        mRecyclerView.setLayoutManager(layoutManager);
        mBtn = (Button) findViewById(R.id.add_btn);
        mBtn.setOnClickListener(this);

        //注册本地广播
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.screeningapplication.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver,intentFilter);

        //获取外部文件的读权限
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]
                    {Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
        }
        mMediaProjectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);

        dialog = new ProgressDialog(this);
        dialog.setTitle("投屏");
        dialog.setCanceledOnTouchOutside(false);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.add_btn:
                LogUtil.i("isCast", isCast + "");
                if (isCast){
                    LogUtil.i("投屏/停止", "停止");
                    mBinder.sendStop();
                    mBtn.setBackgroundColor(Color.parseColor("#d6d7d7"));
                    mBtn.setText("开始投屏");
                    isCast = false;
                }else{
                    toCast();
                }
                break;
            default:
                break;
        }
    }

    public void toCast(){
        LogUtil.i("投屏/停止", "投屏");
        UserInfo userInfo = null;
        for (UserInfo uInfo : usersInfoArrayList){
            if (uInfo.isFlag()){
                userInfo = uInfo;
                break;
            }
        }
        if (userInfo != null){
            if (userInfo.equals(usersInfoArrayList.get(0))){
                mBinder.sendHandover2();
            }else{
                mBinder.sendHandover1(userInfo.getName());
            }
            dialog.setMessage(userInfo.getName() + "  准备投屏...");
            dialog.show();
            mAdapter.notifyDataSetChanged();
            isCast = true;
        }else{
            Toast.makeText(this, "先选择要投屏的用户", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    * 本地广播接收器，
    * 接收用户信息ArrayList
    * */
    class LocalReceiver extends BroadcastReceiver{

        @Override
        public void onReceive(Context context, Intent intent) {
            Toast.makeText(MyApplication.getContext(), "主席收到广播了", Toast.LENGTH_SHORT).show();
            int category = intent.getIntExtra("category", -1);
            String userId;
            int status;
            switch (category){
                //ArrayList
                case Category.USERS_INFO:
                    UserInfo userInfo = (UserInfo) intent.getSerializableExtra("userInfo");
                    mAdapter.notifyItemInserted(usersInfoArrayList.size() - 1);
                    break;
                //开始投屏
                case Category.BEGIN_CAST:
                    isCast = true;
                    userId = intent.getStringExtra("userId");
                    status = intent.getIntExtra("status", 0);
                    for (int i = 0; i < usersInfoArrayList.size(); i++) {
                        if (usersInfoArrayList.get(i).getSocketId().equals(userId)){
                            if (i == 0){
                                Intent captureIntent = mMediaProjectionManager.createScreenCaptureIntent();
                                startActivityForResult(captureIntent, REQUEST_CODE_A);
                                tcpSocketA = new TCPSocket(6664); //音频端口,2018年8月20日09:47:47
                            }
                            usersInfoArrayList.get(i).setStatus(status);
                            mAdapter.notifyDataSetChanged();
                            break;
                        }
                    }
                    dialog.dismiss();
                    mBtn.setBackground(getDrawable(R.drawable.redbutton));
                    mBtn.setText("退出投屏");
                    break;
                //退出投屏
                case Category.STOP_CAST:
                    isCast = false;
                    userId = intent.getStringExtra("userId");
                    status = intent.getIntExtra("status", 0);
                    for (int i = 0; i < usersInfoArrayList.size(); i++) {
                        if (usersInfoArrayList.get(i).getSocketId().equals(userId)){
                            usersInfoArrayList.get(i).setStatus(status);
                            mAdapter.notifyDataSetChanged();
                            if (i == 0){
                                mScreenRecord.release();
                                tcpSocketV.stop();
                                tcpSocketA.stop();
                            }
                            break;
                        }
                    }
                    mBtn.setBackground(getDrawable(R.drawable.button));
                    mBtn.setText("开始投屏");
                    break;
                //用户下线
                case Category.GET_OFF:
                    isCast = false;
                    userId = intent.getStringExtra("userId");
                    for (int i = 0; i < usersInfoArrayList.size(); i++) {
                        if (usersInfoArrayList.get(i).getSocketId().equals(userId)){
                            mAdapter.notifyItemRemoved(i);
                            usersInfoArrayList.remove(i);
                            break;
                        }
                    }
                    mBtn.setBackground(getDrawable(R.drawable.button));
                    mBtn.setText("开始投屏");
                    break;
                //角色转移
                case Category.ROLE_TRANSFER:
                    break;
                default:
                    break;
            }


        }
    }

    // Activity启动函数
    public static void actionStart(Context context){
        Intent intent = new Intent(context, ChairmanActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
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
//            PlayerActivity2.actionStart(this, srcPath);
            Log.e(TAG, "onActivityResult: " );
            VideosActivity.actionStart(ChairmanActivity.this);

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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (!roalChange){
            Log.e(TAG, "onDestroy: no role change" );
            if (mBinder != null){
                mBinder.stopSocket();
            }
        }
        unbindService(connection);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        Log.e(TAG, "onDestroy: chairman"  );
        usersInfoArrayList.clear();
//        android.os.Process.killProcess(android.os.Process.myPid());
    }
}
