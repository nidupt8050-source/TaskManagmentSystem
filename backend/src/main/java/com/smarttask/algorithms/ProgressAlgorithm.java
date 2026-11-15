package com.smarttask.algorithms;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Status;
import com.smarttask.model.Task;

/**
 * Progress calculations for tasks and hierarchical aims.
 * Completion % = (completed / total) * 100
 */
public final class ProgressAlgorithm {

    private ProgressAlgorithm() {
    }

    public static int taskCompletionPercentage(DynamicArray<Task> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return 0;
        }
        int completed = 0;
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getStatus() == Status.COMPLETED) {
                completed++;
            }
        }
        return (int) Math.round((completed * 100.0) / tasks.size());
    }

    public static int percentage(int completed, int total) {
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((completed * 100.0) / total);
    }

    /**
     * Roll up child percentages into a parent percentage.
     * Empty children => 0%.
     */
    public static int averagePercentage(DynamicArray<Integer> childPercentages) {
        if (childPercentages == null || childPercentages.isEmpty()) {
            return 0;
        }
        int sum = 0;
        for (int i = 0; i < childPercentages.size(); i++) {
            Integer value = childPercentages.get(i);
            sum += value == null ? 0 : value;
        }
        return (int) Math.round(sum * 1.0 / childPercentages.size());
    }
}
