package com.kfarms.tenant.service;

import com.kfarms.tenant.repository.TenantRepository;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class TenantFilterTest {

    @Test
    void skipsTenantResolutionForApiPlatformRoutes() throws Exception {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        TenantFilter filter = new TenantFilter(tenantRepository);
        FilterChain filterChain = mock(FilterChain.class);

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/platform/tenants/2");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tenantRepository);
    }
}
