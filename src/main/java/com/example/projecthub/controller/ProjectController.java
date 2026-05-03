package com.example.projecthub.controller;

import com.example.projecthub.dto.ProjectForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectService;
import com.example.projecthub.service.TaskService;
import com.example.projecthub.service.TimerService;
import jakarta.validation.Valid;
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

@Controller
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final TaskService taskService;
    private final CurrentUserService currentUserService;
    private final TimerService timerService;

    public ProjectController(ProjectService projectService,
                             TaskService taskService,
                             CurrentUserService currentUserService,
                             TimerService timerService) {
        this.projectService = projectService;
        this.taskService = taskService;
        this.currentUserService = currentUserService;
        this.timerService = timerService;
    }

    @GetMapping
    public String list(@RequestParam(value = "search", required = false) String search,
                       @RequestParam(value = "page", defaultValue = "0") int page,
                       @RequestParam(value = "size", defaultValue = "10") int size,
                       @RequestParam(value = "sort", defaultValue = "createdAt,desc") String sort,
                       Model model) {
        User current = currentUserService.getCurrent();
        Pageable pageable = PageRequest.of(page, size, parseSort(sort));
        Page<Project> projects = projectService.listForUser(current, search, pageable);
        model.addAttribute("projects", projects);
        model.addAttribute("search", search);
        model.addAttribute("currentSort", sort);
        return "projects/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ProjectForm());
        }
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("isNew", true);
        return "projects/form";
    }

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
        model.addAttribute("timerTotals", timerService.totalsByProject(project));
        model.addAttribute("timerService", timerService);
        return "projects/view";
    }

    @GetMapping("/{id}/board")
    public String board(@PathVariable Long id, Model model) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(id, current);
        java.util.List<Task> all = taskService.listAllForProject(project);
        java.util.Map<TaskStatus, java.util.List<Task>> grouped = new java.util.EnumMap<>(TaskStatus.class);
        for (TaskStatus s : TaskStatus.values()) {
            grouped.put(s, new java.util.ArrayList<>());
        }
        for (Task t : all) {
            grouped.get(t.getStatus()).add(t);
        }
        model.addAttribute("project", project);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("grouped", grouped);
        return "projects/board";
    }

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
            model.addAttribute("form", form);
        }
        model.addAttribute("project", project);
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("isNew", false);
        return "projects/form";
    }

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

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        projectService.delete(id, currentUserService.getCurrent());
        redirectAttributes.addFlashAttribute("flashSuccess", "Проект удалён.");
        return "redirect:/projects";
    }

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
