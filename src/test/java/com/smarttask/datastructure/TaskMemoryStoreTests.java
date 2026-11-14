package com.smarttask.datastructure;

import com.smarttask.model.Status;
import com.smarttask.model.Task;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskMemoryStoreTests {
    private final TaskMemoryStore store = new TaskMemoryStore();

    @Test
    void countsOnlyTheRequestedUsersTasksAndReflectsRefreshes() {
        store.syncUserTasks(1L, List.of(task(Status.PENDING), task(Status.PENDING),
                task(Status.IN_PROGRESS), task(Status.COMPLETED)));
        store.syncUserTasks(2L, List.of(task(Status.COMPLETED)));

        assertEquals(Map.of(Status.PENDING, 2L, Status.IN_PROGRESS, 1L, Status.COMPLETED, 1L),
                store.getStatusCounts(1L));

        store.syncUserTasks(1L, List.of(task(Status.COMPLETED)));
        assertEquals(Map.of(Status.PENDING, 0L, Status.IN_PROGRESS, 0L, Status.COMPLETED, 1L),
                store.getStatusCounts(1L));
    }

    @Test
    void missingUsersHaveZeroCountsAndResultsCannotChangeTheStore() {
        Map<Status, Long> counts = store.getStatusCounts(99L);
        assertEquals(List.of(Status.PENDING, Status.IN_PROGRESS, Status.COMPLETED),
                List.copyOf(counts.keySet()));
        assertEquals(Map.of(Status.PENDING, 0L, Status.IN_PROGRESS, 0L, Status.COMPLETED, 0L), counts);
        counts.put(Status.COMPLETED, 10L);
        assertEquals(0L, store.getStatusCounts(99L).get(Status.COMPLETED));
    }

    private Task task(Status status) {
        Task task = new Task();
        task.setTitle("Example task");
        task.setStatus(status);
        return task;
    }

    @Test
    void timelineIncludesZeroDaysAndExcludesOtherUsersAndDates() {
        java.time.LocalDate start = java.time.LocalDate.of(2026, 11, 11);
        Task first = task(Status.PENDING);
        first.setDeadline(start);
        Task last = task(Status.COMPLETED);
        last.setDeadline(start.plusDays(6));
        Task outside = task(Status.PENDING);
        outside.setDeadline(start.plusDays(7));
        store.syncUserTasks(1L, List.of(first, last, outside));
        store.syncUserTasks(2L, List.of(first));
        var counts = store.getDeadlineCounts(1L, start, 7);
        assertEquals(7, counts.size());
        assertEquals(1L, counts.get(start));
        assertEquals(0L, counts.get(start.plusDays(3)));
        assertEquals(1L, counts.get(start.plusDays(6)));
        assertEquals(2L, counts.values().stream().mapToLong(Long::longValue).sum());
    }
}
