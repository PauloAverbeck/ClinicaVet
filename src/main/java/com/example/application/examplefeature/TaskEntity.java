/*
package com.example.application.examplefeature;

import org.jspecify.annotations.Nullable;

import java.time.LocalDate;



public class TaskEntity extends AbstractEntity {
    private String description;

    @Nullable
    private LocalDate dueDate;

    public TaskEntity() { // To keep Hibernate happy
    }

    public String getDescription() {
        return description;
    }

    public TaskEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public @Nullable LocalDate getDueDate() {
        return dueDate;
    }

    public TaskEntity setDueDate(@Nullable LocalDate dueDate) {
        this.dueDate = dueDate;
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().isAssignableFrom(obj.getClass())) {
            return false;
        }
        if (obj == this) {
            return true;
        }

        TaskEntity other = (TaskEntity) obj;
        return getId().equals(other.getId());
    }

    @Override
    public int hashCode() {
        // Hashcode should never change during the lifetime of an object. Because of
        // this we can't use getId() to calculate the hashcode. Unless you have sets
        // with lots of entities in them, returning the same hashcode should not be a
        // problem.
        return getClass().hashCode();
    }
}
*/
