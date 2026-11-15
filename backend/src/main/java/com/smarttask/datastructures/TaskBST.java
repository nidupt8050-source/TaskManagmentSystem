package com.smarttask.datastructures;

import com.smarttask.model.Task;

import java.util.Locale;

/**
 * Custom Binary Search Tree keyed by lower-cased task title.
 */
public class TaskBST {

    private static class Node {
        private Task task;
        private Node left;
        private Node right;

        private Node(Task task) {
            this.task = task;
        }
    }

    private Node root;

    public void clear() {
        root = null;
    }

    public void insert(Task task) {
        if (task == null || task.getTitle() == null) {
            return;
        }
        root = insertRecursive(root, task);
    }

    public void buildFrom(DynamicArray<Task> tasks) {
        clear();
        if (tasks == null) {
            return;
        }
        for (int i = 0; i < tasks.size(); i++) {
            insert(tasks.get(i));
        }
    }

    public Task searchByTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return searchRecursive(root, normalize(title));
    }

    public DynamicArray<Task> searchContaining(String keyword) {
        DynamicArray<Task> matches = new DynamicArray<>();
        if (keyword == null || keyword.isBlank()) {
            return matches;
        }
        collectContaining(root, normalize(keyword), matches);
        return matches;
    }

    public DynamicArray<Task> inOrderTraversal() {
        DynamicArray<Task> ordered = new DynamicArray<>();
        inOrder(root, ordered);
        return ordered;
    }

    private Node insertRecursive(Node node, Task task) {
        if (node == null) {
            return new Node(task);
        }
        int comparison = normalize(task.getTitle()).compareTo(normalize(node.task.getTitle()));
        if (comparison < 0) {
            node.left = insertRecursive(node.left, task);
        } else if (comparison > 0) {
            node.right = insertRecursive(node.right, task);
        } else {
            node.task = task;
        }
        return node;
    }

    private Task searchRecursive(Node node, String key) {
        if (node == null) {
            return null;
        }
        int comparison = key.compareTo(normalize(node.task.getTitle()));
        if (comparison == 0) {
            return node.task;
        }
        if (comparison < 0) {
            return searchRecursive(node.left, key);
        }
        return searchRecursive(node.right, key);
    }

    private void collectContaining(Node node, String needle, DynamicArray<Task> matches) {
        if (node == null) {
            return;
        }
        collectContaining(node.left, needle, matches);
        if (normalize(node.task.getTitle()).contains(needle)) {
            matches.add(node.task);
        }
        collectContaining(node.right, needle, matches);
    }

    private void inOrder(Node node, DynamicArray<Task> ordered) {
        if (node == null) {
            return;
        }
        inOrder(node.left, ordered);
        ordered.add(node.task);
        inOrder(node.right, ordered);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
