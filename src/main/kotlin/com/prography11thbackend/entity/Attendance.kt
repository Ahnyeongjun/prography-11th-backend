package com.prography11thbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "attendances")
class Attendance(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: MeetingSession = MeetingSession(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    val member: Member = Member(),

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qr_code_id")
    val qrCode: QrCode? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: AttendanceStatus = AttendanceStatus.PRESENT,

    var lateMinutes: Int? = null,

    @Column(nullable = false)
    var penaltyAmount: Int = 0,

    var reason: String? = null,

    val checkedInAt: Instant? = null,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
