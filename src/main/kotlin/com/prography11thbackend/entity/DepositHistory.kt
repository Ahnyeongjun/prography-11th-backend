package com.prography11thbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "deposit_histories")
class DepositHistory(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cohort_member_id", nullable = false)
    val cohortMember: CohortMember = CohortMember(),

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val type: DepositType = DepositType.INITIAL,

    @Column(nullable = false)
    val amount: Int = 0,

    @Column(nullable = false)
    val balanceAfter: Int = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendance_id")
    val attendance: Attendance? = null,

    val description: String? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now()
)
