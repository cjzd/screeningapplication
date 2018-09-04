package com.example.screeningapplication;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.annotation.MainThread;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.Timer;
import java.util.TimerTask;


public class LoginActivity extends AppCompatActivity implements View.OnClickListener{
    private static final String TAG = "LoginActivity";

    private View root;
    private LinearLayout loginBg;
    private ToggleButton togglePwd;
    private EditText usernameText;
    private EditText passwordText;
    private ProgressDialog dialog;
    private SharedPreferences preferences;
    private String userName = "";//用户名
    private String meetingRoom;//wifi密码
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
    private IntentFilter intentFilter;
    private LocalReceiver localReceiver;
    private int chairManFlag = 2;//0=非主席，1=主席
    private int sameNameFlag = 2;//0=不重名，1=重名
    private int rootBottom = Integer.MIN_VALUE;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

//        LogUtil.level = 6;

        root = (View) findViewById(R.id.root);
        loginBg = (LinearLayout) findViewById(R.id.login_bg);
        togglePwd = (ToggleButton) findViewById(R.id.togglePwd);
        passwordText = (EditText) findViewById(R.id.password_edittext);
        usernameText = (EditText) findViewById(R.id.username_edittext);
        Button login = (Button) findViewById(R.id.login_btn);
        login.setOnClickListener(LoginActivity.this);
        dialog = new ProgressDialog(this);
        dialog.setTitle("登录");
        dialog.setMessage("正在登录...");
        dialog.setCanceledOnTouchOutside(false);

        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.screeningapplication.LOCAL_BROADCAST");
        localReceiver = new LocalReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(localReceiver,intentFilter);

        //
        preferences = getSharedPreferences("data", MODE_PRIVATE);
        passwordText.setText(preferences.getString("meetingRoom", ""));
        usernameText.setText(preferences.getString("userName", ""));

        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                root.getGlobalVisibleRect(r);
                // 进入Activity时会布局，第一次调用onGlobalLayout。先记录開始软键盘没有弹出时底部的位置
                if (rootBottom == Integer.MIN_VALUE) {
                    rootBottom = r.bottom;
                    return;
                }
                if (r.bottom < rootBottom) {
                    loginBg.setBackground(getDrawable(R.drawable.bg02));
                }else {
                    loginBg.setBackground(getDrawable(R.drawable.bg01));
                }
            }
        });

        togglePwd.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //如果选中，显示密码
                    passwordText.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                    togglePwd.setBackground(getDrawable(R.drawable.eye01));
                } else {
                    //否则隐藏密码
                    passwordText.setTransformationMethod(PasswordTransformationMethod.getInstance());
                    togglePwd.setBackground(getDrawable(R.drawable.eye02));
                }
            }
        });
    }



    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.login_btn:
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        dialog.dismiss();
                        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this);
                        alertDialog.setTitle("连接失败");
                        alertDialog.setMessage("输入的会议号可能出错了");
                        alertDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {

                            }
                        });
                        alertDialog.setCancelable(false);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alertDialog.show();
                            }
                        });
                       unbindService(connection);
                    }
                }, 4000);
                userName = usernameText.getText().toString();
                meetingRoom = passwordText.getText().toString();
                if (!checkUserName()){
                    Toast.makeText(this, "投屏：请输入用户名", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!checkUseMeetingRoom()){
                    Toast.makeText(this, "投屏：请输入正确的会议号", Toast.LENGTH_SHORT).show();
                    return;
                }
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("meetingRoom", meetingRoom);
                editor.putString("userName", userName);
                editor.apply();

                Intent bindIntent = new Intent(LoginActivity.this, SocketService.class);
                bindService(bindIntent, connection, BIND_AUTO_CREATE);
                bindIntent.putExtra("username", userName);
                startService(bindIntent);

                //判断软键盘是否显示
                if (isSoftShown()){
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.toggleSoftInput(0, InputMethodManager.HIDE_NOT_ALWAYS);
                }
                dialog.show();
                break;
            default:
                break;
        }
    }

    /*
    * 检查用户名是否已经输入
    * */
    private boolean checkUserName(){
        if (userName.isEmpty()){
            return false;
        }
        return true;
    }

    /*
    * 检查会议号是否符合规范
    * */
    private boolean checkUseMeetingRoom(){
        if(meetingRoom.length() != 4){
            return false;
        }
        return true;
    }

    /*
    * 本地广播接收器，
    * 接收用户信息ArrayList
    * */
    class LocalReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
//            Toast.makeText(MyApplication.getContext(), "收到广播了", Toast.LENGTH_SHORT).show();
            int category = intent.getIntExtra("category", -1);
            switch (category){
                //主席信号
                case Category.CHAIRMAN:
                    chairManFlag = 1;
                    if (sameNameFlag == 0){ //是主席，不重名，Activity跳转
                        LogUtil.i("LocalReceiver", "是主席，不重名，Activity跳转");
                        dialog.dismiss();
                        timer.cancel();
                        ChairmanActivity.actionStart(LoginActivity.this);
                        unbindService(connection);
                        LoginActivity.this.finish();

                    }else if(sameNameFlag == 1){ //是主席，重名了，Activity不跳转
                        Toast.makeText(LoginActivity.this, "用户名已经存在，请更换用户名", Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;
                //非主席信号
                case Category.NON_CHAIRMAN:
                    chairManFlag = 0;
                    if (sameNameFlag == 0){ //非主席，不重名，Activity跳转
                        LogUtil.i("LocalReceiver", "非主席，不重名，Activity跳转");
                        dialog.dismiss();
                        timer.cancel();
                        NonchairmanActivity.startActivity(LoginActivity.this);
                        unbindService(connection);
                        LoginActivity.this.finish();

                    }else if(sameNameFlag == 1){ //非主席，重名了，Activity不跳转
                        Toast.makeText(LoginActivity.this, "用户名已经存在，请更换用户名", Toast.LENGTH_SHORT)
                                .show();
                    }
                    break;
                //重名信号
                case Category.SAME_NAME:
                    sameNameFlag = 0;
                    Toast.makeText(LoginActivity.this, "用户名已经存在，请更换用户名", Toast.LENGTH_SHORT)
                            .show();
                    break;
                //不重名信号
                case Category.DIFFERENT_NAME:
                    sameNameFlag = 1;
                    if (chairManFlag == 0){ //不重名，非主席，Activity跳转
                        LogUtil.i("LocalReceiver", "不重名，非主席，Activity跳转");
                        dialog.dismiss();
                        timer.cancel();
                        NonchairmanActivity.startActivity(LoginActivity.this);
                        unbindService(connection);
                        LoginActivity.this.finish();

                    }else if(chairManFlag == 1){ //不重名，是主席，Activity跳转
                        LogUtil.i("LocalReceiver", "不重名，是主席，Activity跳转");
                        dialog.dismiss();
                        timer.cancel();
                        ChairmanActivity.actionStart(LoginActivity.this);
                        unbindService(connection);
//                        LoginActivity.this.finish();
                    }
                    break;
                case Category.FAILED_TO_CONNECT:
                    LogUtil.i("LocalReceiver", "socket连接失败");
                    dialog.dismiss();
                    timer.cancel();
                    //连接失败，弹出提示框
                    final AlertDialog.Builder alertDialog = new AlertDialog.Builder(LoginActivity.this);
                    alertDialog.setTitle("连接失败");
                    alertDialog.setMessage("输入的会议号可能出错了");
                    alertDialog.setPositiveButton("确定", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            //没操作
                        }
                    });
                    alertDialog.setCancelable(false);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            alertDialog.show();
                        }
                    });
                    break;
                default:
                    break;
            }
        }
    }


    private boolean isSoftShown(){
        //获取当屏幕内容的高度
        int screenHeight = this.getWindow().getDecorView().getHeight();
        //获取View可见区域的bottom
        Rect rect = new Rect();
        //DecorView即为activity的顶级view
        this.getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        //考虑到虚拟导航栏的情况（虚拟导航栏情况下：screenHeight = rect.bottom + 虚拟导航栏高度）
        //选取screenHeight*2/3进行判断
        return screenHeight*2/3 > rect.bottom;
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        LogUtil.i("onSaveInstanceState", "");
        Log.i(TAG, "onSaveInstanceState: " );
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localReceiver);
        super.onDestroy();
        LogUtil.i("onDestroy","登录界面被销毁了");
    }
}
