package com.example.taskscheduler.interfaces;

import java.util.function.BiConsumer;

import com.example.taskscheduler.interfaces.Result;
import com.example.taskscheduler.interfaces.Task;

public interface Executor {
    /**
     * Run the task and invoke completionCallback when task is completed.
     * @param task a task to be executed
     * @param completionCallback a callback called on task completion
     */
    void execute(Task task, BiConsumer<Task, Result> completionCallback);
}