package com.example.projecthub.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.projecthub.entity.AchievementUnlocked;
import com.example.projecthub.entity.Role;
import com.example.projecthub.entity.TaskStatus;
import com.example.projecthub.entity.User;
import com.example.projecthub.repository.AchievementUnlockedRepository;
import com.example.projecthub.repository.ProjectStarRepository;
import com.example.projecthub.repository.TaskRepository;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AchievementServiceTest {

    @Mock TaskRepository taskRepository;
    @Mock ProjectStarRepository starRepository;
    @Mock AchievementUnlockedRepository unlockedRepository;

    @InjectMocks
    AchievementService svc;

    User user;

    @BeforeEach
    void setUp() {
        user = new User("ivan", "h", Role.USER);
        user.setId(1L);
    }

    private void stubCounters(long done, long inProgress, long blocked, long todo, long stars) {
    // мок countByAssigneeGroupByStatus — возвращает строки [status, count]
        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        if (done > 0) rows.add(new Object[]{TaskStatus.DONE, done});
        if (inProgress > 0) rows.add(new Object[]{TaskStatus.IN_PROGRESS, inProgress});
        if (blocked > 0) rows.add(new Object[]{TaskStatus.BLOCKED, blocked});
        if (todo > 0) rows.add(new Object[]{TaskStatus.TODO, todo});
        when(taskRepository.countByAssigneeGroupByStatus(user)).thenReturn(rows);
        when(starRepository.countByUser(user)).thenReturn(stars);
    }

    @Test
    void catalogHasEightAchievements() {
        assertThat(svc.catalog()).hasSize(8);
    }

    @Test
    void noTasksMeansNoUnlocks() {
        stubCounters(0, 0, 0, 0, 0);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of());

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).isEmpty();
        assertThat(snap.unlockedCodes()).isEmpty();
        verify(unlockedRepository, never()).save(any());
    }

    @Test
    void firstDoneUnlocksFirstDoneAchievement() {
        stubCounters(1, 0, 0, 0, 0);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of());

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).extracting("code").contains("FIRST_DONE");
        verify(unlockedRepository).save(any(AchievementUnlocked.class));
    }

    @Test
    void tenDoneAlsoUnlocksFirstAndTen() {
        stubCounters(10, 0, 0, 0, 0);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of());

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).extracting("code")
                .contains("FIRST_DONE", "TEN_DONE");
    }

    @Test
    void alreadyUnlockedCodeIsNotSavedAgain() {
        stubCounters(1, 0, 0, 0, 0);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of("FIRST_DONE"));

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).isEmpty();
        assertThat(snap.unlockedCodes()).contains("FIRST_DONE");
        verify(unlockedRepository, never()).save(any());
    }

    @Test
    void busyBeeUnlocksAtFiveInProgress() {
        stubCounters(0, 5, 0, 0, 0);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of());

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).extracting("code").contains("BUSY_BEE");
    }

    @Test
    void noBlockersRequiresAtLeastFiveTotal() {
    // 4 задач всего, без BLOCKED — но порог 5, поэтому не разблокируется
        stubCounters(2, 2, 0, 0, 0);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of());

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).extracting("code").doesNotContain("NO_BLOCKERS");
    }

    @Test
    void noBlockersUnlocksWhenZeroBlockedAndAtLeastFive() {
        stubCounters(3, 2, 0, 0, 0);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of());

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).extracting("code").contains("NO_BLOCKERS");
    }

    @Test
    void starsAchievementsScaleWithStarCount() {
        stubCounters(0, 0, 0, 0, 5);
        when(unlockedRepository.findCodesByUser(user)).thenReturn(Set.of());

        AchievementService.ProgressSnapshot snap = svc.evaluate(user);

        assertThat(snap.newlyUnlocked()).extracting("code")
                .contains("FIRST_STAR", "FIVE_STARS");
        verify(unlockedRepository, times(2)).save(any());
    }
}
