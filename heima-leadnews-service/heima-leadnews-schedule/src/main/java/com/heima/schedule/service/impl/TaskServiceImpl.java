package com.heima.schedule.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.redis.RedisCacheService;
import com.heima.model.common.constants.ScheduleConstants;
import com.heima.model.schedule.dtos.Task;
import com.heima.model.schedule.pojos.Taskinfo;
import com.heima.model.schedule.pojos.TaskinfoLogs;
import com.heima.schedule.mapper.TaskInfoLogsMapper;
import com.heima.schedule.mapper.TaskInfoMapper;
import com.heima.schedule.service.TaskService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

@Service
@Slf4j
public class TaskServiceImpl implements TaskService {

    @Autowired
    private TaskInfoMapper taskInfoMapper;

    @Autowired
    private TaskInfoLogsMapper taskInfoLogsMapper;

    @Autowired
    private RedisCacheService redisCacheService;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    /**
     * 添加任务的方法
     *
     * @param task
     */
    @Override
    @Transactional
    public void saveTask(Task task) {
        threadPoolTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                //1.添加任务到数据库表中
                Boolean flag = saveToDB(task);
                if(flag){
                    saveToRedis(task);
                }
            }
        });

    }

    /**
     * 新增数据到redis中
     * @param task
     */
    private void saveToRedis(Task task) {
        //2.添加任务到redis中
        String suffix_key = task.getTaskType() + "_" + task.getPriority();
        //2.1 如果任务执行时间小于等于当前系统时间 存入list队列中
        String listKey=ScheduleConstants.TOPIC+suffix_key;
        if(task.getExecuteTime()<=System.currentTimeMillis()){
            redisCacheService.lLeftPush(listKey, JSON.toJSONString(task));
        }
        //2.2 如果任务执行时间大于当前系统时间，并小于等于预设时间(未来5分钟)存入到zset队列中
        String zsetKey=ScheduleConstants.FUTURE+suffix_key;
        //2.2.1 获取未来5分钟的时间毫秒值
        //获取日历对象
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,5);//获取未来5分钟的时间
        long time=calendar.getTimeInMillis();
        if(task.getExecuteTime()> System.currentTimeMillis() && task.getExecuteTime()<=time){
            redisCacheService.zAdd(zsetKey,JSON.toJSONString(task), task.getExecuteTime());
        }
    }

    /**
     * 新增数据到数据库中
     * @param task
     */
    private Boolean saveToDB(Task task) {
        //添加任务表
        Taskinfo taskInfo=new Taskinfo();
        BeanUtils.copyProperties(task,taskInfo);
        taskInfo.setExecuteTime(new Date(task.getExecuteTime()));
        taskInfoMapper.insert(taskInfo);

        //要给task对象赋值任务id
        task.setTaskId(taskInfo.getTaskId());


        //添加任务日志表
        TaskinfoLogs taskinfoLogs=new TaskinfoLogs();
        BeanUtils.copyProperties(taskInfo,taskinfoLogs);
        taskinfoLogs.setVersion(1);
        taskinfoLogs.setStatus(ScheduleConstants.SCHEDULED);
        int insert = taskInfoLogsMapper.insert(taskinfoLogs);
        return insert==1;
    }

    /**
     * 消费任务的方法
     *
     * @param taskType
     * @param priority
     */
    @Override
    public Task pullTask(Integer taskType, Integer priority) {

        Future<Task> future = threadPoolTaskExecutor.submit(new Callable<Task>() {
            @Override
            public Task call() throws Exception {
                String suffix_key = taskType + "_" + priority;
                String listKey=ScheduleConstants.TOPIC+suffix_key;
                //1.从redis的list队列中删除数据
                String taskStr = redisCacheService.lLeftPop(listKey);
                Task task =null;
                if(StringUtils.isNotBlank(taskStr)){
                    //2.删除任务表以及修改任务日志表的状态
                    //2.1 转成对象
                    task = JSONObject.parseObject(taskStr, Task.class);
                    //2.2 删除任务表
                    taskInfoMapper.deleteById(task.getTaskId());
                    //2.3 修改任务日志表
                    TaskinfoLogs taskinfoLogs = taskInfoLogsMapper.selectById(task.getTaskId());
                    taskinfoLogs.setStatus(ScheduleConstants.EXECUTED);
                    //注意：此时修改的时候mp会自动对version+1操作
                    taskInfoLogsMapper.updateById(taskinfoLogs);
                }
                return task;
            }
        });

        try {
            return  future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 定时任务，不会被别人调用，没有参数，没有返回值
     */
    @Override
    //@Scheduled(cron = "0 0/1 * * * ?")
    @Scheduled(cron = "*/1 * * * * ?") //换成每秒中执行一次，方便测试
    public void refreshZsetToList() {

        threadPoolTaskExecutor.execute(new Runnable() {
            @Override
            public void run() {
                 /*  String token = redisCacheService.tryLock("FUTURE_TASK_SYNC", 1000 * 30);
        //表示锁没有被占用
        if(StringUtils.isNotBlank(token)){*/
                System.out.println(System.currentTimeMillis() / 1000 + "执行了定时任务");


                try {
                    //加入休眠时间
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }


                //1.获取未来数据的所有key
                Set<String> futureKeys = redisCacheService.scan(ScheduleConstants.FUTURE + "*");

                //2.判断数据有哪些数据到期了
                if(!futureKeys.isEmpty()){
                    for (String futureKey : futureKeys) {
                        //查询到期的数据,对应的value值得集合
                        Set<String> daoqiSet = redisCacheService.zRangeByScore(futureKey, 0, System.currentTimeMillis());
                        if(!daoqiSet.isEmpty()){
                            /**
                             *  3.执行同步操作
                             *  refreshWithPipeline(String future_key,String topic_key,Collection<String> values)
                             *  参数1表示未来数据的key
                             *  参数2表示list队列的key
                             *  参数3表示对应的数据value集合
                             */
                            String[] split = futureKey.split(ScheduleConstants.FUTURE);
                            String suffixKey=split[1];
                            String topicKey=ScheduleConstants.TOPIC+suffixKey;
                            redisCacheService.refreshWithPipeline(futureKey,topicKey,daoqiSet);
                            System.out.println("成功的将" + futureKey + "下的当前需要执行的任务数据刷新到" + topicKey + "下");
                        }
                    }
                }
                // }
            }
        });
    }

    /**
     * 定时同步任务，不会被别人调用，没有参数，没有返回值
     */
    @Override
    @Scheduled(cron = "0 */5 * * * ?")
    public void syncDbToRedis() {
        /**
         * 1.清理redis中的缓存是有争议的，暂时先不做
         * 如果定时同步的时间比如是半个小时执行一次,name定时刷新是每分钟执行一次，数据重复的几率很低
         */
        //2.查询mysql数据taskinfo表中的数据，小于等于未来五分钟的数据
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE,5);//获取未来5分钟的时间
        List<Taskinfo> taskinfoList = taskInfoMapper.selectList(Wrappers.<Taskinfo>lambdaQuery().le(Taskinfo::getExecuteTime, calendar.getTimeInMillis()));

        //3.新增到redis
        if(taskinfoList!=null && taskinfoList.size()>0){
            for (Taskinfo taskinfo : taskinfoList) {
                Task task=new Task();
                BeanUtils.copyProperties(taskinfo,task);
                task.setExecuteTime(taskinfo.getExecuteTime().getTime());
                this.saveToRedis(task);
            }
        }
    }
}
