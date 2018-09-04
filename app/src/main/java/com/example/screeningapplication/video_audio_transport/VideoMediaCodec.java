package com.example.screeningapplication.video_audio_transport;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import com.example.screeningapplication.LogUtil;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by tpf on 2018/7/17.
 */

public class VideoMediaCodec {

    private final static String TAG = "VideoMediaCodec";

    private boolean isRun = false;
    private Surface mSurface;
    private int TIMEOUT_USEC = 10000;

    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test1.h264";
    private BufferedOutputStream outputStream;
    private FileOutputStream outStream;
    private MediaCodec mEncoder = null;

    /**
     *构造函数
     * **/
    public VideoMediaCodec(){
        prepare();
    }

    public Surface getSurface(){
        return mSurface;
    }

    public void isRun(boolean isR){
        this.isRun = isR;
    }

    public void prepare(){
        try{
            MediaFormat format = MediaFormat.createVideoFormat(Constant.MIME_TYPE, Constant.VIDEO_WIDTH, Constant.VIDEO_HEIGHT);
//            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
//                    MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_COLOR_FORMAT,MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
            format.setInteger(MediaFormat.KEY_BIT_RATE, Constant.VIDEO_BITRATE);
            format.setInteger(MediaFormat.KEY_FRAME_RATE, Constant.VIDEO_FRAMERATE);
//            format.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR);
            format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, Constant.VIDEO_IFRAME_INTER);
            mEncoder = MediaCodec.createEncoderByType(Constant.MIME_TYPE);
            mEncoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            mSurface = mEncoder.createInputSurface();
            mEncoder.start();
        }catch (IOException e){

        }
    }

    public void release() {
        this.isRun = false;
    }
    /**
     * 获取h264数据
     * **/
    public void getBuffer(){
        SendRTPPacket sendRTPPacket = new SendRTPPacket();
        try
        {
            MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
            while(isRun){
                if(mEncoder == null){
                    Log.e(TAG, "getBuffer: mEncoder is null" );
                    break;
                }

                int outputBufferIndex  = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
//                Log.i(TAG, "outputBufferIndex: " + outputBufferIndex);
                while (outputBufferIndex >= 0){
//                    Log.e(TAG, "getBuffer: " + "来了一帧");
//                    Log.i(TAG, "outputBufferIndex: " + outputBufferIndex);
                    ByteBuffer outputBuffer = mEncoder.getOutputBuffer(outputBufferIndex);
                    byte[] outData = new byte[mBufferInfo.size];
//                    Log.i(TAG, "bufferSize: " + mBufferInfo.size);
                    outputBuffer.get(outData);
                    sendRTPPacket.sendRTPData(outData, outData.length);
//                    if(mBufferInfo.flags == 2){
//                        configbyte = new byte[mBufferInfo.size];
//                        configbyte = outData;
//                      udpSocket.datagramSend(outData);
//                      out.write(outData);
//                    }
//
//                    else if(mBufferInfo.flags == 1){
//                        byte[] keyframe = new byte[mBufferInfo.size + configbyte.length];
//                        System.arraycopy(configbyte, 0, keyframe, 0, configbyte.length);
//                        System.arraycopy(outData, 0, keyframe, configbyte.length, outData.length);
//                        //MainActivity.putData(keyframe,1,mBufferInfo.presentationTimeUs*1000L);
//                        udpSocket.datagramSend(keyframe);
//                        out.write(outData);
//                    }else{
//                        udpSocket.datagramSend(outData);
//                        out.write(outData);
//                       // MainActivity.putData(outData,2,mBufferInfo.presentationTimeUs*1000L);
//                    }
                    mEncoder.releaseOutputBuffer(outputBufferIndex, false);
                    outputBufferIndex = mEncoder.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
                }
            }
        }
        catch (Exception e){

        }finally { //2018年8月20日09:48:16
            LogUtil.e("getBuffer", "投屏结束了，停止了录屏");
//            SendRTPPacket.udpSocket.mSocket.disconnect();
            mEncoder.stop();
            mEncoder.release();
            mEncoder = null;
        }
    }
}
