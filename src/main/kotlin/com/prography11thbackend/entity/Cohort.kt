package com.prography11thbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "cohorts")
class Cohort(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val generation: Int = 0,

    @Column(nullable = false)
    val name: String = "",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
