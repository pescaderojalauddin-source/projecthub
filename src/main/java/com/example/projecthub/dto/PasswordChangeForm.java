package com.example.projecthub.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Форма смены пароля текущего пользователя. */
public class PasswordChangeForm {

    @NotBlank(message = "Введите текущий пароль")
    private String currentPassword;

    @NotBlank(message = "Введите новый пароль")
    @Size(min = 6, max = 100, message = "Пароль: от 6 до 100 символов")
    private String newPassword;

    @NotBlank(message = "Повторите новый пароль")
    private String newPasswordConfirm;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getNewPasswordConfirm() {
        return newPasswordConfirm;
    }

    public void setNewPasswordConfirm(String newPasswordConfirm) {
        this.newPasswordConfirm = newPasswordConfirm;
    }
}
