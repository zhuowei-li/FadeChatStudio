package com.example.qq;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ProgressBar;

import com.example.qq.Service.ChatService;
import com.example.qq.Service.FragmentListener;
import com.example.qq.adapter.ContactsPageListAdapter;
import com.jakewharton.rxbinding2.view.RxView;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link LoginFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class LoginFragment extends Fragment {
    private PopupWindow popupDialog;
    private ConstraintLayout layoutContext;//正常内容部分
    private LinearLayout layoutHistory;//历史菜单部分
    private EditText editTextQQNum;
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public LoginFragment() {
        // Required empty public constructor
    }


    private FragmentListener fragmentListener;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentListener){
            fragmentListener=(FragmentListener)context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentListener=null;
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment LoginFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static LoginFragment newInstance(String param1, String param2) {
        LoginFragment fragment = new LoginFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @SuppressLint("CheckResult")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v=inflater.inflate(R.layout.fragment_login,container,false);
        layoutContext=v.findViewById(R.id.layoutContext);
        layoutHistory=v.findViewById(R.id.layoutHistory);
        editTextQQNum = v.findViewById(R.id.editTextQQNum);
        v.findViewById(R.id.textViewHistory).setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
              layoutContext.setVisibility(View.INVISIBLE);
              layoutHistory.setVisibility(View.VISIBLE);
              //创建三条菜单项，添加到layoutHistory中
                @SuppressLint("InflateParams")
                View layoutItem= Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.login_history_item,null);
                layoutHistory.addView(layoutItem);
                layoutItem= Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.login_history_item,null);
                layoutHistory.addView(layoutItem);
                layoutItem= Objects.requireNonNull(getActivity()).getLayoutInflater().inflate(R.layout.login_history_item,null);
                layoutHistory.addView(layoutItem);
                //响应菜单项的点击，把它里面的信息填到输入框中。
                layoutItem.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        editTextQQNum.setText("123384328943894893");
                        layoutContext.setVisibility(View.VISIBLE);
                        layoutHistory.setVisibility(View.INVISIBLE);
                    }
                });
                //使用动画显示历史记录
                AnimationSet set=(AnimationSet) AnimationUtils.loadAnimation(
                        getContext(),R.anim.login_history_anim);
                layoutHistory.startAnimation(set);
            }
                                                                }

        );
        //当点击菜单以外的区域时，把历史菜单隐藏
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutHistory.getVisibility()==View.VISIBLE) {
                    layoutContext.setVisibility(View.VISIBLE);
                    layoutHistory.setVisibility(View.INVISIBLE);
                }
            }
        });
        //响应登陆按钮的点击事件
        View buttonLogin = v.findViewById(R.id.buttonLogin);
        RxView.clicks(buttonLogin)
              .throttleFirst(10, TimeUnit.SECONDS)//防止按钮重复点击
              .subscribe(obj->{
            //切换页面需要判断是否登录成功
            //取出用户名，向服务端发出登录请求
            String username = editTextQQNum.getText().toString();
            //Retrofit创建实例并根据接口使用动态代理技术
            ChatService service =
                    fragmentListener.getRetrofit().create(ChatService.class);
            Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable =
                    service.requestLogin(username,null);
            observable.map(result->{
                //判断服务器是否正确返回
                if(result.getRetCode()==0){
                    return result.getData();
                }else{
                    //服务器出错
                    throw new RuntimeException(result.getErrMsg());
                }
            })
            .subscribeOn(Schedulers.computation())
            .observeOn(AndroidSchedulers.mainThread())
            .doFinally(()->hideProgressBar())
            .subscribe(new Observer<ContactsPageListAdapter.ContactInfo>(){

                @Override
                public void onSubscribe(@NonNull Disposable d) {
                    //准备好进度条
                    showProgressBar();
                }

                @Override
                public void onNext(@NonNull ContactsPageListAdapter.ContactInfo contactInfo) {
                    //保存我的信息
                    MainActivity.myInfo= contactInfo;
                    //无错误，进入主页面
                    FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    MainFragment fragment = new MainFragment();
                    //替换掉FrameLayout也就是主活动中的布局里的登录碎片
                    fragmentTransaction.replace(R.id.fragment_container,fragment);
                    fragmentTransaction.addToBackStack("login");
                    fragmentTransaction.commit();
                }

                @Override
                public void onError(@NonNull Throwable e) {
                    //无错误，进入主页面
                    FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
                    FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                    MainFragment fragment = new MainFragment();
                    //替换掉FrameLayout也就是主活动中的布局里的登录碎片
                    fragmentTransaction.replace(R.id.fragment_container,fragment);
                    fragmentTransaction.addToBackStack("login");
                    fragmentTransaction.commit();
                }

                @Override
                public void onComplete() {

                }
            });
        });

        return v;
    }

    //显示进度条的代码
    private void showProgressBar(){
        //在PopWindow上显示进度条
        //进度条
        ProgressBar progressBar = new ProgressBar(getContext());
        //设置窗口覆盖父控件的范围 防止用户多次点击登录按钮
       popupDialog = new PopupWindow(progressBar, ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha=0.4f;
        getActivity().getWindow().setAttributes(lp);
        //显示进度条窗口
        popupDialog.showAtLocation(layoutContext, Gravity.CENTER,0,0);
    }

    //隐藏进度条代码
    private void hideProgressBar(){
        popupDialog.dismiss();
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha=1f;
        getActivity().getWindow().setAttributes(lp);

    }
}