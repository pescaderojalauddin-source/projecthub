package com.example.projecthub.controller;

import com.example.projecthub.dto.ProjectForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.ProjectStarRepository;
import com.example.projecthub.service.BurndownService;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.DeadlineCalendarService;
import com.example.projecthub.service.ProjectProgressService;
import com.example.projecthub.service.ProjectService;
import com.example.projecthub.service.TaskService;
import jakarta.validation.Valid;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * MVC-контроллер проектов: список, просмотр с задачами, формы создания/редактирования и
 * удаление. Рендерит Thymeleaf-шаблоны. RBAC-проверки выполняются в {@link ProjectService}.
 */
@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;
    private final ProjectProgressService progressService;
    private final ProjectStarRepository starRepository;
    private final DeadlineCalendarService calendarService;
    private final BurndownService burndownService;

    public ProjectController(ProjectService projectService,
                             TaskService taskService,
                             CurrentUserService currentUserService,
                             ProjectProgressService progressService,
                             ProjectStarRepository starRepository,
                             DeadlineCalendarService calendarService,
                             BurndownService burndownService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
        this.progressService = progressService;
        this.starRepository = starRepository;
        this.calendarService = calendarService;
        this.burndownService = burndownService;
    }

    /** Список проектов с поиском, сортировкой и пагинацией. USER видит свои, ADMIN — все. */
    @GetMapping
    public String list(@RequestParam(value = "search", required = false) String search,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
                       Model model) {
        User current = currentUserService.getCurrent();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Project> projects = projectService.listForUser(current, search, pageable);
        List<Long> ids = projects.getContent().stream().map(Project::getId).toList();
        model.addAttribute("projects", projects);
        model.addAttribute("progress", progressService.forProjects(ids));
        model.addAttribute("starredIds", ids.isEmpty()
                ? java.util.Set.<Long>of()
                : starRepository.findStarredProjectIds(current, ids));
        model.addAttribute("search", search);
        model.addAttribute("currentSort", sort);
        return "projects/list";
    }

    /** Форма создания нового проекта. */
    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ProjectForm());
        }
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("isNew", true);
        return "projects/form";
    }

    /** Создание проекта. При ошибках валидации возвращает форму с подсвеченными полями. */
    @PostMapping
    public String create(@Valid @ModelAttribute("form") ProjectForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", ProjectStatus.values());
            model.addAttribute("isNew", true);
            return "projects/form";
        }
        Project project = projectService.create(form, currentUserService.getCurrent());
        redirectAttributes.addFlashAttribute("flashSuccess", "Проект «" + project.getTitle() + "» создан.");
        return "redirect:/projects/" + project.getId();
    }

    /** Просмотр проекта со списком задач (фильтр по статусу, сортировка, пагинация). */
    @GetMapping("/{id}")
    public String view(@PathVariable Long id,
                       @RequestParam(value = "status", required = false) TaskStatus status,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       @RequestParam(value = "sort", defaultValue = "deadline,asc") String sort,
                       Model model) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(id, current);
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Task> tasks = taskService.listForProject(project, status, pageable);
        model.addAttribute("project", project);
        model.addAttribute("tasks", tasks);
        model.addAttribute("statusFilter", status);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("currentSort", sort);
        model.addAttribute("progress",
                progressService.forProjects(List.of(project.getId())).get(project.getId()));
        model.addAttribute("starred",
                starRepository.existsByUserAndProject(current, project));
        BurndownService.Series burn = burndownService.forProject(project);
        model.addAttribute("burnLabels", burn.labels());
        model.addAttribute("burnOpen",   burn.open());
        model.addAttribute("burnDone",   burn.done());
        return "projects/view";
    }

    /** Календарь дедлайнов проекта (CSS-grid). */
    @GetMapping("/{id}/calendar")
    public String calendar(@PathVariable Long id,
                           @RequestParam(value = "month", required = false) String month,
                           Model model) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(id, current);
        java.time.YearMonth ym;
        try {
            ym = (month != null && !month.isBlank())
                    ? java.time.YearMonth.parse(month)
                    : java.time.YearMonth.now();
        } catch (java.time.format.DateTimeParseException ex) {
            ym = java.time.YearMonth.now();
        }
        DeadlineCalendarService.Calendar cal = calendarService.build(project, ym);
        model.addAttribute("project", project);
        model.addAttribute("calendar", cal);
        return "projects/calendar";
    }

    /**
     * Канбан-доска проекта: задачи сгруппированы по {@link TaskStatus}, поддерживается
     * drag-drop через SortableJS, статус обновляется HTMX-запросом на
     * {@code POST /tasks/{id}/status}.
     */
    @GetMapping("/{id}/board")
    public String board(@PathVariable Long id, Model model) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(id, current);
        // Полный список без пагинации: канбан показывает всё одной страницей.
        List<Task> all = taskService.findAllForProject(project);

        Map<TaskStatus, List<Task>> byStatus = new EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s, new java.util.ArrayList<>());
        }
        for (Task t : all) {
            byStatus.get(t.getStatus()).add(t);
        }

        model.addAttribute("project", project);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("tasksByStatus", byStatus);
        return "projects/board";
    }

    /** Форма редактирования проекта. Доступна владельцу и ADMIN. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(id, current);
        if (!model.containsAttribute("form")) {
            ProjectForm form = new ProjectForm();
            form.setId(project.getId());
            form.setTitle(project.getTitle());
            form.setDescription(project.getDescription());
            form.setStatus(project.getStatus());
            form.setEmoji(project.getEmoji());
            model.addAttribute("form", form);
        }
        model.addAttribute("project", project);
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("isNew", false);
        return "projects/form";
    }

    /** Сохранение изменений проекта. */
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ProjectForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("project", projectService.getByIdForUser(id, currentUserService.getCurrent()));
            model.addAttribute("statuses", ProjectStatus.values());
            model.addAttribute("isNew", false);
            return "projects/form";
        }
        Project project = projectService.update(id, form, currentUserService.getCurrent());
        redirectAttributes.addFlashAttribute("flashSuccess", "Проект «" + project.getTitle() + "» сохранён.");
        return "redirect:/projects/" + project.getId();
    }

    /** Удаление проекта (вместе с задачами через ON DELETE CASCADE). */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        projectService.delete(id, currentUserService.getCurrent());
        redirectAttributes.addFlashAttribute("flashSuccess", "Проект удалён.");
        return "redirect:/projects";
    }

    /** Парсит строку вида {@code "property,direction"} в {@link Sort}. Дефолт — createdAt DESC. */
    private static Sort parseSort(String sortParam) {
        if (sortParam == null || sortParam.isBlank()) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        String[] parts = sortParam.split(",");
        String property = parts[0];
        Sort.Direction dir = parts.length > 1 && parts[1].equalsIgnoreCase("desc")
                ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(dir, property);
    }
}
