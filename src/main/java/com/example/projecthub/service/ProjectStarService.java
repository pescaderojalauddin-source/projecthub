package com.example.projecthub.service;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStar;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.ProjectStarRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// «звёздочки» избранных проектов
// toggle + выборки для дашборда/списка
@Service
public class ProjectStarService {

    private final ProjectStarRepository starRepository;
    private final ProjectService projectService;

    public ProjectStarService(ProjectStarRepository starRepository,
                              ProjectService projectService) {
        this.starRepository = starRepository;
        this.projectService = projectService;
    }

    // переключает «избранное» для (user, project). Возвращает новое состояние
    @Transactional
    public boolean toggle(Long projectId, User user) {
        Project project = projectService.getByIdForUser(projectId, user);
        if (starRepository.existsByUserAndProject(user, project)) {
            starRepository.deleteByUserAndProject(user, project);
            return false;
        }
        starRepository.save(new ProjectStar(user, project));
        return true;
    }

    // список избранных проектов в порядке добавления (новые сверху)
    @Transactional(readOnly = true)
    public List<Project> listFavourites(User user) {
        return starRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(ProjectStar::getProject)
                .toList();
    }

    @Transactional(readOnly = true)
    public long countFavourites(User user) {
        return starRepository.countByUser(user);
    }
}
