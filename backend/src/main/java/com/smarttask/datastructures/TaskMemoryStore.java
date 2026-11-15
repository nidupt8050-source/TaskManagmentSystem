package com.smarttask.datastructures;

import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.TaskStatusChange;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;

/**
 * In-memory ADS store using custom data structures only.
 */
@Component
public class TaskMemoryStore {

    private final SimpleMap<Long, DynamicArray<Task>> tasksByUser = new SimpleMap<>();
    private final SimpleMap<Long, PriorityQueue<Task>> deadlineQueues = new SimpleMap<>();
    private final SimpleMap<Long, TaskBST> titleTrees = new SimpleMap<>();
    private final SimpleMap<Long, Stack<TaskStatusChange>> undoStacks = new SimpleMap<>();
    private final SimpleMap<Long, LinkedList<TaskStatusChange>> statusHistory = new SimpleMap<>();

    public synchronized void syncUserTasks(Long userId, DynamicArray<Task> tasksFromDb) {
        DynamicArray<Task> list = tasksFromDb.copy();
        tasksByUser.put(userId, list);

        PriorityQueue<Task> queue = new PriorityQueue<>(
                Comparator.comparing(Task::getDeadline, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Task::getId, Comparator.nullsLast(Comparator.naturalOrder()))
        );
        for (int i = 0; i < list.size(); i++) {
            Task task = list.get(i);
            if (task.getStatus() != Status.COMPLETED && task.getDeadline() != null) {
                queue.offer(task);
            }
        }
        deadlineQueues.put(userId, queue);

        TaskBST bst = new TaskBST();
        bst.buildFrom(list);
        titleTrees.put(userId, bst);
    }

    public synchronized DynamicArray<Task> getTasks(Long userId) {
        DynamicArray<Task> tasks = tasksByUser.get(userId);
        return tasks == null ? new DynamicArray<>() : tasks.copy();
    }

    public synchronized DynamicArray<StatusCount> getStatusCounts(Long userId) {
        DynamicArray<StatusCount> counts = new DynamicArray<>();
        for (Status status : Status.values()) {
            counts.add(new StatusCount(status, 0));
        }
        DynamicArray<Task> tasks = tasksByUser.getOrDefault(userId, new DynamicArray<>());
        for (int i = 0; i < tasks.size(); i++) {
            Status status = tasks.get(i).getStatus();
            for (int j = 0; j < counts.size(); j++) {
                if (counts.get(j).status() == status) {
                    counts.set(j, new StatusCount(status, counts.get(j).count() + 1));
                    break;
                }
            }
        }
        return counts;
    }

    public synchronized DynamicArray<DeadlineCount> getDeadlineCounts(Long userId, LocalDate start, int days) {
        DynamicArray<DeadlineCount> counts = new DynamicArray<>();
        for (int day = 0; day < days; day++) {
            counts.add(new DeadlineCount(start.plusDays(day), 0));
        }
        DynamicArray<Task> tasks = tasksByUser.getOrDefault(userId, new DynamicArray<>());
        for (int i = 0; i < tasks.size(); i++) {
            LocalDate deadline = tasks.get(i).getDeadline();
            for (int j = 0; j < counts.size(); j++) {
                if (counts.get(j).date().equals(deadline)) {
                    counts.set(j, new DeadlineCount(deadline, counts.get(j).count() + 1));
                    break;
                }
            }
        }
        return counts;
    }

    public synchronized DynamicArray<Task> getUpcomingByDeadline(Long userId, int limit) {
        PriorityQueue<Task> source = deadlineQueues.getOrDefault(userId,
                new PriorityQueue<>(Comparator.comparing(Task::getDeadline)));
        PriorityQueue<Task> copy = source.copy();
        DynamicArray<Task> result = new DynamicArray<>();
        while (!copy.isEmpty() && result.size() < limit) {
            result.add(copy.poll());
        }
        return result;
    }

    public synchronized TaskBST getTitleTree(Long userId) {
        TaskBST bst = titleTrees.get(userId);
        return bst == null ? new TaskBST() : bst;
    }

    public synchronized void recordStatusChange(TaskStatusChange change) {
        Stack<TaskStatusChange> stack = undoStacks.get(change.getUserId());
        if (stack == null) {
            stack = new Stack<>();
            undoStacks.put(change.getUserId(), stack);
        }
        stack.push(change);

        LinkedList<TaskStatusChange> history = statusHistory.get(change.getTaskId());
        if (history == null) {
            history = new LinkedList<>();
            statusHistory.put(change.getTaskId(), history);
        }
        history.add(change);
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
        LinkedList<TaskStatusChange> history = statusHistory.get(taskId);
        return history == null ? new LinkedList<>() : history.copy();
    }

    public record StatusCount(Status status, long count) {
    }

    public record DeadlineCount(LocalDate date, long count) {
    }
}
