package com.jagrosh.jmusicbot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ScheduledExecutorServiceProvider {

    private static ScheduledExecutorService executorService;

    public static ScheduledExecutorService getInstance(){
        if(executorService == null){
            executorService = Executors.newSingleThreadScheduledExecutor();
        }
        return executorService;
    }
}
