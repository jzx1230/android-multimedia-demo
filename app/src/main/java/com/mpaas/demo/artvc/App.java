package com.mpaas.demo.artvc;

import android.app.Application;

import com.alipay.mobile.framework.quinoxless.IInitCallback;
import com.alipay.mobile.framework.quinoxless.QuinoxlessFramework;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        QuinoxlessFramework.setup(this, new IInitCallback() {
            @Override
            public void onPostInit() {

            }
        });
        QuinoxlessFramework.init();
    }
}
