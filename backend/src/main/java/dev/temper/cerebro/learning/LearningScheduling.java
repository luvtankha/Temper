package dev.temper.cerebro.learning;
@org.springframework.context.annotation.Configuration
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name="temper.learning.enabled",havingValue="true")
@org.springframework.scheduling.annotation.EnableScheduling
public class LearningScheduling {
    @org.springframework.context.annotation.Bean(name="taskScheduler")
    public org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler scheduler(){var scheduler=new org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler();scheduler.setPoolSize(1);scheduler.setThreadNamePrefix("feedback-expiry-");return scheduler;}
}
