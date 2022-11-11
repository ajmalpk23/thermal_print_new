package com.example.printtest;

public interface CallBack {
    public void onSuccess(int serviceId, FormRes res);
    public void onError(int serviceId,String errorMessage);
    
}
