package ru.quest.services;

import org.springframework.stereotype.Service;
import ru.quest.models.Task;
import ru.quest.repositories.TaskRepository;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final HintService hintService;

    public TaskService(TaskRepository taskRepository, HintService hintService) {
        this.taskRepository = taskRepository;
        this.hintService = hintService;
    }

    public Task save(Task task) {
        return taskRepository.save(task);
    }

    public Task get(long id) {
        return taskRepository.getById(id);
    }

    public List<Task> getAll() {
        return taskRepository.findAll();
    }

    public List<Task> getAllByQuestId(long id) {
        return taskRepository.findAllByQuestId(id);
    }

    public void delete(Task task) {
        hintService.deleteAllByTaskId(task.getId());
        taskRepository.delete(task);
    }

    public void deleteAllByQuestId(long id) {
        taskRepository.deleteAllByQuestId(id);
    }
}
