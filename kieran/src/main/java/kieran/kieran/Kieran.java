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
     * ʱ���ֳ���
     * LinkedList<TaskInfo> ����һʱ��ڵ���Ҫִ�е�����
     */
    private List<LinkedList<TaskInfo>>  kieran =null;
    /**
     * ʱ����Ĭ�ϳ��� 60
     */
    private Integer size = 60;
    /**
     * ʱ����Ĭ��delay ʱ��1000
     */
    private Integer timeDelay = 1000;

    /**
     * ִ���������
     */
    private TaskQueue queue = null;
    /**
     * ��ǰʱ��ڵ�
     */
    private Integer index = 0;

    private Boolean run = false;

    /**
     * Ĭ��ʱ���̶ֿ� 60
     * ��ʼ��ʱ����
     */
    public Kieran(){
        kieran = KieranUtils.getTimeLong(this.size);
        if(queue==null){
            queue = new TaskQueue();
        }
    }

    /**
     * �Զ���ʱ���ֳ���
     * ��ʼ��ʱ����
     */
    public Kieran(Integer size)
    {    this.size = size;
        kieran = KieranUtils.getTimeLong(this.size);
        if(queue==null){
            queue = new TaskQueue();
        }
    }

    /**
     * �Զ���ʱ���ֳ��Ⱥ�delay ʱ��
     * ��ʼ��ʱ����
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
                                //**  �ѵ�ǰʱ��ڵ��������õ�������
                                queue.setQueue(info);
                                //������һ�� ִ��ʱ��
                            }
                            //ִ�и���������е�����
                            if(!run) queue.run();
                            if(++index==list.size()){
                                index=0;
                        }
                    }
                }, this.timeDelay);
            }
        } finally {
            synchronized(kieran) {
                kieran.clear();  //ʱ����������
            }
        }
    }

    /**
     * ���������´�ִ��ʱ�� ����������õ�ָ�� ʱ��ڵ�
     *
     * @param info
     */
    private void kieranNext(TaskInfo info){
        CronSequenceGenerator generator = new CronSequenceGenerator(info.getCron());
        long next = generator.next(new Date()).getTime();
        long now = new Date().getTime();
        Map<String, Object> map = DateUtils.formatTime(next - now);
        //�´�ִ��kieran����
        Object dim = map.get("dim");
        //kieran ����ʱ��ڵ�
        Object kieranIndex = map.get("index");

    }

    /**
     * ʱ����������task
     * ��ʱ����Ϊ��ʱ ��ʼ��ʱ���� ���ѵ�ǰ��������ڵ�ǰʱ������Ҫִ�е�ʱ��ڵ�
     * ����¼����������±꣬ɾ������ʱ����ʱ��ڵ�������±��Ƴ�����
     * @param info ������Ϣ
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
     * ���ݵ�ǰ��Ҫ�Ƴ������񣬻�ȡ��ָ��ʱ��ڵ�����񼯺ϲ����������±��Ƴ�����
     * �Ƴ�����������±�仯 ������Ҫ�ѵ�ǰ�ڵ�������±� ����
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
                            log.info("��ʱ����ʼִ�� - bean��{}��������{}��������{}", handler,methodName,params);
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
                                log.error(String.format("��ʱ����ִ���쳣 - bean��%s��������%s��������%s ",handler,methodName,params), ex);
                            }
                            long times = System.currentTimeMillis() - startTime;
                            log.info("��ʱ����ִ�н��� - bean��{}��������{}��������{}����ʱ��{} ����", handler,methodName,params, times);
                        }else {
                            //����ά�Ƚ��� �����γ��ƫ����,����������ӵ���Ӧά����ȥ
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
     * ���ݵ�ǰ�����ƫ���� ����ά�Ƚ���
     * @param taskInfo
     */
     private void calcDimension(TaskInfo taskInfo){
         Map<String, Object> map = DateUtils.formatTime(taskInfo.getKieranOffset());
         //�´�ִ��kieran����
         Object dim = map.get("dim");
         //kieran ����ʱ��ڵ�
         Object kieranIndex = map.get("index");
         //��������
     }

}



