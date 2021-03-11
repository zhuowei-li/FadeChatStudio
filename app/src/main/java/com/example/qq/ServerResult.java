package com.example.qq;

public class ServerResult<T> {
    //等于0表示无错误。无错时errMsg没有值
    private int retCode;
    //出错时的信息
    private String errMsg;
    //返回的数据，由T决定
    private  T data;

    public ServerResult(int retCode){
        this.retCode=retCode;
    }

    public ServerResult(int retCode, String errMsg) {
        this.retCode = retCode;
        this.errMsg = errMsg;
    }

    public ServerResult(int retCode, String errMsg, T data) {
        this.retCode = retCode;
        this.errMsg = errMsg;
        this.data = data;
    }

    public int getRetCode() {
        return retCode;
    }

    public void setRetCode(int retCode) {
        this.retCode = retCode;
    }

    public String getErrMsg() {
        return errMsg;
    }

    public void setErrMsg(String errMsg) {
        this.errMsg = errMsg;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

}
