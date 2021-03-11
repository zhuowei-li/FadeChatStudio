package com.example.qq.Service;


import java.util.List;

import io.reactivex.Observable;
import com.example.qq.Message;
import com.example.qq.ServerResult;
import com.example.qq.adapter.ContactsPageListAdapter;
import okhttp3.MultipartBody;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Query;

public interface ChatService {
    @GET("/apis/login")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestLogin(
            @Query("name") String name,
            @Query("password") String password);

    @GET("/apis/get_contacts")
    Observable<ServerResult<List<ContactsPageListAdapter.ContactInfo>>> getContacts();

    @POST("/apis/upload_message")
    Observable<ServerResult> uploadMessage(@Body Message msg);

    @Multipart
    @POST("/apis/register")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestRegister(
            @Part MultipartBody.Part fileData,
            @Query("name")String name,
            @Query("password") String password);

    //获取从index开始的所有的消息
    @GET("/apis/get_messages")
    Observable<ServerResult<List<Message>>> getMessagesFromIndex(@Query("from") int index);
}
