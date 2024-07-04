package com.heima.wemedia.service.impl;

import com.heima.api.feign.IScheduleFeginClient;
import com.heima.model.common.enums.TaskTypeEnum;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.utils.common.ProtostuffUtil;
import com.heima.wemedia.service.WmAutoScanService;
import com.heima.wemedia.service.WmNewsTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@Slf4j
public class WmNewsTaskServiceImpl implements WmNewsTaskService {

    @Autowired
    private IScheduleFeginClient scheduleFeginClient;

    @Autowired
    private WmAutoScanService wmAutoScanService;
    /**
     * 添加任务到延迟队列中
     *
     * @param id          文章的id
     * @param publishTime 发布的时间  可以做为任务的执行时间
     */
    @Override
    @Async
    public void addNewsToTask(Integer id, Date publishTime) {
        //直接调用接口
        Task task=new Task();

        task.setTaskType(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType());
        task.setPriority(TaskTypeEnum.NEWS_SCAN_TIME.getPriority());
        task.setExecuteTime(publishTime.getTime());

        WmNews wmNews=new WmNews();
        wmNews.setId(id);
        byte[] serialize = ProtostuffUtil.serialize(wmNews);
        task.setParameters(serialize);

        scheduleFeginClient.saveTask(task);
    }

//    /**
//     * 消费延迟队列数据
//     * 定时任务
//     */
//    @Override
//    @Scheduled(fixedRate = 60000)//设置为50s，要不时间太短
//    public void scanNewsByTask() {
//        log.info("文章审核---消费任务执行---begin---");
//        //1.调用feign接口中的消费任务的方法
//        Task task = scheduleFeginClient.pullTask(TaskTypeEnum.NEWS_SCAN_TIME.getTaskType(), TaskTypeEnum.NEWS_SCAN_TIME.getPriority());
//        if(task!=null && task.getParameters().length>0){
//            //1.5 反序列化操作
//            byte[] parameters = task.getParameters();
//            WmNews wmNews = ProtostuffUtil.deserialize(parameters, WmNews.class);
//
//            //2.调用自动审核的逻辑业务
//            wmAutoScanService.auditWmNews(wmNews.getId());
//            log.info("文章审核---消费任务执行---end---");
//        }
//
//    }
}
