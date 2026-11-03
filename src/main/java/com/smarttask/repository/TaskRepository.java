package com.smarttask.repository;

import com.smarttask.model.Priority;
import com.smarttask.model.Status;
import com.smarttask.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByUserIdOrderByDeadlineAsc(Long userId);

    Optional<Task> findByIdAndUserId(Long id, Long userId);

    List<Task> findByUserIdAndStatus(Long userId, Status status);

    List<Task> findByUserIdAndPriority(Long userId, Priority priority);
}
