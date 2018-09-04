package com.example.screeningapplication.add_play_video;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.screeningapplication.R;

import java.util.List;

/**
 * Created by tpf on 2018-08-24.
 */

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder>{
    private static final String TAG = "VideoAdapter";
    private List<Video> videoList;
    private onAddPicClickListener mOnAddPicClickListener;

    public interface onAddPicClickListener{
        void onAddPicOrPlayClick(int position);
    }

    public VideoAdapter(List<Video> videoList, onAddPicClickListener mOnAddPicClickListener) {
        this.videoList = videoList;
        this.mOnAddPicClickListener = mOnAddPicClickListener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private View videoView;
        private ImageView imageView;
        private TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            videoView = itemView;
            imageView = (ImageView) itemView.findViewById(R.id.video_image_view);
            textView = (TextView) itemView.findViewById(R.id.video_text_view);
        }
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.video_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mOnAddPicClickListener.onAddPicOrPlayClick(holder.getAdapterPosition());
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Video video = videoList.get(position);
        holder.imageView.setImageBitmap(video.getImage());
        holder.textView.setText(video.getVideoName());
    }

    @Override
    public int getItemCount() {
        if (videoList != null){
            return videoList.size();
        }
        return 0;
    }

}
