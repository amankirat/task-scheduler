package com.example.taskscheduler.core;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import com.example.taskscheduler.interfaces.Executor;
import com.example.taskscheduler.interfaces.Result;
import com.example.taskscheduler.interfaces.Task;
import com.example.taskscheduler.interfaces.TaskScheduler;
import com.example.taskscheduler.model.OperationType;

public class TaskSchedulerImpl implements TaskScheduler {
    private final BlockingQueue<Task> taskQueue = new LinkedBlockingQueue<>();
    private final Map<UUID, Result> resultMap = new ConcurrentHashMap<>();
    private final Set<String> activeGIDs = ConcurrentHashMap.newKeySet();

    private final Object lock = new Object();
    private final Executor executor = new SimpleExecutor();

    private int activeReadCount = 0;
    private int activeWriteCount = 0;

    public TaskSchedulerImpl() {
        Thread dispatcher = new Thread(this::dispatchLoop);
        dispatcher.setDaemon(true);
        dispatcher.start();
    }

    @Override
    public void submitTask(Task task) {
        taskQueue.offer(task);
    }

    @Override
    public Result getResult(UUID uuid) {
        return resultMap.get(uuid);
    }

    private void dispatchLoop() {
        while (true) {
            try {
                Task task = taskQueue.take();

                synchronized (lock) {
                    while (!canExecute(task)) {
                        lock.wait();
                    }
                    markAsRunning(task);
                }

                executor.execute(task, (t, result) -> {
                    resultMap.put(t.getUUID(), result);
                    synchronized (lock) {
                        markAsFinished(t);
                        lock.notifyAll();
                    }
                });

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean canExecute(Task task) {
        if (activeGIDs.contains(task.getGID())) return false;

        OperationType type = task.getOperationType();
        switch (type) {
            case READ:
                return activeWriteCount == 0;
            case WRITE:
                return activeWriteCount == 0 && activeReadCount == 0;
            default:
                return false;
        }
    }

    private void markAsRunning(Task task) {
        activeGIDs.add(task.getGID());

        switch (task.getOperationType()) {
            case READ:
                activeReadCount++;
                break;
            case WRITE:
                activeWriteCount++;
                break;
        }
    }

    private void markAsFinished(Task task) {
        activeGIDs.remove(task.getGID());

        switch (task.getOperationType()) {
            case READ:
                activeReadCount--;
                break;
            case WRITE:
                activeWriteCount--;
                break;
        }
    }
}
