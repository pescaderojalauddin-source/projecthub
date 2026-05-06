package com.example.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Форма регистрации нового пользователя. */
public class RegistrationForm {

    @NotBlank(message = "Логин обязателен")
    @Size(min = 3, max = 64, message = "Логин: от 3 до 64 символов")
    @Pattern(regexp = "^[a-zA-Z0-9_.-]+$", message = "Логин: только латиница, цифры, _ . -")
    private String login;

    @NotBlank(message = "Пароль обязателен")
    @Size(min = 6, max = 100, message = "Пароль: от 6 до 100 символов")
    private String password;

    @NotBlank(message = "Повтор пароля обязателен")
    private String passwordConfirm;

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPasswordConfirm() {
        return passwordConfirm;
    }

    public void setPasswordConfirm(String passwordConfirm) {
        this.passwordConfirm = passwordConfirm;
    }
}
