package com.smarttask.model;

public class PriorityBreakdown {

    private final String key;
    private final String label;
    private final int count;
    private final int percent;

    public PriorityBreakdown(String key, String label, int count, int percent) {
        this.key = key;
        this.label = label;
        this.count = count;
        this.percent = percent;
    }

    public String getKey() {
        return key;
    }

    public String getLabel() {
        return label;
    }

    public int getCount() {
        return count;
    }

    public int getPercent() {
        return percent;
    }
}
