package com.heima.schedule.client;

import com.heima.model.schedule.dtos.Task;
import com.heima.schedule.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/task")
public class ScheduleClient {

    @Autowired
    private TaskService taskService;

    /**
     * 添加任务的方法
     * @param task
     */
    @PostMapping("/add")
    public void saveTask(@RequestBody Task task){
        taskService.saveTask(task);
    }

    /**
     * 消费任务的方法
     * @param taskType
     * @param priority
     */
    @GetMapping("/poll/{type}/{priority}")
    public Task pullTask(@PathVariable("type") Integer taskType,@PathVariable Integer priority){
        Task task = taskService.pullTask(taskType, priority);
        return task;
    }
}
