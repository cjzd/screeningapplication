package com.example.screeningapplication.add_play_video;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.util.Log;
import android.widget.Toast;
import android.widget.VideoView;

import com.example.screeningapplication.R;

import java.util.ArrayList;

public class VideosActivity extends AppCompatActivity {
    private static final String TAG = "VideosActivity";
    private String path = null;
    private VideoView mVideoView = null;
    private static ArrayList<Video> videoList = new ArrayList<>();
    private RecyclerView mRecyclerView;
    private VideoAdapter adapter;
    private String srcPath = Environment.getExternalStorageDirectory() + "/movie3.mp4";

    private VideoAdapter.onAddPicClickListener onAddPicClickListener = new VideoAdapter.onAddPicClickListener() {
        @Override
        public void onAddPicOrPlayClick(int position) {
            if (videoList.get(position).isAddBtn()){
                chooseVideo();
            }else {
                playVideo(videoList.get(position).getVideoPath());
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_videos);

        Log.e(TAG, "onCreate: " );
        mRecyclerView = (RecyclerView) findViewById(R.id.video_recycler_view);
        if (videoList.size() == 0){
            Video video = new Video();
            video.setVideoPath(null);
            video.setImage(BitmapFactory.decodeResource(getResources(), R.drawable.add));
            video.setVideoName("");
            video.setAddBtn(true);
            videoList.add(video);
        }
        adapter = new VideoAdapter(videoList, onAddPicClickListener);
        GridLayoutManager gridLayoutManager = new
                GridLayoutManager(this, 3, StaggeredGridLayoutManager.VERTICAL, false);
        mRecyclerView.setLayoutManager(gridLayoutManager);
        mRecyclerView.setAdapter(adapter);

    }

    private void chooseVideo() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Video.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, 1);
    }

    private void handleVideo(Intent data) {
        path = null;
        Uri uri = data.getData();
        String[] filePathColumn = {MediaStore.Video.Media.DATA};

        Cursor cursor = getContentResolver().query(uri,
                filePathColumn, null, null, null);
        cursor.moveToFirst();

        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        path = cursor.getString(columnIndex);
        if (path != null){
            Video video = new Video();
            video.setVideoPath(path);
            String[] strings = path.split("/");
            video.setVideoName(strings[strings.length - 1]);
            MediaMetadataRetriever media = new MediaMetadataRetriever();
            Log.e(TAG, "handleVideo: " + path );
            try {
                media.setDataSource(path);
            }catch (Exception e){
                Toast.makeText(VideosActivity.this, "选择的视频无效。", Toast.LENGTH_SHORT).show();
                return;
            }
            Bitmap bitmap = media.getFrameAtTime();
            if (bitmap != null){
                video.setImage(scaleBitmap(bitmap));
                video.setAddBtn(false);
                if (isValid(video)){
                    videoList.add(videoList.size(), video);
                    sort(videoList);
                    adapter.notifyDataSetChanged();
                }
            }
        }else {
            Toast.makeText(VideosActivity.this, "Failed to get video.", Toast.LENGTH_SHORT).show();
        }
        cursor.close();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode){
            case 1:
                if (resultCode == RESULT_OK){
                    Log.e(TAG, "handleVideo: " );
                    handleVideo(data);
                }
                break;
            default:
                break;
        }
    }

    //按比例缩放图片
    private Bitmap scaleBitmap(Bitmap bitmap){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int newWidth=150;
        int newHeight=150;
        //计算压缩的比率
        float scaleWidth=((float)newWidth)/width;
        float scaleHeight=((float)newHeight)/height;
        //获取想要缩放的matrix
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth,scaleHeight);
        bitmap= Bitmap.createBitmap(bitmap,0,0,width,height,matrix,true);
        return bitmap;
    }


    private void playVideo(String videoPath) {
        PlayerActivity.actionStart(this, videoPath);
    }

    private void sort(ArrayList<Video> list){
        int len = list.size();
        Video video = list.get(len - 1);
        list.set(len - 1, list.get(len - 2));
        list.set(len - 2, video);
    }

    private boolean isValid(Video video){
        for (Video mVideo : videoList){
            if (mVideo.getVideoName().equals(video.getVideoName())){
                return false;
            }
        }
        return true;
    }

    public static void actionStart(Context context){
        Intent intent = new Intent(context, VideosActivity.class);
        context.startActivity(intent);
        Log.e(TAG, "actionStart: 打开videos" );
    }
}
