package com.smarttask.model;

public enum Urgency {
    NONE("None"),
    NORMAL("Normal"),
    UPCOMING("Upcoming"),
    URGENT("Urgent"),
    OVERDUE("Overdue");

    private final String label;

    Urgency(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
