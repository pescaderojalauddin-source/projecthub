package com.example.projecthub.controller.api;

import com.example.projecthub.dto.api.ChangeRoleRequest;
import com.example.projecthub.dto.api.UserDto;
import com.example.projecthub.service.StatisticsService;
import com.example.projecthub.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// rEST API админ-раздела. Все эндпоинты доступны только ADMIN
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Админ-операции: пользователи, статистика")
public class AdminRestController {

    private final UserService userService;
    private final StatisticsService statisticsService;

    public AdminRestController(UserService userService, StatisticsService statisticsService) {
        this.userService = userService;
        this.statisticsService = statisticsService;
    }

    // постраничный список юзеров с поиском по логину
    @GetMapping("/users")
    @Operation(summary = "Список пользователей")
    public Page<UserDto> users(@RequestParam(value = "search", required = false) String search,
                               @RequestParam(value = "page", defaultValue = "0") int page,
                               @RequestParam(value = "size", defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "login"));
        return userService.search(search, pageable).map(UserDto::of);
    }

    // смена роли юзера
    @PutMapping("/users/{id}/role")
    @Operation(summary = "Сменить роль пользователя")
    public UserDto changeRole(@PathVariable Long id, @Valid @RequestBody ChangeRoleRequest request) {
        return UserDto.of(userService.changeRole(id, request.role()));
    }

    // сводная статистика
    @GetMapping("/stats")
    @Operation(summary = "Сводная статистика")
    public Map<String, Object> stats() {
        return statisticsService.snapshot();
    }
}
