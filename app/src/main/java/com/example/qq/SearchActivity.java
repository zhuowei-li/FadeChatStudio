package com.example.qq;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.example.qq.adapter.ContactsPageListAdapter;
import com.niuedu.ListTree;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends AppCompatActivity {
    //用于存放当前的搜索结果
    private List<MyContactInfo> searchResultList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        initSearching();
    }

    //设置搜索相关的东西
    private void initSearching() {
        //搜索控件
        SearchView searchView = findViewById(R.id.searchView);
        //不以图标形式显示
        searchView.setIconifiedByDefault(false);
        //searchView.setSubmitButtonEnabled(true);

        //取消按钮
        TextView cancelView = findViewById(R.id.tvCancel);
        //搜索结果列表
        final RecyclerView resultListView = findViewById(R.id.resultListView);
        resultListView.setLayoutManager(new LinearLayoutManager(this));
        resultListView.setAdapter(new ResultListAdapter());
        //响应SearchView的文本输入事件，以实现实时搜索
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                //点搜索键时执行，不做处理
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //根据newNext中的字符串进行搜索，搜索其中包含关键字的节点
                ListTree tree = MainFragment.getContactsTree();
                //每次都要清空保存搜索结果的集合对象
                searchResultList.clear();

                //搜索框的字符串为非空时才遍历列表
                if (!newText.equals("")){
                    //遍历整个树
                   ListTree.EnumPos pos = tree.startEnumNode();
                   while (pos!=null){
                       ListTree.TreeNode node = tree.getNodeByEnumPos(pos);
                       if(node.getData() instanceof
        ContactsPageListAdapter.ContactInfo){
                         //获取联系人信息对象
                         ContactsPageListAdapter.ContactInfo contactInfo =
                                 (ContactsPageListAdapter.ContactInfo)
                                 node.getData();
                         //获取联系人组名
                         ListTree.TreeNode groupNode = node.getParent();
                         ContactsPageListAdapter.GroupInfo groupInfo =
                                 (ContactsPageListAdapter.GroupInfo)
                                 groupNode.getData();
                         String groupName = groupInfo.getTitle();

                         if(contactInfo.getName().contains(newText)||
                         contactInfo.getStatus().contains(newText)){
                           searchResultList.add(new MyContactInfo(contactInfo,groupName));
                         }
                       }
                       pos=tree.enumNext(pos);
                   }
                }
                resultListView.getAdapter().notifyDataSetChanged();

                return true;
            }
        });

    }

    //为了让联系人信息里包含组信息
    class MyContactInfo {
        private String groupName;
        private ContactsPageListAdapter.ContactInfo info;

        public MyContactInfo(ContactsPageListAdapter.ContactInfo info, String groupName) {
            this.info = info;
            this.groupName = groupName;
        }

        public String getGroupName() {
            return groupName;
        }
    }

    //显示结果的RecyclerView的适配器
    class ResultListAdapter extends
            RecyclerView.Adapter<ResultListAdapter.MyViewHolder> {
        @NonNull
        @Override
        public ResultListAdapter.MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v =
                    getLayoutInflater().inflate(R.layout.search_result_item, parent, false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ResultListAdapter.MyViewHolder holder, int position) {
            //获取联系人信息,设置到对应的控件中
            MyContactInfo info = searchResultList.get(position);
            //holder.imageViewHead.setImageBitmap(info.info.getAvatar());
            holder.textViewName.setText(info.info.getName());
            String groupName = info.groupName;
            holder.textViewDetail.setText("来自分组" + groupName);
        }

        @Override
        public int getItemCount() {
            return searchResultList.size();
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imageViewHead;
            TextView textViewName;
            TextView textViewDetail;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);

                imageViewHead = itemView.findViewById(R.id.imageViewHead);
                textViewName = itemView.findViewById(R.id.textViewName);
                textViewDetail = itemView.findViewById(R.id.textViewDetail);
            }
        }
    }
}

