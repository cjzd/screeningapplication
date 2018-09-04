package com.example.screeningapplication.video_audio_transport;

import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;
import android.widget.VideoView;

import com.example.screeningapplication.LogUtil;
import com.example.screeningapplication.add_play_video.SimpleVideoView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;

/**
 * Created by tpf on 2018-07-24.
 */

public class VoiceRTPPacket {
    private static final String TAG = "VoiceRTPPacket";
    private String path = null;
    public static boolean isRun = false;
    private ArrayList<byte[]> pcmDataContainer = null;
    public static MediaExtractor mediaextractor = null; //2018年8月20日22:24:42
    private MediaCodec mediaDecodec = null;
    private MediaCodec mediaEncodec = null;
    private AudioTrack mAudioTrack = null;
    private UdpSocket udpSocket = null;
    private byte[] sendbuf=new byte[1500];
    private static int seq_num = 0;
    private int timestamp_increse=(int)(86);//framerate是帧率
    private static int ts_current=0;
    private int bytes=0;
    private long currentTimeU;
    private VideoView customVideoView;

    public VoiceRTPPacket(String path, UdpSocket udpSocketA, VideoView customVideoView){
        udpSocket = udpSocketA;
        this.customVideoView = customVideoView;
        this.path = path;
        pcmDataContainer = new ArrayList<byte[]>();
        initMediaDecode();
        initMediaEncodec();
        isRun = true;
//        initAudioTrack();
        new Thread(new Runnable() {
            @Override
            public void run() {
                srcAudioFormatToPCM();
            }
        }).start();
        new Thread(new Runnable() {
            @Override
            public void run() {
                aacAudioFromPcm();
            }
        }).start();
    }


    /*初始化解码器*/
    public void initMediaDecode(){
        mediaextractor = new MediaExtractor();//此类可分离视频文件的音轨和视频轨道
        try {
            mediaextractor.setDataSource(path);
            for (int i = 0; i < mediaextractor.getTrackCount(); i++){
                MediaFormat format = mediaextractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                Log.i(TAG, "initMediaDecode: " + mime);
                if (mime.startsWith("audio")){//获取音频轨道
                    mediaextractor.selectTrack(i);//选择此音频轨道
                    mediaDecodec = MediaCodec.createDecoderByType(mime);//创建Decode解码器
                    mediaDecodec.configure(format, null, null, 0);
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mediaDecodec == null){
            Log.e(TAG, "create mediadecodec failed" );
            return;
        }
        mediaDecodec.start();
    }

    /*初始化编码器*/
    public void initMediaEncodec(){
        MediaFormat format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 48000, 2);
        format.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectERLC);
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);
        try {
            mediaEncodec = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
            mediaEncodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mediaEncodec == null){
            Log.e(TAG, "create mediaencodec falied " );
            return;
        }
        mediaEncodec.start();
    }

    /**
     * 解码{@link #path}音频文件 得到PCM数据块
     * @return 是否解码完所有数据
     */
    public void srcAudioFormatToPCM(){
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        while (isRun){
            if (mediaDecodec == null && customVideoView == null){
                break;
            }
            try {
                currentTimeU = customVideoView.getCurrentPosition();
            }catch (Exception e){
                Log.e(TAG, "srcAudioFormatToPCM: customVideoView可能消失了" );
            }
//            Log.i(TAG, "srcAudioFormatToPCM 声音取样时间1: " + mediaextractor.getSampleTime());
//            Log.i(TAG, "srcAudioFormatToPCM 播放器当前时间1: " + customVideoView.getCurrentPosition());
//            LogUtil.i("要发送音频数据了  并没有发", mediaextractor.getSampleTime() + "");

            while (mediaextractor.getSampleTime() < currentTimeU * 1000){
                Log.e(TAG, "srcAudioFormatToPCM: " + mediaextractor.getSampleTime() + "   "
                + currentTimeU + "000");
                LogUtil.i("要发送音频数据了", mediaextractor.getSampleTime() + "");
                if (SimpleVideoView.timeChanged){
                    SimpleVideoView.timeChanged = false;
                    cleanPcmData();
                    Log.e(TAG, "srcAudioFormatToPCM:   应该退出循环了" );
                    break;
                }
                if (mediaextractor.getSampleTime() == -1){
                    break;
                }
                //mediaextractor.getSampleTime() == -1 的时候，会卡死在这里
                int inputIndex = mediaDecodec.dequeueInputBuffer(-1);
                if (inputIndex < 0){
                    LogUtil.i("srcAudioFormatToPCM", "here  " + inputIndex);
                    break;
                }
                ByteBuffer inputBuffer = mediaDecodec.getInputBuffer(inputIndex);
                inputBuffer.clear();
//                Log.i(TAG, "srcAudioFormatToPCM 声音取样时间2: " + mediaextractor.getSampleTime());
//                Log.i(TAG, "srcAudioFormatToPCM 播放器当前时间2: " + customVideoView.getCurrentPosition());
                int sampleSize = mediaextractor.readSampleData(inputBuffer, 0); //MediaExtractor读取数据到inputBuffer中
                if (sampleSize > 0 ){
                    mediaDecodec.queueInputBuffer(inputIndex, 0, sampleSize, 0, 0) ;
                    mediaextractor.advance();
                }
                int outputIndex = mediaDecodec.dequeueOutputBuffer(mBufferInfo, 10000);
                ByteBuffer outputBuffer;
                byte[] pcmData;
                while(outputIndex > 0){
                    LogUtil.i("要发送音频数据了  还在发送", mediaextractor.getSampleTime() + "");
                    if (isRun == false){
                        return;
                    }
                    outputBuffer = mediaDecodec.getOutputBuffer(outputIndex);
                    pcmData = new byte[mBufferInfo.size];
                    outputBuffer.get(pcmData);
                    outputBuffer.clear();
                    putPcmData(pcmData);//存放pcm数据包
                    //play
//                mAudioTrack.write(pcmData, 0, mBufferInfo.size);
//                Log.e(TAG, "srcAudioFormatToPCM: " + mBufferInfo.size);
                    mediaDecodec.releaseOutputBuffer(outputIndex, false);
                    outputIndex = mediaDecodec.dequeueOutputBuffer(mBufferInfo, 10000);
                }
            }
        }
        Log.e(TAG, "srcAudioFormatToPCM: 结束了" );
    }

    /**
     * 将pcm编码成aac
     */
    public void aacAudioFromPcm(){
        MediaCodec.BufferInfo mBufferInfo = new MediaCodec.BufferInfo();
        while (isRun){
            if (mediaEncodec == null || mediaDecodec == null){
                Log.e(TAG, "mediacodec is null");
                break;
            }
            int inputIndex = mediaEncodec.dequeueInputBuffer(-1);
            if (inputIndex < 0){
                isRun = false;
                break;
            }
            ByteBuffer inputBuffer = mediaEncodec.getInputBuffer(inputIndex);
            inputBuffer.clear();
            byte[] pcmdata;
            while(true){
                if ((pcmdata = getPcmData()) != null){
                    inputBuffer.put(pcmdata);
                    break;
                }
                if (!isRun){
                    break;
                }
            }
            if (!isRun){
                break;
            }
            mediaEncodec.queueInputBuffer(inputIndex, 0, pcmdata.length, 0, 0);
            int outputIndex = mediaEncodec.dequeueOutputBuffer(mBufferInfo, 10000);
            while(outputIndex >= 0){
                if (isRun == false){
                    break;
                }
                int outBitsize = mBufferInfo.size;
                ByteBuffer outputBuffer = mediaEncodec.getOutputBuffer(outputIndex);
                byte[] aacData = new byte[outBitsize];
                outputBuffer.get(aacData, 0, outBitsize);
                try {
                    sendRTPData(aacData, outBitsize);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mediaEncodec.releaseOutputBuffer(outputIndex, false);
                outputIndex = mediaEncodec.dequeueOutputBuffer(mBufferInfo, 10000);
            }
        }
        Log.e(TAG, "aacAudioFromPcm: 结束了" );
    }

    //给aac添加rtp报头和AU_HEADER、AU_HEADER_LENGTH
    public void sendRTPData(byte[] data, int len)throws IOException {
//        Log.i(TAG, "sendRTPData ts_current: " + ts_current);
        memset(sendbuf, 0, 1500);
        sendbuf[1] = (byte) (sendbuf[1] | 96); // 负载类型号96,其值为：01100000
        sendbuf[0] = (byte) (sendbuf[0] | 0x80); // 版本号,此版本固定为2
        sendbuf[1] = (byte) (sendbuf[1] & 254); //标志位，由具体协议规定其值，其值为：01100000
        sendbuf[11] = 10;//随机指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换（同源标识符的最后一个字节）
        sendbuf[1] = (byte) (sendbuf[1] | 0x80); // 设置rtp M位为1，其值为：11100000，分包的最后一片，M位（第一位）为0，后7位是十进制的96，表示负载类型
        System.arraycopy(intToByte(seq_num++), 0, sendbuf, 2, 2);//send[2]和send[3]为序列号，共两位
        {
            // java默认的网络字节序是大端字节序（无论在什么平台上），因为windows为小字节序，所以必须倒序
            /**参考：
             * http://blog.csdn.net/u011068702/article/details/51857557
             * http://cpjsjxy.iteye.com/blog/1591261
             */
            byte temp = 0;
            temp = sendbuf[3];
            sendbuf[3] = sendbuf[2];
            sendbuf[2] = temp;
        }
        //AU_HEADER_LENGTH
        sendbuf[14] = (byte)((len & 0xff) >> 5);
        sendbuf[15] = (byte)((len & 0xff) << 3);
        //AU_HEADER
        sendbuf[12] = 0x0;
        sendbuf[13] = 0x10;
        // 同理将sendbuf[17]赋给nalu_payload
        //NALU头已经写到sendbuf[12]中，接下来则存放的是NAL的第一个字节之后的数据。所以从r的第二个字节开始复制
        System.arraycopy(data, 0, sendbuf, 16,  len);
        ts_current = ts_current + timestamp_increse;
        System.arraycopy(intToByte(ts_current), 0, sendbuf, 4, 4);//序列号接下来是时间戳，4个字节，存储后也需要倒序
        {
            byte temp = 0;
            temp = sendbuf[4];
            sendbuf[4] = sendbuf[7];
            sendbuf[7] = temp;
            temp = sendbuf[5];
            sendbuf[5] = sendbuf[6];
            sendbuf[6] = temp;
        }
        bytes = len + 16;//获sendbuf的长度,为nalu的长度(包含nalu头但取出起始前缀,加上rtp_header固定长度12个字节)
        udpSocket.datagramSend(sendbuf, bytes);
//        Log.i(TAG, "sendRTPData: " + 1);
    }

//    public void initAudioTrack(){
//        int mMinBufferSize = AudioTrack.getMinBufferSize(48000,
//                AudioFormat.CHANNEL_CONFIGURATION_STEREO ,
//                AudioFormat.ENCODING_PCM_16BIT);
//        mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//                48000,
//                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
//                AudioFormat.ENCODING_PCM_16BIT,
//                mMinBufferSize,
//                AudioTrack.MODE_STREAM);
//        if (mAudioTrack == null){
//            Log.e(TAG, "create audioTrack failed " );
//        }
//        mAudioTrack.play();
//    }
//
//    public void release(){
//        if (mediaDecodec != null){
//            mediaDecodec.stop();
//            mediaDecodec.release();
//            mediaDecodec = null;
//        }
//        if (mediaEncodec != null){
//            mediaEncodec.stop();
//            mediaEncodec.release();
//            mediaEncodec = null;
//        }
//        if (mediaextractor != null){
//            mediaextractor.release();
//            mediaextractor = null;
//        }
//        if (mAudioTrack != null){
//            mAudioTrack.stop();
//            mAudioTrack.release();
//        }
//        Log.e(TAG, "audiotrack had stop" );
//    }



    /**
     * 添加ADTS头
     */
//    private void addADTStoPacket(byte[] packet, int packetLen) {
//        int profile = 2; // AAC LC
//        int freqIdx = 3; // 48KHz
//        int chanCfg = 2; // CPE
//
//        // fill in ADTS data
//        packet[0] = (byte) 0xFF;
//        packet[1] = (byte) 0xF9;
//        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
//        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
//        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
//        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
//        packet[6] = (byte) 0xFC;
//    }

    public void putPcmData(byte[] data){
        synchronized (VoiceRTPPacket.class){
            pcmDataContainer.add(data);
        }
    }

    public byte[] getPcmData(){
        synchronized (VoiceRTPPacket.class){
            if (pcmDataContainer.isEmpty()){
                return null;
            }else {
                byte[] pcmData = pcmDataContainer.get(0);
                pcmDataContainer.remove(0);
//                Log.e(TAG, "aacAudioFromPcm: " + pcmDataContainer.size() );
                return pcmData;
            }
        }
    }

    public void cleanPcmData(){
        pcmDataContainer.clear();
        LogUtil.i("cleanPcmData", "pcm音频数据队列清空了");
    }

    // 清空buf的值
    public void memset(byte[] buf, int value, int size) {
        for (int i = 0; i < size; i++) {
            buf[i] = (byte) value;
        }
    }

    //返回的是4个字节的数组。
    public byte[] intToByte(int number) {
        int temp = number;
        byte[] b = new byte[4];
        for (int i = 0; i < b.length; i++) {
            b[i] = new Integer(temp & 0xff).byteValue();
            temp = temp >> 8;
        }
        return b;
    }

    public void setMediaextractorTime(long currentTimeU){
        mediaextractor.seekTo(currentTimeU, MediaExtractor.SEEK_TO_CLOSEST_SYNC);
    }

    public long getMediaextractorTime(){
        return mediaextractor.getSampleTime();
    }
}
