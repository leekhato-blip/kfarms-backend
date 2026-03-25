package com.kfarms.platform.service;

import com.kfarms.entity.BillingSubscriptionStatus;
import com.kfarms.entity.Role;
import com.kfarms.platform.dto.PlatformAppPortfolioDto;
import com.kfarms.platform.dto.PlatformAppSummaryDto;
import com.kfarms.platform.dto.PlatformDashboardOverviewDto;
import com.kfarms.repository.AppUserRepository;
import com.kfarms.repository.BillingSubscriptionRepository;
import com.kfarms.tenant.entity.TenantStatus;
import com.kfarms.tenant.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformDashboardServiceImpl implements PlatformDashboardService {

    private static final String DEFAULT_REVENUE_CURRENCY = "NGN";

    private static final List<PlatformAppDescriptor> APP_CATALOG = List.of(
            new PlatformAppDescriptor(
                    "kfarms",
                    "KFarms",
                    "Agribusiness Operations",
                    "LIVE",
                    "Farm operations for poultry, fish, and mixed agribusiness teams.",
                    "Manage workspaces, operators, billing posture, and operational visibility for live farm businesses.",
                    "/platform/tenants",
                    "/dashboard",
                    List.of("Tenant workspaces", "Poultry and fish modules", "Billing and operator controls")
            ),
            new PlatformAppDescriptor(
                    "property-rent-management",
                    "Property / Rent Management",
                    "Property Operations",
                    "PLANNED",
                    "Rental operations, occupancy, and landlord workflows for managed properties.",
                    "Working-title app for lease tracking, rent collection, maintenance coordination, and portfolio visibility.",
                    "/platform/apps",
                    null,
                    List.of("Unit inventory", "Rent and lease tracking", "Maintenance workflows")
            ),
            new PlatformAppDescriptor(
                    "school-management-system",
                    "School Management System",
                    "Education Operations",
                    "PLANNED",
                    "School operations for academics, administration, and parent-facing coordination.",
                    "Working-title app for student records, attendance, finance, staff operations, and campus reporting.",
                    "/platform/apps",
                    null,
                    List.of("Admissions and records", "Attendance and grading", "School finance operations")
            ),
            new PlatformAppDescriptor(
                    "clinic-hospital-management",
                    "Clinic / Hospital Management",
                    "Healthcare Operations",
                    "PLANNED",
                    "Clinical operations, patient journeys, and facility workflows for care teams.",
                    "Working-title app for patient intake, appointments, ward operations, billing, and treatment visibility.",
                    "/platform/apps",
                    null,
                    List.of("Patient records", "Appointments and admissions", "Billing and care operations")
            )
    );

    private final TenantRepository tenantRepository;
    private final AppUserRepository appUserRepository;
    private final BillingSubscriptionRepository billingSubscriptionRepository;

    @Override
    public PlatformDashboardOverviewDto getOverview() {
        PlatformAppPortfolioDto portfolio = getAppPortfolio();
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long totalUsers = appUserRepository.count();
        long platformAdmins = appUserRepository.countByRole(Role.PLATFORM_ADMIN);

        return PlatformDashboardOverviewDto.builder()
                .totalApps(portfolio.getTotalApps())
                .liveApps(portfolio.getLiveApps())
                .plannedApps(portfolio.getPlannedApps())
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .totalUsers(totalUsers)
                .platformAdmins(platformAdmins)
                .build();
    }

    @Override
    public PlatformAppPortfolioDto getAppPortfolio() {
        long totalTenants = tenantRepository.count();
        long activeTenants = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long suspendedTenants = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long totalUsers = appUserRepository.count();
        BigDecimal totalRevenue = billingSubscriptionRepository.sumActiveRecurringRevenue();
        String revenueCurrency = billingSubscriptionRepository
                .findFirstByStatusOrderByIdAsc(BillingSubscriptionStatus.ACTIVE)
                .map(subscription -> subscription.getCurrency())
                .filter((currency) -> currency != null && !currency.isBlank())
                .orElse(DEFAULT_REVENUE_CURRENCY);

        List<PlatformAppSummaryDto> apps = APP_CATALOG.stream()
                .map((descriptor) -> mapToAppSummary(
                        descriptor,
                        totalTenants,
                        activeTenants,
                        suspendedTenants,
                        totalUsers,
                        totalRevenue,
                        revenueCurrency
                ))
                .toList();

        long liveApps = apps.stream()
                .filter((app) -> "LIVE".equalsIgnoreCase(app.getLifecycle()))
                .count();

        return PlatformAppPortfolioDto.builder()
                .totalApps(apps.size())
                .liveApps(liveApps)
                .plannedApps(Math.max(apps.size() - liveApps, 0))
                .totalWorkspaces(totalTenants)
                .totalOperators(totalUsers)
                .apps(apps)
                .build();
    }

    private PlatformAppSummaryDto mapToAppSummary(
            PlatformAppDescriptor descriptor,
            long totalTenants,
            long activeTenants,
            long suspendedTenants,
            long totalUsers,
            BigDecimal totalRevenue,
            String revenueCurrency
    ) {
        boolean live = "LIVE".equalsIgnoreCase(descriptor.lifecycle());

        return PlatformAppSummaryDto.builder()
                .id(descriptor.id())
                .name(descriptor.name())
                .category(descriptor.category())
                .lifecycle(descriptor.lifecycle())
                .headline(descriptor.headline())
                .description(descriptor.description())
                .consolePath(descriptor.consolePath())
                .workspacePath(descriptor.workspacePath())
                .tenantCount(live ? totalTenants : 0)
                .activeTenantCount(live ? activeTenants : 0)
                .suspendedTenantCount(live ? suspendedTenants : 0)
                .operatorCount(live ? totalUsers : 0)
                .revenueGenerated(live ? totalRevenue : BigDecimal.ZERO)
                .revenueCurrency(revenueCurrency)
                .capabilities(descriptor.capabilities())
                .build();
    }

    private record PlatformAppDescriptor(
            String id,
            String name,
            String category,
            String lifecycle,
            String headline,
            String description,
            String consolePath,
            String workspacePath,
            List<String> capabilities
    ) {
    }
}
