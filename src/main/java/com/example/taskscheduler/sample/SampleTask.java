package com.example.taskscheduler.sample;

import java.util.UUID;

import com.example.taskscheduler.interfaces.Result;
import com.example.taskscheduler.interfaces.Task;
import com.example.taskscheduler.model.OperationType;
import com.example.taskscheduler.model.SimpleResult;

public class SampleTask implements Task {
    private final UUID uuid;
    private final String gid;
    private final OperationType type;

    public SampleTask(String gid, OperationType type) {
        this.uuid = UUID.randomUUID();
        this.gid = gid;
        this.type = type;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public String getGID() {
        return gid;
    }

    @Override
    public OperationType getOperationType() {
        return type;
    }

    @Override
    public Result execute() {
        try {
            Thread.sleep(100); // Simulate task work
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new SimpleResult(true, "Task " + uuid + " completed.");
    }

    @Override
    public String toString() {
        return "SampleTask{" +
                "uuid=" + uuid +
                ", gid='" + gid + '\'' +
                ", type=" + type +
                '}';
    }
}
