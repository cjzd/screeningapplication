package com.example.screeningapplication;

import android.app.Application;
import android.content.Context;
import android.util.Log;

/**
 * Created by tpf on 2018-08-16.
 */

public class MyApplication extends Application{
    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
    }

    public static Context getContext(){
        return context;
    }
}
