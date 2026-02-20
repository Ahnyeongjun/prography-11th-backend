package com.prography11thbackend.entity

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "members")
class Member(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(unique = true, nullable = false)
    val loginId: String = "",

    @Column(nullable = false)
    val password: String = "",

    @Column(nullable = false)
    var name: String = "",

    @Column(nullable = false)
    var phone: String = "",

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: MemberStatus = MemberStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val role: MemberRole = MemberRole.MEMBER,

    @Column(nullable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = Instant.now()
)
