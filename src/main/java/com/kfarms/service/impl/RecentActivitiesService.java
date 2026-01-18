package com.kfarms.service.impl;


import com.kfarms.dto.RecentActivityDto;
import com.kfarms.repository.FeedRepository;
import com.kfarms.repository.SalesRepository;
import com.kfarms.repository.SuppliesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecentActivitiesService {

    @Autowired
    private SalesRepository salesRepo;

    @Autowired
    private SuppliesRepository suppliesRepo;

    @Autowired
    private FeedRepository feedRepo;

    public List<RecentActivityDto> getRecentActivities(int limit) {

        int perCategory = Math.max(1, limit / 3);
        Pageable pageable = PageRequest.of(0, perCategory);

        List<RecentActivityDto> sales = salesRepo
                .findAllByOrderByCreatedAtDesc(pageable)
                .stream()
                .map(s -> new RecentActivityDto(
                        "sale-" + s.getId(),
                        "Sale",
                        s.getItemName(),
                        s.getQuantity(),
                        "Completed",
                        s.getCreatedAt()
                ))
                .toList();

        List<RecentActivityDto> supplies = suppliesRepo
                .findAllByOrderByCreatedAtDesc(pageable)
                .stream()
                .map(s -> new RecentActivityDto(
                        "supply-" + s.getId(),
                        "Supply",
                        s.getItemName(),
                        s.getQuantity(),
                        "Completed",
                        s.getCreatedAt()
                ))
                .toList();

        List<RecentActivityDto> feeds = feedRepo
                .findAllByOrderByCreatedAtDesc(pageable)
                .stream()
                .map(f -> new RecentActivityDto(
                        "feed-" + f.getId(),
                        "Feed",
                        f.getFeedName(),
                        f.getQuantityUsed(),
                        "Completed",
                        f.getCreatedAt()
                ))
                .toList();

        List<RecentActivityDto> all = new ArrayList<>();
        all.addAll(sales);
        all.addAll(supplies);
        all.addAll(feeds);

        // Sort everything together
        all.sort((a, b) -> {
            if (a.getDate() == null && b.getDate() == null) return 0;
            if (a.getDate() == null) return 1;  // nulls go last
            if (b.getDate() == null) return -1;
            return b.getDate().compareTo(a.getDate());
        });


        return all.stream().limit(limit).toList();
    }
}
