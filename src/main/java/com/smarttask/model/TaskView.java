package com.smarttask.model;

public class TaskView {

    private final Task task;
    private final Urgency urgency;
    private final String reminder;

    public TaskView(Task task, Urgency urgency, String reminder) {
        this.task = task;
        this.urgency = urgency;
        this.reminder = reminder;
    }

    public Task getTask() {
        return task;
    }

    public Urgency getUrgency() {
        return urgency;
    }

    public String getReminder() {
        return reminder;
    }
}
