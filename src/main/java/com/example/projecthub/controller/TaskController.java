package com.example.projecthub.controller;

import com.example.projecthub.dto.CommentForm;
import com.example.projecthub.dto.TaskForm;
import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskPriority;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.service.CommentService;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectService;
import com.example.projecthub.service.TaskAttachmentService;
import com.example.projecthub.service.TaskService;
import com.example.projecthub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.history.Revision;
import org.springframework.data.history.Revisions;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

// mVC-контр задач и комментариев: просмотр задачи, смена статуса, CRUD формы,
@Controller
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;
    private final CommentService commentService;
    private final UserService userService;
    private final CurrentUserService currentUserService;
    private final TaskAttachmentService attachmentService;

    public TaskController(TaskService taskService,
                          ProjectService projectService,
                          CommentService commentService,
                          UserService userService,
                          CurrentUserService currentUserService,
                          TaskAttachmentService attachmentService) {
        this.taskService = taskService;
        this.projectService = projectService;
        this.commentService = commentService;
        this.userService = userService;
        this.currentUserService = currentUserService;
        this.attachmentService = attachmentService;
    }

    // форма создания новой задачи в проекте
    @GetMapping("/projects/{projectId}/tasks/new")
    public String newForm(@PathVariable Long projectId, Model model) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(projectId, current);
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new TaskForm());
        }
        model.addAttribute("project", project);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("isNew", true);
        return "tasks/form";
    }

    // создание задачи
    @PostMapping("/projects/{projectId}/tasks")
    public String create(@PathVariable Long projectId,
                         @Valid @ModelAttribute("form") TaskForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        User current = currentUserService.getCurrent();
        Project project = projectService.getByIdForUser(projectId, current);
        if (bindingResult.hasErrors()) {
            model.addAttribute("project", project);
            model.addAttribute("statuses", TaskStatus.values());
            model.addAttribute("priorities", TaskPriority.values());
            model.addAttribute("users", userService.findAll());
            model.addAttribute("isNew", true);
            return "tasks/form";
        }
        Task task = taskService.create(project, form, current);
        redirectAttributes.addFlashAttribute("flashSuccess", "Задача создана.");
        return "redirect:/tasks/" + task.getId();
    }

    // просмотр задачи с комментариями и формой добавления комментария
    @GetMapping("/tasks/{id}")
    public String view(@PathVariable Long id, Model model) {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(id, current);
        model.addAttribute("task", task);
        model.addAttribute("comments", commentService.listByTask(task));
        if (!model.containsAttribute("commentForm")) {
            model.addAttribute("commentForm", new CommentForm());
        }
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("attachments", attachmentService.listForTask(task, current));
    // cSV-список логинов для @-меншен автокомплита
        String logins = userService.findAll().stream()
                .map(User::getLogin)
                .collect(java.util.stream.Collectors.joining(","));
        model.addAttribute("mentionLogins", logins);
        return "tasks/view";
    }

    // форма редактирования задачи
    @GetMapping("/tasks/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(id, current);
        if (!model.containsAttribute("form")) {
            TaskForm form = new TaskForm();
            form.setId(task.getId());
            form.setTitle(task.getTitle());
            form.setDescription(task.getDescription());
            form.setStatus(task.getStatus());
            form.setDeadline(task.getDeadline());
            form.setAssigneeId(task.getAssignee() != null ? task.getAssignee().getId() : null);
            form.setPriority(task.getPriority() != null ? task.getPriority() : TaskPriority.MEDIUM);
            form.setTagsCsv(String.join(", ", task.getTags()));
            model.addAttribute("form", form);
        }
        model.addAttribute("project", task.getProject());
        model.addAttribute("task", task);
        model.addAttribute("statuses", TaskStatus.values());
        model.addAttribute("priorities", TaskPriority.values());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("isNew", false);
        return "tasks/form";
    }

    // сохранение изменений задачи
    @PostMapping("/tasks/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") TaskForm form,
                         BindingResult bindingResult,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        User current = currentUserService.getCurrent();
        if (bindingResult.hasErrors()) {
            Task task = taskService.getByIdForUser(id, current);
            model.addAttribute("project", task.getProject());
            model.addAttribute("task", task);
            model.addAttribute("statuses", TaskStatus.values());
            model.addAttribute("priorities", TaskPriority.values());
            model.addAttribute("users", userService.findAll());
            model.addAttribute("isNew", false);
            return "tasks/form";
        }
        Task task = taskService.update(id, form, current);
        redirectAttributes.addFlashAttribute("flashSuccess", "Задача сохранена.");
        if (task.getStatus() == TaskStatus.DONE) {
            redirectAttributes.addFlashAttribute("flashConfetti", Boolean.TRUE);
        }
        return "redirect:/tasks/" + task.getId();
    }

    // быстрая смена статуса задачи
    @PostMapping("/tasks/{id}/status")
    public Object changeStatus(@PathVariable Long id,
                               @RequestParam("status") TaskStatus newStatus,
                               @RequestHeader(value = "HX-Request", required = false) String hxRequest,
                               RedirectAttributes redirectAttributes) {
        Task task = taskService.changeStatus(id, newStatus, currentUserService.getCurrent());
        if (hxRequest != null) {
            return ResponseEntity.noContent().build();
        }
        redirectAttributes.addFlashAttribute("flashSuccess", "Статус задачи: " + newStatus.getLabel() + ".");
        if (newStatus == TaskStatus.DONE) {
            redirectAttributes.addFlashAttribute("flashConfetti", Boolean.TRUE);
        }
        return "redirect:/tasks/" + task.getId();
    }

    // история изменений задачи через Hibernate Envers
    @GetMapping("/tasks/{id}/history")
    public String history(@PathVariable Long id, Model model) {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(id, current);
        Revisions<Integer, Task> revisions = taskService.findRevisionsForUser(id, current);
    // sortировка в Envers — ascending по revision-номеру. На UI удобнее desc
        java.util.List<Revision<Integer, Task>> ordered = new java.util.ArrayList<>(revisions.getContent());
        java.util.Collections.reverse(ordered);
        model.addAttribute("task", task);
        model.addAttribute("revisions", ordered);
        return "tasks/history";
    }

    // удаление задачи (вместе с комментариями через ON DELETE CASCADE)
    @PostMapping("/tasks/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(id, current);
        Long projectId = task.getProject().getId();
        taskService.delete(id, current);
        redirectAttributes.addFlashAttribute("flashSuccess", "Задача удалена.");
        return "redirect:/projects/" + projectId;
    }

    // добавление комментария к задаче
    @PostMapping("/tasks/{id}/comments")
    public String addComment(@PathVariable Long id,
                             @Valid @ModelAttribute("commentForm") CommentForm form,
                             BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        User current = currentUserService.getCurrent();
        Task task = taskService.getByIdForUser(id, current);
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("commentError",
                    bindingResult.getAllErrors().get(0).getDefaultMessage());
            return "redirect:/tasks/" + id;
        }
        commentService.add(task, form, current);
        redirectAttributes.addFlashAttribute("flashSuccess", "Комментарий добавлен.");
        return "redirect:/tasks/" + id;
    }

    // удаление комментария. Разрешено автору или ADMIN
    @PostMapping("/tasks/{taskId}/comments/{commentId}/delete")
    public String deleteComment(@PathVariable Long taskId,
                                @PathVariable Long commentId,
                                RedirectAttributes redirectAttributes) {
        commentService.delete(commentId, currentUserService.getCurrent());
        redirectAttributes.addFlashAttribute("flashSuccess", "Комментарий удалён.");
        return "redirect:/tasks/" + taskId;
    }
}
