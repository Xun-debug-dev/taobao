package com.heima.schedule.service;

import com.heima.model.schedule.dtos.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;

@SpringBootTest
class TaskServiceTest {

    @Autowired
    private TaskService taskService;

    @Test
    void saveTask() {
        Task task=new Task();
        task.setTaskType(200);
        task.setExecuteTime(new Date().getTime());
        task.setPriority(20);
        task.setParameters("task test".getBytes());
        taskService.saveTask(task);
    }

    //添加任务
    @Test
    public void test222(){
        for (int i = 0; i <5 ; i++) {
            Task task=new Task();
            task.setTaskType(1110+i);
            task.setExecuteTime(new Date().getTime()+500*i);
            task.setPriority(530);
            task.setParameters("task test".getBytes());
           taskService.saveTask(task);
        }
    }


    @Test
    void pullTask() {
        taskService.pullTask(1110,530);
    }

    @Test
    public void test(){
        for (int i = 0; i <5 ; i++) {
            Task task=new Task();
            task.setTaskType(110+i);
            task.setExecuteTime(new Date().getTime()+500*i);
            task.setPriority(50);
            task.setParameters("task test".getBytes());
           taskService.saveTask(task);
        }
    }
}