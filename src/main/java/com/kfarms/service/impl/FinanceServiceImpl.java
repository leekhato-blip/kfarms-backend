package com.kfarms.service.impl;

import com.kfarms.repository.SalesRepository;
import com.kfarms.repository.SuppliesRepository;
import com.kfarms.service.FinanceService;
import com.kfarms.tenant.service.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;


@Service
@RequiredArgsConstructor
public class FinanceServiceImpl implements FinanceService {

    private final SalesRepository salesRepo;
    private final SuppliesRepository suppliesRepo;

    @Override
    public Map<String, Object> getMonthlyFinance() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new IllegalStateException("Missing tenant context");
        }
        int year = LocalDate.now().getYear();
        List<Object[]> revenueRaw = salesRepo.getMonthlyRevenue(tenantId, year);
        List<Object[]> expenseRaw = suppliesRepo.getMonthlyExpenses(tenantId, year);

        Map<String, BigDecimal> revenueMap = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseMap = new LinkedHashMap<>();

        // Convert revenue rows
        revenueRaw.forEach(r -> {
            String month = (String) r[0];
            BigDecimal amount = (BigDecimal) r[1];
            revenueMap.put(month, amount);
        });

        // Convert expenses rows
        expenseRaw.forEach(e -> {
            String month = (String) e[0];
            BigDecimal amount = (BigDecimal) e[1];
            expenseMap.put(month, amount);
        });

        // Combine all months into a sorted set
        Set<String> months = new TreeSet<>();
        months.addAll(revenueMap.keySet());
        months.addAll(expenseMap.keySet());

        List<Map<String, Object>> monthly = new ArrayList<>();

        for (String month : months) {
            Map<String, Object> entry = new HashMap<>();
            entry.put("month", month);
            entry.put("revenue", revenueMap.getOrDefault(month, BigDecimal.ZERO));
            entry.put("expense", expenseMap.getOrDefault(month, BigDecimal.ZERO));
            monthly.add(entry);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("monthly", monthly);

        return response;
    }
}
