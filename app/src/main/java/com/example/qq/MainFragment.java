package com.example.qq;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;

import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.example.qq.Service.ChatService;
import com.example.qq.Service.FragmentListener;
import com.example.qq.adapter.ContactsPageListAdapter;
import com.example.qq.adapter.MessagePageListAdapter;
import com.niuedu.ListTree;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link MainFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {

    //创建一棵树
    private static ListTree tree = new ListTree();
    public static ListTree getContactsTree(){
        return tree;
    }

    private Disposable observableDisposable;

    private ContactsPageListAdapter contactsAdapter;

    private ListTree.TreeNode groupNode1;
    private ListTree.TreeNode groupNode2;
    private ListTree.TreeNode groupNode3;
    private ListTree.TreeNode groupNode4;
    private ListTree.TreeNode groupNode5;

    private FragmentListener fragmentListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentListener){
            fragmentListener=(FragmentListener)context;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        //停止定时器
        observableDisposable.dispose();
        observableDisposable=null;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentListener=null;
    }

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private View[] listViews ={null,null,null};
    private TabLayout tabLayout;
    private ViewPager viewPager;
    private ViewGroup rootView;

    public MainFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment MainFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static MainFragment newInstance(String param1, String param2) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ChatService service = fragmentListener.getRetrofit().create(ChatService.class);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }

    }

    @Override
    public void onStart() {
        super.onStart();

        //创建一个定时器Observable
        Observable.interval(10, TimeUnit.SECONDS)
         .retry()
         .flatMap( v -> {
                //向服务端获取联系人
                ChatService service =
                        fragmentListener.getRetrofit().create(ChatService.class);
                return service.getContacts().map(result->{
                    if (result.getRetCode()==0){
                       return result.getData();
                    }else {
                        throw new RuntimeException(result.getErrMsg());
                    }
                });
            })
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Observer<List<ContactsPageListAdapter.ContactInfo>>() {

                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    observableDisposable=d;
                }

                @Override
                public void onNext(@NonNull List<ContactsPageListAdapter.ContactInfo> contactInfos) {
                //将联系人保存到我的好友，要先清空现有的好友
                    tree.clearDescendant(groupNode2);
                    for(ContactsPageListAdapter.ContactInfo info:contactInfos){
                        ListTree.TreeNode node2 = tree.addNode(groupNode2,info,
                                R.layout.contacts_contact_item);
                        node2.setShowExpandIcon(false);
                    }
                //通知RecyclerView更新数据
                    contactsAdapter.notifyDataSetChanged();
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    //提示错误信息
                    String errmsg = e.getLocalizedMessage();
                    Snackbar.make(rootView, "大王祸事了：" + errmsg, Snackbar.LENGTH_LONG)
                            .setAction("Action", null).show();
                    Log.e("qqapp1", errmsg);
                }

                @Override
                public void onComplete() {

                }
            });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.rootView = (ViewGroup) inflater.inflate(R.layout.fragment_main,container,false);
        viewPager = this.rootView.findViewById(R.id.viewPaper);
        viewPager.setAdapter(new ViewPageAdapter());
        tabLayout = this.rootView.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);
        tabLayout.getTabAt(0).setIcon(R.drawable.message_normal);
        tabLayout.getTabAt(1).setIcon(R.drawable.contacts_normal);
        tabLayout.getTabAt(2).setIcon(R.drawable.space_normal);
        RecyclerView v1 = new RecyclerView(getContext());
        View v2 =createContactPage();
        RecyclerView v3 = new RecyclerView(getContext());
        listViews[0]=v1;
        listViews[1]=v2;
        listViews[2]=v3;
        //为消息Recyclerview设置layoutManager
        v1.setLayoutManager(new LinearLayoutManager(getContext()));
        //为消息Recyclerview设置Adapter
        v1.setAdapter(new MessagePageListAdapter(getActivity()));
        //为+设置侦听器
        TextView popMenu = this.rootView.findViewById(R.id.textViewPopMenu);
        popMenu.setOnClickListener(new View.OnClickListener() {
            //把弹出窗口作为成员变量
            PopupWindow pop;
            View mask;

            @Override
            public void onClick(View view) {
                if(mask==null) {
                    //加入一个view作为最上层阴影
                    mask = new View(getContext());
                    mask.setBackgroundColor(Color.DKGRAY);
                    mask.setAlpha(0.5f);
                    //阴影View的点击事件
                    mask.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            //去掉阴影
                            MainFragment.this.rootView.removeView(mask);
                            //菜单窗口隐藏
                            pop.dismiss();

                        }
                    });
                }
                MainFragment.this.rootView.addView(mask,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                //如果弹出窗口还未创建。则创建它
                if (pop == null) {
                    //创建PopupWindow，用于承载气泡菜单
                    pop = new PopupWindow(getActivity());
                    //加载菜单项资源
                    LinearLayout menu = (LinearLayout) LayoutInflater.from(getActivity()).inflate(
                            R.layout.pop_menu_layout,null
                    );
                    //设置窗口要显示的view
                    pop.setContentView(menu);
                    //设置窗口动画
                    pop.setAnimationStyle(R.style.popMenuAnim);
                    //加载气泡图像
                    Drawable drawable = getResources().getDrawable(R.drawable.pop_bk);
                    //设置气泡图像为窗口背景
                    pop.setBackgroundDrawable(drawable);
                    //根据菜单大小设置窗口大小
                    menu.measure(0,0);
                    int w = menu.getMeasuredWidth();
                    int h = menu.getMeasuredHeight();
                    pop.setWidth(w+60);
                    pop.setHeight(h+60);
                    //设置焦点。这样按返回键时菜单才会消失
                    pop.setFocusable(true);
                    //让阴影随窗口消失而消失
                    pop.setOnDismissListener(new PopupWindow.OnDismissListener() {
                        @Override
                        public void onDismiss() {
                            //去掉阴影
                            MainFragment.this.rootView.removeView(mask);
                        }
                    });
                }
                //显示窗口
                pop.showAsDropDown(view,-pop.getWidth()+40,-10);

            }
        });

        ImageView headImage = rootView.findViewById(R.id.headImage);
        headImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final View contentLayout = rootView.findViewById(R.id.contentLayout);
                int messageImageWidth = Utils.dip2px(getActivity(),60);
                final int drawerWidth = rootView.getWidth() - messageImageWidth;
                //动画持续时间
                final int duration = 400;
                //创建抽屉页面
                final View drawerLayout = getActivity().getLayoutInflater().
                        inflate(R.layout.drawer_layout,rootView,false);
                //设置抽屉页面的宽度
                drawerLayout.getLayoutParams().width = drawerWidth;
                //把抽屉页面加入frameLayout
                rootView.addView(drawerLayout);
                final View maskView = new View(getContext());
                maskView.setBackgroundColor(Color.GRAY);
                maskView.setAlpha(0);
                maskView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //动画反着来，抽屉消失
                        //移动原内容
                        ObjectAnimator animatorContent = ObjectAnimator.ofFloat
                                (contentLayout,"translationX",drawerWidth,0);
                        //移动蒙板
                        ObjectAnimator animatorMask = ObjectAnimator.ofFloat(maskView,"translationX",
                                drawerWidth,0);
                        //蒙板动画刷新
                        animatorMask.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator animation) {
                                float progress = (animation.getCurrentPlayTime()/(float)duration);
                                maskView.setAlpha(1-progress);
                            }
                        });
                        //抽屉页面
                        ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat
                                (drawerLayout,"translationX",0,-drawerWidth/2);
                        //动画合集
                        @SuppressLint("Recycle") AnimatorSet animatorSet = new AnimatorSet();
                        animatorSet.playTogether(animatorContent,animatorDrawer,animatorMask);
                        animatorSet.setDuration(duration);
                        //设置侦听器，主要侦听动画关闭事件
                        animatorSet.addListener(new Animator.AnimatorListener() {
                            @Override
                            public void onAnimationStart(Animator animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animator animation) {
                                //动画结束，删除蒙板和抽屉页面
                                rootView.removeView(maskView);
                                rootView.removeView(drawerLayout);
                            }

                            @Override
                            public void onAnimationCancel(Animator animation) {

                            }

                            @Override
                            public void onAnimationRepeat(Animator animation) {

                            }
                        });
                        animatorSet.start();
                    }
                });
                rootView.addView(maskView);
                //创建抽屉动画
                ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat
                        (drawerLayout,"translationX",-drawerWidth/2,0);
                //把它搞到最上层
                contentLayout.bringToFront();
                //蒙板view弄到最上层
                maskView.bringToFront();
                //创建动画，移动原内容
                ObjectAnimator animatorContent = ObjectAnimator.ofFloat
                        (contentLayout,"translationX",0,drawerWidth);
                //移动蒙板的动画
                ObjectAnimator animatorMask = ObjectAnimator.ofFloat(maskView,"translationX",
                        0,drawerWidth);
                //响应移动动画的刷新时间，逐渐变暗
                animatorMask.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float progress = (animation.getCurrentPlayTime()/(float)duration)/2;
                        maskView.setAlpha(progress);
                    }
                });

                //创建动画集合，同时播放三个动画（内容、蒙板、抽屉页面）
                @SuppressLint("Recycle") AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(animatorContent,animatorDrawer,animatorMask);
                animatorSet.setDuration(duration);
                animatorSet.start();
            }
        });
        //下拉刷新
        final SwipeRefreshLayout refreshLayout=rootView.findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //执行刷新数据的代码 一般都是耗时操作会开启其他的线程
                refreshLayout.setRefreshing(false);
            }
        });

        return rootView;
    }
    //为viewpage派生一个适配器类
    class ViewPageAdapter extends PagerAdapter {
        ViewPageAdapter(){

        }

        @Override
        public int getCount(){
            return listViews.length;
        }

        @Override
        public boolean isViewFromObject(View view,Object object){
            return view == object;
        }

        //实例化一个子view，container是子view容器，就是viewpager,position是当前页数
        @Override
        public Object instantiateItem(ViewGroup container,int position){
            View v = listViews[position];
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container,int position,Object object){
            container.removeView((View) object);
        }

        @Override
        public CharSequence getPageTitle(int position){
            if(position==0){
                return "消息";
            }else if(position==1){
                return "联系人";
            }else if(position==2) {
                return "动态";
            }
            return null;
        }


    }
    //自定义像素转换工具类
    public static final class Utils{
        //从dp转为px
        public static int dip2px(Context context,float dpValue){
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (dpValue*scale+0.5f);
        }
        //从px转为dp
        public static int px2dip(Context context,float pxValue){
            final float scale = context.getResources().getDisplayMetrics().density;
            return (int) (pxValue/scale+0.5f);
        }
    }
private View createContactPage(){
        //整个联系人页面的view
        View v = getLayoutInflater().inflate(R.layout.contacts_page_layout,null);
        //向树中添加结点 先创建组结点 组是树的根结点 父结点为null
    ContactsPageListAdapter.GroupInfo group1=
            new ContactsPageListAdapter.GroupInfo("特别关心",0);
    ContactsPageListAdapter.GroupInfo group2=
            new ContactsPageListAdapter.GroupInfo("我的好友",0);
    ContactsPageListAdapter.GroupInfo group3=
            new ContactsPageListAdapter.GroupInfo("朋友",0);
    ContactsPageListAdapter.GroupInfo group4=
            new ContactsPageListAdapter.GroupInfo("家人",0);
    ContactsPageListAdapter.GroupInfo group5=
            new ContactsPageListAdapter.GroupInfo("同学",0);

     groupNode1=tree.addNode(null,group1,R.layout.contacts_group_item);
     groupNode2=tree.addNode(null,group2,R.layout.contacts_group_item);
     groupNode3=tree.addNode(null,group3,R.layout.contacts_group_item);
     groupNode4=tree.addNode(null,group4,R.layout.contacts_group_item);
     groupNode5=tree.addNode(null,group5,R.layout.contacts_group_item);
    //获取页面里的recyclerview,为它创建适配器
    RecyclerView recyclerView = v.findViewById(R.id.contactListView);
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
    recyclerView.setAdapter(new ContactsPageListAdapter(tree));

    //搜索控件的点击事件
    View fakeSearchView=v.findViewById(R.id.searchViewStub);
    fakeSearchView.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Intent intent=new Intent(getContext(),SearchActivity.class);
            startActivity(intent);
        }
    });


    return v;
    }
}