package com.example.screeningapplication;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class SocketService extends Service {

    private static final String TAG = "SocketService";
    private SendMsgBinder mBinder = new SendMsgBinder();
    private Socket mSocket;
    private String ipAddr = "192.168.43.1";
    private int port = 8888;
    private OutputStream os;
    private InputStream is;
    private String username;
    private ArrayList<UserInfo> usersInfoArrayList;
    private LocalBroadcastManager localBroadcastManager =
            LocalBroadcastManager.getInstance(MyApplication.getContext());
    private Timer timer;

    public SocketService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        LogUtil.e(TAG, "onCreate");
        usersInfoArrayList = new ArrayList<UserInfo>();
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        LogUtil.i(TAG, "onBind: ");
        return mBinder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        LogUtil.i(TAG, "onStartCommand: ");
        if (intent != null){
            username = intent.getStringExtra("username");
            LogUtil.e("onStartCommand", username);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mSocket = new Socket(ipAddr, port);
                        if (mSocket.isConnected()){
                            os = mSocket.getOutputStream();
                            is = mSocket.getInputStream();
                            String chairRequest = "ChairRequest\0";
                            os.write(chairRequest.getBytes());
                            os.flush();

                            String data = "##" + username + "\0";
                            os = mSocket.getOutputStream();
                            os.write(data.getBytes("GBK"));
                            os.flush();
                            reciveMsg();
                            stopSelf();
                        }else {
                            LogUtil.e(TAG, "onCreate: mSocket failed");
                            stopSelf();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        LogUtil.e("mSocket","socket连接失败");
                        mSocket = null;
                        Intent intent = new Intent("com.example.screeningapplication.LOCAL_BROADCAST");
                        intent.putExtra("category", Category.FAILED_TO_CONNECT);
                        localBroadcastManager.sendBroadcast(intent);
                    }
                }
            }).start();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        LogUtil.i(TAG, "onDestroy: ");
        super.onDestroy();
        try {
            if (mSocket != null){
                mSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        LogUtil.i(TAG, "onUnbind: ");
        return super.onUnbind(intent);
    }

    class SendMsgBinder extends Binder{
        public ArrayList<UserInfo> getUsersInfo(){
            return usersInfoArrayList;
        }

        /*
        * 告诉服务端，非主席为name的要投屏了
        * */
        public void sendHandover1(String name){
            try {
                LogUtil.i("sendHandover1", name);
                os = mSocket.getOutputStream();
                String data = "Handover1#" + name + "\0";
                os.write(data.getBytes("GBK"));
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

       /*
       * 自己要投屏了
       * */
        public void sendHandover2(){
            try {
                os = mSocket.getOutputStream();
                String data = "Handover2#" + username + "\0";
                os.write(data.getBytes("GBK"));
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
        * 发送停止投屏指令
        * */
        public void sendStop(){
            try {
                os = mSocket.getOutputStream();
                String data = "stop";
                os.write(data.getBytes());
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /*
        * 要进行角色转移了
        * */
        public void sendTransfer(String name){
            try {
                os = mSocket.getOutputStream();
                String data = "Transfer#" + name + "\0";
                os.write(data.getBytes("GBK"));
                os.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void stopSocket(){
            if (mSocket != null){
                try {
                     mSocket.shutdownOutput();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void reciveMsg(){
        while (mSocket.isConnected()) {
            byte[] recBuf = new byte[1024];
            try {
                int len = is.read(recBuf);
                Log.i(TAG, "接收到数据的长度: len is " + len);
                if (len >= 0){
                    String data = new String(recBuf, 0, len, "GBK");
                    handleMsg(data);
                }else{
                    LogUtil.e("mSocket", "socket断开连接了");
                    break;
                }
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    private void handleMsg(String msg){
        char[] cMsg = msg.toCharArray();
        int command1 = cMsg[0] - '0';
        LogUtil.i(TAG, "handleMsg: " + msg);
        Intent intent = new Intent("com.example.screeningapplication.LOCAL_BROADCAST");
        switch (command1){
            case 0:
                break;
            case 1:
                //新加一个用户
                if (cMsg[1] == '#'){
                    String[] sMsg = msg.split("#");
                    UserInfo uInfo = new UserInfo();
                    uInfo.setStatus(Integer.valueOf(sMsg[1]));
                    uInfo.setSocketId(sMsg[2]);
                    uInfo.setIp(sMsg[3]);
                    uInfo.setName(sMsg[4].replaceAll("\0", ""));
                    if (uInfo.getName().equals(username)){
                        usersInfoArrayList.add(0,uInfo);
                    }else{
                        usersInfoArrayList.add(uInfo);
                    }
                    //发送广播,category=1,通过Bundle发送数组对象
                    intent.putExtra("category", Category.USERS_INFO);
                    intent.putExtra("userInfo", uInfo);
                    localBroadcastManager.sendBroadcast(intent);

                }else if (cMsg[1] == '0'){  //你成为了主席
                    intent.putExtra("category", Category.ROLE_TRANSFER);
                    localBroadcastManager.sendBroadcast(intent);
                    break;
                } else if (cMsg[1] == '1'){   //你是主席，将所有在线用户信息都发给你
                    String[] sMsg = msg.split("#");
                    int i =1;
                    while (i < sMsg.length - 1){
                        UserInfo uInfo = new UserInfo();
                        uInfo.setStatus(Integer.valueOf(sMsg[i++]));
                        uInfo.setSocketId(sMsg[i++]);
                        uInfo.setIp(sMsg[i++]);
                        uInfo.setName(sMsg[i++]);
                        usersInfoArrayList.add(uInfo);
                    }
                    for (UserInfo usersInfo:usersInfoArrayList
                         ) {
                        LogUtil.i("用户信息", usersInfo.getStatus() + "  "+ usersInfo.getSocketId()
                                + "  "+ usersInfo.getIp() + "  "+ usersInfo.getName());
                    }

                } else if (cMsg[1] == '4'){
                    LogUtil.i(TAG, "handleMsg: " + msg);
                    if (cMsg[3] == '0'){
                        intent.putExtra("category", Category.DIFFERENT_NAME);
                        localBroadcastManager.sendBroadcast(intent);
                    }else if(cMsg[3] == '1'){
                        intent.putExtra("category", Category.SAME_NAME);
                        localBroadcastManager.sendBroadcast(intent);
                    }
                }
                break;
            //你是主席
            case 2:
                LogUtil.i(TAG, "handleMsg: " + msg);
                intent.putExtra("category", Category.CHAIRMAN);
                localBroadcastManager.sendBroadcast(intent);
                break;
            //你是非主席
            case 3:
                LogUtil.i(TAG, "handleMsg: " + msg);
                intent.putExtra("category", Category.NON_CHAIRMAN);
                localBroadcastManager.sendBroadcast(intent);
                break;
            //有个用户下线了
            case 4:
                LogUtil.i(TAG, "handleMsg: " + msg);
                String socketId = msg.split("#")[1];
                intent.putExtra("category", Category.GET_OFF);
                intent.putExtra("userId", socketId);
                localBroadcastManager.sendBroadcast(intent);
                break;
            //有个用户的状态要改变了
            case 5:
                LogUtil.i(TAG, "handleMsg: " + msg);
                intent.putExtra("userId", msg.split("#")[1]);
                intent.putExtra("status", Integer.valueOf(msg.split("#")[2]));
                if (Integer.valueOf(msg.split("#")[2]) == 0){
                    intent.putExtra("category", Category.STOP_CAST);
                }else{
                    intent.putExtra("category", Category.BEGIN_CAST);
                }
                localBroadcastManager.sendBroadcast(intent);
                break;
            case 6:
                mBinder.sendHandover2();
                break;
            default:
                LogUtil.i(TAG, "handleMsg: " + msg);
                break;
        }
        //收到心跳了，给回一个
        if (msg.contains("Heart1")){
            if (timer != null){
                timer.cancel();
                timer = null;
            }
            timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Intent intent = new Intent("com.example.screeningapplication.LOCAL_BROADCAST");
                    intent.putExtra("category", Category.FAILED_TO_CONNECT);
                    localBroadcastManager.sendBroadcast(intent);
                }
            }, 7000);
            sendHeart1();
        }
    }

    public void sendHeart1(){
        try {
            os = mSocket.getOutputStream();
            String data = "Heart1\0";
            os.write(data.getBytes());
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
