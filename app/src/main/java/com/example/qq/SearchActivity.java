package com.example.qq;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SearchView;
import android.widget.TextView;

import com.niuedu.ListTree;

import java.util.ArrayList;
import java.util.List;

import com.example.qq.adapter.ContactsPageListAdapter;

public class SearchActivity extends AppCompatActivity {
    //存放当前的搜索结果
    private List<MyContactInfo> searchResultList=new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //设置搜索
        initSearching();
    }

    //设置搜索相关的东西
    private void initSearching() {
        //搜索控件
        SearchView searchView = findViewById(R.id.searchView);
        //不以图标的形式显示
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
                //当点了“搜索”键时执行，因使用了实时搜索，此处
                //没有实现的必要了，所以返回false，表示我们并没有处理，
                //交由系统处理，但其实系统也没做什么处理。
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                //跟据newText中的字符串进行搜索，搜索其中包含关键字的节点
                ListTree tree = MainFragment.getContactsTree();
                //必须每次都清空保存结果的集合对象
                searchResultList.clear();

                //只有当要搜索的字符串非空时，才遍历列表
                if(!newText.equals("")) {
                   //遍历整个树
                    ListTree.EnumPos pos = tree.startEnumNode();
                    while (pos!=null) {
                        //如果这个节点中存的是联系人信息
                        ListTree.TreeNode node = tree.getNodeByEnumPos(pos);
                        if (node.getData() instanceof ContactsPageListAdapter.ContactInfo) {
                            //获取联系人信息对象
                            ContactsPageListAdapter.ContactInfo contactInfo =
                                    (ContactsPageListAdapter.ContactInfo) node.getData();
                            //获取此联系人的组名
                            ListTree.TreeNode groupNode = node.getParent();
                            ContactsPageListAdapter.GroupInfo groupInfo =
                                    (ContactsPageListAdapter.GroupInfo) groupNode.getData();
                            String groupName = groupInfo.getTitle();
                            //查看联系人的名字中或状态中是否包含了要搜索的字符串
                            if (contactInfo.getName().contains(newText) ||
                                    contactInfo.getStatus().contains(newText)) {
                                //搜到了！列出这个联系人的信息
                                searchResultList.add(new MyContactInfo(contactInfo, groupName));
                            }
                        }
                        //System.out.println(node.getData().toString());
                        pos = tree.enumNext(pos);
                    }
                }

                //通知RecyclerView，刷新数据
                resultListView.getAdapter().notifyDataSetChanged();
                return true;
            }
        });
    }

    //为了能保存所在组的组名，创建此类
    class MyContactInfo{
        //增加一个信息：所在组的组名
        private String groupName;
        private ContactsPageListAdapter.ContactInfo info;

        public MyContactInfo(ContactsPageListAdapter.ContactInfo info, String groupName) {
            this.info=info;
            this.groupName = groupName;
        }

        public String getGroupName() {
            return groupName;
        }
    }

    class ResultListAdapter extends RecyclerView.Adapter<ResultListAdapter.MyViewHolder>{
        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v= getLayoutInflater().inflate(R.layout.search_result_item,parent,false);
            return new MyViewHolder(v);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            //获取联系人信息，设置到对应的控件中
            MyContactInfo info = searchResultList.get(position);
            //holder.imageViewHead.setImageBitmap(info.info.getAvatar());
            holder.textViewName.setText(info.info.getName());
            String groupName =info.groupName;
            holder.textViewDetail.setText("来自分组 "+groupName);
        }

        @Override
        public int getItemCount() {
            return searchResultList.size();
        }
        public class MyViewHolder extends RecyclerView.ViewHolder {
            ImageView imageViewHead;
            TextView textViewName;
            TextView textViewDetail;

            public MyViewHolder(View itemView) {
                super(itemView);

                imageViewHead = itemView.findViewById(R.id.imageViewHead);
                textViewName = itemView.findViewById(R.id.textViewName);
                textViewDetail = itemView.findViewById(R.id.textViewDetail);
            }
        }
    }
}
