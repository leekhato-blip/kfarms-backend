package com.kfarms.tenant.service;

import com.kfarms.tenant.entity.Tenant;
import com.kfarms.tenant.entity.TenantAuditAction;
import com.kfarms.tenant.entity.TenantAuditLog;
import com.kfarms.tenant.repository.TenantAuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TenantAuditLogService {

    private final TenantAuditLogRepository tenantAuditLogRepository;

    public TenantAuditLog record(
            Tenant tenant,
            String actor,
            TenantAuditAction action,
            String subjectType,
            Long subjectId,
            String targetName,
            String targetEmail,
            String previousValue,
            String nextValue,
            String description
    ) {
        TenantAuditLog log = new TenantAuditLog();
        log.setTenant(tenant);
        log.setAction(action);
        log.setSubjectType(subjectType);
        log.setSubjectId(subjectId);
        log.setTargetName(blankToNull(targetName));
        log.setTargetEmail(blankToNull(targetEmail));
        log.setPreviousValue(blankToNull(previousValue));
        log.setNextValue(blankToNull(nextValue));
        log.setDescription(description == null || description.isBlank() ? action.name() : description.trim());

        String actorValue = blankToNull(actor);
        if (actorValue != null) {
            log.setCreatedBy(actorValue);
            log.setUpdatedBy(actorValue);
        }

        return tenantAuditLogRepository.save(log);
    }

    public Page<TenantAuditLog> search(Long tenantId, String search, String action, Pageable pageable) {
        Specification<TenantAuditLog> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("tenant").get("id"), tenantId));
            predicates.add(cb.isFalse(root.get("deleted")));

            String actionFilter = normalize(action);
            if (!actionFilter.isBlank()) {
                try {
                    predicates.add(cb.equal(root.get("action"), TenantAuditAction.valueOf(actionFilter)));
                } catch (IllegalArgumentException ignored) {
                    predicates.add(cb.disjunction());
                }
            }

            String searchFilter = normalize(search).toLowerCase();
            if (!searchFilter.isBlank()) {
                String like = "%" + searchFilter + "%";
                predicates.add(
                        cb.or(
                                cb.like(cb.lower(root.get("createdBy")), like),
                                cb.like(cb.lower(root.get("targetName")), like),
                                cb.like(cb.lower(root.get("targetEmail")), like),
                                cb.like(cb.lower(root.get("description")), like),
                                cb.like(cb.lower(root.get("previousValue")), like),
                                cb.like(cb.lower(root.get("nextValue")), like)
                        )
                );
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return tenantAuditLogRepository.findAll(specification, pageable);
    }

    private String blankToNull(String value) {
        String normalized = normalize(value);
        return normalized.isBlank() ? null : normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
