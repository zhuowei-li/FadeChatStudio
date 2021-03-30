package com.example.qq.adapter;

import android.view.View;
import android.view.ViewGroup;

import androidx.recyclerview.widget.RecyclerView;

/**
 * Created by nkm on 2017/9/21.
 */
public class SpacePageListAdapter extends RecyclerView.Adapter<SpacePageListAdapter.MyViewHolder> {
    @Override
    public SpacePageListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(SpacePageListAdapter.MyViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 0;
    }

    //将ViewHolder声明为Adapter的内部类，反正外面也用不到
    class MyViewHolder extends RecyclerView.ViewHolder{
        public MyViewHolder(View itemView) {
            super(itemView);
        }
    }
}