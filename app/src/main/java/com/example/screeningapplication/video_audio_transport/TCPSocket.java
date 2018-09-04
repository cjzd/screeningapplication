package com.example.screeningapplication.video_audio_transport;

import android.util.Log;

import com.example.screeningapplication.ChairmanActivity;
import com.example.screeningapplication.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Created by tpf on 2018-07-17.
 */

public class TCPSocket {
    private static final String TAG = "TCPSocket";
    private Socket mSocket = null;
    private String ipAddr = "192.168.43.1";
    private String ipAddrAndPortForVideo = "rtsp://192.168.43.1:6665";
    private String ipAddrAndPortForAudeo = "rtsp://192.168.43.1:6664";
    private int PORT ;
    private String udpPort;
    private OutputStream outputStream = null;
    private InputStream inputStream = null;
    private String session = null;
    private ScreenRecord mScreenRecord = null;
    private String srcPath = null;

    /*
    * 创建视频端口
    * */
    public TCPSocket(ScreenRecord mScreenRecord, int port){
        PORT = port;
        this.mScreenRecord = mScreenRecord;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e(TAG, "run1: ");
                    mSocket = new Socket(ipAddr, PORT);
                    Log.e(TAG, "video: "+PORT);
                    if (mSocket.isConnected()){
                        outputStream = mSocket.getOutputStream();
                        inputStream = mSocket.getInputStream();
                        sendFirstOptionV();
                        recieveV();
                        LogUtil.e("recieveV", "关闭了视频通信socket");
                    }else {
                        Log.e(TAG, "failed to new a mSocket.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /*
    * 创建音频端口
    * */
    public TCPSocket(int port){
        PORT = port;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Log.e(TAG, "run1: ");
                    mSocket = new Socket(ipAddr, PORT);
                    Log.e(TAG, "audeo: "+PORT);
                    if (mSocket.isConnected()){
                        outputStream = mSocket.getOutputStream();
                        inputStream = mSocket.getInputStream();
                        sendFirstOptionA();
                        recieveA();
                        LogUtil.e("recieveA", "关闭了音频通信socket");
                    }else {
                        Log.e(TAG, "failed to new a mSocket.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


    private void recieveV() {
        while (mSocket.isConnected()){
            byte[] recBuf = new byte[1024];
            try {
                int len = inputStream.read(recBuf);
                if (len > 0){
                    String data = new String(recBuf, 0, len, "UTF-8");
                    Log.i(TAG, "recieve: " + data);
                    if (data.indexOf("CSeq: 1") != -1){
                        sendSecondOptionV();
                    }else if (data.indexOf("CSeq: 2") != -1){
                        sendThirdOptionV();
                    }else if (data.indexOf("CSeq: 3") != -1){
                        String[] strs = data.split(":");
                        session = strs[strs.length - 1];
                        Log.i(TAG, "recieveVVVV:" + session);
                        udpPort = getUdpPort(data);
                        Log.e(TAG, "recieveV: " + udpPort );
                        sendFourthOptionV();
                    }else if (data.indexOf("CSeq: 4") != -1){
                        if (SendRTPPacket.udpSocket == null){
                            SendRTPPacket.udpSocket = new UdpSocket(Integer.valueOf(udpPort));
                        }
                        mScreenRecord.start();
                    }
                }else {
                    break;
                }
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    private void recieveA() {
        while (mSocket.isConnected()){
            byte[] recBuf = new byte[1024];
            try {
                int len = inputStream.read(recBuf);
                if (len > 0){
                    String data = new String(recBuf, 0, len, "UTF-8");
                    Log.i(TAG, "recieve: " + data);
                    if (data.indexOf("CSeq: 1") != -1){
                        sendSecondOptionA();
                    }else if (data.indexOf("CSeq: 2") != -1){
                        sendThirdOptionA();
                    }else if (data.indexOf("CSeq: 3") != -1){
                        String[] strs = data.split(":");
                        session = strs[strs.length - 1];
                        Log.i(TAG, "recieveAAAA:" + session);
                        udpPort = getUdpPort(data);
                        Log.e(TAG, "recieveA: " + udpPort );
                        sendFourthOptionA();
                    }else if (data.indexOf("CSeq: 4") != -1){
                        if (ChairmanActivity.udpSocketA == null){
                            ChairmanActivity.udpSocketA = new UdpSocket(Integer.valueOf(udpPort));
                        }
                        Log.e(TAG, "SendRTPPacket: " + "建立了5002udp端口" );
//                        new VoiceRTPPacket(srcPath);
//                        sendTeardownA();
                    }
                }else {
                    break;
                }
            } catch (IOException e) {
//                e.printStackTrace();
            }
        }
    }

    private void sendFirstOptionV() {
        String data = "OPTIONS " + ipAddrAndPortForVideo + " RTSP/1.0\r\n"
                + "CSeq: 1\r\n"
                + "User-Agent: Lavf57.77.100\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendFirstOptionA() {
        String data = "OPTIONS " + ipAddrAndPortForAudeo + " RTSP/1.0\r\n"
                + "CSeq: 1\r\n"
                + "User-Agent: Lavf57.77.100\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendSecondOptionV() {
        String session = "v=0\r\n"
                + "o=- 0 0 IN IP4 127.0.0.1\r\n"
                + "s=No Name\r\n"
                + "c=IN Ip4 "
                + ipAddr + "\r\n"
                + "t=0 0\r\n"
                + "a=tool:libavformat 57.77.100\r\n"
                + "m=video 0 RTP/AVP 96\r\n"
                + "a=rtpmap:96 H264/90000\r\n"
                + "a=control:streamid=0\r\n";
        String data = "ANNOUNCE " + ipAddrAndPortForVideo + " RTSP/1.0\r\n"
                + "Content-Type: application/sdp\r\n"
                + "CSeq: 2\r\n"
                + "User-Agent: Lavf57.77.100\r\n"
                + "Content-length: 296\r\n"
                + "Content-length: " + session.length() + "\r\n\r\n"
                + session;
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendSecondOptionA() {
        String session = "v=0\r\n"
                + "o=- 0 0 IN IP4 127.0.0.1\r\n"
                + "s=No Name\r\n"
                + "c=IN Ip4 "
                + ipAddr + "\r\n"
                + "t=0 0\r\n"
                + "a=tool:libavformat 57.77.100\r\n"
                + "m=audio 0 RTP/AVP 96\r\n"
                + "b=AS:128\r\n"
                + "a=rtpmap:96 MPEG4-GENERIC/44100/2\r\n"
                + "a=fmtp:96 profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3; config=1210\r\n"
                + "a=control:streamid=0\r\n";
        String data = "ANNOUNCE " + ipAddrAndPortForAudeo + " RTSP/1.0\r\n"
                + "Content-Type: application/sdp\r\n"
                + "CSeq: 2\r\n"
                + "User-Agent: Lavf57.77.100\r\n"
//                + "Content-length: 296\r\n"
                + "Content-length: " + session.length() + "\r\n\r\n"
                + session;
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendThirdOptionV() {
        String data = "SETUP " + ipAddrAndPortForVideo + "/streamid=0 RTSP/1.0\r\n"
                + "Transport: RTP/AVP/UDP;unicast;client_port=24922-24923;mode=record\r\n"
                + "CSeq: 3\r\n"
                + "User-Agent: Lavf57.77.100\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendThirdOptionA() {
        String data = "SETUP " + ipAddrAndPortForAudeo + "/streamid=0 RTSP/1.0\r\n"
                + "Transport: RTP/AVP/UDP;unicast;client_port=24922-24923;mode=record\r\n"
                + "CSeq: 3\r\n"
                + "User-Agent: Lavf57.77.100\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendFourthOptionV() {
        String data = "RECORD " + ipAddrAndPortForVideo + " RTSP/1.0\r\n"
                + "Range: npt=0.000-\r\n"
                + "CSeq: 4\r\n"
                + "User-Agent: Lavf57.77.100\r\n"
                + "Session:" + session + "\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendFourthOptionA() {
        String data = "RECORD " + ipAddrAndPortForAudeo + " RTSP/1.0\r\n"
                + "Range: npt=0.000-\r\n"
                + "CSeq: 4\r\n"
                + "User-Agent: Lavf57.77.100\r\n"
                + "Session:" + session + "\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendTeardownV(){
        String data = "TEARDOWN " + ipAddrAndPortForVideo + "RTSP/1.0\r\n"
                + "CSeq: 5\r\n"
                + "Session: " + session + "\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private void sendTeardownA(){
        String data = "TEARDOWN " + ipAddrAndPortForAudeo + "RTSP/1.0\r\n"
                + "CSeq: 5\r\n"
                + "Session: " + session + "\r\n\r\n";
        try {
            outputStream.write(data.getBytes());
            outputStream.flush();
            Log.i(TAG,"发送成功");
        } catch (IOException e) {
            e.printStackTrace();
            Log.i(TAG,"发送失败");
        }
    }

    private String getUdpPort(String data){
        String st = data.split("-")[1];
        return st.substring(18,22);
    }

    public void stop() {
        if (mSocket != null){
            try {
                mSocket.shutdownOutput();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
