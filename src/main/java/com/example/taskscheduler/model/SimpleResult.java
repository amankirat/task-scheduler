package com.example.taskscheduler.model;

import com.example.taskscheduler.interfaces.Result;

public class SimpleResult implements Result {
    private final boolean success;
    private final Object value;

    public SimpleResult(boolean success, Object value) {
        this.success = success;
        this.value = value;
    }

    public boolean isSuccess() { return success; }
    public Object getValue() { return value; }
}
