package com.example.projecthub.repository;

import com.example.projecthub.entity.AchievementUnlocked;
import com.example.projecthub.entity.AchievementUnlocked.AchievementUnlockedId;
import com.example.projecthub.entity.User;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AchievementUnlockedRepository extends JpaRepository<AchievementUnlocked, AchievementUnlockedId> {

    List<AchievementUnlocked> findByUserOrderByUnlockedAtDesc(User user);

    @Query("select a.id.code from AchievementUnlocked a where a.user = :user")
    Set<String> findCodesByUser(@Param("user") User user);
}
