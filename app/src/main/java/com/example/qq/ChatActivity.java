package com.example.qq;

import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import com.example.qq.Service.ChatService;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity {

    //存放所有的聊天消息
    private List<Message> chatMessages = new ArrayList<>();

    //用于网络通讯
    private Retrofit retrofit;
    private ChatService chatService;
    private Disposable uploadDisposable;//用于上传时取消请求
    private Disposable downloadDisposable;//用于下载时取消请求

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //设置Layout
        setContentView(R.layout.activity_chat);
        //设置动作栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        //获取启动此Activity时传过来的数据
        //在启动聊天界面时，通过此方式把对方的名字传过来
        String contactName = getIntent().getStringExtra("contact_name");
        if (contactName != null) {
            toolbar.setTitle(contactName);
        }

        setSupportActionBar(toolbar);
        //设置显示动作栏上的返回图标
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //获取Recycler控件并设置适配器
        final RecyclerView recyclerView = findViewById(R.id.chatMessageListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new ChatMessagesAdapter());

        //响应按钮的点击，发出消息
        findViewById(R.id.buttonSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //现在还不能真正发出消息，把消息放在chatMessages中，显示出来即可
                //从EditText控件取得消息
                EditText editText = findViewById(R.id.editMessage);
                String msg = editText.getText().toString();

                //创建消息对象，准备上传
                Message chatMessage = new Message(MainActivity.myInfo.getName(),
                        new Date().getTime(), msg);

                //上传到服务端
                Observable<ServerResult> observable = chatService.uploadMessage(chatMessage);
                observable.retry().map(result -> {
                    //判断服务端是否正确返回
                    if (result.getRetCode() == 0) {
                        //服务端无错误，随便返回点东西吧，反正也不用处理
                        return 0;
                    } else {
                        //服务端出错了，抛出异常，在Observer中捕获之
                        throw new RuntimeException(result.getErrMsg());
                    }
                }).subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Object>() {//onNext()
                            @Override
                            public void accept(Object data) throws Exception {
                                //对应onNext()，但是什么也不需要做
                            }
                        }, new Consumer<Throwable>() {//onError()
                            @Override
                            public void accept(Throwable e) throws Exception {
                                //对应onError()，向用户提示错误
                                String errmsg = e.getLocalizedMessage();
                                Snackbar.make(view, "大王祸事了：" + errmsg, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }, new Action() { //onComplete()
                            @Override
                            public void run() throws Exception {

                            }
                        }, new Consumer<Disposable>() { //onSubcribe()
                            @Override
                            public void accept(Disposable disposable) throws Exception {
                                //保存下disposable以取消订阅
                                uploadDisposable = disposable;
                            }
                        });

                //添加到集合中，从而能在RecyclerView中显示
                chatMessages.add(chatMessage);
                //在view中显示出来。通知RecyclerView，更新一行
                recyclerView.getAdapter().notifyItemInserted(chatMessages.size() - 1);
                //让RecyclerView向下滚动，以显示最新的消息
                recyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        });

        //创建Retrofit对象
        retrofit = new Retrofit.Builder()
                .baseUrl(MainActivity.serverHostURL)
                //本来接口方法返回的是Call，由于现在返回类型变成了Observable，
                //所以必须设置Call适配器将Observable与Call结合起来
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                //Json数据自动转换
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        chatService = retrofit.create(ChatService.class);

        //每隔2秒向服务端获取一下新的聊天消息
        Observable.interval(2, TimeUnit.SECONDS).flatMap(v -> {

            //创建获取聊天消息的Observable
            //参数是下一坨Message的起始Index
            return chatService.getMessagesFromIndex(chatMessages.size())
                    .map(result -> {
                        //判断服务端是否正确返回
                        if (result.getRetCode() == 0) {
                            //服务端无错误，随便返回点东西吧，反正也不用处理
                            return result.getData();
                        } else {
                            //服务端出错了，抛出异常，在Observer中捕获之
                            throw new RuntimeException(result.getErrMsg());
                        }
                    });

        }).retry()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Message>>() {//onNext()
                    @Override
                    public void accept(List<Message> messages) throws Exception {
                        //将消息显示在RecyclerView中
                        chatMessages.addAll(messages);
                        //在view中显示出来。通知RecyclerView，更新一行
                        recyclerView.getAdapter().notifyItemRangeInserted(
                                chatMessages.size(),chatMessages.size());
                        //让RecyclerView向下滚动，以显示最新的消息
                        recyclerView.scrollToPosition(chatMessages.size() - 1);
                    }
                }, new Consumer<Throwable>() {//onError()
                    @Override
                    public void accept(Throwable e) throws Exception {
                        //反正要重试，什么也不做了
                        Log.e("chatactivity",e.getLocalizedMessage());
                    }
                }, new Action() { //onComplete()
                    @Override
                    public void run() throws Exception {

                    }
                }, new Consumer<Disposable>() { //onSubcribe()
                    @Override
                    public void accept(Disposable disposable) throws Exception {
                        //保存下downloadDisposable以取消订阅
                        downloadDisposable = disposable;
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(uploadDisposable!=null) {
            uploadDisposable.dispose();
            uploadDisposable = null;
        }

        if(downloadDisposable!=null) {
            downloadDisposable.dispose();
            downloadDisposable = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            //当点击动作栏上的返回图标时执行
            //关闭自己，返回来时的页面
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    //为RecyclerView提供数据的适配器
    public class ChatMessagesAdapter extends
            RecyclerView.Adapter<ChatMessagesAdapter.MyViewHolder> {

        @Override
        public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            //参数viewType即行的Layout资源Id，由getItemViewType()的返回值决定的
            View itemView = getLayoutInflater().inflate(viewType, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(MyViewHolder holder, int position) {
            Message message = chatMessages.get(position);
            holder.textView.setText(message.getContent());
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        //有两种行layout，所以Override此方法
        @Override
        public int getItemViewType(int position) {
            Message message = chatMessages.get(position);
            if (message.getContactName().equals(MainActivity.myInfo.getName())) {
                //如果是我的，靠右显示
                return R.layout.chat_message_right_item;
            } else {
                //对方的，靠左显示
                return R.layout.chat_message_left_item;
            }
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private TextView textView;
            private ImageView imageView;

            public MyViewHolder(View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }
}
