package com.smarttask.algorithms;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;

import java.util.Comparator;

/**
 * Custom sorting algorithms (no Collections.sort).
 */
public final class SortingAlgorithms {

    private SortingAlgorithms() {
    }

    /** Quick Sort – sort tasks by priority (HIGH first). */
    public static DynamicArray<Task> sortByPriority(DynamicArray<Task> tasks) {
        DynamicArray<Task> copy = tasks.copy();
        quickSort(copy, 0, copy.size() - 1, priorityComparator());
        return copy;
    }

    /** Merge Sort – sort tasks by deadline ascending. */
    public static DynamicArray<Task> sortByDeadline(DynamicArray<Task> tasks) {
        DynamicArray<Task> copy = tasks.copy();
        mergeSort(copy, deadlineComparator());
        return copy;
    }

    /** Merge Sort – sort tasks by status. */
    public static DynamicArray<Task> sortByStatus(DynamicArray<Task> tasks) {
        DynamicArray<Task> copy = tasks.copy();
        mergeSort(copy, statusComparator());
        return copy;
    }

    /** Merge Sort – sort tasks by title. */
    public static DynamicArray<Task> sortByTitle(DynamicArray<Task> tasks) {
        DynamicArray<Task> copy = tasks.copy();
        mergeSort(copy, titleComparator());
        return copy;
    }

    /** Insertion Sort – sort tasks by creation date. */
    public static DynamicArray<Task> sortByCreationDate(DynamicArray<Task> tasks) {
        DynamicArray<Task> copy = tasks.copy();
        insertionSort(copy, creationComparator());
        return copy;
    }

    public static DynamicArray<Task> sortByImportance(DynamicArray<Task> tasks,
                                                     Comparator<Task> importanceComparator) {
        DynamicArray<Task> copy = tasks.copy();
        quickSort(copy, 0, copy.size() - 1, importanceComparator);
        return copy;
    }

    // ---------- Quick Sort ----------
    private static void quickSort(DynamicArray<Task> tasks, int low, int high, Comparator<Task> comparator) {
        if (low >= high) {
            return;
        }
        int pivotIndex = partition(tasks, low, high, comparator);
        quickSort(tasks, low, pivotIndex - 1, comparator);
        quickSort(tasks, pivotIndex + 1, high, comparator);
    }

    private static int partition(DynamicArray<Task> tasks, int low, int high, Comparator<Task> comparator) {
        Task pivot = tasks.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (comparator.compare(tasks.get(j), pivot) <= 0) {
                i++;
                swap(tasks, i, j);
            }
        }
        swap(tasks, i + 1, high);
        return i + 1;
    }

    // ---------- Merge Sort ----------
    private static void mergeSort(DynamicArray<Task> tasks, Comparator<Task> comparator) {
        if (tasks.size() <= 1) {
            return;
        }
        int mid = tasks.size() / 2;
        DynamicArray<Task> left = new DynamicArray<>();
        DynamicArray<Task> right = new DynamicArray<>();
        for (int i = 0; i < mid; i++) {
            left.add(tasks.get(i));
        }
        for (int i = mid; i < tasks.size(); i++) {
            right.add(tasks.get(i));
        }
        mergeSort(left, comparator);
        mergeSort(right, comparator);
        merge(tasks, left, right, comparator);
    }

    private static void merge(DynamicArray<Task> target, DynamicArray<Task> left,
                              DynamicArray<Task> right, Comparator<Task> comparator) {
        int i = 0;
        int j = 0;
        int k = 0;
        while (i < left.size() && j < right.size()) {
            if (comparator.compare(left.get(i), right.get(j)) <= 0) {
                target.set(k++, left.get(i++));
            } else {
                target.set(k++, right.get(j++));
            }
        }
        while (i < left.size()) {
            target.set(k++, left.get(i++));
        }
        while (j < right.size()) {
            target.set(k++, right.get(j++));
        }
    }

    // ---------- Insertion Sort ----------
    private static void insertionSort(DynamicArray<Task> tasks, Comparator<Task> comparator) {
        for (int i = 1; i < tasks.size(); i++) {
            Task current = tasks.get(i);
            int j = i - 1;
            while (j >= 0 && comparator.compare(tasks.get(j), current) > 0) {
                tasks.set(j + 1, tasks.get(j));
                j--;
            }
            tasks.set(j + 1, current);
        }
    }

    private static void swap(DynamicArray<Task> tasks, int i, int j) {
        Task temp = tasks.get(i);
        tasks.set(i, tasks.get(j));
        tasks.set(j, temp);
    }

    private static Comparator<Task> priorityComparator() {
        return Comparator
                .comparingInt((Task task) -> priorityRank(task.getPriority()))
                .thenComparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static Comparator<Task> deadlineComparator() {
        return Comparator
                .comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private static Comparator<Task> creationComparator() {
        return Comparator
                .comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private static Comparator<Task> statusComparator() {
        return Comparator
                .comparingInt((Task task) -> statusRank(task.getStatus()))
                .thenComparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()));
    }

    private static Comparator<Task> titleComparator() {
        return Comparator.comparing(Task::getTitle, String.CASE_INSENSITIVE_ORDER);
    }

    private static int priorityRank(Priority priority) {
        if (priority == Priority.HIGH) {
            return 0;
        }
        if (priority == Priority.MEDIUM) {
            return 1;
        }
        return 2;
    }

    private static int statusRank(Status status) {
        if (status == Status.PENDING) {
            return 0;
        }
        if (status == Status.IN_PROGRESS) {
            return 1;
        }
        return 2;
    }
}
