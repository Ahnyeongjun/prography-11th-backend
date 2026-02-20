package com.prography11thbackend.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

@Configuration
class AppConfig {

    @Bean
    fun passwordEncoder(): BCryptPasswordEncoder = BCryptPasswordEncoder(12)
}
