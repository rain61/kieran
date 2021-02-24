package kieran.kieran;



import kieran.uitls.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.support.CronSequenceGenerator;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ReflectionUtils;
import kieran.model.TaskInfo;
import kieran.uitls.SpringContextUtils;
import kieran.uitls.KieranUtils;

import java.lang.reflect.Method;
import java.util.*;

@Component
@Slf4j
public class Kieran {

    /**
     * 时间轮长度
     * LinkedList<TaskInfo> 放这一时间节点所要执行的任务
     */
    private List<LinkedList<TaskInfo>>  kieran =null;
    /**
     * 时间轮默认长度 60
     */
    private Integer size = 60;
    /**
     * 时间轮默认delay 时间1000
     */
    private Integer timeDelay = 1000;

    /**
     * 执行任务队列
     */
    private TaskQueue queue = null;
    /**
     * 当前时间节点
     */
    private Integer index = 0;

    private Boolean run = false;

    /**
     * 默认时间轮刻度 60
     * 初始化时间轮
     */
    public Kieran(){
        kieran = KieranUtils.getTimeLong(this.size);
        if(queue==null){
            queue = new TaskQueue();
        }
    }

    /**
     * 自定义时间轮长度
     * 初始化时间轮
     */
    public Kieran(Integer size)
    {    this.size = size;
        kieran = KieranUtils.getTimeLong(this.size);
        if(queue==null){
            queue = new TaskQueue();
        }
    }

    /**
     * 自定义时间轮长度和delay 时长
     * 初始化时间轮
     */
    public Kieran(Integer size,Integer timeDelay)
    {    this.size = size;
         this.timeDelay = timeDelay;
         kieran = KieranUtils.getTimeLong(this.size);
        if(queue==null){
            queue = new TaskQueue();
        }
    }

    public void run(){
        try {
            synchronized (kieran){
                    new Timer().schedule(new TimerTask() {
                    @Override
                    public void run() {
                            List<LinkedList<TaskInfo>> list = kieran;
                            LinkedList<TaskInfo> infos = list.get(index);
                            for (int i = 0; i < infos.size(); i++) {
                                TaskInfo info = infos.get(i);
                                //**  把当前时间节点的任务放置到队列中
                                queue.setQueue(info);
                                //计算下一次 执行时间
                            }
                            //执行该任务队列中的任务
                            if(!run) queue.run();
                            if(++index==list.size()){
                                index=0;
                        }
                    }
                }, this.timeDelay);
            }
        } finally {
            synchronized(kieran) {
                kieran.clear();  //时间容器销毁
            }
        }
    }

    /**
     * 计算任务下次执行时间 并把任务放置到指定 时间节点
     *
     * @param info
     */
    private void kieranNext(TaskInfo info){
        CronSequenceGenerator generator = new CronSequenceGenerator(info.getCron());
        long next = generator.next(new Date()).getTime();
        long now = new Date().getTime();
        Map<String, Object> map = DateUtils.formatTime(next - now);
        //下次执行kieran级别
        Object dim = map.get("dim");
        //kieran 所在时间节点
        Object kieranIndex = map.get("index");

    }

    /**
     * 时间轮中新增task
     * 当时间轮为空时 初始化时间轮 并把当前任务放置在当前时间轮需要执行的时间节点
     * 并记录放入链表的下标，删除任务时根据时间节点和链表下标移除任务
     * @param info 任务信息
     */
    public void addTask(TaskInfo info){
        if(CollectionUtils.isEmpty(kieran)){
            kieran = KieranUtils.getTimeLong(60);
            addTask(info);
        }else {
            for (int i = 1; i <= kieran.size(); i++) {
                if(info.getFirstIndex()==i){
                    int size = kieran.get(i - 1).size();
                    info.setLikeIndex(size);
                    kieran.get(i-1).add(info);
                }
            }
        }
    }

    /**
     * 根据当前需要移除的任务，获取到指定时间节点的任务集合并根据任务下标移除任务
     * 移除任务后链表下标变化 所以需要把当前节点的任务下标 更新
     * @param info
     */
    public void removeTask(TaskInfo info){
        if(CollectionUtils.isEmpty(kieran)){
          log.info("Kieran.removeTask over.The result for Kieran.kieran is null");
        }else {
            LinkedList<TaskInfo> taskInfos = kieran.get(info.getFirstIndex());
            taskInfos.remove(info.getLikeIndex());

            for (int i = 0; i < taskInfos.size(); i++) {
                TaskInfo taskInfo = taskInfos.get(i);
                if(taskInfo.getLikeIndex()!= i ){
                    taskInfo.setLikeIndex(i);
                }
            }
        }
    }

    class  TaskQueue{
        private LinkedList<TaskInfo> taskQueue = new LinkedList<TaskInfo>();

        public void setQueue(TaskInfo taskInfo){
            taskQueue.add(taskInfo);
        }

        public void run() {
            run = true;
            try{
                synchronized (taskQueue){

                    while (!CollectionUtils.isEmpty(taskQueue)){
                        TaskInfo taskInfo = taskQueue.get(0);
                        String handler = taskInfo.getJobHandler();
                        String methodName = taskInfo.getMethod();
                        String[] params = taskInfo.getParam().split(",");
                        if(taskInfo.getKieranOffset() ==0){
                            log.info("定时任务开始执行 - bean：{}，方法：{}，参数：{}", handler,methodName,params);
                            long startTime = System.currentTimeMillis();
                            try {
                                Object target = SpringContextUtils.getBean(handler);
                                Method method = null;
                                if (null != params && params.length > 0) {
                                    Class<?>[] paramCls = new Class[params.length];
                                    for (int i = 0; i < params.length; i++) {
                                        paramCls[i] = params[i].getClass();
                                    }
                                    method = target.getClass().getDeclaredMethod(methodName, paramCls);
                                } else {
                                    method = target.getClass().getDeclaredMethod(methodName);
                                }
                                ReflectionUtils.makeAccessible(method);
                                if (null != params && params.length > 0) {
                                    method.invoke(target, params);
                                } else {
                                    method.invoke(target);
                                }
                                taskQueue.remove(0);
                            } catch (Exception ex) {
                                log.error(String.format("定时任务执行异常 - bean：%s，方法：%s，参数：%s ",handler,methodName,params), ex);
                            }
                            long times = System.currentTimeMillis() - startTime;
                            log.info("定时任务执行结束 - bean：{}，方法：{}，参数：{}，耗时：{} 毫秒", handler,methodName,params, times);
                        }else {
                            //任务维度降级 计算低纬度偏移量,并把任务添加到对应维度中去
                        }

                    }
                }
                run = false;
            }finally {
                run = false;
            }
        }
    }

    /**
     * 根据当前任务的偏移量 进行维度降级
     * @param taskInfo
     */
     private void calcDimension(TaskInfo taskInfo){
         Map<String, Object> map = DateUtils.formatTime(taskInfo.getKieranOffset());
         //下次执行kieran级别
         Object dim = map.get("dim");
         //kieran 所在时间节点
         Object kieranIndex = map.get("index");
         //放置任务
     }

}



