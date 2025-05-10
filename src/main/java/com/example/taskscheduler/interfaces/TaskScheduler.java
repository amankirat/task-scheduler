package com.example.taskscheduler.interfaces;

import java.util.UUID;

public interface TaskScheduler {
    /**
     * Submit new task to be scheduled and executed.
     * @param task a task to be scheduled and executed
     */
    void submitTask(Task task);

    /**
     * Return result of a completed task
     * @param uuid task UUID
     * @return execution result of the task
     */
    Result getResult(UUID uuid);
}