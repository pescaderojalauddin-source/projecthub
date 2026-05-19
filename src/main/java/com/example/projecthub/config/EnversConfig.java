package com.example.projecthub.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.envers.repository.support.EnversRevisionRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

// подключает Envers через Spring Data Envers
// заменяет деф JpaRepositoryFactoryBean на EnversRevisionRepositoryFactoryBean
@Configuration
@EnableJpaRepositories(
        basePackages = "com.example.projecthub.repository",
        repositoryFactoryBeanClass = EnversRevisionRepositoryFactoryBean.class)
public class EnversConfig {
}
