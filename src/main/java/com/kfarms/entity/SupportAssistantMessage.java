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
@Table(name = "support_assistant_messages", indexes = {
        @Index(name = "idx_support_assistant_message_tenant", columnList = "tenant_id")
})
public class SupportAssistantMessage extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    @ToString.Exclude
    @JsonIgnore
    private Tenant tenant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SupportAssistantRole role = SupportAssistantRole.ASSISTANT;

    @Column(nullable = false, length = 3000)
    private String content;
}
