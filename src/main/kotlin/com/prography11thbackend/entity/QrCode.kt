package com.prography11thbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "qr_codes")
class QrCode(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    val session: MeetingSession = MeetingSession(),

    @Column(nullable = false)
    val hashValue: String = "",

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var expiresAt: Instant = Instant.now()
)
