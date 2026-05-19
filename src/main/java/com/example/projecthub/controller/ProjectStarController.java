package com.example.projecthub.controller;

import com.example.projecthub.entity.User;
import com.example.projecthub.service.CurrentUserService;
import com.example.projecthub.service.ProjectStarService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

// тоггл ☆ избранного проекта через HTMX
// POST /projects/{id}/star перерисовывает иконку звезды
@Controller
public class ProjectStarController {

    private static final String HX_REQUEST_HEADER = "HX-Request";

    private final ProjectStarService starService;
    private final CurrentUserService currentUserService;

    public ProjectStarController(ProjectStarService starService,
                                 CurrentUserService currentUserService) {
        this.starService = starService;
        this.currentUserService = currentUserService;
    }

    // тоггл звезды на проекте
    @PostMapping("/projects/{id}/star")
    public Object toggle(@PathVariable Long id,
                         HttpServletRequest request,
                         Model model) {
        User me = currentUserService.getCurrent();
        boolean starred = starService.toggle(id, me);

        if (request.getHeader(HX_REQUEST_HEADER) != null) {
            model.addAttribute("projectId", id);
            model.addAttribute("starred", starred);
            return "fragments/star :: button";
        }
    // обычная POST-форма — редиректим назад
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/projects/" + id);
    }

    // rEST-ответ без рендера (на случай fetch без HTMX)
    @PostMapping("/api/v1/projects/{id}/star")
    public ResponseEntity<Boolean> apiToggle(@PathVariable Long id) {
        User me = currentUserService.getCurrent();
        boolean starred = starService.toggle(id, me);
        return ResponseEntity.status(HttpStatus.OK).body(starred);
    }
}
