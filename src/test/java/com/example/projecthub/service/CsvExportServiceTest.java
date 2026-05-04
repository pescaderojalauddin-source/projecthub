package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.projecthub.entity.Project;
import com.example.projecthub.entity.ProjectStatus;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.Task;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class CsvExportServiceTest {

    @Test
    void exportProjectTasks_buildsValidCsvWithBomAndEscaping() throws Exception {
        CsvExportService service = new CsvExportService();
        User owner = new User("ivan", "x", Role.USER);
        owner.setId(1L);
        Project project = new Project("Project; with semi", "d", ProjectStatus.ACTIVE, owner);
        project.setId(10L);
        Task t = new Task("Title; with \"quote\"", "d", TaskStatus.IN_PROGRESS,
                LocalDate.of(2026, 5, 1), project, owner);
        t.setId(101L);

        byte[] bytes = service.exportProjectTasks(project, List.of(t)).get();
        String csv = new String(bytes, StandardCharsets.UTF_8);

        // BOM
        assertThat(csv.charAt(0)).isEqualTo('\uFEFF');
        // Header
        assertThat(csv).contains("id;title;status;deadline;assignee;created_at\r\n");
        // Quoted/escaped title with "" doubled
        assertThat(csv).contains("\"Title; with \"\"quote\"\"\"");
        assertThat(csv).contains("IN_PROGRESS");
        assertThat(csv).contains("01.05.2026");
        assertThat(csv).contains("ivan");
    }

    @Test
    void exportProjectTasks_emptyListProducesHeaderOnly() throws Exception {
        CsvExportService service = new CsvExportService();
        User u = new User("u", "x", Role.USER);
        Project p = new Project("p", "d", ProjectStatus.ACTIVE, u);
        p.setId(1L);
        byte[] bytes = service.exportProjectTasks(p, List.of()).get();
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertThat(csv).isEqualTo("\uFEFFid;title;status;deadline;assignee;created_at\r\n");
    }
}
