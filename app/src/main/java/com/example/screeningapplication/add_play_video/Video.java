package com.example.screeningapplication.add_play_video;

import android.graphics.Bitmap;

/**
 * Created by tpf on 2018-08-24.
 */

public class Video {
    private String videoPath;
    private Bitmap image;
    private String videoName;
    private boolean isAddBtn;

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public boolean isAddBtn() {
        return isAddBtn;
    }

    public void setAddBtn(boolean addBtn) {
        isAddBtn = addBtn;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }
}
