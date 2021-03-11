package com.example.qq;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.BottomSheetDialog;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import com.example.qq.Service.ChatService;
import com.example.qq.adapter.ContactsPageListAdapter;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;

public class RegisterActivity extends AppCompatActivity {

    public static final int TAKE_PHOTO = 1; //拍照
    public static final int SELECT_PHOTO = 2;//从图库选择
    public static final int CROP_PHOTO = 3;//剪切编辑
    public static final int ASK_PERMISSIONS = 4;//请求权限

    private Retrofit retrofit;

    private Uri imageUri;//所选图像的URI
    private ImageView imageViewAvatar;

    //buttom sheet dialog for pickpig photo
    private BottomSheetDialog sheetDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //响应头像点击，弹出菜单，让用户选择允何种方式获得头像
        this.imageViewAvatar = findViewById(R.id.imageViewAvatar);
        imageViewAvatar.setOnClickListener(v -> {
            sheetDialog = new BottomSheetDialog(RegisterActivity.this);
            View view = getLayoutInflater().inflate(R.layout.image_pick_sheet_menu, null);
            sheetDialog.setContentView(view);
            sheetDialog.show();
            //响应菜单项的选择
            view.findViewById(R.id.sheetItemTakePhoto).setOnClickListener(v1 -> {
                //从相机中获取

                //需要申请的权限
                List<String> permissionsList = new ArrayList<String>();
                //拍照,先检查是否有权限
                if (ActivityCompat.checkSelfPermission(RegisterActivity.this,
                        Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    // Permission is not granted
                    permissionsList.add(Manifest.permission.CAMERA);
                }
                if (ActivityCompat.checkSelfPermission(RegisterActivity.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    //Permission is not granted
                    permissionsList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                }

                if (permissionsList.isEmpty()) {
                    //不用申请权限了，直接显示拍照页面
                    showTackPhotoView();
                }else{
                    //还需先申请权限
                    ActivityCompat.requestPermissions(RegisterActivity.this,
                            permissionsList.toArray(new String[permissionsList.size()]),
                            ASK_PERMISSIONS);
                }
            });

            view.findViewById(R.id.sheetItemSelectPicture).setOnClickListener(v1 -> {
                //从图库中选
            });

            view.findViewById(R.id.sheetItemCancel).setOnClickListener(v1 -> {
                //隐藏SheetMenu
                sheetDialog.dismiss();
            });
        });

        //点击了提交按钮，注册之
        findViewById(R.id.buttonCommit).setOnClickListener(v1 -> {
            //Retrofit跟据接口实现类并创建实例，这使用了动态代理技术，
            ChatService chatService = getRetrofit().create(ChatService.class);
            //产生文件Part和文本Part
            MultipartBody.Part filePart = createFilePart();
            TextView tvName = findViewById(R.id.editTextName);
            TextView tvPassword = findViewById(R.id.editTextPassword);
            String name = tvName.getText().toString();
            String password = tvPassword.getText().toString();
            Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> observable =
                    chatService.requestRegister(filePart,name,password);

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
                    .subscribe(new Observer<ContactsPageListAdapter.ContactInfo>(){
                        @Override
                        public void onSubscribe(Disposable d) {
                        }

                        @Override
                        public void onNext(ContactsPageListAdapter.ContactInfo contactInfo) {
                            //保存下我的信息
                            MainActivity.myInfo = contactInfo;

                            //提示用户注册成功
                            Snackbar.make(v1, "注册成功！", Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            //关闭Activity
                            Intent intent = new Intent();
                            setResult(Activity.RESULT_OK);
                            finish();
                        }

                        @Override
                        public void onError(Throwable e) {
                            //在这里捕获各种异常，提示错误信息
                            String errmsg = e.getLocalizedMessage();
                            Snackbar.make(v1, "大王祸事了："+errmsg, Snackbar.LENGTH_LONG)
                                    .setAction("Action", null).show();
                            Log.e("qqserver",e.getLocalizedMessage());
                            //弹出Server地址设置对话框
                            showServerAddressSetDlg();
                        }

                        @Override
                        public void onComplete() {

                        }
                    });
        });
    }

    public Retrofit getRetrofit() {
        if(retrofit==null){
            //从本地读取server host name，
            SharedPreferences preferences=getApplicationContext().getSharedPreferences(
                    "qqapp", MODE_PRIVATE);
            String serverHost = preferences.getString("server_addr", "");
            if (serverHost.isEmpty()){
                //弹出输入对话框，让用户设置server地址
                showServerAddressSetDlg();
            }else {
                //创建Retrofit对象
                retrofit = new Retrofit.Builder()
                        .baseUrl(serverHost)
                        //本来接口方法返回的是Call，由于现在返回类型变成了Observable，
                        //所以必须设置Call适配器将Observable与Call结合起来
                        .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                        //Json数据自动转换
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
        }
        return retrofit;
    }

    public MultipartBody.Part createFilePart() {
        if(this.imageUri==null){
            //必须有个Part才行，所以创建一个吧
            return MultipartBody.Part.createFormData("none", "none");
        }

        InputStream inputStream = null;
        byte[] data=null;
        try {
            inputStream = getContentResolver().openInputStream(this.imageUri);
            data=new byte[inputStream.available()];
            inputStream.read(data);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("application/otcet-stream"), data);
        MultipartBody.Part body = MultipartBody.Part.createFormData("file", "png", requestFile);
        return body;
    }


    private void showTackPhotoView() {
        File imageOutputFile = generateOutPutFile(Environment.DIRECTORY_DCIM);
        this.imageUri = FileProvider.getUriForFile(this,
                "com.example.qq.fileprovider", imageOutputFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE); //照相
        intent.putExtra(MediaStore.EXTRA_OUTPUT, this.imageUri); //指定图片输出地址
        startActivityForResult(intent, TAKE_PHOTO); //启动照相
        //隐藏底部的SheetMenu
        sheetDialog.dismiss();
    }

    private File generateOutPutFile(String pathInExternalStorage){
        //图片名称 时间命名
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        Date date = new Date(System.currentTimeMillis());
        String photoFileName = format.format(date)+".png";
        //创建File对象用于存储拍照的图片 SD卡根目录
        //File outputImage = new File(Environment.getExternalStorageDirectory(),test.jpg);
        //存储至DCIM文件夹
        File path = Environment.getExternalStoragePublicDirectory(pathInExternalStorage);
        File outputImage = new File(path, photoFileName );
        try {
            if (outputImage.exists()) {
                outputImage.delete();
            }
            outputImage.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return outputImage;
    }

    public void showServerAddressSetDlg(){
        //弹出输入对话框，让用户设置server地址
        EditText editText = new EditText(this);
        editText.setText("172.20.10.5:8080");
        AlertDialog.Builder inputDialog = new AlertDialog.Builder(this);
        inputDialog.setTitle("请输入服务器地址").setView(editText);
        inputDialog.setPositiveButton("确定",
                (dialog, which) -> {
                    String serverHostURL=editText.getText().toString();
                    //将服务端地址保存到本地
                    SharedPreferences preferences= getApplicationContext().getSharedPreferences("qqapp", MODE_PRIVATE);
                    SharedPreferences.Editor edit = preferences.edit();
                    edit.putString("server_addr",serverHostURL);
                    edit.commit();
                    //创建Retrofit对象
                    retrofit = new Retrofit.Builder()
                            .baseUrl(serverHostURL)
                            //本来接口方法返回的是Call，由于现在返回类型变成了Observable，
                            //所以必须设置Call适配器将Observable与Call结合起来
                            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                            //Json数据自动转换
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                }).show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case TAKE_PHOTO:
                Intent intent = new Intent( "com.android.camera.action.CROP"); //剪裁
                //告诉剪裁Activity，要申请对Uri的读权限
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.setDataAndType(this.imageUri, "image/*");
                intent.putExtra("scale", "true");
                intent.putExtra("crop", "true");
                //设置宽高比例
                intent.putExtra("aspectX", 1);
                intent.putExtra("aspectY", 1);
                //设置裁剪图片宽高
                intent.putExtra("outputX", 340);
                intent.putExtra("outputY", 340);

                //产生写出文件并获取Uri，注意！新版API不允许读和写是同一个文件
                File finalImage = generateOutPutFile(Environment.DIRECTORY_DCIM);
                //Uri imageUri = FileProvider.getUriForFile(this,"com.example.qq.fileprovider", finalImage);
                //写出的Uri不能是FileProvier形式的，Activity不支持！！！！
                Uri imageUri = Uri.fromFile(finalImage);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
//                grantUriPermission("com.android.camera",
//                        imageUri,
//                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                this.imageUri = imageUri;
                startActivityForResult(intent, CROP_PHOTO); //设置裁剪参数显示图片至ImageView
                break;
            case SELECT_PHOTO:
                //TODO:请自行实现
                break;
            case CROP_PHOTO:
                try {
                    //图片解析成Bitmap对象
                    Bitmap bitmap = BitmapFactory.decodeStream(
                            getContentResolver().openInputStream(this.imageUri));
                    //将剪裁后照片显示出来
                    this.imageViewAvatar.setImageBitmap(bitmap);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case ASK_PERMISSIONS:
                int i=0;
                for (; i<permissions.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this,
                                "权限申请被拒绝，无法完成照片选择。",
                                Toast.LENGTH_SHORT).show();
                        break;
                    }
                }
                break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        retrofit = null;
    }
}
