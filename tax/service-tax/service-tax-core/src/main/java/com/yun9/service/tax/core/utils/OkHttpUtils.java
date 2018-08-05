package com.yun9.service.tax.core.utils;

import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.concurrent.TimeUnit;

public class OkHttpUtils {
    private static OkHttpClient client;
    public static OkHttpClient getClient() {
        if (client == null) {
            synchronized (OkHttpUtils.class) {
                if (client == null) {
                    ConnectionPool pool = new ConnectionPool(50, 10, TimeUnit.MINUTES);
                    client = new OkHttpClient.Builder()
                            // TODO 遗留的坑，默认使用web01进行代理
                            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress("172.31.100.157", 38080)))
                            .retryOnConnectionFailure(true)
                            .readTimeout(2, TimeUnit.MINUTES)
                            .writeTimeout(3, TimeUnit.MINUTES)
                            .connectionPool(pool)
                            .connectTimeout(3, TimeUnit.MINUTES) //
                          .build();
                }
            }
        }
        return client;
    }

}
