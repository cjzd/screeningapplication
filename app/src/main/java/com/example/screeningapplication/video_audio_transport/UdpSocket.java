package com.example.screeningapplication.video_audio_transport;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpSocket {
    private static final String TAG = "UdpSocket";
    public DatagramSocket mSocket;
    public  InetAddress serverAddr;
    private int PORT ;

    public UdpSocket(int port){
        try {
            PORT = port;
            mSocket = new DatagramSocket(PORT);
            serverAddr = InetAddress.getByName("192.168.43.1");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void datagramSend(byte[] packet, int lenght){
        DatagramPacket dPacket;
        try {
//            Log.i(TAG, "datagramSend" + PORT + ": " + lenght);
            dPacket = new DatagramPacket(packet, lenght,
                    serverAddr, PORT);
            mSocket.send(dPacket);
//            Log.e(TAG, "datagramSend: OK");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
