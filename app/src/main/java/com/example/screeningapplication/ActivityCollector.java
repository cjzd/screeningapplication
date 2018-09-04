package com.example.screeningapplication;

import android.app.Activity;
import android.content.Context;

import java.util.ArrayList;

/**
 * Created by tpf on 2018-08-19.
 */

public class ActivityCollector {
    public static ArrayList<Activity> activities = new ArrayList<>();

    public static void addActivity(Activity activity){
        activities.add(activity);
    }

    public static void removeActivity(Activity activity){
        activities.remove(activity);
    }

    public static void finishAll(){
        for (Activity activity : activities){
            if (!activity.isFinishing()){
                activity.finish();
            }
        }
    }

}
