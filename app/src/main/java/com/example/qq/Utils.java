package com.example.qq;

import android.content.Context;

import com.example.qq.adapter.ContactsPageListAdapter;

/**
 * Created by nk on 12/6/2017.
 */

public class Utils {
    //保存我自己的信息
    public static ContactsPageListAdapter.ContactInfo myInfo;

    //根据手机的分辨率从 dp 的单位 转成为 px(像素)
    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    //根据手机的分辨率从 px(像素) 的单位 转成为 dp
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
