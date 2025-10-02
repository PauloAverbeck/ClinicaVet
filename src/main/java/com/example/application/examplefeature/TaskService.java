package com.example.application.examplefeature;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class TaskService {

    private final TaskRepository taskRepository;

    TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public void createTask(String description, @Nullable LocalDate dueDate) {
        if ("fail".equals(description)) {
            throw new RuntimeException("This is for testing the error handler");
        }

        taskRepository.saveAndFlush(
                new TaskEntity()
                        .setDescription(description)
                        .setDueDate(dueDate));
    }

    @Transactional(readOnly = true)
    public List<TaskEntity> list(Pageable pageable) {
        return taskRepository.findAllBy(pageable).toList();
    }

}
