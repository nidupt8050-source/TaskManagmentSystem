package com.smarttask.model;

public class ProgressTrendItem {

    private final String title;
    private final String subtitle;
    private final int progressPercent;

    public ProgressTrendItem(String title, String subtitle, int progressPercent) {
        this.title = title;
        this.subtitle = subtitle;
        this.progressPercent = progressPercent;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getProgressPercent() {
        return progressPercent;
    }
}
