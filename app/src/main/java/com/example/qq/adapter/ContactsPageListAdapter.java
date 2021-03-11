package com.example.qq.adapter;

import android.content.Intent;
import android.graphics.Bitmap;
import android.icu.text.CaseMap;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.qq.ChatActivity;
import com.example.qq.R;
import com.niuedu.ListTree;
import com.niuedu.ListTreeAdapter;

import java.io.Serializable;
import java.nio.file.NotDirectoryException;

public class ContactsPageListAdapter extends ListTreeAdapter<ListTreeAdapter.ListTreeViewHolder>{

    //存放组数据
    static public class GroupInfo{
        private String title;//组标题
        private int onlineCount;//此组内在线人数

        public GroupInfo(String title,int onlineCount){
            this.title=title;
            this.onlineCount=onlineCount;
        }

        public String getTitle(){
            return title;
        }

        public int getOnlineCount(){
            return onlineCount;
        }

    }

    static public class ContactInfo implements Serializable {
        private String avatarURL;//头像URL
        private String name;
        private String status;

        public ContactInfo(String avatarURL,String name,String status){
            this.avatarURL = avatarURL;
            this.name = name;
            this.status = status;
        }

        public String getAvatar(){
            return avatarURL;
        }

        public String getName(){
            return name;
        }

        public String getStatus(){
            return status;
        }
    }

    public ContactsPageListAdapter(ListTree tree) {
        super(tree);
    }

    public ContactsPageListAdapter(ListTree tree, Bitmap expandIcon, Bitmap collapseIcon) {
        super(tree, expandIcon, collapseIcon);
    }

    @Override
    protected ListTreeViewHolder onCreateNodeView(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if(viewType==R.layout.contacts_group_item){
            View view = inflater.inflate(viewType,parent,true);
            return new GroupViewHolder(view);
        }else if(viewType==R.layout.contacts_contact_item){
            View view = inflater.inflate(viewType,parent,true);
            return new ContactViewHolder(view);
        }
        return null;
    }

    @Override
    protected void onBindNodeViewHolder(ListTreeAdapter.ListTreeViewHolder viewHolder, int position) {
        //获取行控件
        View view = viewHolder.itemView;
        //获取这一行在树对象中对应的结点
        ListTree.TreeNode node = tree.getNodeByPlaneIndex(position);

        if (node.getLayoutResId()==R.layout.contacts_group_item){
            GroupInfo info = (GroupInfo) node.getData();
            GroupViewHolder gvh = (GroupViewHolder) viewHolder;
            gvh.textViewTitle.setText(info.getTitle());
            gvh.textViewCount.setText(info.getOnlineCount()+"/"+ node.getChildrenCount());
        }else if(node.getLayoutResId()==R.layout.contacts_contact_item){
            ContactInfo info = (ContactInfo)node.getData();
            ContactViewHolder cvh = (ContactViewHolder)viewHolder;
            //cvh.imageViewHead.setImageBitmap(info.getAvatar());
            cvh.textViewTitle.setText(info.getName());
            cvh.textViewDetail.setText(info.getStatus());
        }
    }
    //组holder
    class GroupViewHolder extends ListTreeViewHolder {
        TextView textViewTitle;
        TextView textViewCount;

        public GroupViewHolder(View itemView) {
            super(itemView);
            textViewTitle = itemView.findViewById(R.id.textViewTitle1);
            textViewCount = itemView.findViewById(R.id.textViewCount);
        }

    }
    //联系人holder
    class ContactViewHolder extends ListTreeViewHolder {
        ImageView imageViewHead;
        TextView textViewTitle;
        TextView textViewDetail;

        public ContactViewHolder(View itemView) {
            super(itemView);
            imageViewHead = itemView.findViewById(R.id.imageViewHead);
            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewDetail = itemView.findViewById(R.id.textViewDetail);

            //当点击这一行时开始聊天
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent=new Intent(itemView.getContext(), ChatActivity.class);
                    intent.putExtra("contact_name",(String) containerView.getTag());
                    itemView.getContext().startActivity(intent);
                }
            });
        }
    }
}
