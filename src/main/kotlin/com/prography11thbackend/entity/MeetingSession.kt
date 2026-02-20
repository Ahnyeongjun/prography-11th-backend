package com.prography11thbackend.entity

import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

@Entity
@Table(name = "sessions")
class MeetingSession(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    val cohort: Cohort = Cohort(),

    @Column(nullable = false)
    var title: String = "",

    @Column(nullable = false)
    var date: LocalDate = LocalDate.now(),

    @Column(nullable = false)
    var time: LocalTime = LocalTime.now(),

    @Column(nullable = false)
    var location: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: SessionStatus = SessionStatus.SCHEDULED,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
