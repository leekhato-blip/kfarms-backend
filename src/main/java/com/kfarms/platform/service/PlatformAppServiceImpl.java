package com.kfarms.platform.service;

import com.kfarms.platform.dto.PlatformAppPortfolioDto;
import com.kfarms.platform.dto.PlatformAppSummaryDto;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.tenant.entity.TenantStatus;
import com.kfarms.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformAppServiceImpl implements PlatformAppService {

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;

    @Override
    public PlatformAppPortfolioDto getPortfolio() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long totalUsers = appUserRepository.count();

        PlatformAppSummaryDto kfarms = PlatformAppSummaryDto.builder()
                .id("kfarms")
                .name("KFarms")
                .category("Agribusiness Operations")
                .lifecycle("LIVE")
                .headline("Farm operating system for poultry, fish, and mixed agribusiness teams.")
                .description("Farm operations for poultry, fish, and mixed agribusiness teams.")
                .consolePath("/app/kfarms/dashboard")
                .workspacePath("/app/kfarms/dashboard")
                .tenantCount(totalTenants)
                .activeTenantCount(activeTenants)
                .suspendedTenantCount(suspendedTenants)
                .operatorCount(totalUsers)
                .revenueGenerated(BigDecimal.ZERO)
                .revenueCurrency("NGN")
                .sortOrder(10)
                .capabilities(List.of(
                        "Tenant workspaces",
                        "Poultry and fish modules",
                        "Billing and operator controls"
                ))
                .source("catalog")
                .build();

        return PlatformAppPortfolioDto.builder()
                .totalApps(1)
                .liveApps(1)
                .plannedApps(0)
                .totalWorkspaces(totalTenants)
                .totalOperators(totalUsers)
                .apps(List.of(kfarms))
                .build();
    }
}
