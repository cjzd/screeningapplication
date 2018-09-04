package com.example.screeningapplication;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.IBinder;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Created by tpf on 2018-08-17.
 */

public class UserInfoAdapter extends RecyclerView.Adapter<UserInfoAdapter.ViewHolder>{
    private List<UserInfo> mUserInfoList;
    private String oldName;
    private SocketService.SendMsgBinder mBinder;
    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mBinder = (SocketService.SendMsgBinder) iBinder;
            LogUtil.i("onServiceConnected", "userinfoadapter");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtil.i("onServiceDisconnected", "adapter解除了与服务的绑定");
        }
    };

    public UserInfoAdapter(List<UserInfo> userInfoList){
        mUserInfoList = userInfoList;
        Intent bindIntent = new Intent(MyApplication.getContext(), SocketService.class);
        MyApplication.getContext().bindService(bindIntent, connection,
                MyApplication.getContext().BIND_AUTO_CREATE);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.userinfo_item,
                parent, false);
        final ViewHolder holder = new ViewHolder(view);
        holder.userInfoView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                LogUtil.e("RecyclerView", "长按");
                int position = holder.getAdapterPosition();
                if (position != 0){
                    ChairmanActivity.roalChange = true;
                    mBinder.sendTransfer(mUserInfoList.get(position).getName());
                    NonchairmanActivity.startActivity(MyApplication.getContext());
                }
                return true;
            }
        });
        holder.userInfoView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int position = holder.getAdapterPosition();
                for (int i = 0; i < mUserInfoList.size(); i++){
                    if (i != position){
                        //将没有点击到的item的flag设置为false
                        mUserInfoList.get(i).setFlag(false);
                    }else {
                        mUserInfoList.get(i).setFlag(true);
                    }
                }
                notifyDataSetChanged();
            }
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        UserInfo userInfo = mUserInfoList.get(position);
        holder.imageView.setImageResource(R.drawable.user);
        holder.nameTextView.setText(userInfo.getName());
        if (position == 0){
            holder.statusTextView.setText("主席");
        }else{
            holder.statusTextView.setText(" 非主席");
        }
        if (userInfo.isFlag()){
            holder.userInfoView.setBackgroundColor(Color.parseColor("#eeeeee"));
        }else{
            holder.userInfoView.setBackgroundColor(Color.parseColor("#ffffff"));
        }
        if (userInfo.getStatus() == 0){
            holder.nameTextView.setTextColor(Color.parseColor("#000000"));
            holder.statusTextView.setTextColor(Color.parseColor("#000000"));
        }else{
            holder.nameTextView.setTextColor(Color.parseColor("#49db5d"));
            holder.statusTextView.setTextColor(Color.parseColor("#49db5d"));
        }
        LogUtil.i("刷新显示了", userInfo.getName());
    }

    @Override
    public int getItemCount() {
        return mUserInfoList.size();
    }

    //自定义的ViewHolder，持有每个Item的的所有界面元素
    static class ViewHolder extends RecyclerView.ViewHolder{
        private View userInfoView;
        private ImageView imageView;
        private TextView nameTextView;
        private TextView statusTextView;

        public ViewHolder(View itemView) {
            super(itemView);
            userInfoView = itemView;
            imageView = (ImageView) itemView.findViewById(R.id.avatar_iamge);
            nameTextView = (TextView) itemView.findViewById(R.id.user_name);
            statusTextView = (TextView) itemView.findViewById(R.id.status);
        }
    }
}
