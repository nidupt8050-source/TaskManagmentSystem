package com.smarttask.datastructure;

import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.TaskStatusChange;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Stack;

/**
 * In-memory ADS structures layered on top of MySQL-persisted tasks.
 * - ArrayList: primary task list per user
 * - HashMap: fast userId -> task list lookup
 * - PriorityQueue: earliest deadline first
 * - Stack: undo latest status change
 * - LinkedList: status history per task
 * - TaskBST: title search / sorted titles
 */
@Component
public class TaskMemoryStore {

    private final HashMap<Long, ArrayList<Task>> tasksByUser = new HashMap<>();
    private final HashMap<Long, PriorityQueue<Task>> deadlineQueues = new HashMap<>();
    private final HashMap<Long, TaskBST> titleTrees = new HashMap<>();
    private final HashMap<Long, Stack<TaskStatusChange>> undoStacks = new HashMap<>();
    private final HashMap<Long, LinkedList<TaskStatusChange>> statusHistory = new HashMap<>();

    public synchronized void syncUserTasks(Long userId, List<Task> tasksFromDb) {
        ArrayList<Task> list = new ArrayList<>(tasksFromDb);
        tasksByUser.put(userId, list);

        PriorityQueue<Task> queue = new PriorityQueue<>(
                Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Task::getId, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        for (Task task : list) {
            if (task.getStatus() != Status.COMPLETED && task.getDeadline() != null) {
                queue.offer(task);
            }
        }
        deadlineQueues.put(userId, queue);

        TaskBST bst = new TaskBST();
        bst.buildFrom(list);
        titleTrees.put(userId, bst);
    }

    public synchronized ArrayList<Task> getTasks(Long userId) {
        return new ArrayList<>(tasksByUser.getOrDefault(userId, new ArrayList<>()));
    }

    public synchronized List<Task> getUpcomingByDeadline(Long userId, int limit) {
        PriorityQueue<Task> source = deadlineQueues.getOrDefault(userId, new PriorityQueue<>());
        PriorityQueue<Task> copy = new PriorityQueue<>(source);
        List<Task> result = new ArrayList<>();
        while (!copy.isEmpty() && result.size() < limit) {
            result.add(copy.poll());
        }
        return result;
    }

    public synchronized TaskBST getTitleTree(Long userId) {
        return titleTrees.getOrDefault(userId, new TaskBST());
    }

    public synchronized void recordStatusChange(TaskStatusChange change) {
        undoStacks.computeIfAbsent(change.getUserId(), key -> new Stack<>()).push(change);
        statusHistory.computeIfAbsent(change.getTaskId(), key -> new LinkedList<>()).add(change);
    }

    public synchronized TaskStatusChange popUndo(Long userId) {
        Stack<TaskStatusChange> stack = undoStacks.get(userId);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return stack.pop();
    }

    public synchronized boolean canUndo(Long userId) {
        Stack<TaskStatusChange> stack = undoStacks.get(userId);
        return stack != null && !stack.isEmpty();
    }

    public synchronized LinkedList<TaskStatusChange> getStatusHistory(Long taskId) {
        return new LinkedList<>(statusHistory.getOrDefault(taskId, new LinkedList<>()));
    }

    public synchronized void clearUser(Long userId) {
        ArrayList<Task> tasks = tasksByUser.remove(userId);
        deadlineQueues.remove(userId);
        titleTrees.remove(userId);
        undoStacks.remove(userId);
        if (tasks != null) {
            for (Task task : tasks) {
                statusHistory.remove(task.getId());
            }
        }
    }

    public synchronized LocalDate nextDeadline(Long userId) {
        PriorityQueue<Task> queue = deadlineQueues.get(userId);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        Task soonest = queue.peek();
        return soonest == null ? null : soonest.getDeadline();
    }
}
