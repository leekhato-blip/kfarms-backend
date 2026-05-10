package com.kfarms.platform.service;

import com.kfarms.platform.dto.TenantAdminDetailsDto;
import com.kfarms.platform.dto.TenantAdminListItemDto;
import com.kfarms.tenant.entity.TenantPlan;
import com.kfarms.tenant.entity.TenantStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PlatformTenantService {

    Page<TenantAdminListItemDto> searchTenants(
            String search,          // Name, slug, email
            TenantStatus status,
            TenantPlan plan,
            Pageable pageable
    );

    TenantAdminDetailsDto getTenantDetails(Long tenantId);

    void updateTenantPlan(Long tenantId, TenantPlan plan);

    void updateTenantStatus(Long tenantId, TenantStatus status);
}
