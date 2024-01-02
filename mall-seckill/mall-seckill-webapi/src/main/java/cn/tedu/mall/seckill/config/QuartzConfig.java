package cn.tedu.mall.seckill.config;

import cn.tedu.mall.seckill.timer.SeckillBloomInitialJob;
import cn.tedu.mall.seckill.timer.SeckillInitialJob;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class QuartzConfig {
    @Bean
    public JobDetail initJobDetail() {
        log.info("预热任务开始运行!");
        return JobBuilder.newJob(SeckillInitialJob.class)
                .withIdentity("initalJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger initTrigger() {
        log.info("预热触发定时器运行!");
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("0 0/1 * * * ?");
        return TriggerBuilder.newTrigger()
                .forJob(initJobDetail())
                .withIdentity("initTrigger")
                .withSchedule(cron)
                .build();
    }

    @Bean
    public JobDetail initBloomJobDetail() {
        log.info("布隆过滤器开始运行!");
        return JobBuilder.newJob(SeckillBloomInitialJob.class)
                .withIdentity("initalBloomJob")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger initBloomTrigger() {
        log.info("布隆过滤器触发定时器运行!");
        CronScheduleBuilder cron = CronScheduleBuilder.cronSchedule("0 0/1 * * * ? ");
        return TriggerBuilder.newTrigger()
                .forJob(initBloomJobDetail())
                .withIdentity("initBloomTrigger")
                .withSchedule(cron)
                .build();
    }
}
