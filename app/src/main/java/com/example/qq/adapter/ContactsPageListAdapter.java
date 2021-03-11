package com.example.qq.adapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.niuedu.ListTree;
import com.niuedu.ListTreeAdapter;

import java.io.Serializable;

import com.example.qq.ChatActivity;
import com.example.qq.MainActivity;
import com.example.qq.R;

/**
 * 为联系人页面的“好友页面” RecyclerView提供数据
 */
public class ContactsPageListAdapter extends
        ListTreeAdapter<ListTreeAdapter.ListTreeViewHolder> {

    //存放组数据
    static public class GroupInfo{
        private String title;//组标题
        private int onlineCount;//此组内在线的人数

        public GroupInfo(String title, int onlineCount) {
            this.title = title;
            this.onlineCount = onlineCount;
        }

        public String getTitle() {
            return title;
        }

        public int getOnlineCount() {
            return onlineCount;
        }
    }

    //存放联系人数据
    //实现Serializable接口是为了在Activity间传递
    public class ContactInfo implements Serializable{
        //头像URL中的部分路径
        private int id;
        private String name; //名字
        private String status; //状态

        public ContactInfo(int id, String name, String status) {
            this.id = id;
            this.name = name;
            this.status = status;
        }

        public int getId(){
            return this.id;
        }

        public String getAvatarUrl() {
            return "/image/head/"+id+".png";
        }

        public String getName() {
            return name;
        }

        public String getStatus() {
            return status;
        }
    }


    public ContactsPageListAdapter(ListTree tree) {
        super(tree);
    }

    public ContactsPageListAdapter(ListTree tree,Bitmap expandIcon,Bitmap collapseIcon) {
        super(tree,expandIcon,collapseIcon);
    }

    @Override
    protected ListTreeAdapter.ListTreeViewHolder onCreateNodeView(ViewGroup parent, int viewType) {
        //获取从Layout创建View的对象
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        //创建不同的行View
        if(viewType== R.layout.contacts_group_item){
            //最后一个参数必须传true
            View view = inflater.inflate(viewType,parent,true);
            return new GroupViewHolder(view);
        }else if(viewType == R.layout.contacts_contact_item){
            View view  = inflater.inflate(viewType,parent,true);
            return new ContactViewHolder(view);
        }

        return null;
    }

    @Override
    protected void onBindNodeViewHolder(ListTreeAdapter.ListTreeViewHolder viewHoler, int position) {
        //获取行控件
        View view = viewHoler.itemView;
        //获取这一行这树对象中对应的节点
        ListTree.TreeNode node = tree.getNodeByPlaneIndex(position);

        if(node.getLayoutResId() == R.layout.contacts_group_item){
            //group node
            GroupInfo info = (GroupInfo)node.getData();
            GroupViewHolder gvh= (GroupViewHolder) viewHoler;
            gvh.textViewTitle.setText(info.getTitle());
            gvh.textViewCount.setText(info.getOnlineCount()+"/"+node.getChildrenCount());
        }else if(node.getLayoutResId() == R.layout.contacts_contact_item){
            //child node
            ContactInfo info = (ContactInfo) node.getData();

            ContactViewHolder cvh= (ContactViewHolder) viewHoler;
            //使用Glide下载网络图片并设置到图像控件中
            String imgURL = MainActivity.serverHostURL+info.getAvatarUrl();
            Glide.with(view.getContext()).load(imgURL).placeholder(R.drawable.contacts_focus).into(cvh.imageViewHead);

            //cvh.imageViewHead.setImageBitmap(info.getAvatar());
            cvh.textViewTitle.setText(info.getName());
            cvh.textViewDetail.setText(info.getStatus());
        }
    }

    //组ViewHolder
    class GroupViewHolder extends ListTreeAdapter.ListTreeViewHolder{
        TextView textViewTitle;//显示标题的控件
        TextView textViewCount;//显示好友数/在线数的控件

        public GroupViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewCount = itemView.findViewById(R.id.textViewCount);
        }
    }

    //好友ViewHolder
    class ContactViewHolder extends ListTreeAdapter.ListTreeViewHolder{
        ImageView imageViewHead;//显示好友头像的控件
        TextView textViewTitle;//显示好友名字的控件
        TextView textViewDetail;//显示好友状态的控件

        public ContactViewHolder(final View itemView) {
            super(itemView);

            imageViewHead = itemView.findViewById(R.id.imageViewHead);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDetail = itemView.findViewById(R.id.textViewDetail);

            //当点击这一行时，开始聊天
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //进入聊天页面
                    Intent intent = new Intent(itemView.getContext(), ChatActivity.class);
                    //将对方的名字作为参数传过去
                    ListTree.TreeNode node = tree.getNodeByPlaneIndex(getAdapterPosition());
                    ContactInfo info = (ContactInfo) node.getData();
                    intent.putExtra("contact_name",info.name);
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }
}