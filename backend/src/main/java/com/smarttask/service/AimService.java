package com.smarttask.service;

import com.smarttask.algorithms.ProgressAlgorithm;
import com.smarttask.database.AimDao;
import com.smarttask.database.GoalDao;
import com.smarttask.database.ObjectiveDao;
import com.smarttask.database.TaskDao;
import com.smarttask.datastructures.DynamicArray;
import com.smarttask.model.Aim;
import com.smarttask.model.Goal;
import com.smarttask.model.Objective;
import com.smarttask.model.ObjectiveStatus;
import com.smarttask.model.Task;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class AimService {

    private final AimDao aimDao;
    private final GoalDao goalDao;
    private final ObjectiveDao objectiveDao;
    private final TaskDao taskDao;

    public AimService(AimDao aimDao, GoalDao goalDao, ObjectiveDao objectiveDao, TaskDao taskDao) {
        this.aimDao = aimDao;
        this.goalDao = goalDao;
        this.objectiveDao = objectiveDao;
        this.taskDao = taskDao;
    }

    public DynamicArray<Aim> getAimsWithProgress(Long userId) {
        DynamicArray<Aim> aims = aimDao.findByUserId(userId);
        for (int i = 0; i < aims.size(); i++) {
            Aim aim = aims.get(i);
            aim.setProgressPercent(calculateAimProgress(aim.getId(), userId));
        }
        return aims;
    }

    public Aim getAim(Long aimId, Long userId) {
        Aim aim = aimDao.findByIdAndUserId(aimId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Aim not found."));
        aim.setProgressPercent(calculateAimProgress(aimId, userId));
        return aim;
    }

    public Aim createAim(Long userId, String title, String description, LocalDate deadline) {
        Aim aim = new Aim();
        aim.setUserId(userId);
        aim.setTitle(title.trim());
        aim.setDescription(description == null ? "" : description.trim());
        aim.setDeadline(deadline);
        return aimDao.save(aim);
    }

    public Aim updateAim(Long aimId, Long userId, String title, String description, LocalDate deadline) {
        Aim aim = getAim(aimId, userId);
        aim.setTitle(title.trim());
        aim.setDescription(description == null ? "" : description.trim());
        aim.setDeadline(deadline);
        return aimDao.save(aim);
    }

    public void deleteAim(Long aimId, Long userId) {
        getAim(aimId, userId);
        aimDao.delete(aimId, userId);
    }

    public DynamicArray<Goal> getGoals(Long aimId, Long userId) {
        getAim(aimId, userId);
        DynamicArray<Goal> goals = goalDao.findByAimId(aimId, userId);
        for (int i = 0; i < goals.size(); i++) {
            Goal goal = goals.get(i);
            goal.setProgressPercent(calculateGoalProgress(goal.getId(), userId));
        }
        return goals;
    }

    public Goal getGoal(Long goalId, Long userId) {
        Goal goal = goalDao.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found."));
        goal.setProgressPercent(calculateGoalProgress(goalId, userId));
        return goal;
    }

    public Goal createGoal(Long aimId, Long userId, String title, String description, LocalDate deadline) {
        getAim(aimId, userId);
        Goal goal = new Goal();
        goal.setAimId(aimId);
        goal.setUserId(userId);
        goal.setTitle(title.trim());
        goal.setDescription(description == null ? "" : description.trim());
        goal.setDeadline(deadline);
        return goalDao.save(goal);
    }

    public void deleteGoal(Long goalId, Long userId) {
        goalDao.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found."));
        goalDao.delete(goalId, userId);
    }

    public DynamicArray<Objective> getObjectives(Long goalId, Long userId) {
        goalDao.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found."));
        DynamicArray<Objective> objectives = objectiveDao.findByGoalId(goalId, userId);
        for (int i = 0; i < objectives.size(); i++) {
            Objective objective = objectives.get(i);
            objective.setProgressPercent(calculateObjectiveProgress(objective, userId));
        }
        return objectives;
    }

    public DynamicArray<Objective> getAllObjectives(Long userId) {
        return objectiveDao.findByUserId(userId);
    }

    public Objective createObjective(Long goalId, Long userId, String title, String description,
                                     LocalDate deadline, ObjectiveStatus status) {
        goalDao.findByIdAndUserId(goalId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Goal not found."));
        Objective objective = new Objective();
        objective.setGoalId(goalId);
        objective.setUserId(userId);
        objective.setTitle(title.trim());
        objective.setDescription(description == null ? "" : description.trim());
        objective.setDeadline(deadline);
        objective.setStatus(status == null ? ObjectiveStatus.ACTIVE : status);
        return objectiveDao.save(objective);
    }

    public Objective changeObjectiveStatus(Long objectiveId, Long userId, ObjectiveStatus status) {
        Objective objective = objectiveDao.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found."));
        objective.setStatus(status);
        return objectiveDao.save(objective);
    }

    public void deleteObjective(Long objectiveId, Long userId) {
        objectiveDao.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found."));
        objectiveDao.delete(objectiveId, userId);
    }

    /**
     * Completed objective => 100%.
     * Otherwise progress comes from linked tasks (0% if none).
     * This value rolls up into Goal and Aim progress.
     */
    public int calculateObjectiveProgress(Objective objective, Long userId) {
        if (objective.getStatus() == ObjectiveStatus.COMPLETED) {
            return 100;
        }
        DynamicArray<Task> tasks = taskDao.findByObjectiveId(objective.getId(), userId);
        return ProgressAlgorithm.taskCompletionPercentage(tasks);
    }

    public int calculateObjectiveProgress(Long objectiveId, Long userId) {
        Objective objective = objectiveDao.findByIdAndUserId(objectiveId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Objective not found."));
        return calculateObjectiveProgress(objective, userId);
    }

    public int calculateGoalProgress(Long goalId, Long userId) {
        DynamicArray<Objective> objectives = objectiveDao.findByGoalId(goalId, userId);
        DynamicArray<Integer> child = new DynamicArray<>();
        for (int i = 0; i < objectives.size(); i++) {
            child.add(calculateObjectiveProgress(objectives.get(i), userId));
        }
        if (child.isEmpty()) {
            return 0;
        }
        return ProgressAlgorithm.averagePercentage(child);
    }

    public int calculateAimProgress(Long aimId, Long userId) {
        DynamicArray<Goal> goals = goalDao.findByAimId(aimId, userId);
        DynamicArray<Integer> child = new DynamicArray<>();
        for (int i = 0; i < goals.size(); i++) {
            child.add(calculateGoalProgress(goals.get(i).getId(), userId));
        }
        if (child.isEmpty()) {
            return 0;
        }
        return ProgressAlgorithm.averagePercentage(child);
    }

    public int overallAimProgress(Long userId) {
        DynamicArray<Aim> aims = getAimsWithProgress(userId);
        DynamicArray<Integer> values = new DynamicArray<>();
        for (int i = 0; i < aims.size(); i++) {
            values.add(aims.get(i).getProgressPercent());
        }
        return ProgressAlgorithm.averagePercentage(values);
    }
}
