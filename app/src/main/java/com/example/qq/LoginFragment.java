package com.example.qq;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
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
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.snackbar.Snackbar;
import com.jakewharton.rxbinding2.view.RxView;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import com.example.qq.Service.ChatService;
import com.example.qq.Service.FragmentListener;
import com.example.qq.adapter.ContactsPageListAdapter;

public class LoginFragment extends Fragment {
    private ConstraintLayout layoutContext;//正常内容部分，是一个ConstraintLayout
    private LinearLayout layoutHistory;//历史菜单部分，是一个LinearLayout
    private EditText editTextQQNum;//QQ号输个框
    private PopupWindow popupDialog;//用于显示进度条

    private FragmentListener fragmentListener;

    public LoginFragment() {
        // Required empty public constructor
    }

    @SuppressLint("CheckResult")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_login, container, false);

        layoutContext = v.findViewById(R.id.layoutContext);
        layoutHistory = v.findViewById(R.id.layoutHistory);
        editTextQQNum = v.findViewById(R.id.editTextQQNum);

        //响应下拉箭头的点击事件，弹出登录历史记录菜单
        v.findViewById(R.id.textViewHistory).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                layoutContext.setVisibility(View.INVISIBLE);
                layoutHistory.setVisibility(View.VISIBLE);

                //创建两条历史记录菜单项，添加到layoutHistory中
                for(int i=0;i<3;i++) {
                    View layoutItem = getActivity().getLayoutInflater().inflate(R.layout.login_history_item, null);
                    //响应菜单项的点击，把它里面的信息填到输入框中。
                    layoutItem.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            editTextQQNum.setText("617540836");
                            layoutContext.setVisibility(View.VISIBLE);
                            layoutHistory.setVisibility(View.INVISIBLE);
                        }
                    });
                    layoutHistory.addView(layoutItem);
                }

                //使用动画显示历史记录
                AnimationSet set = (AnimationSet) AnimationUtils.loadAnimation(
                        getContext(), R.anim.login_history_anim);
                layoutHistory.startAnimation(set);
            }
        });

        //当点击菜单项之外的区域时，把历史菜单隐藏
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(layoutHistory.getVisibility()==View.VISIBLE){
                    layoutContext.setVisibility(View.VISIBLE);
                    layoutHistory.setVisibility(View.INVISIBLE);
                }
            }
        });

        //响应登录按钮的点击事件
        View buttonLogin = v.findViewById(R.id.buttonLogin);
        RxView.clicks(buttonLogin)
                .throttleFirst(10 , TimeUnit.SECONDS)
                .subscribe(obj -> {

            //切换页面之前要先判断是否登录成功
            //取出用户名，向服务端发出登录请求。
            String username = editTextQQNum.getText().toString();
            //Retrofit跟据接口实现类并创建实例，这使用了动态代理技术，
            ChatService service = fragmentListener.getRetrofit().create(ChatService.class);
            Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable =
                    service.requestLogin(username,null);
            observable.map(result -> {
                //判断服务端是否正确返回
                if(result.getRetCode()==0) {
                    //服务端无错误，处理返回的数据
                    return result.getData();
                }else{
                    //服务端出错了，抛出异常，在Observer中捕获之
                    throw new RuntimeException(result.getErrMsg());
                }
            }).subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doFinally(() -> hideProgressBar())
                    .subscribe(new Observer<ContactsPageListAdapter.ContactInfo>(){
                        @Override
                        public void onSubscribe(Disposable d) {
                            //准备好进度条
                            showProgressBar();
                        }

                        @Override
                        public void onNext(ContactsPageListAdapter.ContactInfo contactInfo) {
                            //保存下我的信息
                            Utils.myInfo=contactInfo;
                            //无错误时执行,登录成功，进入主页面
                            FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
                            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                            MainFragment fragment = new MainFragment();
                            //替换掉FrameLayout中现有的Fragment
                            fragmentTransaction.replace(R.id.fragment_container, fragment);
                            //将这次切换放入后退栈中，这样可以在点后退键时自动返回上一个页面
                            fragmentTransaction.addToBackStack("login");
                            fragmentTransaction.commit();
                        }

                        @Override
                        public void onError(Throwable e) {
                            //在这里捕获各种异常，提示错误信息
                            String errmsg = e.getLocalizedMessage();
                            Snackbar.make(layoutContext, "大王祸事了："+errmsg, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            Log.e("qqserver",e.getLocalizedMessage());
                            //弹出Server地址设置对话框
                            fragmentListener.showServerAddressSetDlg();
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        });

        v.findViewById(R.id.textViewRegister).setOnClickListener(v1 -> {
            //启动注册Activity
            Intent intent = new Intent(getContext(),RegisterActivity.class);
            startActivity(intent);
        });


        return v;
    }

    //显示进度条
    private void showProgressBar(){
        //显示一个PopWindow，在这个Window中显示进度条
        //进度条
        ProgressBar progressBar = new ProgressBar(getContext());
        //设置进度条窗口覆盖整个父控件的范围，这样可以防止用户多次
        //点击按钮
        popupDialog = new PopupWindow(progressBar,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        //将当前主窗口变成40%半透明，以实现背景变暗效果
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha = 0.4f;
        getActivity().getWindow().setAttributes(lp);
        //显示进度条窗口
        popupDialog.showAtLocation(layoutContext, Gravity.CENTER, 0, 0);
    }

    //隐藏进度条
    private void hideProgressBar(){
        popupDialog.dismiss();
        WindowManager.LayoutParams lp = getActivity().getWindow().getAttributes();
        lp.alpha = 1f;
        getActivity().getWindow().setAttributes(lp);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if(context instanceof FragmentListener){
            fragmentListener = (FragmentListener) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentListener = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(Utils.myInfo!=null) {
            editTextQQNum.setText(Utils.myInfo.getName());
        }
    }
}
