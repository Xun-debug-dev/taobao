package com.heima.api.feign;

import com.heima.model.schedule.dtos.Task;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("leadnews-schedule")
public interface IScheduleFeginClient {

    /**
     * 添加任务的方法
     * @param task
     */
    @PostMapping("/api/v1/task/add")
    void saveTask(@RequestBody Task task);

    /**
     * 消费任务的方法
     * @param taskType
     * @param priority
     */
    @GetMapping("/api/v1/task/poll/{type}/{priority}")
    Task pullTask(@PathVariable("type") Integer taskType, @PathVariable Integer priority);
}
