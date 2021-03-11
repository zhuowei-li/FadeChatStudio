package com.example.qq.Service;

import retrofit2.Retrofit;

//Activity实现此接口，为Fragment提供服务
public interface FragmentListener {
    Retrofit getRetrofit();
    void showServerAddressSetDlg();
}
