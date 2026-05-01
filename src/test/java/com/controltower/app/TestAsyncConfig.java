package com.controltower.app;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;

@TestConfiguration
public class TestAsyncConfig {

    @Bean(name = "taskExecutor")
    @Primary
    TaskExecutor taskExecutor() {
        return new SyncTaskExecutor();
    }

    @Bean(name = "applicationEventMulticaster")
    @Primary
    org.springframework.context.event.SimpleApplicationEventMulticaster applicationEventMulticaster() {
        org.springframework.context.event.SimpleApplicationEventMulticaster multicaster =
                new org.springframework.context.event.SimpleApplicationEventMulticaster();
        multicaster.setTaskExecutor(new SyncTaskExecutor());
        return multicaster;
    }
}
