package com.example.qq;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.niuedu.ListTree;

import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import com.example.qq.R;

import com.example.qq.Service.ChatService;
import com.example.qq.Service.FragmentListener;
import com.example.qq.adapter.ContactsPageListAdapter;
import com.example.qq.adapter.MessagePageListAdapter;

/**
 * A simple {@link Fragment} subclass.
 */
public class MainFragment extends Fragment {
    final static int TAB_MESSAGE = 0; //QQ消息
    final static int TAB_CONTACTS = 1;//QQ联系人
    final static int TAB_SPACE = 2;//QQ动态（空间）

    private TabLayout tabLayout;//引用TabLayout控件
    private ViewPager viewPager;
    private ViewGroup rootView;

    private FragmentListener fragmentListener;
    private ChatService service;//Retrofit所需要的接口
    private Disposable observableDisposable;//用于停止订阅的东西


    //联系人Adpater，为了更新数据而设
    private ContactsPageListAdapter contactsAdapter;
    //创建集合（一棵树）
    private static ListTree tree = new ListTree();

    public static ListTree getContactsTree() {
        return tree;
    }

    private ListTree.TreeNode groupNode1;
    private ListTree.TreeNode groupNode2;
    private ListTree.TreeNode groupNode3;
    private ListTree.TreeNode groupNode4;
    private ListTree.TreeNode groupNode5;

    //用一个数组保存三个View的实例
    private View listViews[] = {null, null, null};

    public MainFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        service = fragmentListener.getRetrofit().create(ChatService.class);
        //跟据某种条件，决定是否重新定阅
//        observable.repeat()
//                repeatWhen(new Function<Observable<Object>,  ObservableSource<?>>(){
//            //这个方法就是决定是否继续定阅的回调方法
//            @Override
//            public ObservableSource<?> apply(Observable<Object> objectObservable) throws Exception {
//                return objectObservable.flatMap(object->{
//                    //传入的这个参数没有用
//                    //返回这个东西，能使重新定阅间隔一段时间，相当于定时器
//                    return Observable.timer(20,TimeUnit.SECONDS);
//                });
//            }
//        })
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        this.rootView = (ViewGroup) inflater.inflate(R.layout.fragment_main,
                container, false);

        //获取刷新控件
        final SwipeRefreshLayout refreshLayout = rootView.findViewById(R.id.refreshLayout);
        //响应它发出的事件
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                //执行刷新数据的代码写在这里，不过一般都是耗时的操作或访问网络，所以需要
                //开启另外的线程。

                //刷新完成，隐藏UFO
                refreshLayout.setRefreshing(false);
            }
        });

        //创建三个RecyclerView，分别对应QQ消息页，QQ联系人页，QQ空间页
        RecyclerView v1 = new RecyclerView(getContext());
        View v2 = createContactsPage();
        RecyclerView v3 = new RecyclerView(getContext());

        //将这三个View设置到数组中
        listViews[0] = v1;
        listViews[1] = v2;
        listViews[2] = v3;

        //别忘了设置layout管理器，否则不显示条目
        v1.setLayoutManager(new LinearLayoutManager(getContext()));
        //v3.setLayoutManager(new LinearLayoutManager(getContext()));

        //为RecyclerView设置Adapter
        v1.setAdapter(new MessagePageListAdapter(getActivity()));
        //v3.setAdapter(new SpacePageListAdapter());

        //获取ViewPager实例，将Adapter设置给它
        viewPager = this.rootView.findViewById(R.id.viewPager);
        viewPager.setAdapter(new ViewPageAdapter());
        //获取TabLayou并配置它
        tabLayout = this.rootView.findViewById(R.id.tabLayout);
        tabLayout.setupWithViewPager(viewPager);

        //响应+号图标点击事件，显示遮罩和气泡菜单
        TextView popMenu = this.rootView.findViewById(R.id.textViewPopMenu);
        popMenu.setOnClickListener(new View.OnClickListener() {
            //把弹出窗口作为成员变量
            PopupWindow pop;
            View mask = new View(getContext());

            @Override
            public void onClick(View view) {
                //向Fragment容器(FrameLayout)中加入一个View作为上层容器和遮罩

                mask.setBackgroundColor(Color.DKGRAY);
                mask.setAlpha(0.5f);
                MainFragment.this.rootView.addView(mask,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT);
                //响应蒙板View的点击事件
                mask.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //隐藏弹出窗口
                        pop.dismiss();
                    }
                });

                //如果弹出窗口还未创建，则创建它
                if (pop == null) {
                    //创建PopupWindow，用于承载气泡菜单
                    pop = new PopupWindow(getActivity());

                    //加载气泡图像，以作为window的背景
                    Drawable drawable = getResources().getDrawable(R.drawable.pop_bk);
                    //设置气泡图像为window的背景
                    pop.setBackgroundDrawable(drawable);
                    //加载菜单项资源，是用LinearLayout模拟的菜单
                    LinearLayout menu = (LinearLayout) LayoutInflater
                            .from(getActivity())
                            .inflate(R.layout.pop_menu_layout, null);
                    //设置window中要显示的View
                    pop.setContentView(menu);
                    //计算一下菜单layout的实际大小然后获取之
                    menu.measure(0, 0);

                    pop.setAnimationStyle(R.style.popoMenuAnim);
                    //设置窗口出现时获取焦点，这样在按下返回键时，窗口才会消失
                    pop.setFocusable(true);
                    pop.setOnDismissListener(() -> {
                        //去掉蒙板
                        rootView.removeView(mask);
                    });
                }
                //显示窗口
                pop.showAsDropDown(view, -pop.getContentView().getWidth() -10, -10);
            }
        });

        //响应左上角的图标点击事件，显示抽屉页面
        ImageView headImage = rootView.findViewById(R.id.headImage);
        headImage.setOnClickListener(v -> {
            //创建抽屉页面
            final View drawerLayout = getActivity().getLayoutInflater().inflate(
                    R.layout.drawer_layout, rootView, false);

            //获取原内容的根控件
            final View contentLayout = rootView.findViewById(R.id.contentLayout);

            //动画持续的时间
            final int duration = 400;

            //先计算一下消息页面中，左边一排图像的大小，在界面构建器中设置的是dp
            //在代码中只能用像素，所以这里要换算一下，因为不同的屏幕分辩率，dp对应
            //的像素数是不同的
            int messageImageWidth = Utils.dip2px(getActivity(), 60);
            //计算抽屉页面的宽度，rootView是FrameLayout，
            //利用getWidth()即可获得它当前的宽度
            final int drawerWidth = rootView.getWidth() - messageImageWidth;
            //设置抽屉页面的宽度
            drawerLayout.getLayoutParams().width = drawerWidth;
            //将抽屉页面加入FrameLayout中
            rootView.addView(drawerLayout);

            //创建蒙板View
            final View maskView = new View(getContext());
            maskView.setBackgroundColor(Color.GRAY);
            //必须将其初始透明度设为完全透明
            maskView.setAlpha(0);
            //当点击蒙板View时，隐藏抽屉页面
            maskView.setOnClickListener(v4 -> {
                //动画反着来，让抽屉消失

                //创建动画，移动原内容，从0位置移动抽屉页面宽度的距离（注意其宽度不变）
                ObjectAnimator animatorContent = ObjectAnimator.ofFloat(
                        contentLayout,
                        "translationX",
                        drawerWidth, 0);

                //移动蒙板的动画
                ObjectAnimator animatorMask = ObjectAnimator.ofFloat(
                        maskView,
                        "translationX",
                        drawerWidth, 0);
                //响应此动画的刷新事件，在其中改变原页面的背景色，使其逐渐变暗
                animatorMask.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    //响应动画更新的方法
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        //计算当前进度比例,最后除以2的原因是因为透明度最终只降到一半,约127
                        float progress = (animation.getCurrentPlayTime() / (float) duration);
                        maskView.setAlpha(1 - progress);
                    }
                });

                //创建动画，让抽屉页面向右移，注意它是从左移出来的，
                //所以其初始位值设置为-drawerWidth/2，即有一半位于屏幕之外。
                ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat(
                        drawerLayout,
                        "translationX",
                        0, -drawerWidth / 2);

                //创建动画集合，同时播放三个动画
                AnimatorSet animatorSet = new AnimatorSet();
                animatorSet.playTogether(animatorContent, animatorMask, animatorDrawer);
                animatorSet.setDuration(duration);
                //设置侦听器，主要侦听动画关闭事件
                animatorSet.addListener(new Animator.AnimatorListener() {
                    @Override
                    public void onAnimationStart(Animator animation) { }

                    @Override
                    public void onAnimationEnd(Animator animation) {
                        //动画结束，将蒙板和抽屉页面删除
                        rootView.removeView(maskView);
                        rootView.removeView(drawerLayout);
                    }

                    @Override
                    public void onAnimationCancel(Animator animation) { }

                    @Override
                    public void onAnimationRepeat(Animator animation) { }
                });
                animatorSet.start();
            });
            rootView.addView(maskView);

            //把它搞到最上层，这样在移动时能一直看到它（QQ就是这个效果）
            contentLayout.bringToFront();
            //再将蒙板View搞到最上层
            maskView.bringToFront();
            //创建动画，移动原内容，从0位置移动抽屉页面宽度的距离（注意其宽度不变）
            ObjectAnimator animatorContent = ObjectAnimator.ofFloat(contentLayout,
                    "translationX", 0, drawerWidth);

            //移动蒙板的动画
            ObjectAnimator animatorMask = ObjectAnimator.ofFloat(maskView,
                    "translationX", 0, drawerWidth);

            //响应此动画的刷新事件，在其中改变原页面的背景色，使其逐渐变暗
            //响应动画更新的方法
            animatorMask.addUpdateListener(animation -> {
                //计算当前进度比例,最后除以2的原因是因为透明度最终只降到一半,约127
                float progress = (animation.getCurrentPlayTime() / (float) duration) / 2;
                maskView.setAlpha(progress);
            });

            //创建动画，让抽屉页面向右移，注意它是从左移出来的，
            //所以其初始位值设置为-drawerWidth/2，即有一半位于屏幕之外。
            ObjectAnimator animatorDrawer = ObjectAnimator.ofFloat(drawerLayout,
                    "translationX", -drawerWidth / 2, 0);

            //创建动画集合，同时播放三个动画
            AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playTogether(animatorContent, animatorMask, animatorDrawer);
            animatorSet.setDuration(duration);
            animatorSet.start();
        });

        return rootView;
    }

    @Override
    public void onStart() {
        //必须调用父类的相同方法
        super.onStart();

        //创建一个定时器Observable
        Observable intervalObservable = Observable.interval(10, TimeUnit.SECONDS);
        intervalObservable.retry().flatMap(v -> {
            //向服务端发出获取联系人列表的请求
            return service.getContacts().map(result -> {
                //转换服务端返回的数据，将真正的负载发给观察者
                if (result.getRetCode() == 0) {
                    return result.getData();
                } else {
                    throw new RuntimeException(result.getErrMsg());
                }
            });
        }).retry().subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<ContactsPageListAdapter.ContactInfo>>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        observableDisposable=d;
                    }

                    @Override
                    public void onNext(List<ContactsPageListAdapter.ContactInfo> contactInfos) {
                        //将联系人们保存到“我的好友”组
                        //但注意，需先清空现有好友
                        tree.clearDescendant(groupNode2);
                        for (ContactsPageListAdapter.ContactInfo info : contactInfos) {
                            if (!info.getName().equals(Utils.myInfo.getName())) {
                                ListTree.TreeNode node2 = tree.addNode(groupNode2, info, R.layout.contacts_contact_item);
                                //没有子节点了，不显示展开、收起图标
                                node2.setShowExpandIcon(false);
                            }
                        }
                        //通知RecyclerView更新数据
                        contactsAdapter.notifyDataSetChanged();
                    }

                    @Override
                    public void onError(Throwable e) {
                        //提示错误信息
                        String errmsg = e.getLocalizedMessage();
                        Snackbar.make(rootView, "大王祸事了：" + errmsg, Snackbar.LENGTH_LONG)
                                .setAction("Action", null).show();
                        Log.e("qqapp1", errmsg);
                    }

                    @Override
                    public void onComplete() {
                        Log.i("qqapp1", "get contacts completed!");
                    }
                });
    }

    @Override
    public void onStop() {
        super.onStop();

        //停止RxJava定时器
        observableDisposable.dispose();
        observableDisposable=null;
    }

    //创建并初始化联系人页面，返回这个页面
    private View createContactsPage() {
        //创建View
        View v = getLayoutInflater().inflate(R.layout.contacts_page_layout, null);

        //向树中添加节点

        //创建组们，组是树的根节点，它们的父节点为null
        ContactsPageListAdapter.GroupInfo group1 = new ContactsPageListAdapter.GroupInfo("特别关心", 0);
        ContactsPageListAdapter.GroupInfo group2 = new ContactsPageListAdapter.GroupInfo("我的好友", 0);
        ContactsPageListAdapter.GroupInfo group3 = new ContactsPageListAdapter.GroupInfo("朋友", 0);
        ContactsPageListAdapter.GroupInfo group4 = new ContactsPageListAdapter.GroupInfo("家人", 0);
        ContactsPageListAdapter.GroupInfo group5 = new ContactsPageListAdapter.GroupInfo("同学", 0);

        groupNode1 = tree.addNode(null, group1, R.layout.contacts_group_item);
        groupNode2 = tree.addNode(null, group2, R.layout.contacts_group_item);
        groupNode3 = tree.addNode(null, group3, R.layout.contacts_group_item);
        groupNode4 = tree.addNode(null, group4, R.layout.contacts_group_item);
        groupNode5 = tree.addNode(null, group5, R.layout.contacts_group_item);
        Log.d("fzj", "tree add");
        //获取页面里的RecyclerView，为它创建Adapter
        RecyclerView recyclerView = v.findViewById(R.id.contactListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        contactsAdapter = new ContactsPageListAdapter(tree);
        recyclerView.setAdapter(contactsAdapter);

        //响应假搜索控件的点击事件，显示搜索页面
        View fakeSearchView = v.findViewById(R.id.searchViewStub);
        fakeSearchView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), SearchActivity.class);
                startActivity(intent);
            }
        });
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof FragmentListener) {
            fragmentListener = (FragmentListener) context;
        }
    }

    @Override
    public void onDestroyView() {
        Log.d("fzj", "onDestroyView");
        super.onDestroyView();
        tree.removeNode(groupNode1);
        tree.removeNode(groupNode2);
        tree.removeNode(groupNode3);
        tree.removeNode(groupNode4);
        tree.removeNode(groupNode5);
        contactsAdapter = null;
        fragmentListener = null;
    }

    //为ViewPager派生一个适配器类
    class ViewPageAdapter extends PagerAdapter {
        //构造方法
        ViewPageAdapter() {
        }

        @Override
        public int getCount() {
            return listViews.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

        //实例化一个子View，container是子View容器，就是ViewPager，
        //position是当前的页数，从0开始计
        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            View v = listViews[position];
            //必须加入容器中
            container.addView(v);
            return v;
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            container.removeView((View) object);
        }

        //返回每一页的标题，参数是页号，从0开始
        @Override
        public CharSequence getPageTitle(int position) {
            if (position == 0) {
                return makeTabItemTitle("消息", R.drawable.message_normal);
            } else if (position == 1) {
                return makeTabItemTitle("联系人", R.drawable.contacts_normal);
            } else if (position == 2) {
                return makeTabItemTitle("动态", R.drawable.space_normal);
            }
            return null;
        }

        //为参数title中的字符串前面加上iconResId所引用图像
        public CharSequence makeTabItemTitle(String title, int iconResId) {
            Drawable image = getResources().getDrawable(iconResId);
            image.setBounds(0, 0, 40, 40);
            // Replace blank spaces with image icon
            SpannableString sb = new SpannableString(" \n" + title);
            ImageSpan imageSpan = new ImageSpan(image, ImageSpan.ALIGN_BASELINE);
            sb.setSpan(imageSpan, 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            return sb;
        }
    }

}

