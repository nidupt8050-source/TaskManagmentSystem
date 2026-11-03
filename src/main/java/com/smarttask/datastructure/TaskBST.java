package com.smarttask.datastructure;

import com.smarttask.model.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Custom Binary Search Tree keyed by lower-cased task title.
 * Supports insert, search, and in-order traversal for sorted title order.
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

    public void buildFrom(List<Task> tasks) {
        clear();
        if (tasks == null) {
            return;
        }
        for (Task task : tasks) {
            insert(task);
        }
    }

    public Task searchByTitle(String title) {
        if (title == null || title.isBlank()) {
            return null;
        }
        return searchRecursive(root, normalize(title));
    }

    public List<Task> searchContaining(String keyword) {
        List<Task> matches = new ArrayList<>();
        if (keyword == null || keyword.isBlank()) {
            return matches;
        }
        String needle = normalize(keyword);
        collectContaining(root, needle, matches);
        return matches;
    }

    public List<Task> inOrderTraversal() {
        List<Task> ordered = new ArrayList<>();
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
            // Same title: keep newest task reference at this node.
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

    private void collectContaining(Node node, String needle, List<Task> matches) {
        if (node == null) {
            return;
        }
        collectContaining(node.left, needle, matches);
        if (normalize(node.task.getTitle()).contains(needle)) {
            matches.add(node.task);
        }
        collectContaining(node.right, needle, matches);
    }

    private void inOrder(Node node, List<Task> ordered) {
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
