package com.example.screeningapplication;

/**
 * Created by tpf on 2018-08-17.
 */

public class Category {
    public static final int CHAIRMAN = 0; //是主席
    public static final int NON_CHAIRMAN = 1; //是非主席
    public static final int SAME_NAME = 2; //用户名重复
    public static final int DIFFERENT_NAME = 3; //用户名不重复
    public static final int USERS_INFO = 4; //用户信息
    public static final int BEGIN_CAST = 5; //开始投屏
    public static final int STOP_CAST = 6; //停止投屏
    public static final int GET_OFF = 7; //用户下线
    public static final int ROLE_TRANSFER = 8; //角色转移
    public static final int FAILED_TO_CONNECT = 9; //socket连接失败

}
