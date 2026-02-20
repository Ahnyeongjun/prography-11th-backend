package com.prography11thbackend.entity

import jakarta.persistence.*

@Entity
@Table(name = "teams")
class Team(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false)
    val name: String = "",

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    val cohort: Cohort = Cohort()
)
