package com.prography11thbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "cohort_members")
class CohortMember(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member = Member(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_id", nullable = false)
    val cohort: Cohort = Cohort(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "part_id")
    var part: Part? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id")
    var team: Team? = null,

    @Column(nullable = false)
    var deposit: Int = 100000,

    @Column(nullable = false)
    var excuseCount: Int = 0,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
