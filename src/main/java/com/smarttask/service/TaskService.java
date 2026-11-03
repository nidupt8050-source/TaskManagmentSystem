package com.smarttask.service;

import com.smarttask.datastructure.TaskAlgorithms;
import com.smarttask.datastructure.TaskBST;
import com.smarttask.datastructure.TaskMemoryStore;
import com.smarttask.model.DashboardStats;
import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import com.smarttask.model.TaskStatusChange;
import com.smarttask.model.TaskView;
import com.smarttask.model.Urgency;
import com.smarttask.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final TaskMemoryStore memoryStore;

    public TaskService(TaskRepository taskRepository, TaskMemoryStore memoryStore) {
        this.taskRepository = taskRepository;
        this.memoryStore = memoryStore;
    }

    public List<Task> getUserTasks(Long userId) {
        List<Task> tasks = taskRepository.findByUserIdOrderByDeadlineAsc(userId);
        memoryStore.syncUserTasks(userId, tasks);
        return memoryStore.getTasks(userId);
    }

    public Task getTask(Long taskId, Long userId) {
        return taskRepository.findByIdAndUserId(taskId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    @Transactional
    public Task createTask(Long userId, String title, String description,
                           Priority priority, Status status, LocalDate deadline) {
        Task task = new Task();
        task.setUserId(userId);
        applyTaskFields(task, title, description, priority, status, deadline);
        Task saved = taskRepository.save(task);
        refreshMemory(userId);
        return saved;
    }

    @Transactional
    public Task updateTask(Long taskId, Long userId, String title, String description,
                           Priority priority, Status status, LocalDate deadline) {
        Task task = getTask(taskId, userId);
        Status previous = task.getStatus();
        applyTaskFields(task, title, description, priority, status, deadline);
        Task saved = taskRepository.save(task);
        if (previous != saved.getStatus()) {
            memoryStore.recordStatusChange(new TaskStatusChange(taskId, userId, previous, saved.getStatus()));
        }
        refreshMemory(userId);
        return saved;
    }

    @Transactional
    public void deleteTask(Long taskId, Long userId) {
        Task task = getTask(taskId, userId);
        taskRepository.delete(task);
        refreshMemory(userId);
    }

    @Transactional
    public Task changeStatus(Long taskId, Long userId, Status status) {
        Task task = getTask(taskId, userId);
        Status previous = task.getStatus();
        if (previous != status) {
            memoryStore.recordStatusChange(new TaskStatusChange(taskId, userId, previous, status));
        }
        updateCompletionTimestamp(task, status);
        task.setStatus(status);
        Task saved = taskRepository.save(task);
        refreshMemory(userId);
        return saved;
    }

    @Transactional
    public Task undoLastStatusChange(Long userId) {
        TaskStatusChange change = memoryStore.popUndo(userId);
        if (change == null) {
            throw new IllegalArgumentException("Nothing to undo.");
        }

        Task task = getTask(change.getTaskId(), userId);
        updateCompletionTimestamp(task, change.getPreviousStatus());
        task.setStatus(change.getPreviousStatus());
        Task saved = taskRepository.save(task);
        refreshMemory(userId);
        return saved;
    }

    public boolean canUndo(Long userId) {
        return memoryStore.canUndo(userId);
    }

    public LinkedList<TaskStatusChange> getStatusHistory(Long taskId) {
        return memoryStore.getStatusHistory(taskId);
    }

    /**
     * Marks a task as OVERDUE, URGENT, UPCOMING, or NORMAL
     * based on remaining time before the deadline.
     */
    public Urgency calculateUrgency(Task task) {
        if (task.getStatus() == Status.COMPLETED) {
            return Urgency.NONE;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime dueAt = task.getDeadline().atTime(LocalTime.of(23, 59));

        if (dueAt.isBefore(now)) {
            return Urgency.OVERDUE;
        }

        long hoursLeft = ChronoUnit.HOURS.between(now, dueAt);

        if (hoursLeft <= 24) {
            return Urgency.URGENT;
        }
        if (hoursLeft <= 72) {
            return Urgency.UPCOMING;
        }
        return Urgency.NORMAL;
    }

    public String buildReminder(Task task) {
        Urgency urgency = calculateUrgency(task);

        if (urgency == Urgency.NONE || urgency == Urgency.NORMAL) {
            return null;
        }

        String title = task.getTitle();

        if (urgency == Urgency.OVERDUE) {
            return "Reminder: Your \"" + title + "\" task is overdue.";
        }

        if (task.getDeadline().equals(LocalDate.now().plusDays(1))) {
            return "Reminder: Your \"" + title + "\" task is due tomorrow.";
        }

        if (urgency == Urgency.URGENT) {
            return "Reminder: Your \"" + title + "\" task is due within 24 hours.";
        }

        return "Reminder: Your \"" + title + "\" task is due in a few days.";
    }

    /**
     * Completion Percentage = (Completed Tasks / Total Tasks) × 100
     */
    public int calculateCompletionPercentage(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return 0;
        }

        int completed = 0;
        for (Task task : tasks) {
            if (task.getStatus() == Status.COMPLETED) {
                completed++;
            }
        }

        return (int) Math.round((completed * 100.0) / tasks.size());
    }

    /**
     * Higher score means the task should appear first.
     * Uses both deadline urgency and user-assigned priority.
     */
    public int importanceScore(Task task) {
        int urgencyScore;
        Urgency urgency = calculateUrgency(task);

        if (urgency == Urgency.OVERDUE) {
            urgencyScore = 400;
        } else if (urgency == Urgency.URGENT) {
            urgencyScore = 300;
        } else if (urgency == Urgency.UPCOMING) {
            urgencyScore = 200;
        } else if (urgency == Urgency.NORMAL) {
            urgencyScore = 100;
        } else {
            urgencyScore = 0;
        }

        int priorityScore;
        if (task.getPriority() == Priority.HIGH) {
            priorityScore = 30;
        } else if (task.getPriority() == Priority.MEDIUM) {
            priorityScore = 20;
        } else {
            priorityScore = 10;
        }

        return urgencyScore + priorityScore;
    }

    public TaskView toView(Task task) {
        return new TaskView(task, calculateUrgency(task), buildReminder(task));
    }

    public List<TaskView> toViews(List<Task> tasks) {
        List<TaskView> views = new ArrayList<>();
        for (Task task : tasks) {
            views.add(toView(task));
        }
        return views;
    }

    public List<TaskView> sortedViews(List<Task> tasks) {
        return toViews(sortTasks(tasks, "importance"));
    }

    public List<TaskView> attentionViews(List<Task> tasks, int limit) {
        List<Task> incomplete = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getStatus() != Status.COMPLETED) {
                incomplete.add(task);
            }
        }

        List<TaskView> views = sortedViews(incomplete);
        if (views.size() <= limit) {
            return views;
        }
        return views.subList(0, limit);
    }

    public List<String> getReminders(List<Task> tasks) {
        List<String> reminders = new ArrayList<>();
        // PriorityQueue drives "earliest deadline first" reminder ordering in memory.
        List<Task> byDeadline = TaskAlgorithms.sortByDeadline(tasks);
        for (Task task : byDeadline) {
            String reminder = buildReminder(task);
            if (reminder != null) {
                reminders.add(reminder);
            }
        }
        return reminders;
    }

    public DashboardStats buildStats(List<Task> tasks) {
        long completed = 0;
        long pending = 0;
        long inProgress = 0;
        long overdue = 0;
        long highPriority = 0;

        for (Task task : tasks) {
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

    public List<Task> queryTasks(Long userId, Status status, Priority priority, String keyword, String sortBy) {
        List<Task> tasks = getUserTasks(userId);
        List<Task> filtered = filterInMemory(tasks, status, priority);
        List<Task> searched = searchTasks(userId, filtered, keyword);
        return sortTasks(searched, sortBy);
    }

    public List<Task> filterTasks(Long userId, Status status, Priority priority) {
        return queryTasks(userId, status, priority, null, "importance");
    }

    public List<Task> searchTasks(Long userId, List<Task> source, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return new ArrayList<>(source);
        }

        String needle = keyword.trim();
        List<Task> linearMatches = TaskAlgorithms.linearSearch(source, needle);

        TaskBST bst = memoryStore.getTitleTree(userId);
        List<Task> bstMatches = bst.searchContaining(needle);

        // Prefer exact BST title hit when keyword looks like a full title.
        Task exact = bst.searchByTitle(needle);
        if (exact == null) {
            List<Task> titleSorted = TaskAlgorithms.sortByTitle(source);
            exact = TaskAlgorithms.binarySearchByTitle(titleSorted, needle);
        }

        List<Task> merged = new ArrayList<>();
        if (exact != null && containsTask(source, exact.getId()) && !containsTask(merged, exact.getId())) {
            merged.add(exact);
        }
        for (Task task : bstMatches) {
            if (containsTask(source, task.getId()) && !containsTask(merged, task.getId())) {
                merged.add(task);
            }
        }
        for (Task task : linearMatches) {
            if (!containsTask(merged, task.getId())) {
                merged.add(task);
            }
        }
        return merged;
    }

    public List<Task> sortTasks(List<Task> tasks, String sortBy) {
        String key = sortBy == null ? "importance" : sortBy.trim().toLowerCase(Locale.ROOT);
        return switch (key) {
            case "priority" -> TaskAlgorithms.sortByPriority(tasks);
            case "deadline" -> TaskAlgorithms.sortByDeadline(tasks);
            case "created", "creation", "creationdate" -> TaskAlgorithms.sortByCreationDate(tasks);
            case "status" -> TaskAlgorithms.sortByStatus(tasks);
            case "title" -> TaskAlgorithms.sortByTitle(tasks);
            default -> {
                List<Task> copy = new ArrayList<>(tasks);
                copy.sort((a, b) -> Integer.compare(importanceScore(b), importanceScore(a)));
                yield copy;
            }
        };
    }

    public List<Task> earliestDeadlines(Long userId, int limit) {
        getUserTasks(userId);
        return memoryStore.getUpcomingByDeadline(userId, limit);
    }

    private List<Task> filterInMemory(List<Task> tasks, Status status, Priority priority) {
        List<Task> filtered = new ArrayList<>();
        for (Task task : tasks) {
            boolean statusOk = status == null || task.getStatus() == status;
            boolean priorityOk = priority == null || task.getPriority() == priority;
            if (statusOk && priorityOk) {
                filtered.add(task);
            }
        }
        return filtered;
    }

    private void refreshMemory(Long userId) {
        List<Task> tasks = taskRepository.findByUserIdOrderByDeadlineAsc(userId);
        memoryStore.syncUserTasks(userId, tasks);
    }

    private boolean containsTask(List<Task> tasks, Long taskId) {
        for (Task task : tasks) {
            if (task.getId() != null && task.getId().equals(taskId)) {
                return true;
            }
        }
        return false;
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
