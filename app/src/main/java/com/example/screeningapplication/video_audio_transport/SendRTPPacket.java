package com.example.screeningapplication.video_audio_transport;

import java.io.IOException;

/**
 * Created by tpf on 2018-07-17.
 */

public class SendRTPPacket {
    private static final String TAG = "SendRTPPacket";
    private byte[] sendbuf=new byte[1500];
    private int packageSize=1400;
    private  int seq_num = 0;
    private int timestamp_increse=(int)(90000.0/35);//framerate是帧率
    private int ts_current=0;
    private int bytes=0;
    public static UdpSocket udpSocket = null;

    public SendRTPPacket(){
    }

    public void sendRTPData(byte[] data, int h264len)throws IOException {
        memset(sendbuf, 0, 1500);
        sendbuf[1] = (byte) (sendbuf[1] | 96); // 负载类型号96,其值为：01100000
        sendbuf[0] = (byte) (sendbuf[0] | 0x80); // 版本号,此版本固定为2
        sendbuf[1] = (byte) (sendbuf[1] & 254); //标志位，由具体协议规定其值，其值为：01100000
        sendbuf[11] = 10;//随机指定10，并在本RTP回话中全局唯一,java默认采用网络字节序号 不用转换（同源标识符的最后一个字节）
        if (h264len <= packageSize) {
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
            // FU-A HEADER, 并将这个HEADER填入sendbuf[12]
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) (data[0] & 0x80)) << 7);
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((data[0] & 0x60) >> 5)) << 5);
            sendbuf[12] = (byte) (sendbuf[12] | ((byte) (data[0] & 0x1f)));
            // 同理将sendbuf[13]赋给nalu_payload
            //NALU头已经写到sendbuf[12]中，接下来则存放的是NAL的第一个字节之后的数据。所以从r的第二个字节开始复制
            System.arraycopy(data, 1, sendbuf, 13,  h264len - 1);
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
            bytes = h264len + 12;//获sendbuf的长度,为nalu的长度(包含nalu头但取出起始前缀,加上rtp_header固定长度12个字节)
            udpSocket.datagramSend(sendbuf, bytes);
//            Log.i(TAG, "sendRTPData: " + 1);

        } else if (h264len > packageSize) {
            int k = 0, l = 0;
            k = h264len / packageSize;
            l = h264len % packageSize;
            int t = 0;
            ts_current = ts_current + timestamp_increse;
            System.arraycopy(intToByte(ts_current), 0, sendbuf, 4, 4);//时间戳，并且倒序
            {
                byte temp = 0;
                temp = sendbuf[4];
                sendbuf[4] = sendbuf[7];
                sendbuf[7] = temp;
                temp = sendbuf[5];
                sendbuf[5] = sendbuf[6];
                sendbuf[6] = temp;
            }
            while (t <= k) {
                System.arraycopy(intToByte(seq_num++), 0, sendbuf, 2, 2);//序列号，并且倒序
                {
                    byte temp = 0;
                    temp = sendbuf[3];
                    sendbuf[3] = sendbuf[2];
                    sendbuf[2] = temp;
                }
                if (t == 0) {//分包的第一片
                    sendbuf[1] = (byte) (sendbuf[1] & 0x7F);//其值为：01100000，不是最后一片，M位（第一位）设为0
                    //FU indicator，一个字节，紧接在RTP header之后，包括F,NRI，header
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (data[0] & 0x80)) << 7);//禁止位，为0
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((data[0] & 0x60) >> 5)) << 5);//NRI，表示包的重要性
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));//TYPE，表示此FU-A包为什么类型，一般此处为28
                    //FU header，一个字节，S,E，R，TYPE
                    sendbuf[13] = (byte) (sendbuf[13] & 0xBF);//E=0，表示是否为最后一个包，是则为1
                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF);//R=0，保留位，必须设置为0
                    sendbuf[13] = (byte) (sendbuf[13] | 0x80);//S=1，表示是否为第一个包，是则为1
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (data[0] & 0x1f)));//TYPE，即NALU头对应的TYPE
                    //将除去NALU头剩下的NALU数据写入sendbuf的第14个字节之后。前14个字节包括：12字节的RTP Header，FU indicator，FU header
                    System.arraycopy(data, 1, sendbuf, 14, packageSize );
                    bytes = 14 + packageSize ;
                    udpSocket.datagramSend(sendbuf, bytes);
//                    Log.i(TAG, "sendRTPData: " + t);
                    t++;
                } else if (t == k) {//分片的最后一片
                    sendbuf[1] = (byte) (sendbuf[1] | 0x80);

                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (data[0] & 0x80)) << 7);
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((data[0] & 0x60) >> 5)) << 5);
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0，保留位必须设为0
                    sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0，不是第一个包
                    sendbuf[13] = (byte) (sendbuf[13] | 0x40); //E=1，是最后一个包
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (data[0] & 0x1f)));//NALU头对应的type

                    if (0 != l) {//如果不能整除，则有剩下的包，执行此代码。如果包大小恰好是1400的倍数，不执行此代码。
                        System.arraycopy(data, t * packageSize + 1, sendbuf, 14, l - 1);//l-1，不包含NALU头
                        bytes = l - 1 + 14; //bytes=l-1+14;
                        udpSocket.datagramSend(sendbuf, bytes);
//                        Log.i(TAG, "sendRTPData: " + t);
                    }//pl
                    t++;
                } else if (t < k && 0 != t) {//既不是第一片，又不是最后一片的包
                    sendbuf[1] = (byte) (sendbuf[1] & 0x7F); //M=0，其值为：01100000，不是最后一片，M位（第一位）设为0.
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) (data[0] & 0x80)) << 7);
                    sendbuf[12] = (byte) (sendbuf[12] | ((byte) ((data[0] & 0x60) >> 5)) << 5);
                    sendbuf[12] = (byte) (sendbuf[12] | (byte) (28));

                    sendbuf[13] = (byte) (sendbuf[13] & 0xDF); //R=0，保留位必须设为0
                    sendbuf[13] = (byte) (sendbuf[13] & 0x7F); //S=0，不是第一个包
                    sendbuf[13] = (byte) (sendbuf[13] & 0xBF); //E=0，不是最后一个包
                    sendbuf[13] = (byte) (sendbuf[13] | ((byte) (data[0] & 0x1f)));//NALU头对应的type
                    if (l == 0 && t == k -1){
                        System.arraycopy(data, t * packageSize + 1, sendbuf, 14, packageSize - 1);//不包含NALU头
                    }else {
                        System.arraycopy(data, t * packageSize + 1, sendbuf, 14, packageSize);//不包含NALU头
                    }
                    bytes = 14 + packageSize;
                    udpSocket.datagramSend(sendbuf, bytes);
//                    Log.i(TAG, "sendRTPData: " + t);
                    t++;
                }
            }
        }
//        Log.i(TAG, "sendRTPData: finished" );
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
}
