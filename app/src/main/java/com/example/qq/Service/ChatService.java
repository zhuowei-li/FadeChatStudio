package com.example.qq.Service;

import com.example.qq.ChatActivity;
import com.example.qq.ServerResult;
import com.example.qq.adapter.ContactsPageListAdapter;

import java.util.List;

import io.reactivex.Observable;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

public interface ChatService {
    @GET("/apis/login")
    Observable<ServerResult<ContactsPageListAdapter.ContactInfo>> requestLogin(
            @Query("name") String name,
            @Query("password") String password);

    @GET("/apis/get_contacts")
    Observable<ServerResult<List<ContactsPageListAdapter.ContactInfo>>>
    getContacts();

    @POST("/apis/upload_message")
    Observable <ServerResult> uploadMessage(@Body ChatActivity.Message msg);

    @GET("/apis/get_message")
    Observable<ServerResult<List<ChatActivity.Message>>>getMessageFromIndex
            (@Query("after")int index);
}
