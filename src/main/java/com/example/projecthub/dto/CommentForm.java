package com.example.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// форма добавления комментария к задаче
public class CommentForm {

    @NotBlank(message = "Комментарий не может быть пустым")
    @Size(max = 4000, message = "Комментарий не должен превышать 4000 символов")
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
