package ru.quest.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.quest.models.TaskCompleting;
import ru.quest.repositories.TaskCompletingRepository;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TaskCompletingService {
    private final TaskCompletingRepository taskCompletingRepository;

    public TaskCompletingService(TaskCompletingRepository taskCompletingRepository) {
        this.taskCompletingRepository = taskCompletingRepository;
    }

    public TaskCompleting save(TaskCompleting taskCompleting) {
        return taskCompletingRepository.save(taskCompleting);
    }

    public TaskCompleting get(long questGameId, long taskId) {
        return taskCompletingRepository.findTaskCompletingByQuestGameIdAndTaskId(questGameId, taskId).orElse(null);
    }

    public List<TaskCompleting> getAllByQuestGameId(long questGameId) {
        return taskCompletingRepository.findAllByQuestGameId(questGameId);
    }

    public boolean isPresentActiveTaskCompleting(long questGameId) {
        return taskCompletingRepository.findAllByQuestGameId(questGameId).stream()
                .anyMatch(taskCompleting -> taskCompleting.getEndTime() == null);
    }

    public List<Long> getCompletedTaskIdList(long questGameId) {
        List<Long> taskIdList = new ArrayList<>();
        taskCompletingRepository.findAllByQuestGameId(questGameId).forEach(taskCompleting -> {
            if (taskCompleting.getEndTime() != null) {
                taskIdList.add(taskCompleting.getTaskId());
            }
        });
        return taskIdList;
    }
}
