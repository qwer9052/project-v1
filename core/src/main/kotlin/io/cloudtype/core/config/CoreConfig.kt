package io.cloudtype.core.config

import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(basePackages = ["io.cloudtype.core.repository"])
@EntityScan(basePackages = ["io.cloudtype.core.model"])
class CoreConfig