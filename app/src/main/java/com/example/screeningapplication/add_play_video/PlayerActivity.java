package com.example.screeningapplication.add_play_video;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.screeningapplication.R;
import com.example.screeningapplication.video_audio_transport.VoiceRTPPacket;

public class PlayerActivity extends AppCompatActivity {
    private static final String TAG = "PlayerActivity2";
    private String path = null;
    private SimpleVideoView mVideoView = null;
    public static int playTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        Intent intent = getIntent();
        path = intent.getStringExtra("path");
        Log.i(TAG, "onCreate: " + path);

        mVideoView = (SimpleVideoView) findViewById(R.id.video);
        mVideoView.setVideoUri(path);
        //videoview加载需要一段时间，用视频的第一帧图片覆盖这段黑屏时间
//        MediaMetadataRetriever media = new MediaMetadataRetriever();
//        media.setDataSource(path);
//        Bitmap bitmap = media.getFrameAtTime();
//
//        mVideoView.setInitPicture(new BitmapDrawable(getResources(), bitmap));
    }

    public static void actionStart(Context context, String path){
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("path", path);
        context.startActivity(intent);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        Log.e(TAG, "onRestoreInstanceState: " );
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.e(TAG, "onSaveInstanceState: " );
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.e(TAG, "onPause: " );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VoiceRTPPacket.isRun = false;
        Log.e(TAG, "onDestroy: " );
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "onStop: " );
        mVideoView.stopPlayer();
    }
}
