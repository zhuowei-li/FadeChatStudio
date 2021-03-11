package com.example.qq.adapter;

import android.app.Activity;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.qq.R;

public class MessagePageListAdapter extends RecyclerView.Adapter<MessagePageListAdapter.MyViewHolder>{

    private Activity activity;

    public MessagePageListAdapter(Activity activity){
        this.activity = activity;
    }

    @Override
    public MessagePageListAdapter.MyViewHolder onCreateViewHolder(ViewGroup parent,int viewType){
        LayoutInflater inflater = activity.getLayoutInflater();
        View view = null;
        if(viewType==R.layout.message_list_item_search){
            view = inflater.inflate(R.layout.message_list_item_search,parent,false);
        }else{
            view = inflater.inflate(R.layout.message_list_item_normal,parent,false);
        }
        MyViewHolder viewHolder = new MyViewHolder (view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(
            MessagePageListAdapter.MyViewHolder holder,int position
    ){
    }

    @Override
    public int getItemCount(){
        return 10;
    }

    @Override
    public int getItemViewType(int position){
        if(0==position){
            return R.layout.message_list_item_search;
        }
        return R.layout.message_list_item_normal;
    }
    class MyViewHolder extends  RecyclerView.ViewHolder{
        public MyViewHolder(View itemView){
            super(itemView);
        }
    }
}
