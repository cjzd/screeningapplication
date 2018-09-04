package com.example.screeningapplication;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * Created by tpf on 2018-08-16.
 */

public class UserInfo implements Serializable{
    private String socketId;
    private String name;
    private String ip;
    private int status;
    private boolean flag;

    public UserInfo() {
    }
    public UserInfo(String socketId, String name, String ip, int status){
        this.socketId = socketId;
        this.name = name;
        this.ip = ip;
        this.status = status;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSocketId() {
        return socketId;
    }

    public void setSocketId(String socketId) {
        this.socketId = socketId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }
}
