package com.example.taskscheduler.core;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.BiConsumer;

import com.example.taskscheduler.interfaces.Executor;
import com.example.taskscheduler.interfaces.Result;
import com.example.taskscheduler.interfaces.Task;

public class SimpleExecutor implements Executor {
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public void execute(Task task, BiConsumer<Task, Result> callback) {
        pool.submit(() -> {
            Result result = task.execute();
            callback.accept(task, result);
        });
    }
}
