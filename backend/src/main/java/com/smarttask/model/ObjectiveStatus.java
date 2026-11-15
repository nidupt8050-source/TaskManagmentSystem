package com.smarttask.model;

public enum ObjectiveStatus {
    ACTIVE("Active"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed");

    private final String label;

    ObjectiveStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
