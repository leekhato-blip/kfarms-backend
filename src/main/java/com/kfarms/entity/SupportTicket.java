package com.kfarms.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kfarms.tenant.entity.Tenant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "support_tickets", indexes = {
        @Index(name = "idx_support_ticket_tenant", columnList = "tenant_id"),
        @Index(name = "idx_support_ticket_code", columnList = "ticket_code", unique = true)
})
public class SupportTicket extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_code", nullable = false, unique = true, length = 40)
    private String ticketCode;

    @Column(nullable = false, length = 140)
    private String subject;

    @Column(nullable = false, length = 120)
    private String category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SupportTicketPriority priority = SupportTicketPriority.MEDIUM;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SupportTicketStatus status = SupportTicketStatus.OPEN;

    @Column(nullable = false, length = 2500)
    private String description;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Tenant tenant;

    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    @ToString.Exclude
    @JsonIgnore
    private List<SupportTicketMessage> messages = new ArrayList<>();
}
