package com.yun9.service.tax.core;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

/**
 * @author lvpanfeng
 * @version 1.0
 * @since 2018-06-13 18:02
 */
@Component("serviceTaxThreadPoolContainer")
public class ServiceTaxThreadPoolContainer implements DisposableBean, InitializingBean {
    private ThreadFactory namedThreadFactory;
    private ExecutorService singleThreadPool;
    private ExecutorService eventThreadPool;
    private ExecutorService multTaskThreadPool;


    @Override
    public void destroy() throws Exception {
        singleThreadPool.shutdown();
        multTaskThreadPool.shutdown();
        eventThreadPool.shutdown();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        namedThreadFactory = new ThreadFactoryBuilder()
                .setNameFormat("yun9-Thread-pool-%d").build();
        //单线程池初始化
        singleThreadPool = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        //回调处理线程池初始化
        eventThreadPool = new ThreadPoolExecutor(5, 20,
                30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(1024), namedThreadFactory, new ThreadPoolExecutor.AbortPolicy());
        // 批量任务线程池
        //回调处理线程池初始化
        multTaskThreadPool = new ThreadPoolExecutor(5, 30,
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(), namedThreadFactory);


    }

    public ExecutorService getSingleThreadPool() {
        return singleThreadPool;
    }

    public ExecutorService getEventThreadPool() {
        return eventThreadPool;
    }

    public ExecutorService getMultTaskThreadPool() {
        return multTaskThreadPool;
    }


}
