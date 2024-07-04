package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;

public interface TaskService {
    /**
     * 添加任务的方法
     * @param task
     */
    void saveTask(Task task);

    /**
     * 消费任务的方法
     * @param taskType
     * @param priority
     */
    Task pullTask(Integer taskType, Integer priority);

    /**
     * 定时任务，不会被别人调用，没有参数，没有返回值
     */
    void refreshZsetToList();


    /**
     * 定时任务，不会被别人调用，没有参数，没有返回值
     */
    void syncDbToRedis();
}
