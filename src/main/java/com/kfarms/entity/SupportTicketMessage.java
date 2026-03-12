package com.kfarms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "support_ticket_messages", indexes = {
        @Index(name = "idx_support_ticket_message_ticket", columnList = "ticket_id"),
        @Index(name = "idx_support_ticket_message_tenant", columnList = "tenant_id")
})
public class SupportTicketMessage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private SupportTicket ticket;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SupportMessageAuthorType authorType = SupportMessageAuthorType.USER;

    @Column(nullable = false, length = 120)
    private String authorName;

    @Column(nullable = false, length = 2500)
    private String body;
}
