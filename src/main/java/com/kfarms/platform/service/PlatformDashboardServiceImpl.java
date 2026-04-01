package com.kfarms.platform.service;

import com.kfarms.entity.Role;
import com.kfarms.platform.dto.PlatformDashboardOverviewDto;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.entity.TenantStatus;
import com.kfarms.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public PlatformDashboardOverviewDto getOverview() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long totalUsers = appUserRepository.count();
        long platformAdmins = appUserRepository.countByRole(Role.PLATFORM_ADMIN);

        return PlatformDashboardOverviewDto.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .totalUsers(totalUsers)
                .platformAdmins(platformAdmins)
                .build();
    }
}
