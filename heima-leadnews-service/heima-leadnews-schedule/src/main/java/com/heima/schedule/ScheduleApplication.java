package com.heima.schedule;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@SpringBootApplication
@MapperScan("com.heima.schedule.mapper")
@EnableScheduling//开启定时任务
public class ScheduleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ScheduleApplication.class,args);
    }

    /**
     * 开启乐观锁拦截器
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        return interceptor;
    }

    @Bean
    public ThreadPoolTaskExecutor threadPoolTaskExecutor(){
        ThreadPoolTaskExecutor threadPoolTaskExecutor=new ThreadPoolTaskExecutor();

        //设置核心线程数
        threadPoolTaskExecutor.setCorePoolSize(5);
        //设置最大线程数
        threadPoolTaskExecutor.setMaxPoolSize(100);
        //设置线程等待超时时间
        threadPoolTaskExecutor.setKeepAliveSeconds(60);
        //设置任务等待队列的大小
        threadPoolTaskExecutor.setQueueCapacity(20);
        //设置线程池内现成的名字前缀 -----阿里编码规约推荐---- 方便后期错误调试
        threadPoolTaskExecutor.setThreadNamePrefix("myThreadPool_");
        //设置任务拒绝策略
        threadPoolTaskExecutor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        //初始化线程池
        threadPoolTaskExecutor.initialize();


        return threadPoolTaskExecutor;
    }
}
