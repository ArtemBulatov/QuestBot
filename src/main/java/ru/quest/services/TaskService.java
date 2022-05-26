package ru.quest.services;

import org.springframework.stereotype.Service;
import ru.quest.models.Task;
import ru.quest.repositories.TaskRepository;

import java.util.List;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final HintService hintService;
    private final PhotoService photoService;
    private final LocationService locationService;

    public TaskService(TaskRepository taskRepository, HintService hintService, PhotoService photoService, LocationService locationService) {
        this.taskRepository = taskRepository;
        this.hintService = hintService;
        this.photoService = photoService;
        this.locationService = locationService;
    }

    public Task save(Task task) {
        return taskRepository.save(task);
    }

    public Task get(long id) {
        return taskRepository.getById(id);
    }

    public List<Task> getAllByQuestId(long id) {
        return taskRepository.findAllByQuestId(id);
    }

    public void delete(Task task) {
        hintService.deleteAllByTaskId(task.getId());
        taskRepository.delete(task);
        if (task.getPhoto() != null) photoService.delete(task.getPhoto());
        if (task.getLocation() != null) locationService.delete(task.getLocation());
    }

    public void deleteAllByQuestId(long id) {
        taskRepository.findAllByQuestId(id).forEach(this::delete);
    }
}
