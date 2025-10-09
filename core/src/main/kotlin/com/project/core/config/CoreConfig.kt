package com.project.core.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["com.project.core.repository"])
@EntityScan(basePackages = ["com.project.core.model"])
class CoreConfig