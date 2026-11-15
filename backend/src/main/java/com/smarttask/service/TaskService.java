package com.smarttask.service;

import com.smarttask.algorithms.ProgressAlgorithm;
import com.smarttask.algorithms.SearchingAlgorithms;
import com.smarttask.algorithms.SortingAlgorithms;
import com.smarttask.algorithms.TaskPriorityAlgorithm;
import com.smarttask.database.TaskDao;
import com.smarttask.datastructures.DynamicArray;
import com.smarttask.datastructures.LinkedList;
import com.smarttask.datastructures.TaskMemoryStore;
import com.smarttask.model.DashboardStats;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.TaskStatusChange;
import com.smarttask.model.TaskView;
import com.smarttask.model.Urgency;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

@Service
public class TaskService {

    private final TaskDao taskDao;
    private final TaskMemoryStore memoryStore;

    public TaskService(TaskDao taskDao, TaskMemoryStore memoryStore) {
        this.taskDao = taskDao;
        this.memoryStore = memoryStore;
    }

    public DynamicArray<Task> getUserTasks(Long userId) {
        DynamicArray<Task> tasks = taskDao.findByUserIdOrderByDeadlineAsc(userId);
        memoryStore.syncUserTasks(userId, tasks);
        return memoryStore.getTasks(userId);
    }

    public Task getTask(Long taskId, Long userId) {
        return taskDao.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    public DynamicArray<TaskMemoryStore.StatusCount> statusCounts(Long userId) {
        getUserTasks(userId);
        return memoryStore.getStatusCounts(userId);
    }

    public DynamicArray<TaskMemoryStore.DeadlineCount> deadlineCounts(Long userId) {
        getUserTasks(userId);
        return memoryStore.getDeadlineCounts(userId,
                LocalDate.now(java.time.ZoneId.of("Asia/Thimphu")).minusDays(3), 7);
    }

    public Task createTask(Long userId, String title, String description,
                           Priority priority, Status status, LocalDate deadline, Long objectiveId) {
        Task task = new Task();
        task.setUserId(userId);
        task.setObjectiveId(objectiveId);
        applyTaskFields(task, title, description, priority, status, deadline);
        Task saved = taskDao.save(task);
        refreshMemory(userId);
        return saved;
    }

    public Task updateTask(Long taskId, Long userId, String title, String description,
                           Priority priority, Status status, LocalDate deadline, Long objectiveId) {
        Task task = getTask(taskId, userId);
        Status previous = task.getStatus();
        task.setObjectiveId(objectiveId);
        applyTaskFields(task, title, description, priority, status, deadline);
        Task saved = taskDao.save(task);
        if (previous != saved.getStatus()) {
            memoryStore.recordStatusChange(new TaskStatusChange(taskId, userId, previous, saved.getStatus()));
        }
        refreshMemory(userId);
        return saved;
    }

    public void deleteTask(Long taskId, Long userId) {
        Task task = getTask(taskId, userId);
        taskDao.delete(task);
        refreshMemory(userId);
    }

    public Task changeStatus(Long taskId, Long userId, Status status) {
        Task task = getTask(taskId, userId);
        Status previous = task.getStatus();
        if (previous != status) {
            memoryStore.recordStatusChange(new TaskStatusChange(taskId, userId, previous, status));
        }
        updateCompletionTimestamp(task, status);
        task.setStatus(status);
        Task saved = taskDao.save(task);
        refreshMemory(userId);
        return saved;
    }

    public Task undoLastStatusChange(Long userId) {
        TaskStatusChange change = memoryStore.popUndo(userId);
        if (change == null) {
            throw new IllegalArgumentException("Nothing to undo.");
        }
        Task task = getTask(change.getTaskId(), userId);
        updateCompletionTimestamp(task, change.getPreviousStatus());
        task.setStatus(change.getPreviousStatus());
        Task saved = taskDao.save(task);
        refreshMemory(userId);
        return saved;
    }

    public boolean canUndo(Long userId) {
        return memoryStore.canUndo(userId);
    }

    public LinkedList<TaskStatusChange> getStatusHistory(Long taskId) {
        return memoryStore.getStatusHistory(taskId);
    }

    public Urgency calculateUrgency(Task task) {
        return TaskPriorityAlgorithm.calculateUrgency(task);
    }

    public String buildReminder(Task task) {
        return TaskPriorityAlgorithm.buildReminder(task);
    }

    public int calculateCompletionPercentage(DynamicArray<Task> tasks) {
        return ProgressAlgorithm.taskCompletionPercentage(tasks);
    }

    public int importanceScore(Task task) {
        return TaskPriorityAlgorithm.importanceScore(task);
    }

    public TaskView toView(Task task) {
        return new TaskView(task, calculateUrgency(task), buildReminder(task));
    }

    public DynamicArray<TaskView> toViews(DynamicArray<Task> tasks) {
        DynamicArray<TaskView> views = new DynamicArray<>();
        for (int i = 0; i < tasks.size(); i++) {
            views.add(toView(tasks.get(i)));
        }
        return views;
    }

    public DynamicArray<TaskView> attentionViews(DynamicArray<Task> tasks, int limit) {
        DynamicArray<Task> incomplete = new DynamicArray<>();
        for (int i = 0; i < tasks.size(); i++) {
            if (tasks.get(i).getStatus() != Status.COMPLETED) {
                incomplete.add(tasks.get(i));
            }
        }
        DynamicArray<Task> sorted = SortingAlgorithms.sortByImportance(
                incomplete,
                (a, b) -> Integer.compare(importanceScore(b), importanceScore(a))
        );
        DynamicArray<TaskView> views = toViews(sorted);
        if (views.size() <= limit) {
            return views;
        }
        DynamicArray<TaskView> limited = new DynamicArray<>();
        for (int i = 0; i < limit; i++) {
            limited.add(views.get(i));
        }
        return limited;
    }

    public DynamicArray<String> getReminders(DynamicArray<Task> tasks) {
        DynamicArray<String> reminders = new DynamicArray<>();
        DynamicArray<Task> byDeadline = SortingAlgorithms.sortByDeadline(tasks);
        for (int i = 0; i < byDeadline.size(); i++) {
            String reminder = buildReminder(byDeadline.get(i));
            if (reminder != null) {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    public DashboardStats buildStats(DynamicArray<Task> tasks) {
        long completed = 0;
        long pending = 0;
        long inProgress = 0;
        long overdue = 0;
        long highPriority = 0;

        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            if (task.getStatus() == Status.COMPLETED) {
                completed++;
            } else if (task.getStatus() == Status.PENDING) {
                pending++;
            } else if (task.getStatus() == Status.IN_PROGRESS) {
                inProgress++;
            }
            if (calculateUrgency(task) == Urgency.OVERDUE) {
                overdue++;
            }
            if (task.getPriority() == Priority.HIGH && task.getStatus() != Status.COMPLETED) {
                highPriority++;
            }
        }

        return new DashboardStats(
                tasks.size(),
                completed,
                pending,
                inProgress,
                overdue,
                highPriority,
                calculateCompletionPercentage(tasks)
        );
    }

    public DynamicArray<Task> queryTasks(Long userId, Status status, Priority priority, String keyword, String sortBy) {
        DynamicArray<Task> tasks = getUserTasks(userId);
        DynamicArray<Task> filtered = filterInMemory(tasks, status, priority);
        DynamicArray<Task> searched = SearchingAlgorithms.searchTasks(filtered, memoryStore.getTitleTree(userId), keyword);
        return sortTasks(searched, sortBy);
    }

    public DynamicArray<Task> sortTasks(DynamicArray<Task> tasks, String sortBy) {
        String key = sortBy == null ? "importance" : sortBy.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "priority" -> SortingAlgorithms.sortByPriority(tasks);
            case "deadline" -> SortingAlgorithms.sortByDeadline(tasks);
            case "created", "creation", "creationdate" -> SortingAlgorithms.sortByCreationDate(tasks);
            case "status" -> SortingAlgorithms.sortByStatus(tasks);
            case "title" -> SortingAlgorithms.sortByTitle(tasks);
            default -> SortingAlgorithms.sortByImportance(
                    tasks,
                    (a, b) -> Integer.compare(importanceScore(b), importanceScore(a))
            );
        };
    }

    public DynamicArray<Task> earliestDeadlines(Long userId, int limit) {
        getUserTasks(userId);
        return memoryStore.getUpcomingByDeadline(userId, limit);
    }

    public DynamicArray<Task> findByObjective(Long objectiveId, Long userId) {
        return taskDao.findByObjectiveId(objectiveId, userId);
    }

    private DynamicArray<Task> filterInMemory(DynamicArray<Task> tasks, Status status, Priority priority) {
        DynamicArray<Task> filtered = new DynamicArray<>();
        for (int i = 0; i < tasks.size(); i++) {
            Task task = tasks.get(i);
            boolean statusOk = status == null || task.getStatus() == status;
            boolean priorityOk = priority == null || task.getPriority() == priority;
            if (statusOk && priorityOk) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    private void refreshMemory(Long userId) {
        DynamicArray<Task> tasks = taskDao.findByUserIdOrderByDeadlineAsc(userId);
        memoryStore.syncUserTasks(userId, tasks);
    }

    private void applyTaskFields(Task task, String title, String description,
                                 Priority priority, Status status, LocalDate deadline) {
        task.setTitle(title.trim());
        task.setDescription(description == null ? "" : description.trim());
        task.setPriority(priority);
        task.setDeadline(deadline);
        updateCompletionTimestamp(task, status);
        task.setStatus(status);
    }

    private void updateCompletionTimestamp(Task task, Status status) {
        if (status == Status.COMPLETED) {
            if (task.getCompletedAt() == null) {
                task.setCompletedAt(LocalDateTime.now());
            }
        } else {
            task.setCompletedAt(null);
        }
    }

}
