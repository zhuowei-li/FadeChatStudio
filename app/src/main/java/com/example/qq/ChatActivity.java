package com.example.qq;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.qq.Service.ChatService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class ChatActivity extends AppCompatActivity {

    //为网络想通信做准备
    private Retrofit retrofit;
    private ChatService chatService;
    private Disposable uploadDisposable;//用于上传时取消请求
    private Disposable downloadDisposable;//用于下载时取消请求

    //存放消息的类
    public class Message {
        private String contactName;//发出人的名字
        private Long time;
        private String content;

        //构造方法
        public Message(String contactName, Long time, String content) {
            this.contactName = contactName;
            this.time = time;
            this.content = content;
        }

        public String getContactName() {
            return contactName;
        }

        public void setContactName(String contactName) {
            this.contactName = contactName;
        }

        public Long getTime() {
            return time;
        }

        public void setTime(Long time) {
            this.time = time;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }

    private List<Message> chatMessages = new ArrayList<>();

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressLint("CheckResult")
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //创建Retrofit对象
        retrofit = new Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8080/")
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        chatService = retrofit.create(ChatService.class);

        //设置动作栏
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);

        String contactName = getIntent().getStringExtra("contact_name");
        if (contactName != null) {
            toolbar.setTitle(contactName);
        }

        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        RecyclerView recyclerView = findViewById(R.id.chatMessageListView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(new chatMessagesAdapter());

        //每隔两秒向服务器获取一下新的聊天记录
        Observable.interval(2, TimeUnit.SECONDS).flatMap(v -> {
            //创建获取聊天信息的Observer
            //请求聊天数据的参数是下一段Message的起始Index
            return chatService.getMessageFromIndex(chatMessages.size())
                    .map(result -> {
                        //判断服务器是否正确返回
                        if (result.getRetCode() == 0) {
                            return result.getData();
                        } else {
                            throw new RuntimeException(result.getErrMsg());
                        }
                    });
        }).retry()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<Message>>() {

                               @Override
                               public void accept(List<Message> messages) throws Exception {
                                   chatMessages.addAll(messages);
                                   //在view中显示，通知RecyclerView更新一行
                                   recyclerView.getAdapter().notifyItemRangeInserted
                                           (chatMessages.size(), chatMessages.size());
                                   //滚动显示最新消息
                                   recyclerView.scrollToPosition(chatMessages.size() - 1);
                               }
                           }, new Consumer<Throwable>() {

                               @Override
                               public void accept(Throwable throwable) {

                               }
                           }, new Action() {

                               @Override
                               public void run() throws Exception {

                               }
                           }, new Consumer<Disposable>() {

                               @Override
                               public void accept(Disposable disposable) {
                                   downloadDisposable = disposable;
                               }
                           }

                );

        //响应按钮的点击，发出消息
        findViewById(R.id.buttonSend).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //拿到输入框中的消息
                EditText editText = findViewById(R.id.editMessage);
                String msg = editText.getText().toString();

                //创建消息，准备上传
                Message chatMessage = new Message(MainActivity.myInfo.getName(),
                        new Date().getTime(), msg);

                //上传到服务端
                Observable<ServerResult> observable = chatService.uploadMessage(chatMessage);
                observable.retry().map(result -> {
                    //判断服务器是否正确返回
                    if (result.getRetCode() == 0) {
                        return 0;
                    } else {
                        //抛出异常
                        throw new RuntimeException(result.getErrMsg());
                    }
                }).subscribeOn(Schedulers.computation())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Consumer<Object>() {
                            @Override
                            public void accept(Object o) throws Exception {
                                //对应onNext,不做操作
                            }
                        }, new Consumer<Throwable>() {

                            @Override
                            public void accept(Throwable throwable) {
                                //对应onError
                                String errmsg = throwable.getLocalizedMessage();
                                Snackbar.make(view, "失败：" + errmsg, Snackbar.LENGTH_LONG)
                                        .setAction("Action", null).show();
                            }
                        }, new Action() {
                            @Override
                            public void run() throws Exception {

                            }
                            //onComplete
                        }, new Consumer<Disposable>() {
                            @Override
                            public void accept(Disposable disposable) {
                                uploadDisposable = disposable;
                            }
                        });
                //上传消息完毕，再显示消息
                chatMessages.add(chatMessage);
                //在view中显示出来
                Objects.requireNonNull(recyclerView.getAdapter()).notifyItemInserted(chatMessages.size() - 1);
                //向下滚动，显示新消息
                recyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        });

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (uploadDisposable != null) {
            uploadDisposable.dispose();
            uploadDisposable = null;
        }
        if (downloadDisposable != null) {
            downloadDisposable.dispose();
            downloadDisposable = null;
        }

    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    class chatMessagesAdapter extends RecyclerView.Adapter<chatMessagesAdapter.MyViewHolder> {

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View itemView = getLayoutInflater().inflate(viewType, parent, false);
            return new MyViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            Message message = chatMessages.get(position);
            holder.textView.setText(message.content);
        }

        @Override
        public int getItemCount() {
            return chatMessages.size();
        }

        @Override
        public int getItemViewType(int position) {
            Message message = chatMessages.get(position);
            if (message.getContactName().equals(MainActivity.myInfo.getName())) {
                return R.layout.chat_message_right_item;
            } else {
                return R.layout.chat_message_left_item;
            }
        }

        class MyViewHolder extends RecyclerView.ViewHolder {
            private TextView textView;
            private ImageView imageView;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                textView = itemView.findViewById(R.id.textView);
                imageView = itemView.findViewById(R.id.imageView);
            }
        }
    }
}
