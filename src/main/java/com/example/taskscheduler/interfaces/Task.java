package com.example.taskscheduler.interfaces;

import com.example.taskscheduler.model.OperationType;

import java.util.UUID;

public interface Task {
    Result execute();
    UUID getUUID();
    String getGID();
    OperationType getOperationType();
}