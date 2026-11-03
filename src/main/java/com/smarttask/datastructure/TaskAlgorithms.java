package com.smarttask.datastructure;

import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Sorting and searching algorithms required by the project proposal.
 */
public final class TaskAlgorithms {

    private TaskAlgorithms() {
    }

    public static List<Task> sortByPriority(List<Task> tasks) {
        List<Task> copy = new ArrayList<>(tasks);
        quickSort(copy, 0, copy.size() - 1, priorityComparator());
        return copy;
    }

    public static List<Task> sortByDeadline(List<Task> tasks) {
        List<Task> copy = new ArrayList<>(tasks);
        mergeSort(copy, deadlineComparator());
        return copy;
    }

    public static List<Task> sortByCreationDate(List<Task> tasks) {
        List<Task> copy = new ArrayList<>(tasks);
        insertionSort(copy, creationComparator());
        return copy;
    }

    public static List<Task> sortByStatus(List<Task> tasks) {
        List<Task> copy = new ArrayList<>(tasks);
        mergeSort(copy, statusComparator());
        return copy;
    }

    public static List<Task> sortByTitle(List<Task> tasks) {
        List<Task> copy = new ArrayList<>(tasks);
        mergeSort(copy, titleComparator());
        return copy;
    }

    /** Linear search across title, priority label, and status label. */
    public static List<Task> linearSearch(List<Task> tasks, String keyword) {
        List<Task> matches = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>(tasks);
        }

        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        for (Task task : tasks) {
            if (containsIgnoreCase(task.getTitle(), needle)
                    || containsIgnoreCase(task.getDescription(), needle)
                    || containsIgnoreCase(task.getPriority().getLabel(), needle)
                    || containsIgnoreCase(task.getStatus().getLabel(), needle)) {
                matches.add(task);
            }
        }
        return matches;
    }

    /** Binary search for an exact title in a title-sorted list. */
    public static Task binarySearchByTitle(List<Task> titleSortedTasks, String title) {
        if (title == null || title.isBlank() || titleSortedTasks.isEmpty()) {
            return null;
        }

        String key = title.trim().toLowerCase(Locale.ROOT);
        int low = 0;
        int high = titleSortedTasks.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            String midTitle = titleSortedTasks.get(mid).getTitle().trim().toLowerCase(Locale.ROOT);
            int comparison = midTitle.compareTo(key);

            if (comparison == 0) {
                return titleSortedTasks.get(mid);
            }
            if (comparison < 0) {
                low = mid + 1;
            } else {
                high = mid - 1;
            }
        }
        return null;
    }

    private static void quickSort(List<Task> tasks, int low, int high, Comparator<Task> comparator) {
        if (low >= high) {
            return;
        }
        int pivotIndex = partition(tasks, low, high, comparator);
        quickSort(tasks, low, pivotIndex - 1, comparator);
        quickSort(tasks, pivotIndex + 1, high, comparator);
    }

    private static int partition(List<Task> tasks, int low, int high, Comparator<Task> comparator) {
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

    private static void mergeSort(List<Task> tasks, Comparator<Task> comparator) {
        if (tasks.size() <= 1) {
            return;
        }
        int mid = tasks.size() / 2;
        List<Task> left = new ArrayList<>(tasks.subList(0, mid));
        List<Task> right = new ArrayList<>(tasks.subList(mid, tasks.size()));
        mergeSort(left, comparator);
        mergeSort(right, comparator);
        merge(tasks, left, right, comparator);
    }

    private static void merge(List<Task> target, List<Task> left, List<Task> right, Comparator<Task> comparator) {
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

    private static void insertionSort(List<Task> tasks, Comparator<Task> comparator) {
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

    private static void swap(List<Task> tasks, int i, int j) {
        Task temp = tasks.get(i);
        tasks.set(i, tasks.get(j));
        tasks.set(j, temp);
    }

    private static boolean containsIgnoreCase(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
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
