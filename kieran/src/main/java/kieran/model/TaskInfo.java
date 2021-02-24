package kieran.model;



import lombok.Data;
import kieran.config.CronValid;

import javax.validation.constraints.NotNull;

@Data
public class TaskInfo {

    /**
     * task id
     */
    private String id;

    /**
     * job 执行类名
     */
    @NotNull(message = "jobHandler is not null")
    private String jobHandler;


    /**
     * 定时任务cron
     */
    @NotNull(message = "task cron is not null")
    @CronValid
    private String cron;

    /**
     * job参数
     */
    private String param;

    private String method;

    /**
     * job名称
     */
    @NotNull(message = "task name is not null")
    private String jobName;

    private Integer firstIndex;

    private Integer secondIndex;

    private Integer thirdIndex;

    private Integer likeIndex;
    /**
     * job 描述
     */
    private String describe;

    private Integer limit;

    private Integer offset;

    /**
     * 任务位于节点的偏移量
     */
    private Long KieranOffset;

}
