package com.smarttask.algorithms;

import com.smarttask.datastructures.DynamicArray;
import com.smarttask.datastructures.TaskBST;
import com.smarttask.model.Task;

import java.util.Locale;

/**
 * Custom searching algorithms (linear, binary, and BST-assisted).
 */
public final class SearchingAlgorithms {

    private SearchingAlgorithms() {
    }

    /** Linear Search – scan title, description, priority, and status. */
    public static DynamicArray<Task> linearSearch(DynamicArray<Task> tasks, String keyword) {
        DynamicArray<Task> matches = new DynamicArray<>();
        if (keyword == null || keyword.isBlank()) {
            return tasks.copy();
        }

        String needle = keyword.trim().toLowerCase(Locale.ROOT);
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (contains(task.getTitle(), needle)
                    || contains(task.getDescription(), needle)
                    || contains(task.getPriority().getLabel(), needle)
                    || contains(task.getStatus().getLabel(), needle)) {
                matches.add(task);
            }
        }
        return matches;
    }

    /** Binary Search – exact title match on a title-sorted array. */
    public static Task binarySearchByTitle(DynamicArray<Task> titleSortedTasks, String title) {
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

    /** Combine linear + BST search results without duplicates. */
    public static DynamicArray<Task> searchTasks(DynamicArray<Task> source, TaskBST bst, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return source.copy();
        }

        DynamicArray<Task> linearMatches = linearSearch(source, keyword);
        DynamicArray<Task> bstMatches = bst == null ? new DynamicArray<>() : bst.searchContaining(keyword);

        DynamicArray<Task> titleSorted = SortingAlgorithms.sortByTitle(source);
        Task exact = bst != null ? bst.searchByTitle(keyword) : null;
        if (exact == null) {
            exact = binarySearchByTitle(titleSorted, keyword);
        }

        DynamicArray<Task> merged = new DynamicArray<>();
        if (exact != null && containsTask(source, exact.getId()) && !containsTask(merged, exact.getId())) {
            merged.add(exact);
        }
        for (int i = 0; i < bstMatches.size(); i++) {
            Task task = bstMatches.get(i);
            if (containsTask(source, task.getId()) && !containsTask(merged, task.getId())) {
                merged.add(task);
            }
        }
        for (int i = 0; i < linearMatches.size(); i++) {
            Task task = linearMatches.get(i);
            if (!containsTask(merged, task.getId())) {
                merged.add(task);
            }
        }
        return merged;
    }

    private static boolean contains(String value, String needle) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static boolean containsTask(DynamicArray<Task> tasks, Long taskId) {
        if (taskId == null) {
            return false;
        }
        for (int i = 0; i < tasks.size(); i++) {
            Long id = tasks.get(i).getId();
            if (taskId.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
