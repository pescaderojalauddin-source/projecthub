package com.example.projecthub.web;

import org.springframework.stereotype.Component;

/**
 * Утилита для аватарок-инициалов: по логину выбирает детерминированный цвет и инициал.
 * <p>Используется как Spring-бин из Thymeleaf-шаблонов:
 * <pre>${@avatars.color('ivan')} / ${@avatars.initial('ivan')}</pre>
 */
@Component("avatars")
public class AvatarUtils {

    /** Палитра цветов фона — подобраны достаточно тёмные, чтобы белый текст читался. */
    private static final String[] PALETTE = {
            "#e57373", "#f06292", "#ba68c8", "#9575cd", "#7986cb",
            "#64b5f6", "#4fc3f7", "#4dd0e1", "#4db6ac", "#81c784",
            "#aed581", "#dce775", "#ffb74d", "#ff8a65", "#a1887f",
            "#90a4ae", "#5c6bc0", "#26a69a", "#ec407a", "#ab47bc"
    };

    /** Цвет фона аватарки для данного логина (стабильный, не зависит от запуска). */
    public String color(String login) {
        if (login == null || login.isEmpty()) {
            return "#9e9e9e";
        }
        int hash = 0;
        for (int i = 0; i < login.length(); i++) {
            hash = 31 * hash + login.charAt(i);
        }
        int idx = Math.floorMod(hash, PALETTE.length);
        return PALETTE[idx];
    }

    /** Инициал: первая буква логина в верхнем регистре. */
    public String initial(String login) {
        if (login == null || login.isEmpty()) {
            return "?";
        }
        return String.valueOf(Character.toUpperCase(login.charAt(0)));
    }
}
