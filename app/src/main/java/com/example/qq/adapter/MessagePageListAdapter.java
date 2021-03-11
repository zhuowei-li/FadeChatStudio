package com.example.qq.adapter;

import android.app.Activity;
import android.content.Intent;
import android.media.Image;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.qq.ChatActivity;
import com.example.qq.R;

/**
 * Created by nkm on 2017/9/21.
 */
public class MessagePageListAdapter extends
        RecyclerView.Adapter<MessagePageListAdapter.MyViewHolder> {

    //保存提示消息的数据
    static class MessageInfo{
        private String title;
        private String detail;
        private Date time;//发出提示的时间
        private int id;//消息ID

        public MessageInfo(String title, String detail, Date time) {
            this.title = title;
            this.detail = detail;
            this.time = time;
        }
    }

    //用于获取
    private Activity activity;

    //保存数据的集合
    private List<MessageInfo> messages = new ArrayList<>();

    //创建一个带参数的构造方法，通过参数可以把Activity传过来
    public MessagePageListAdapter(Activity activity){
        this.activity = activity;

        //造几条假数据
        MessageInfo mi=new MessageInfo("服务号",
                "QQ天气：[今天的天气。。。哈哈哈，暂时不能告诉你~~]",
                new Date());
        messages.add(mi);

        mi=new MessageInfo("马爸爸",
                "上月给你打的一百万花完了吗? 快想办法帮我花钱！",
                new Date());
        messages.add(mi);
    }

    @Override
    public MessagePageListAdapter.MyViewHolder onCreateViewHolder(
            ViewGroup parent, int viewType) {
        //从layout资源加载行View
        LayoutInflater inflater = activity.getLayoutInflater();
        View view=null;
        if(viewType == R.layout.message_list_item_search) {
            //加载搜索行控件
            view = inflater.inflate(R.layout.message_list_item_search,
                    parent, false);
        }else{
            //加载其它行控件
            view = inflater.inflate(R.layout.message_list_item_normal,
                    parent, false);
        }

        MyViewHolder viewHolder=new MyViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(MessagePageListAdapter.MyViewHolder holder, int position) {
        if(position==0){
            //搜索行
            return;
        }

        MessageInfo mi = messages.get(position-1);
        holder.textViewTitle.setText(mi.title);
        holder.textViewDetial.setText(mi.detail);
        String str=new SimpleDateFormat("HH:mm:ss").format(mi.time);
        holder.textViewTime.setText(str);
        //记下title，以备响应点击事件时取出
        holder.itemView.setTag(mi.title);
    }

    @Override
    public int getItemCount() {
        return messages.size()+1;//多出一行是搜索行
    }

    @Override
    public int getItemViewType(int position) {
        if(0==position){
            //只有最顶端这行是搜索
            return R.layout.message_list_item_search;
        }
        //其余各行都一样的控件
        return R.layout.message_list_item_normal;
    }

    //将ViewHolder声明为Adapter的内部类，反正外面也用不到
    class MyViewHolder extends RecyclerView.ViewHolder{
        ImageView imageView;
        TextView textViewTitle;
        TextView textViewTime;
        TextView textViewDetial;

        public MyViewHolder(View itemView) {
            super(itemView);
            this.imageView = itemView.findViewById(R.id.imageView);
            this.textViewTitle = itemView.findViewById(R.id.textViewTitle);
            this.textViewTime = itemView.findViewById(R.id.textViewTime);
            this.textViewDetial = itemView.findViewById(R.id.textViewDetial);
            if(this.textViewDetial!=null) {
                //保证只显示一行且显示不了文字时最后出现省略号
                this.textViewDetial.setEllipsize(TextUtils.TruncateAt.END);
                this.textViewDetial.setSingleLine();
            }

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //进入聊天页面
                    Intent intent = new Intent(activity, ChatActivity.class);
                    intent.putExtra("title",(String)view.getTag());
                    activity.startActivity(intent);
                }
            });
        }
    }
}
